package com.aboveware.aboveabc80.terminal

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.aboveware.aboveabc80.core.BIOS
import com.aboveware.aboveabc80.printer.VirtualPrinter

@Composable
fun TerminalView(
    modifier: Modifier = Modifier,
    terminal: Terminal = TerminalManager.activeTerminal
) {
    val infiniteTransition = rememberInfiniteTransition()
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        )
    )

    val terminalColor = TerminalManager.terminalColor.color

    val blinkVisible by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(333, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    val columns = if (terminal is VT320) terminal.columns else 80
    val targetRatio = 4f / 3f

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val screenRatio = maxWidth.value / maxHeight.value
        val terminalModifier = if (screenRatio > targetRatio) {
            Modifier.fillMaxHeight().aspectRatio(targetRatio)
        } else {
            Modifier.fillMaxWidth().aspectRatio(targetRatio)
        }

        val screenReverse = terminal.screenReverse
        val terminalBackgroundColor =
            if (screenReverse) terminalColor.copy(alpha = 0.8f) else Color.Black

        Box(
            modifier = terminalModifier
                .background(terminalBackgroundColor)
                .pointerInput(terminal) {
                    val swipeThreshold = 48.dp.toPx()
                    var accumulatedDrag = 0f
                    var didNavigate = false

                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            if (!didNavigate) {
                                accumulatedDrag += dragAmount
                                if (accumulatedDrag <= -swipeThreshold ||
                                    accumulatedDrag >= swipeThreshold
                                ) {
                                    val direction = if (accumulatedDrag < 0f) -1 else 1
                                    if (!terminal.isSetupVisible &&
                                        !BIOS.instance.isTransientProgramRunning &&
                                        TerminalManager.commandHistory.navigate(direction) {
                                            terminal.onKeyEvent(it)
                                            terminal.triggerClick()
                                        }
                                    ) {
                                        didNavigate = true
                                    }
                                }
                            }
                            change.consume()
                        },
                        onDragEnd = {
                            accumulatedDrag = 0f
                            didNavigate = false
                        },
                        onDragCancel = {
                            accumulatedDrag = 0f
                            didNavigate = false
                        }
                    )
                }
                .pointerInput(terminal) {
                    detectTapGestures { offset ->
                        // Request focus when terminal is tapped
                        com.aboveware.aboveabc80.keyboard.Keyboard.instance.requestTerminalFocus()

                        if (terminal is VT320 && terminal.statusLineManager.isVisible()) {
                            val statusVisible = terminal.statusLineManager.isVisible()
                            val effectiveHeight =
                                if (statusVisible) Terminal.DEFAULT_HEIGHT + 1 else Terminal.DEFAULT_HEIGHT
                            val statusLineYStart =
                                (Terminal.DEFAULT_HEIGHT.toFloat() / effectiveHeight) * size.height

                            if (offset.y >= statusLineYStart) {
                                // Clicked on status line. Check if printer status was clicked.
                                val clickColumn = (offset.x / size.width) * columns

                                val printerStatus =
                                    if (terminal.setup.isVisible) terminal.setupPrinterStatus else terminal.printerStatus
                                val printerText = "Printer: ${printerStatus.text}"
                                val startCol = (columns - printerText.length) / 2
                                val endCol = startCol + printerText.length

                                if (clickColumn >= startCol && clickColumn <= endCol) {
                                    VirtualPrinter.instance.isVisible =
                                        !VirtualPrinter.instance.isVisible
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val statusVisible =
                    if (terminal is VT320) terminal.statusLineManager.isVisible() else false
                val effectiveHeight =
                    if (statusVisible) Terminal.DEFAULT_HEIGHT + 1 else Terminal.DEFAULT_HEIGHT

                // 1. Draw Flashing Cursor
                val cx = terminal.cursorX
                val cy =
                    if (terminal is VT320 && terminal.activeStatusLine) Terminal.DEFAULT_HEIGHT else terminal.cursorY
                val setupVisible = if (terminal is VT320) terminal.setup.isVisible else false
                val cursorVisible = terminal.cursorVisible

                // Allow cursor in setup if it's explicitly placed on the status line
                val showCursorInSetup = terminal is VT320 && terminal.activeStatusLine

                if (cx < columns && (!setupVisible || showCursorInSetup) && cursorVisible) {
                    val xBase = (cx * size.width) / columns
                    val xNext = ((cx + 1) * size.width) / columns
                    val yBase = (cy * size.height) / effectiveHeight
                    val yNext = ((cy + 1) * size.height) / effectiveHeight

                    val cursorColor = if (screenReverse) Color.Black else terminalColor
                    val cursorRect = if (terminal.cursorStyle == CursorStyle.BLOCK) {
                        Size(xNext - xBase, yNext - yBase)
                    } else {
                        Size(
                            xNext - xBase,
                            (yNext - yBase) * 0.2f
                        ) // Underline is 20% of cell height
                    }
                    val cursorTop = if (terminal.cursorStyle == CursorStyle.BLOCK) {
                        yBase
                    } else {
                        yNext - (yNext - yBase) * 0.2f
                    }

                    drawRect(
                        color = cursorColor.copy(alpha = cursorAlpha * 0.7f),
                        topLeft = Offset(xBase, cursorTop),
                        size = cursorRect
                    )
                }

                // 2. Iterate and draw characters using the Glyph system
                val topClip =
                    if (terminal is VT320) (terminal.topMargin * size.height) / effectiveHeight else 0f
                val bottomClip =
                    if (terminal is VT320) ((terminal.bottomMargin + 1) * size.height) / effectiveHeight else (Terminal.DEFAULT_HEIGHT * size.height) / effectiveHeight
                val scrollOffsetPx =
                    if (terminal is VT320) (terminal.scrollOffset * size.height) / effectiveHeight else 0f

                for (y in 0 until Terminal.DEFAULT_HEIGHT) {
                    val rowData = terminal.screen[y]
                    val inScrollRegion =
                        terminal is VT320 && y in terminal.topMargin..terminal.bottomMargin

                    if (inScrollRegion) {
                        drawContext.canvas.save()
                        drawContext.canvas.clipRect(
                            androidx.compose.ui.geometry.Rect(
                                0f,
                                topClip,
                                size.width,
                                bottomClip
                            )
                        )
                        drawRow(
                            rowData,
                            y,
                            columns,
                            terminal,
                            blinkVisible,
                            terminalColor,
                            scrollOffsetPx,
                            topClip,
                            bottomClip,
                            true,
                            screenReverse,
                            terminalBackgroundColor
                        )
                        drawContext.canvas.restore()
                    } else {
                        drawRow(
                            rowData,
                            y,
                            columns,
                            terminal,
                            blinkVisible,
                            terminalColor,
                            0f,
                            0f,
                            size.height,
                            false,
                            screenReverse,
                            terminalBackgroundColor
                        )
                    }
                }

                // Draw the line that is currently being scrolled out
                if (terminal is VT320 && terminal.scrollOffset != 0f) {
                    terminal.scrolledOutLine?.let { rowData ->
                        val yOut =
                            if (terminal.scrollOffset > 0) terminal.topMargin - 1 else terminal.bottomMargin + 1
                        drawContext.canvas.save()
                        drawContext.canvas.clipRect(
                            androidx.compose.ui.geometry.Rect(
                                0f,
                                topClip,
                                size.width,
                                bottomClip
                            )
                        )
                        drawRow(
                            rowData,
                            yOut,
                            columns,
                            terminal,
                            blinkVisible,
                            terminalColor,
                            scrollOffsetPx,
                            topClip,
                            bottomClip,
                            true,
                            screenReverse,
                            terminalBackgroundColor
                        )
                        drawContext.canvas.restore()
                    }
                }

                // 2.5 Draw Status Line if available
                if (terminal is VT320 && statusVisible) {
                    val y = Terminal.DEFAULT_HEIGHT
                    val yBase = (y * size.height) / (Terminal.DEFAULT_HEIGHT + 1)
                    val cellHeight = size.height - yBase

                    // Run-length background drawing for status line
                    var runStartX = 0f
                    var lastBgColor: Color? = null
                    val statusRowData = terminal.statusLine
                    val actualStatusColumns = statusRowData.size

                    for (x in 0..actualStatusColumns) {
                        val cellBgColor = if (x < actualStatusColumns) {
                            val cell = statusRowData[x]
                            val isActuallyInverted = cell.attr.inverse xor screenReverse
                            if (isActuallyInverted) {
                                if (cell.attr.bold) terminalColor else terminalColor.copy(alpha = 0.8f)
                            } else {
                                if (screenReverse) Color.Black else terminalBackgroundColor
                            }
                        } else null // Sentinel to flush last run

                        val currentX = (x * size.width) / columns

                        if (cellBgColor != lastBgColor) {
                            if (lastBgColor != null && lastBgColor != terminalBackgroundColor) {
                                drawRect(
                                    color = lastBgColor,
                                    topLeft = Offset(runStartX, yBase),
                                    size = Size(currentX - runStartX + 0.5f, cellHeight + 0.5f)
                                )
                            }
                            runStartX = currentX
                            lastBgColor = cellBgColor
                        }
                    }

                    // Draw characters
                    for (x in 0 until minOf(actualStatusColumns, columns)) {
                        val cell = statusRowData[x]
                        val char = cell.char
                        val attr = cell.attr
                        val isActuallyInverted = attr.inverse xor screenReverse

                        if (char == ' ') continue
                        if (attr.blink && blinkVisible < 0.5f) continue

                        val glyph = terminal.getGlyph(char) ?: continue
                        val xBase = (x * size.width) / columns
                        val xNext = ((x + 1) * size.width) / columns
                        val cellWidth = xNext - xBase

                        val drawColor = if (isActuallyInverted) Color.Black else {
                            when {
                                attr.dim -> terminalColor.copy(alpha = 0.4f)
                                attr.bold -> terminalColor
                                else -> terminalColor.copy(alpha = 0.8f)
                            }
                        }
                        val currentData = glyph.data80
                        val gWidth = glyph.glyphWidth80
                        val gHeight = glyph.height

                        val nominalWidth = terminal.nominalWidth
                        val nominalHeight = terminal.nominalHeight

                        val dotWidth = cellWidth / nominalWidth
                        val dotHeight = cellHeight / nominalHeight

                        // Centering (Text font behavior)
                        val startX = xBase + (cellWidth - gWidth * dotWidth) / 2f
                        val startY = yBase + (cellHeight - gHeight * dotHeight) / 2f

                        for (gy in 0 until gHeight) {
                            val bits = currentData[gy]
                            for (gx in 0 until minOf(bits.size, gWidth)) {
                                if (bits[gx]) {
                                    val stretchedWidth = dotWidth * terminal.dotStretch
                                    drawRect(
                                        color = drawColor,
                                        topLeft = Offset(
                                            startX + gx * dotWidth,
                                            startY + gy * dotHeight
                                        ),
                                        size = Size(stretchedWidth, dotHeight + 0.1f)
                                    )
                                    if (attr.bold) {
                                        drawRect(
                                            color = drawColor,
                                            topLeft = Offset(
                                                startX + gx * dotWidth + (dotWidth * 0.5f),
                                                startY + gy * dotHeight
                                            ),
                                            size = Size(stretchedWidth, dotHeight + 0.1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. CRT Scan lines Effect
                val scanlineHeight = 2f
                for (i in 0 until size.height.toInt() step (scanlineHeight * 2).toInt()) {
                    drawRect(
                        color = Color.Black.copy(alpha = TerminalManager.scanlineIntensity),
                        topLeft = Offset(0f, i.toFloat()),
                        size = Size(size.width, scanlineHeight)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRow(
    rowData: Array<TerminalCell>,
    y: Int,
    columns: Int,
    terminal: Terminal,
    blinkVisible: Float,
    terminalColor: Color,
    scrollOffsetPx: Float,
    topClip: Float,
    bottomClip: Float,
    isOffsetApplied: Boolean,
    screenReverse: Boolean,
    terminalBackgroundColor: Color
) {
    val lineAttr = if (terminal is VT320) {
        if (y in 0 until Terminal.DEFAULT_HEIGHT) terminal.lineAttributes[y] else VT320.LineAttribute.NORMAL
    } else VT320.LineAttribute.NORMAL

    val isDoubleWidth = lineAttr != VT320.LineAttribute.NORMAL
    val lineColumns = if (isDoubleWidth) columns / 2 else columns

    // Use higher precision for base coordinates to avoid cumulative rounding errors
    val effectiveHeight =
        if (terminal is VT320 && terminal.statusLineManager.isVisible()) Terminal.DEFAULT_HEIGHT + 1 else Terminal.DEFAULT_HEIGHT
    val yBaseNormal = (y * size.height) / effectiveHeight
    val yBaseNext = ((y + 1) * size.height) / effectiveHeight
    val cellHeight = yBaseNext - yBaseNormal
    val yBaseOffset = if (isOffsetApplied) yBaseNormal + scrollOffsetPx else yBaseNormal

    // Skip drawing if completely outside the clipping area (optimization)
    if (yBaseOffset + cellHeight < topClip || yBaseOffset > bottomClip) return

    // Run-length background drawing
    var runStartX = 0f
    var lastBgColor: Color? = null

    // Defensive check: use the actual size of the data provided
    val actualColumns = rowData.size

    for (x in 0..actualColumns) {
        val cellBgColor = if (x < actualColumns) {
            val cell = rowData[x]
            val isActuallyInverted = cell.attr.inverse xor screenReverse
            if (isActuallyInverted) {
                if (cell.attr.bold) terminalColor else terminalColor.copy(alpha = 0.8f)
            } else {
                if (screenReverse) Color.Black else terminalBackgroundColor
            }
        } else null // Sentinel

        val currentX = (x * size.width) / lineColumns

        if (cellBgColor != lastBgColor) {
            if (lastBgColor != null && lastBgColor != terminalBackgroundColor) {
                drawRect(
                    color = lastBgColor,
                    topLeft = Offset(runStartX, yBaseOffset),
                    size = Size(currentX - runStartX + 0.5f, cellHeight + 0.5f)
                )
            }
            runStartX = currentX
            lastBgColor = cellBgColor
        }
    }

    // Draw characters
    for (x in 0 until minOf(actualColumns, lineColumns)) {
        val cell = rowData[x]
        val char = cell.char
        val attr = cell.attr
        val isActuallyInverted = attr.inverse xor screenReverse

        if (char == ' ' && !attr.underline && (x != terminal.cursorX || y != terminal.cursorY || y < 0 || y >= Terminal.DEFAULT_HEIGHT)) continue
        if (attr.blink && blinkVisible < 0.5f) continue

        val glyph = terminal.getGlyph(char) ?: continue

        val xBase = (x * size.width) / lineColumns
        val xNext = ((x + 1) * size.width) / lineColumns
        val cellWidth = xNext - xBase

        val drawColor = if (isActuallyInverted) Color.Black else {
            when {
                attr.dim -> terminalColor.copy(alpha = 0.4f)
                attr.bold -> terminalColor
                else -> terminalColor.copy(alpha = 0.8f)
            }
        }

        val currentData = glyph.data80
        val gWidth = glyph.glyphWidth80
        val gHeight = glyph.height

        val nominalWidth = terminal.nominalWidth
        val nominalHeight = terminal.nominalHeight

        val isDoubleHeight =
            lineAttr == VT320.LineAttribute.DOUBLE_HEIGHT_TOP || lineAttr == VT320.LineAttribute.DOUBLE_HEIGHT_BOTTOM
        val dotWidth = cellWidth / nominalWidth
        val dotHeight =
            if (isDoubleHeight) (cellHeight / nominalHeight) * 2 else cellHeight / nominalHeight

        // Centering
        val startX = xBase + (cellWidth - gWidth * dotWidth) / 2f
        val startY =
            if (isDoubleHeight) yBaseOffset else yBaseOffset + (cellHeight - gHeight * dotHeight) / 2f

        val gyStart = if (lineAttr == VT320.LineAttribute.DOUBLE_HEIGHT_BOTTOM) gHeight / 2 else 0
        val gyEnd = if (lineAttr == VT320.LineAttribute.DOUBLE_HEIGHT_TOP) gHeight / 2 else gHeight

        for (gy in gyStart until gyEnd) {
            val bits = currentData[gy]
            val drawY = startY + (gy - gyStart) * dotHeight
            for (gx in 0 until minOf(bits.size, gWidth)) {
                if (bits[gx]) {
                    // Dot Stretching: Every lit pixel is stretched.
                    // This bridges gaps between adjacent pixels and adds weight.
                    val stretchedWidth = dotWidth * terminal.dotStretch

                    drawRect(
                        color = drawColor,
                        topLeft = Offset(startX + gx * dotWidth, drawY),
                        size = Size(stretchedWidth, dotHeight + 0.1f)
                    )
                    if (attr.bold) {
                        // For bold, we draw again with a slight offset to increase thickness further
                        drawRect(
                            color = drawColor,
                            topLeft = Offset(startX + gx * dotWidth + (dotWidth * 0.5f), drawY),
                            size = Size(stretchedWidth, dotHeight + 0.1f)
                        )
                    }
                }
            }
        }

        if (attr.underline && lineAttr != VT320.LineAttribute.DOUBLE_HEIGHT_TOP) {
            drawRect(
                color = drawColor,
                topLeft = Offset(xBase, yBaseOffset + cellHeight - dotHeight),
                size = Size(cellWidth, dotHeight)
            )
        }
    }
}
