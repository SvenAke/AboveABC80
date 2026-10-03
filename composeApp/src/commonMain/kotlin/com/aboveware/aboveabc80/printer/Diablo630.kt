package com.aboveware.aboveabc80.printer

import com.aboveware.aboveabc80.playBell

class Diablo630 : Printer("Diablo 630") {
    companion object {
        val instance = Diablo630()
    }

    private var escState = 0
    private var isGraphicsMode = false
    private var lineSpacingPlotInch = 1.0f / 48.0f
    private var charSpacingPlotInch = 1.0f / 60.0f

    override val testData: List<String>
        get() {
            val esc = "\u001B"
            val cr = "\r"
            return listOf(
                "DIABLO 630: LETTER QUALITY DAISY WHEEL$cr",
                "---------------------------------------$cr",
                "NORMAL BLACK TEXT$cr",
                "${esc}BRED RIBBON TEXT ENABLED (ESC B)$cr",
                "${esc}ABACK TO BLACK RIBBON (ESC A)$cr",
                "${esc}EBOLD TEXT ENABLED${esc}F$cr",
                "${esc}XUNDERLINED TEXT ENABLED${esc}Y$cr",
                "TABS:\t1\t2\t3$cr",
                "${esc}2TABS CLEARED (NEXT TAB SHOULD FAIL)$cr",
                "TAB TEST: |\t|$cr",
                "12345678901234567890$cr",
                "${esc}1SET TAB AT CURRENT POS (COL 1)$cr",
                "TAB TEST: \tRE-ADDED TAB$cr",
                "${cr}DIABLO PLOT MODE (ESC 3):$cr",
                "${esc}3" + ". ".repeat(60) + cr,
                "${esc}4BACK TO NORMAL MODE$cr",
                "DIABLO 630 TEST DONE$cr"
            )
        }

    init {
        syncWithDipSwitches()
        resetTabs()
    }

    override fun syncWithDipSwitches() {
        val s = PrinterManager.diablo630Settings
        autoLineFeed = s.isSwOn(1)
    }

    override fun reset() {
        super.reset()
        syncWithDipSwitches()
        escState = 0
        isGraphicsMode = false
        resetTabs()
    }

    override fun calculateCharWidthInch(c: Char): Float {
        if (isProportional) {
            val glyph = getFont8x8(c)
            var firstCol = 8
            var lastCol = -1
            for (y in 0 until 8) {
                val row = glyph[y].toInt() and 0xFF
                for (x in 0 until 8) {
                    if ((row and (0x80 shr x)) != 0) {
                        firstCol = minOf(firstCol, x)
                        lastCol = maxOf(lastCol, x)
                    }
                }
            }
            if (lastCol < 0) return 4.0f / 120.0f // Space width
            return (lastCol - firstCol + 2).toFloat() / 120.0f
        }
        return 1.0f / 10.0f // 10 CPI default for Diablo
    }

    override fun printChar(c: Char) {
        if (handleEsc(c)) return

        when (c.code) {
            0x07 -> {
                playBell()
            }

            0x08 -> { // Backspace
                val move = if (isGraphicsMode) charSpacingPlotInch else calculateCharWidthInch(' ')
                cursorXInch = maxOf(0f, cursorXInch - move)
            }

            0x20 -> { // Space
                val move = if (isGraphicsMode) charSpacingPlotInch else calculateCharWidthInch(' ')
                cursorXInch += move
            }

            0x0D -> { // CR
                cursorXInch = 0f
                isGraphicsMode = false // CR exits graphics mode on Diablo
                if (autoLineFeed) {
                    doLineFeed()
                }
            }

            0x0A -> { // LF
                doLineFeed()
            }

            0x0C -> { // FF
                VirtualPrinter.instance.formFeed()
                cursorXInch = 0f
            }

            0x09 -> { // TAB
                handleTab()
            }

            else -> {
                if (c.code < 32 || c.code == 127) return
                val charWidthInch = calculateCharWidthInch(c)
                if (cursorXInch + charWidthInch > paperWidthInches) {
                    doLineFeed()
                    cursorXInch = 0f
                }
                drawGlyph(c, cursorXInch)
                // In graphics mode, the carriage does NOT automatically advance
                if (!isGraphicsMode) {
                    cursorXInch += charWidthInch
                }
            }
        }
    }

    private fun handleEsc(c: Char): Boolean {
        when (escState) {
            0 -> {
                if (c == '\u001B') {
                    escState = 1
                    return true
                }
            }

            1 -> {
                escState = 0
                when (c) {
                    '@' -> reset()
                    'A' -> activeColor = 1 // Black
                    'B' -> activeColor = 2 // Red
                    '1' -> horizontalTabs.add(cursorXInch) // Set tab at current pos
                    '2' -> horizontalTabs.clear()         // Clear all tabs
                    '3' -> isGraphicsMode = true
                    '4' -> isGraphicsMode = false
                    'E' -> isBold = true
                    'F' -> isBold = false
                    'X' -> isUnderline = true
                    'Y' -> isUnderline = false
                    'W' -> isBold = true
                    '&' -> {
                        isBold = false; isUnderline = false
                    }

                    'P' -> isProportional = true
                    'Q' -> isProportional = false
                }
                return true
            }
        }
        return false
    }

    private fun doLineFeed() {
        val move = if (isGraphicsMode) lineSpacingPlotInch else (1.0f / 6.0f)
        val totalSpacingRows = (move * 72.0f).toInt()
        val emptyRows = if (isGraphicsMode) 0 else maxOf(0, totalSpacingRows - 8)

        repeat(emptyRows) {
            lines.add(IntArray(VirtualPrinter.PRINTER_WIDTH_TOTAL))
        }
        currentLineStartIndex = -1
    }

    override fun drawGlyph(c: Char, xInch: Float) {
        val glyph = getFont8x8(c)
        val charWidthInch = calculateCharWidthInch(c)
        val startX = (xInch * virtualDpi).toInt()
        val dotWidth = maxOf(1, (charWidthInch * virtualDpi / 8.0f).toInt())

        for (y in 0 until 8) {
            val row = glyph[y].toInt() and 0xFF
            for (gx in 0 until 8) {
                if ((row and (0x80 shr gx)) != 0) {
                    val dx = startX + (gx * dotWidth)
                    setPixel(y, dx)
                    if (isBold) {
                        setPixel(y, dx + (virtualDpi / 120))
                    }
                }
            }
        }

        if (isUnderline) {
            val endX = startX + (8 * dotWidth)
            for (x in startX until endX) {
                setPixel(7, x)
            }
        }
    }
}
