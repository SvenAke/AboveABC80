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

    @Test
    fun decodesPackedHighResolutionPixelsFromMostSignificantBitsFirst() {
        val videoRam = ByteArray(64 * 240)
        videoRam[0] = 0b00_01_10_11.toByte()
        videoRam[64] = 0b11_00_10_01.toByte()

        val pixels = decodeABC800MHighResolutionScreen(videoRam)

        assertEquals(256 * 240, pixels.size)
        assertEquals(listOf(0, 1, 2, 3), (0..3).map { pixels[it].toInt() })
        assertEquals(listOf(3, 0, 2, 1), (0..3).map { pixels[256 + it].toInt() })
    }

    @Test
    fun rejectsShortHighResolutionVideoRam() {
        assertFailsWith<IllegalArgumentException> {
            decodeABC800MHighResolutionScreen(ByteArray(64 * 240 - 1))
        }
    }

    @Test
    fun decodesABC800C40ColumnText() {
        val characterRam = ByteArray(40 * 24) { 0x20 }
        characterRam[0] = 'A'.code.toByte()
        characterRam[39] = 'B'.code.toByte()
        characterRam[40] = 'C'.code.toByte()

        val rows = decodeABC800CScreen(characterRam)

        assertEquals(24, rows.size)
        assertEquals(40, rows.first().length)
        assertEquals('A', rows[0][0])
        assertEquals('B', rows[0][39])
        assertEquals('C', rows[1][0])
    }

    @Test
    fun ABC800CTextCellsMatchIts480By480DisplayRaster() {
        assertEquals(480, 40 * ABC800C_TEXT_CELL_WIDTH)
        assertEquals(480, 24 * ABC800C_TEXT_CELL_HEIGHT)
    }

    @Test
    fun decodesABC800CGraphicsWithTheSelectedPaletteColor() {
        val videoRam = ByteArray(64 * 240)
        videoRam[0] = 0b00_01_10_11.toByte()

        val pixels = decodeABC800CHighResolutionScreen(videoRam, 2 shl 8)

        assertEquals(240 * 240, pixels.size)
        assertEquals(listOf(0, 2, 2, 2), (0..3).map { pixels[it].toInt() })
    }

    @Test
    fun rejectsShortABC800CGraphicsMemory() {
        assertFailsWith<IllegalArgumentException> {
            decodeABC800CHighResolutionScreen(ByteArray(64 * 240 - 1), 0)
        }
    }

    @Test
    fun highResolutionUsesAbc800MActiveRasterGeometry() {
        assertEquals(320f, ABC800M_HIGH_RESOLUTION_DISPLAY_WIDTH)
        assertEquals(71.875f, ABC800M_HIGH_RESOLUTION_LEFT)
        assertEquals(192f, ABC800M_HIGH_RESOLUTION_DISPLAY_HEIGHT)
        assertEquals(0.8f, ABC800M_HIGH_RESOLUTION_TOP_SCALE)
        assertEquals(240, ABC800M_HIGH_RESOLUTION_HEIGHT)
    }

    @Test
    fun ignoresRepeatedPhysicalKeyDownEvents() {
        assertEquals(true, shouldForwardABC800MKey(isKeyDown = true, repeatCount = 0))
        assertEquals(false, shouldForwardABC800MKey(isKeyDown = true, repeatCount = 1))
        assertEquals(false, shouldForwardABC800MKey(isKeyDown = false, repeatCount = 0))
    }
}
