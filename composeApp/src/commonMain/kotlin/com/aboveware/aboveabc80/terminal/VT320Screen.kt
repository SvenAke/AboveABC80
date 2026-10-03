package com.aboveware.aboveabc80.terminal

import com.aboveware.aboveabc80.ZXLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun VT320.drawChar(c: Char) {
    if (activeStatusLine) {
        statusLineManager.writeFromHost(cursorX, c, currentAttr)
        if (cursorX < columns - 1) cursorX++
        return
    }

    if (cursorX >= columns) {
        if (autoWrap) {
            if (printerStatus == PrinterStatus.AUTO) printCurrentLine()
            cursorX = 0
            lineFeed()
        } else {
            cursorX = columns - 1
        }
    }

    val newScreen = screen.copyOf()
    val newRow = newScreen[cursorY].copyOf()

    if (insertMode) {
        for (x in columns - 1 downTo cursorX + 1) {
            newRow[x] = newRow[x - 1]
        }
    }

    val glyph = graphics.getGlyph(c, this)
    val finalChar = glyph?.char ?: c
    newRow[cursorX] = TerminalCell(finalChar, currentAttr)
    newScreen[cursorY] = newRow
    screen = newScreen

    // Trace character drawing if it's the prompt
    if (finalChar == '*') {
        ZXLog.terminal("VT320: DRAWING '*' at ($cursorX, $cursorY) with attr=$currentAttr")
    }

    if (autoWrap || cursorX < columns - 1) {
        cursorX++
    }
}

internal fun VT320.lineFeed() {
    if (activeStatusLine) return
    if (printerStatus == PrinterStatus.AUTO) printCurrentLine()
    if (cursorY == bottomMargin) {
        scrollUp()
    } else if (cursorY < rows - 1) {
        cursorY++
    }
}

internal fun VT320.reverseIndex() {
    if (activeStatusLine) return
    if (cursorY == topMargin) {
        scrollDown()
    } else if (cursorY > 0) {
        cursorY--
    }
}

internal fun VT320.scrollUp() {
    if (activeStatusLine) return
    val newScreen = screen.copyOf()
    scrolledOutLine = newScreen[topMargin].copyOf()

    for (y in topMargin until bottomMargin) {
        newScreen[y] = newScreen[y + 1]
    }
    newScreen[bottomMargin] = Array(columns) { TerminalCell() }
    screen = newScreen

    if (smoothScroll) {
        scrollJob?.cancel()
        scrollOffset = 1.0f
        scrollJob = terminalScope.launch {
            val step = 0.15f // Speed: ~9 lines per second
            while (scrollOffset > 0f) {
                delay(16.milliseconds)
                scrollOffset = (scrollOffset - step).coerceAtLeast(0f)
            }
            scrolledOutLine = null
        }
    }
}

internal fun VT320.scrollDown() {
    if (activeStatusLine) return
    val newScreen = screen.copyOf()
    scrolledOutLine = newScreen[bottomMargin].copyOf()

    for (y in bottomMargin downTo topMargin + 1) {
        newScreen[y] = newScreen[y - 1]
    }
    newScreen[topMargin] = Array(columns) { TerminalCell() }
    screen = newScreen

    if (smoothScroll) {
        scrollJob?.cancel()
        scrollOffset = -1.0f
        scrollJob = terminalScope.launch {
            val step = 0.15f
            while (scrollOffset < 0f) {
                delay(16.milliseconds)
                scrollOffset = (scrollOffset + step).coerceAtMost(0f)
            }
            scrolledOutLine = null
        }
    }
}

internal fun VT320.eraseInDisplayInternal(mode: Int, selective: Boolean = false) {
    val newScreen = screen.copyOf()

    // Helper to erase a range of rows
    fun eraseRows(start: Int, end: Int) {
        for (y in start..end) {
            val row = newScreen[y].copyOf()
            for (x in 0 until columns) {
                if (!selective || !row[x].attr.selectiveErase) {
                    row[x] = TerminalCell()
                }
            }
            newScreen[y] = row
        }
    }

    // Helper to erase a part of a line
    fun eraseLinePart(y: Int, startX: Int, endX: Int) {
        val row = newScreen[y].copyOf()
        for (x in startX..endX) {
            if (!selective || !row[x].attr.selectiveErase) {
                row[x] = TerminalCell()
            }
        }
        newScreen[y] = row
    }

    when (mode) {
        0 -> { // From cursor to end
            eraseLinePart(cursorY, cursorX, columns - 1)
            if (cursorY < rows - 1) eraseRows(cursorY + 1, rows - 1)
        }

        1 -> { // From start to cursor
            if (cursorY > 0) eraseRows(0, cursorY - 1)
            eraseLinePart(cursorY, 0, cursorX)
        }

        2 -> { // Entire display
            eraseRows(0, rows - 1)
        }
    }
    screen = newScreen
}

internal fun VT320.eraseInLineInternal(mode: Int, selective: Boolean = false) {
    val newScreen = screen.copyOf()
    val newRow = newScreen[cursorY].copyOf()
    when (mode) {
        0 -> for (x in cursorX until columns) {
            if (!selective || !newRow[x].attr.selectiveErase) newRow[x] = TerminalCell()
        }

        1 -> for (x in 0..cursorX) {
            if (!selective || !newRow[x].attr.selectiveErase) newRow[x] = TerminalCell()
        }

        2 -> for (x in 0 until columns) {
            if (!selective || !newRow[x].attr.selectiveErase) newRow[x] = TerminalCell()
        }
    }
    newScreen[cursorY] = newRow
    screen = newScreen
}

internal fun VT320.insertLine(count: Int) {
    if (cursorY !in topMargin..bottomMargin) return
    val newScreen = screen.copyOf()
    val scrollCount = count.coerceAtMost(bottomMargin - cursorY + 1)
    for (y in bottomMargin downTo cursorY + scrollCount) {
        newScreen[y] = newScreen[y - scrollCount]
    }
    for (y in cursorY until cursorY + scrollCount) {
        newScreen[y] = Array(columns) { TerminalCell() }
    }
    screen = newScreen
}

internal fun VT320.deleteLine(count: Int) {
    if (cursorY !in topMargin..bottomMargin) return
    val newScreen = screen.copyOf()
    val scrollCount = count.coerceAtMost(bottomMargin - cursorY + 1)
    for (y in cursorY until bottomMargin - scrollCount + 1) {
        newScreen[y] = newScreen[y + scrollCount]
    }
    for (y in bottomMargin - scrollCount + 1..bottomMargin) {
        newScreen[y] = Array(columns) { TerminalCell() }
    }
    screen = newScreen
}

internal fun VT320.deleteChar(count: Int) {
    val newScreen = screen.copyOf()
    val newRow = newScreen[cursorY].copyOf()
    val moveCount = (columns - cursorX - count).coerceAtLeast(0)
    for (x in 0 until moveCount) {
        newRow[cursorX + x] = newRow[cursorX + x + count]
    }
    for (x in cursorX + moveCount until columns) {
        newRow[x] = TerminalCell()
    }
    newScreen[cursorY] = newRow
    screen = newScreen
}

internal fun VT320.insertChar(count: Int) {
    if (activeStatusLine) return
    val newScreen = screen.copyOf()
    val newRow = newScreen[cursorY].copyOf()
    val scrollCount = count.coerceAtMost(columns - cursorX)
    for (x in columns - 1 downTo cursorX + scrollCount) {
        newRow[x] = newRow[x - scrollCount]
    }
    for (x in cursorX until cursorX + scrollCount) {
        newRow[x] = TerminalCell()
    }
    newScreen[cursorY] = newRow
    screen = newScreen
}

internal fun VT320.eraseChar(count: Int) {
    if (activeStatusLine) return
    val newScreen = screen.copyOf()
    val newRow = newScreen[cursorY].copyOf()
    val eraseCount = count.coerceAtMost(columns - cursorX)
    for (x in cursorX until cursorX + eraseCount) {
        newRow[x] = TerminalCell()
    }
    newScreen[cursorY] = newRow
    screen = newScreen
}

internal fun VT320.clearScreenInternal() {
    screen = Array(rows) { Array(columns) { TerminalCell() } }
    statusLineManager.clearHostContent()
    cursorX = 0
    cursorY = 0
}

internal fun VT320.decaln() {
    // DEC Screen Alignment Test: fill screen with 'E'
    // Reset margins and home cursor
    topMargin = 0
    bottomMargin = rows - 1
    cursorX = 0
    cursorY = 0

    val attr = TerminalAttributes()
    for (y in 0 until rows) {
        for (x in 0 until columns) {
            screen[y][x] = TerminalCell('E', attr)
        }
    }
}
