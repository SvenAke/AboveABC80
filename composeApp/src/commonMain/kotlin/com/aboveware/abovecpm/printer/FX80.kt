package com.aboveware.abovecpm.printer

import kotlin.math.abs
import kotlin.math.sqrt

class FX80 : MX80() {
    companion object {
        val instance = FX80()
    }

    init {
        horizontalDpi = 240
    }

    private var startCharCode = 0
    private var endCharCode = 0
    private var currentCharCode = 0
    private var currentAttr = 0
    private var currentDataIndex = 0
    private var currentData = ByteArray(11)

    override fun reset() {
        super.reset()
        startCharCode = 0
        endCharCode = 0
        currentCharCode = 0
        currentAttr = 0
        currentDataIndex = 0
        currentData = ByteArray(11)
    }

    override val testData: List<String>
        get() {
            val cr = PrinterCommands.CR
            val lf = PrinterCommands.LF

            // Design for "Square Root" (√) character. 11 columns.
            val rootData = intArrayOf(
                0x20, 0x10, 0x08, 0x04, 0x08, 0x10, 0x20, 0x40, 0x80, 0x80, 0x80
            )

            val defineRoot = StringBuilder().apply {
                append("\u001B&")
                append(0.toChar())
                append('A')
                append('A')
                append(0x8B.toChar())
                rootData.forEach { append(it.toChar()) }
            }.toString()

            // Generate a mathematically perfect circle for 72 DPI (m=5)
            // Radius 32 dots -> 64 dots wide/high.
            // We need 8 passes (each 8 dots high) = 64 dots total height.
            val radius = 31.0
            val circlePasses = mutableListOf<String>()
            for (pass in 0 until 8) {
                val passData = IntArray(64)
                for (x in 0 until 64) {
                    val dx = x - 32.0
                    // Calculate y positions for this x on the circle: (x-32)^2 + (y-32)^2 = 31^2
                    val yDistSq = radius * radius - dx * dx
                    if (yDistSq >= 0) {
                        val dy = sqrt(yDistSq)
                        val y1 = 32.0 - dy
                        val y2 = 32.0 + dy

                        // Check which dots in this 8-pin pass should be ON
                        for (pin in 0 until 8) {
                            val currentY = (pass * 8 + pin).toDouble()
                            if (abs(currentY - y1) < 0.7 || abs(currentY - y2) < 0.7) {
                                passData[x] = passData[x] or (0x80 shr pin)
                            }
                        }
                    }
                }
                circlePasses.add(graphicsStar(5, passData))
            }

            val fxSpecific = mutableListOf(
                "${cr}FX-80 SPECIFIC TESTS:$cr$lf",
                "${PrinterCommands.PROPORTIONAL_ON}PROPORTIONAL: THE QUICK BROWN FOX (iii lll WWW)${PrinterCommands.PROPORTIONAL_OFF}$cr$lf",
                "${PrinterCommands.MASTER_SELECT}${1.toChar()}ELITE (12 CPI)${PrinterCommands.MASTER_SELECT}${0.toChar()}$cr$lf",
                "${PrinterCommands.MASTER_SELECT}${(32 + 64 + 128).toChar()}MASTER SELECT: WIDE + ITALIC + UNDERLINE${PrinterCommands.MASTER_SELECT}${0.toChar()}$cr$lf",
                "${cr}FX-80 DOWNLOAD CHAR TEST (ESC &):$cr$lf",
                defineRoot + "DOWNLOADED ROOT SYMBOL TO CHARACTER 'A'$cr$lf",
                "\u001B%${1.toChar()}USER DEFINED ENABLED: A(144) = A144$cr$lf",
                "\u001B%${0.toChar()}USER DEFINED DISABLED: A(144) = A144$cr$lf",
                "\u001B3\u0018LINE SPACING 24/216\" (VERY TIGHT)$cr$lf",
                "\u001B3\u0048LINE SPACING 72/216\" (1/3\" - WIDE)$cr$lf",
                "${PrinterCommands.LINE_SPACING_1_6}BACK TO 1/6\" SPACING$cr$lf",
                "${cr}FX-80 72 DPI CIRCLE (ESC * 5):$cr$lf",
                "${PrinterCommands.SET_LINE_SPACING_N_72}\u0008" // Set line spacing to 8/72" for seamless tiling
            )

            fxSpecific.addAll(circlePasses)

            fxSpecific.add("${PrinterCommands.LINE_SPACING_1_6}$cr$lf")
            fxSpecific.add("${cr}FX-80 BIT IMAGE MODES (ESC *):$cr$lf")
            fxSpecific.add(
                "M=0 (60 DPI): " + graphicsStar(
                    0,
                    intArrayOf(0xAA, 0x55, 0xAA, 0x55, 0xAA, 0x55)
                )
            )
            fxSpecific.add(
                "M=1 (120 DPI): " + graphicsStar(
                    1,
                    intArrayOf(
                        0xAA,
                        0x55,
                        0xAA,
                        0x55,
                        0xAA,
                        0x55,
                        0xAA,
                        0x55,
                        0xAA,
                        0x55,
                        0xAA,
                        0x55
                    )
                )
            )
            fxSpecific.add(
                "M=4 (80 DPI): " + graphicsStar(
                    4,
                    intArrayOf(0xAA, 0x55, 0xAA, 0x55, 0xAA, 0x55, 0xAA, 0x55)
                )
            )
            fxSpecific.add(
                "M=6 (90 DPI): " + graphicsStar(
                    6,
                    intArrayOf(0xAA, 0x55, 0xAA, 0x55, 0xAA, 0x55, 0xAA, 0x55, 0xAA, 0x55)
                )
            )
            fxSpecific.add("${cr}FX-80 240 DPI GRAPHICS (ESC Z):$cr$lf")
            fxSpecific.add(graphics240(IntArray(40) { if (it % 2 == 0) 0xAA else 0x55 }))
            fxSpecific.add("FX-80 TEST DONE$cr$lf")

            val baseTests =
                super.testData.filter { !it.contains("TEST DONE") && !it.contains("OVAL") }
            return baseTests + fxSpecific
        }

    private fun graphicsStar(mode: Int, data: IntArray): String {
        val n1 = data.size % 256
        val n2 = data.size / 256
        val header = "\u001B*${mode.toChar()}${n1.toChar()}${n2.toChar()}"
        return header + data.map { it.toChar() }.joinToString("") + "\r\n"
    }

    private fun graphics240(data: IntArray): String {
        val n1 = data.size % 256
        val n2 = data.size / 256
        val header = "\u001BZ${n1.toChar()}${n2.toChar()}"
        return header + data.map { it.toChar() }.joinToString("") + "\r\n"
    }

    override fun handleEscP(c: Char): Boolean {
        if (super.handleEscP(c)) return true

        when (escState) {
            1 -> {
                when (c) {
                    '@' -> {
                        reset(); escState = 0; return true
                    }

                    '3' -> {
                        escState = 21; return true
                    }

                    'Z' -> {
                        graphicsStepInch = 1.0f / 240.0f; escState = 3; return true
                    }

                    'p' -> {
                        escState = 30; return true
                    }

                    '!' -> {
                        escState = 40; return true
                    }

                    '*' -> {
                        escState = 5; return true
                    }

                    '&' -> {
                        escState = 50; return true
                    }

                    '%' -> {
                        escState = 60; return true
                    }
                }
            }

            5 -> { // ESC * m n1 n2
                graphicsStepInch = when (c.code) {
                    0 -> 1.0f / 60.0f
                    1, 2 -> 1.0f / 120.0f
                    3 -> 1.0f / 240.0f
                    4 -> 1.0f / 80.0f
                    5 -> 1.0f / 72.0f
                    6 -> 1.0f / 90.0f
                    else -> 1.0f / 60.0f
                }
                escState = 3
                return true
            }

            21 -> {
                lineSpacingInch = c.code.toFloat() / 216.0f; escState = 0; return true
            }

            30 -> {
                isProportional = (c == '1' || c.code == 1); escState = 0; return true
            }

            40 -> { // ESC ! n
                val n = c.code
                isElite = (n and 0x01) != 0
                isProportional = (n and 0x02) != 0
                isCompressed = (n and 0x04) != 0
                isBold = (n and 0x08) != 0 || (n and 0x10) != 0
                isDoubleWide = (n and 0x20) != 0
                isItalic = (n and 0x40) != 0
                isUnderline = (n and 0x80) != 0
                escState = 0
                return true
            }

            50 -> {
                escState = 51; return true
            } // Reserved NUL
            51 -> {
                startCharCode = c.code; escState = 52; return true
            }

            52 -> {
                endCharCode = c.code; currentCharCode = startCharCode; escState = 53; return true
            }

            53 -> {
                currentAttr = c.code; currentDataIndex = 0; escState = 54; return true
            }

            54 -> {
                currentData[currentDataIndex++] = c.code.toByte()
                if (currentDataIndex >= 11) {
                    userDefinedChars[currentCharCode] = currentData.copyOf()
                    if (currentCharCode < endCharCode) {
                        currentCharCode++
                        escState = 53
                    } else {
                        escState = 0
                    }
                }
                return true
            }

            60 -> {
                isUserDefinedEnabled = (c == '1' || c.code == 1); escState = 0; return true
            }
        }
        return false
    }

    override fun drawGlyph(c: Char, xInch: Float) {
        super.drawGlyph(c, xInch)
    }
}
