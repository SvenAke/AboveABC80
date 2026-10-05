package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ABC80ScreenTest {
    @Test
    fun wideScreenUsesAll80ColumnsAnd24Rows() {
        val memory = ByteArray(0x10000)
        for (row in 0 until 24) {
            memory[TKN80.rowAddress(row, true)] = 'A'.code.toByte()
            memory[TKN80.rowAddress(row, true) + 79] = 'Z'.code.toByte()
        }
        val rows = decodeABC80ScreenGlyphs(memory, true)
        assertEquals(24, rows.size)
        rows.forEach {
            assertEquals(80, it.length)
            assertEquals('A', it[0])
            assertEquals('Z', it[79])
        }
        assertEquals(0x5fa0, TKN80.rowAddress(23, true))
        memory[SCREEN_COLUMN] = 79
        assertEquals(0 to 79, decodeABC80Cursor(memory, 80))
        assertEquals(null, decodeABC80Cursor(memory, 40))
        memory[SCREEN_COLUMN] = 80
        assertEquals(null, decodeABC80Cursor(memory, 80))
        assertEquals(" ".repeat(40), decodeABC80Screen(memory, false)[0])
    }

    @Test
    fun graphicsControlsSelectMosaicsUntilTextModeOrNextRow() {
        for (wide in listOf(false, true)) {
            val memory = ByteArray(0x10000)
            byteArrayOf(0x17, 0x35, 0x6a, 0x2f, 0x16, 0x35).copyInto(memory, TKN80.rowAddress(0, wide))
            memory[TKN80.rowAddress(1, wide)] = 0x35
            val glyphs = decodeABC80ScreenGlyphs(memory, wide)
            assertEquals(" \u00b5\u00ea\u00af 5", glyphs[0].take(6))
            assertEquals('5', glyphs[1][0])
            assertEquals(" 5j/ 5", decodeABC80Screen(memory, wide)[0].take(6))
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    @Test
    fun keyboardMosaicsHaveSolidEdgesAndLettersStayText() {
        val map = Abc80MonitorCharacterMap()
        for (row in 0 until map.charHeight) {
            assertEquals(0xf0, map.character(0xb5, row).toInt())
            assertEquals(0x0f, map.character(0xea, row).toInt())
            assertEquals(map.character('W'.code, row), map.character(0xd7, row))
        }
        assertEquals(0xff, map.character(0xaf, 0).toInt())
        assertEquals(0, map.character(0xaf, 13).toInt())
    }

    @Test
    fun readsAsciiFromInterleavedRowAddresses() {
        val memory = ByteArray(0x10000)
        memory[0x7c00] = 'A'.code.toByte()
        memory[0x7c80] = 'B'.code.toByte()
        memory[0x7d01] = 'C'.code.toByte()
        memory[0x7c28] = 'I'.code.toByte()
        memory[0x7fd0 + 39] = 'X'.code.toByte()
        memory[0x7d80] = 0x01

        val rows = decodeABC80Screen(memory, false)

        assertEquals(24, rows.size)
        assertEquals(40, rows[0].length)
        assertEquals('A', rows[0][0])
        assertEquals('B', rows[1][0])
        assertEquals('C', rows[2][1])
        assertEquals(' ', rows[3][0])
        assertEquals('I', rows[8][0])
        assertEquals('X', rows[23][39])
    }

    @Test
    fun requiresMemoryCoveringScreen() {
        assertFailsWith<IllegalArgumentException> { decodeABC80Screen(ByteArray(0x7fff)) }
    }
}
