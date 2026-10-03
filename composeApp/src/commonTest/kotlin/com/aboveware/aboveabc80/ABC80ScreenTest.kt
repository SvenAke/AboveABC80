package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ABC80ScreenTest {
    @Test
    fun readsAsciiFromInterleavedRowAddresses() {
        val memory = ByteArray(0x10000)
        memory[0x7c00] = 'A'.code.toByte()
        memory[0x7c80] = 'B'.code.toByte()
        memory[0x7d01] = 'C'.code.toByte()
        memory[0x7c28] = 'I'.code.toByte()
        memory[0x7fd0 + 39] = 'X'.code.toByte()
        memory[0x7d80] = 0x01

        val rows = decodeABC80Screen(memory)

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
