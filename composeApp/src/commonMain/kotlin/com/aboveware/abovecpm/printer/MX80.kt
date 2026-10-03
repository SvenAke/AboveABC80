package com.aboveware.abovecpm.printer

import com.aboveware.abovecpm.playBell
import com.aboveware.abovecpm.printer.PrinterCommands.BOLD_OFF
import com.aboveware.abovecpm.printer.PrinterCommands.BOLD_ON
import com.aboveware.abovecpm.printer.PrinterCommands.ITALIC_OFF
import com.aboveware.abovecpm.printer.PrinterCommands.ITALIC_ON
import com.aboveware.abovecpm.printer.PrinterCommands.LINE_SPACING_1_6
import com.aboveware.abovecpm.printer.PrinterCommands.SET_LINE_SPACING_N_72
import com.aboveware.abovecpm.printer.PrinterCommands.UNDERLINE_OFF
import com.aboveware.abovecpm.printer.PrinterCommands.UNDERLINE_ON

open class MX80 : Printer("Epson MX-80") {
    companion object {
        val instance = MX80()
    }

    protected var escState = 0
    protected var lastEscCode = ' '
    protected var graphicsRemaining = 0
    protected var graphicsStepInch: Float = 1.0f / 60.0f
    protected var graphicsCount1 = 0

    protected var lineSpacingInch: Float = 1.0f / 6.0f
    protected var pageLengthLines: Int = 66
    protected var skipOverPerforation: Int = 0
    protected var currentLineInPage: Int = 0

    protected var isOneLineDoubleWide = false
    protected var isLineDirty = false

    override val testData: List<String>
        get() {
            val cr = PrinterCommands.CR

            fun graphics60(data: IntArray): String {
                val header = "\u001BK${data.size.toChar()}\u0000"
                return header + data.map { it.toChar() }.joinToString("") + cr
            }

            fun graphics120(data: IntArray): String {
                val header = "\u001BL${data.size.toChar()}\u0000"
                return header + data.map { it.toChar() }.joinToString("") + cr
            }

            val row0 = graphics60(
                intArrayOf(
                    0,
                    3,
                    6,
                    28,
                    24,
                    48,
                    96,
                    64,
                    192,
                    192,
                    128,
                    128,
                    128,
                    128,
                    128,
                    192,
                    192,
                    64,
                    96,
                    48,
                    24,
                    28,
                    6,
                    3,
                    0
                )
            )
            val row1 = graphics60(
                intArrayOf(
                    255,
                    193,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    193,
                    255
                )
            )
            val row2 = graphics60(
                intArrayOf(
                    128,
                    224,
                    48,
                    28,
                    12,
                    6,
                    3,
                    1,
                    1,
                    1,
                    0,
                    0,
                    0,
                    0,
                    0,
                    1,
                    1,
                    1,
                    3,
                    6,
                    12,
                    28,
                    48,
                    224,
                    128
                )
            )

            // Same oval in 120 DPI (repeated pixels)
            val doubleRow0 = graphics120(
                intArrayOf(
                    0,
                    0,
                    3,
                    3,
                    6,
                    6,
                    28,
                    28,
                    24,
                    24,
                    48,
                    48,
                    96,
                    96,
                    64,
                    64,
                    192,
                    192,
                    192,
                    192,
                    128,
                    128,
                    128,
                    128,
                    128,
                    128,
                    128,
                    128,
                    128,
                    128,
                    192,
                    192,
                    192,
                    192,
                    64,
                    64,
                    96,
                    96,
                    48,
                    48,
                    24,
                    24,
                    28,
                    28,
                    6,
                    6,
                    3,
                    3,
                    0,
                    0
                )
            )
            val doubleRow1 = graphics120(
                intArrayOf(
                    255,
                    255,
                    193,
                    193,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    193,
                    193,
                    255,
                    255
                )
            )
            val doubleRow2 = graphics120(
                intArrayOf(
                    128,
                    128,
                    224,
                    224,
                    48,
                    48,
                    28,
                    28,
                    12,
                    12,
                    6,
                    6,
                    3,
                    3,
                    1,
                    1,
                    1,
                    1,
                    1,
                    1,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    0,
                    1,
                    1,
                    1,
                    1,
                    1,
                    1,
                    3,
                    3,
                    6,
                    6,
                    12,
                    12,
                    28,
                    28,
                    48,
                    48,
                    224,
                    224,
                    128,
                    128
                )
            )

            val langName = PrinterManager.mx80Settings.getInternationalCharset().name
            val langTest = "LANG: $langName - [#\\]^ `{|}~ -> £ÉÄÖÅÜ éäöåü$cr"

            return listOf(
                "MX-80 NORMAL: THE QUICK BROWN FOX ÅÄÖ$cr",
                "BELL TEST: \u0007$cr",
                langTest,
                "TABS:\t1\t2\t3$cr",
                "${BOLD_ON}BOLD ON: THE QUICK BROWN FOX$BOLD_OFF$cr",
                "${ITALIC_ON}ITALIC ON: THE QUICK BROWN FOX$ITALIC_OFF$cr",
                "${UNDERLINE_ON}UNDERLINE ON: THE QUICK BROWN FOX$UNDERLINE_OFF$cr",
                "${cr}MX-80 OVAL (60 DPI):$cr",
                "${SET_LINE_SPACING_N_72}\u0008",
                row0, row1, row2,
                "${cr}MX-80 OVAL (120 DPI):$cr",
                "${SET_LINE_SPACING_N_72}\u0008",
                doubleRow0, doubleRow1, doubleRow2,
                "${LINE_SPACING_1_6}$cr$cr",
                "MX-80 TEST DONE$cr"
            )
        }

    init {
        horizontalDpi = 60
        verticalDpi = 72
        syncWithDipSwitches()
        resetTabs()
    }

    override fun syncWithDipSwitches() {
        val s = PrinterManager.mx80Settings
        // SW1-4: Line spacing
        lineSpacingInch = if (s.isSw1On(4)) 1.0f / 8.0f else 1.0f / 6.0f
        // SW1-8: Default Italic
        isItalic = s.isSw1On(8)
        // SW2-1: Auto line feed
        autoLineFeed = s.isSw2On(1)
    }

    override fun reset() {
        super.reset()
        syncWithDipSwitches()
        escState = 0
        graphicsRemaining = 0
        isOneLineDoubleWide = false
        isLineDirty = false
        currentLineInPage = 0
    }

    override fun resetTabs() {
        horizontalTabs.clear()
        // Default tabs every 8 characters @ 10 CPI = 0.8 inches
        for (i in 1..32) horizontalTabs.add(i * 0.8f)
    }

    override fun printChar(c: Char) {
        if (graphicsRemaining > 0) {
            drawGraphicsByte(c.code)
            graphicsRemaining--
            isLineDirty = true
            return
        }
        val wasInEsc = escState != 0
        if (handleEscP(c)) return
        if (wasInEsc) escState = 0 // Reset if an escape sequence was broken/unknown

        when (c.code) {
            0x07 -> {
                if (PrinterManager.mx80Settings.isSw2On(2)) {
                    playBell()
                }
                return
            }

            0x08 -> {
                cursorXInch = maxOf(0f, cursorXInch - calculateCharWidthInch(c)); return
            }

            0x09 -> {
                handleTab(); return
            }

            0x0A -> { // LF
                doLineFeed()
                return
            }

            0x0C -> {
                VirtualPrinter.instance.formFeed()
                currentLineInPage = 0
                cursorXInch = 0f
                return
            }

            0x0D -> { // CR
                cursorXInch = 0f
                if (autoLineFeed) {
                    doLineFeed()
                }
                return
            }

            0x0E -> {
                isOneLineDoubleWide = true; return
            }

            0x0F -> {
                isCompressed = true; return
            }

            0x12 -> {
                isCompressed = false; return
            }

            0x14 -> {
                isOneLineDoubleWide = false; return
            }

            0x18 -> {
                cursorXInch = 0f
                isLineDirty = false
                return
            }
        }
        if (c.code < 32 || c.code == 127) return

        val charWidthInch = calculateCharWidthInch(c)
        if (cursorXInch + charWidthInch > paperWidthInches) {
            doLineFeed()
            cursorXInch = 0f
        }
        drawGlyph(c, cursorXInch)
        cursorXInch += charWidthInch
        isLineDirty = true
    }

    protected open fun doLineFeed() {
        val totalSpacingLines = (lineSpacingInch * 72.0f).toInt()
        val emptyRows = maxOf(0, totalSpacingLines - 8)
        repeat(emptyRows) { lines.add(IntArray(VirtualPrinter.PRINTER_WIDTH_TOTAL)) }

        // Signal that the next print should start on a new 8-row block
        currentLineStartIndex = -1

        isOneLineDoubleWide = false
        isLineDirty = false
        currentLineInPage++
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
            if (lastCol < 0) return 3.0f / 120.0f // Space width

            // Standard proportional width is (content dots + 2) / 120 inch
            val dots = lastCol - firstCol + 2
            return dots.toFloat() / 120.0f
        }

        val cpi = if (isElite) 12.0f else if (isCompressed) 17.1f else 10.0f
        var width = 1.0f / cpi
        if (isDoubleWide || isOneLineDoubleWide) width *= 2.0f
        return width
    }

    protected open fun handleEscP(c: Char): Boolean {
        when (escState) {
            0 -> {
                if (c == PrinterCommands.ESC[0]) {
                    escState = 1; return true
                }
            }

            1 -> {
                lastEscCode = c
                when (c) {
                    '@' -> {
                        reset(); escState = 0
                    }

                    '4' -> {
                        isItalic = true; escState = 0
                    }

                    '5' -> {
                        isItalic = false; escState = 0
                    }

                    'E', 'G' -> {
                        isBold = true; escState = 0
                    }

                    'F', 'H' -> {
                        isBold = false; escState = 0
                    }

                    'K' -> {
                        graphicsStepInch = 1.0f / 60.0f; escState = 3
                    }

                    'L', 'Y' -> {
                        graphicsStepInch = 1.0f / 120.0f; escState = 3
                    }

                    '0' -> {
                        lineSpacingInch = 1.0f / 8.0f; escState = 0
                    }

                    '1' -> {
                        lineSpacingInch = 7.0f / 72.0f; escState = 0
                    }

                    '2' -> {
                        lineSpacingInch = 1.0f / 6.0f; escState = 0
                    }

                    'A' -> {
                        escState = 10
                    }

                    'C' -> {
                        escState = 11
                    }

                    'D' -> {
                        horizontalTabs.clear(); escState = 12
                    }

                    'N' -> {
                        escState = 13
                    }

                    'O' -> {
                        skipOverPerforation = 0; escState = 0
                    }

                    'W' -> {
                        escState = 2
                    }

                    'p' -> {
                        escState = 30
                    }

                    '-' -> {
                        escState = 20
                    }

                    else -> return false // Let subclasses handle other ESC codes
                }
                return true
            }

            2 -> {
                isDoubleWide = (c == '1' || c.code == 1); escState = 0; return true
            }

            3 -> {
                graphicsCount1 = c.code; escState = 4; return true
            }

            4 -> {
                graphicsRemaining = graphicsCount1 + (c.code * 256); escState = 0; return true
            }

            10 -> {
                lineSpacingInch = c.code.toFloat() / 72.0f; escState = 0; return true
            }

            11 -> {
                if (c.code == 0) escState = 14 else {
                    pageLengthLines = c.code; escState = 0
                }; return true
            }

            12 -> {
                if (c.code == 0) escState =
                    0 else horizontalTabs.add(c.code.toFloat() / 10.0f); return true
            }

            13 -> {
                skipOverPerforation = c.code; escState = 0; return true
            }

            14 -> {
                pageLengthLines = (c.code.toFloat() / lineSpacingInch).toInt(); escState =
                    0; return true
            }

            20 -> {
                isUnderline = (c == '1' || c.code == 1); escState = 0; return true
            }

            30 -> {
                isProportional = (c == '1' || c.code == 1); escState = 0; return true
            }
        }
        return false
    }

    protected fun drawGraphicsByte(byte: Int) {
        val x = (cursorXInch * virtualDpi).toInt()
        for (y in 0 until 8) {
            if ((byte and (0x80 shr y)) != 0) {
                setPixel(y, x)
            }
        }
        cursorXInch += graphicsStepInch
    }

    override fun drawGlyph(c: Char, xInch: Float) {
        val rawCharCode = c.code
        val userGlyph = if (isUserDefinedEnabled) userDefinedChars[rawCharCode] else null

        if (userGlyph != null) {
            drawUserDefinedGlyph(userGlyph, xInch)
            return
        }

        val glyph = getFont8x8(mapCharacter(c))

        // Find actual content boundaries for proportional spacing
        var firstCol = 0
        var lastCol = 7
        if (isProportional) {
            firstCol = 8
            lastCol = -1
            for (y in 0 until 8) {
                val row = glyph[y].toInt() and 0xFF
                for (x in 0 until 8) {
                    if ((row and (0x80 shr x)) != 0) {
                        firstCol = minOf(firstCol, x)
                        lastCol = maxOf(lastCol, x)
                    }
                }
            }
            if (lastCol < 0) { // Space character
                firstCol = 0
                lastCol = 2 // Standard space width in prop mode
            }
        }

        val charWidthInch = calculateCharWidthInch(c)
        val startX = (xInch * virtualDpi).toInt()

        // In proportional mode, dots are always 1/120" wide.
        // In fixed modes, we scale the 8 dots to fill the character cell.
        val dotWidth = if (isProportional)
            (virtualDpi / 120.0f).toInt()
        else
            (charWidthInch * virtualDpi / 8.0f).toInt()

        // Italic slant offsets per row (top rows shift more to the right)
        val italicOffsets =
            if (isItalic) intArrayOf(3, 2, 2, 1, 1, 0, 0, 0) else intArrayOf(0, 0, 0, 0, 0, 0, 0, 0)

        for (y in 0 until 8) {
            val row = glyph[y].toInt() and 0xFF
            val rowOffset = italicOffsets[y] * dotWidth
            for (gx in firstCol..lastCol) {
                if ((row and (0x80 shr gx)) != 0) {
                    val dx = startX + ((gx - firstCol) * dotWidth) + rowOffset
                    setPixel(y, dx)

                    // Bold is rendered by adding a secondary dot shifted by 1/120"
                    if (isBold) {
                        val boldShift = virtualDpi / 120
                        setPixel(y, dx + boldShift)
                    }
                }
            }
        }

        if (isUnderline) {
            // Draw a solid line at row 7
            val endX = startX + ((lastCol - firstCol + 1) * dotWidth)
            for (x in startX until endX) {
                setPixel(7, x)
            }
        }
    }

    override fun mapCharacter(c: Char): Char {
        val s = PrinterManager.mx80Settings
        val charset = s.getInternationalCharset()

        return when (charset) {
            InternationalCharset.FRANCE -> when (c) {
                '@' -> 'à'; '[' -> '°'; '\\' -> 'ç'; ']' -> '§'; '{' -> 'é'; '|' -> 'ù'; '}' -> 'è'; '~' -> '¨'; else -> c
            }

            InternationalCharset.GERMANY -> when (c) {
                '@' -> '§'; '[' -> 'Ä'; '\\' -> 'Ö'; ']' -> 'Ü'; '{' -> 'ä'; '|' -> 'ö'; '}' -> 'ü'; '~' -> 'ß'; else -> c
            }

            InternationalCharset.UK -> when (c) {
                '#' -> '£'; else -> c
            }

            InternationalCharset.DENMARK -> when (c) {
                '[' -> 'Æ'; '\\' -> 'Ø'; ']' -> 'Å'; '{' -> 'æ'; '|' -> 'ø'; '}' -> 'å'; else -> c
            }

            InternationalCharset.SWEDEN -> when (c) {
                '#' -> '£'; '@' -> 'É'; '[' -> 'Ä'; '\\' -> 'Ö'; ']' -> 'Å'; '^' -> 'Ü'; '`' -> 'é'; '{' -> 'ä'; '|' -> 'ö'; '}' -> 'å'; '~' -> 'ü'; else -> c
            }

            InternationalCharset.ITALY -> when (c) {
                '@' -> '@'; '[' -> '°'; '\\' -> '\\'; ']' -> 'é'; '`' -> 'ù'; '{' -> 'à'; '|' -> 'ò'; '}' -> 'è'; '~' -> 'ì'; else -> c
            }

            InternationalCharset.SPAIN -> when (c) {
                '@' -> '@'; '[' -> '¡'; '\\' -> 'Ñ'; ']' -> '¿'; '{' -> '¨'; '|' -> 'ñ'; '}' -> '}'; '~' -> '~'; else -> c
            }

            else -> c
        }
    }

    protected fun drawUserDefinedGlyph(columns: ByteArray, xInch: Float) {
        val charWidthInch = calculateCharWidthInch(' ') // Use standard width
        val startX = (xInch * virtualDpi).toInt()

        // Use exactly the provided columns if possible, scaling to fit the cell
        val dotWidth = if (isProportional)
            (virtualDpi / 120.0f).toInt()
        else
            (charWidthInch * virtualDpi / columns.size).toInt()

        for (gx in columns.indices) {
            val colData = columns[gx].toInt() and 0xFF
            val dx = startX + (gx * dotWidth)
            for (y in 0 until 8) {
                if ((colData and (0x80 shr y)) != 0) {
                    setPixel(y, dx)
                    if (isBold) {
                        val boldShift = virtualDpi / 120
                        setPixel(y, dx + boldShift)
                    }
                }
            }
        }
    }
}
