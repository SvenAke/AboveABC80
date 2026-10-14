package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ABC800MScreenTest {
    @Test
    fun decodes80By24CharacterRamWithoutChangingCharacterCodes() {
        val characterRam = ByteArray(80 * 24) { 0x20 }
        characterRam[0] = 'A'.code.toByte()
        characterRam[79] = 0x7f
        characterRam[80] = 'B'.code.toByte()

        val rows = decodeABC800MScreen(characterRam)

        assertEquals(24, rows.size)
        assertEquals(80, rows.first().length)
        assertEquals('A', rows[0][0])
        assertEquals(0x7f, rows[0][79].code)
        assertEquals('B', rows[1][0])
    }

    @Test
    fun rejectsShortCharacterRam() {
        assertFailsWith<IllegalArgumentException> {
            decodeABC800MScreen(ByteArray(80 * 24 - 1))
        }
    }

    @Test
    fun cursorUsesTheCrtcRasterRange() {
        assertEquals(listOf(12..15), decodeABC800MCursorRasterRanges(12 or (15 shl 8)))
        assertEquals(
            listOf(13..15, 0..2),
            decodeABC800MCursorRasterRanges(13 or (2 shl 8))
        )
        assertEquals(emptyList(), decodeABC800MCursorRasterRanges(16 or (20 shl 8)))
    }
}
