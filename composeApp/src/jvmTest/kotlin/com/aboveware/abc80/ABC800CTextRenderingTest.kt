package com.aboveware.abc80

import aboveabc80.composeapp.generated.resources.Res
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalResourceApi::class)
class ABC800CTextRenderingTest {
    @Test
    fun graphicsPromPreservesTextOverBlueButSupportsOpaqueGraphics() = runBlocking {
        val font = Res.readBytes("files/font800.bin")
        val prom = Res.readBytes("files/hru800c.bin")
        assertEquals(512, prom.size)
        assertEquals(0x0c, prom[3 * 4 + 3].toInt() and 15)
        val screen = ByteArray(40 * 24) { 0x20 }
        screen[0] = 'd'.code.toByte()
        val text = renderABC800CTextBitmap(screen, font, true)
        val textPixels = text.toPixelMap()
        val video = ByteArray(64 * 240) { 0xff.toByte() }
        val visible = renderABC800CDisplay(text, video, 3 shl 8, prom).toPixelMap()
        val opaqueProm = prom.copyOf().apply { this[3 * 4 + 3] = 4 }
        val opaque = renderABC800CDisplay(text, video, 3 shl 8, opaqueProm).toPixelMap()
        val textDisabled = renderABC800CDisplay(text, video, 0x8300, prom).toPixelMap()
        val colorZero = renderABC800CDisplay(text, video, 0, prom).toPixelMap()
        for (y in 0 until 20) {
            for (x in 0 until 12) {
                assertEquals(if (textPixels[x, y] == Color.Black) Color.Blue else textPixels[x, y], visible[x, y])
                assertEquals(Color.Blue, opaque[x, y])
                assertEquals(Color.Blue, textDisabled[x, y])
                assertEquals(textPixels[x, y], colorZero[x, y])
            }
        }
    }

    @Test
    fun graphicsUseAllTwoBitValuesAndClipAtTheDisplayEdge() = runBlocking {
        val font = Res.readBytes("files/font800.bin")
        val prom = Res.readBytes("files/hru800c.bin")
        val text = renderABC800CTextBitmap(ByteArray(960) { 0x20 }, font, true)
        val video = ByteArray(64 * 240)
        video[0] = 0b00_01_10_11.toByte()
        // Offscreen pixels in the 256-pixel VRAM stride must not wrap to the next row.
        video[60] = 0xff.toByte()
        val pixels = renderABC800CDisplay(text, video, (3 shl 8) or 1, prom).toPixelMap()
        for (x in 0 until 480) assertEquals(Color.Black, pixels[x, 0])
        for (y in 1..2) {
            listOf(Color.Black, Color.Red, Color.Green, Color.Blue).forEachIndexed { i, color ->
                assertEquals(color, pixels[i * 2, y])
                assertEquals(color, pixels[i * 2 + 1, y])
            }
            assertEquals(Color.Black, pixels[479, y])
        }
        assertEquals(Color.Black, pixels[0, 3])
        val clipped = renderABC800CDisplay(text, video, (3 shl 8) or 255, prom).toPixelMap()
        assertEquals(Color.Red, clipped[2, 255])
        assertEquals(Color.Black, clipped[2, 479])
    }

    @Test
    fun glyphsUseTheTeletextCellHeightInsteadOfIncludingVumPadding() = runBlocking {
        val font = Res.readBytes("files/font800.bin")
        val screen = ByteArray(40 * 24) { 0x20 }
        screen[0] = 'd'.code.toByte()
        screen[1] = 'g'.code.toByte()
        val pixels = renderABC800CTextBitmap(screen, font, flashVisible = true).toPixelMap()
        fun litRows(column: Int) = (0 until 20).filter { y ->
            (column * 12 until (column + 1) * 12).any { x -> pixels[x, y] == Color.White }
        }
        assertEquals((2..15).toList(), litRows(0))
        assertEquals((6..19).toList(), litRows(1))
    }

    @Test
    fun firmwareCursorInvertsOnlyTheCellAfterTheInput() = runBlocking {
        val font = Res.readBytes("files/font800.bin")
        val screen = ByteArray(40 * 24) { 0x20 }
        "ddd".forEachIndexed { column, char -> screen[40 + column] = char.code.toByte() }
        val hidden = renderABC800CTextBitmap(screen, font, flashVisible = true).toPixelMap()
        screen[40 + 3] = 0xa0.toByte()
        val visible = renderABC800CTextBitmap(screen, font, flashVisible = true).toPixelMap()
        screen[40 + 3] = 0x20
        val hiddenAgain = renderABC800CTextBitmap(screen, font, flashVisible = true).toPixelMap()

        for (y in 0 until visible.height) {
            for (x in 0 until visible.width) {
                assertEquals(hidden[x, y], hiddenAgain[x, y], "Cursor left a stale pixel at ($x, $y)")
                if (y in 20 until 40 && x in 36 until 48) {
                    assertEquals(Color.Black, hidden[x, y])
                    assertEquals(Color.White, visible[x, y])
                } else {
                    assertEquals(hidden[x, y], visible[x, y], "Cursor changed pixel ($x, $y) outside its cell")
                }
            }
        }
    }

    @Test
    fun teletextFlashPhasesChangePixelsWithoutChangingCharacterRam() = runBlocking {
        val font = Res.readBytes("files/font800.bin")
        val screen = ByteArray(40 * 24) { 0x20 }
        screen[0] = 0x08
        screen[1] = 'd'.code.toByte()
        val visible = renderABC800CTextBitmap(screen, font, flashVisible = true).toPixelMap()
        val hidden = renderABC800CTextBitmap(screen, font, flashVisible = false).toPixelMap()
        assertTrue((0 until 20).any { y -> (12 until 24).any { x -> visible[x, y] == Color.White } })
        for (y in 0 until 20) {
            for (x in 12 until 24) assertEquals(Color.Black, hidden[x, y])
        }
    }
}
