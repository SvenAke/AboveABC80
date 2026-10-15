package com.aboveware.aboveabc80

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
