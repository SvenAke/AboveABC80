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

    val columns = 80
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
                    detectTapGestures {
                        com.aboveware.aboveabc80.keyboard.Keyboard.instance.requestTerminalFocus()
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val effectiveHeight = Terminal.DEFAULT_HEIGHT

                // 1. Draw Flashing Cursor
                val cx = terminal.cursorX
                val cy = terminal.cursorY
                val cursorVisible = terminal.cursorVisible

                if (cx < columns && cursorVisible) {
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

                // 2. Iterate and draw characters
                for (y in 0 until Terminal.DEFAULT_HEIGHT) {
                    val rowData = terminal.screen[y]
                    drawRow(
                        rowData,
                        y,
                        columns,
                        terminal,
                        blinkVisible,
                        terminalColor,
                        screenReverse,
                        terminalBackgroundColor
                    )
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
    screenReverse: Boolean,
    terminalBackgroundColor: Color
) {
    val effectiveHeight = Terminal.DEFAULT_HEIGHT
    val yBaseNormal = (y * size.height) / effectiveHeight
    val yBaseNext = ((y + 1) * size.height) / effectiveHeight
    val cellHeight = yBaseNext - yBaseNormal

    // Run-length background drawing
    var runStartX = 0f
    var lastBgColor: Color? = null
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

        val currentX = (x * size.width) / columns

        if (cellBgColor != lastBgColor) {
            if (lastBgColor != null && lastBgColor != terminalBackgroundColor) {
                drawRect(
                    color = lastBgColor,
                    topLeft = Offset(runStartX, yBaseNormal),
                    size = Size(currentX - runStartX + 0.5f, cellHeight + 0.5f)
                )
            }
            runStartX = currentX
            lastBgColor = cellBgColor
        }
    }

    // Draw characters
    for (x in 0 until minOf(actualColumns, columns)) {
        val cell = rowData[x]
        val char = cell.char
        val attr = cell.attr
        val isActuallyInverted = attr.inverse xor screenReverse

        if (char == ' ' && !attr.underline && (x != terminal.cursorX || y != terminal.cursorY || y < 0 || y >= Terminal.DEFAULT_HEIGHT)) continue
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

        // Centering
        val startX = xBase + (cellWidth - gWidth * dotWidth) / 2f
        val startY = yBaseNormal + (cellHeight - gHeight * dotHeight) / 2f

        for (gy in 0 until gHeight) {
            val bits = currentData[gy]
            val drawY = startY + gy * dotHeight
            for (gx in 0 until minOf(bits.size, gWidth)) {
                if (bits[gx]) {
                    val stretchedWidth = dotWidth * terminal.dotStretch

                    drawRect(
                        color = drawColor,
                        topLeft = Offset(startX + gx * dotWidth, drawY),
                        size = Size(stretchedWidth, dotHeight + 0.1f)
                    )
                    if (attr.bold) {
                        drawRect(
                            color = drawColor,
                            topLeft = Offset(startX + gx * dotWidth + (dotWidth * 0.5f), drawY),
                            size = Size(stretchedWidth, dotHeight + 0.1f)
                        )
                    }
                }
            }
        }

        if (attr.underline) {
            drawRect(
                color = drawColor,
                topLeft = Offset(xBase, yBaseNormal + cellHeight - dotHeight),
                size = Size(cellWidth, dotHeight)
            )
        }
    }
}
