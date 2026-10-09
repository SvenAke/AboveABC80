package com.aboveware.aboveabc80.terminal

import com.aboveware.aboveabc80.Abc80MonitorCharacterMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalUnsignedTypes::class)
class ABC80GlyphTest {
    @Test
    fun terminalUsesMonitorPixelsForEveryCharacterWithoutLoadingRoms() {
        val terminal = ABC80()
        val map = Abc80MonitorCharacterMap()
        assertEquals(map.charWidth, terminal.nominalWidth)
        assertEquals(map.charHeight, terminal.nominalHeight)
        assertEquals(1f, terminal.dotStretch)
        for (code in 0..255) {
            val glyph = assertNotNull(terminal.getGlyph(code.toChar()))
            assertEquals(map.charWidth, glyph.glyphWidth80)
            assertEquals(map.charHeight, glyph.height)
            map.forEach(code) { bits, row ->
                map.forEachBit(bits.toInt()) { on, column ->
                    assertEquals(on, glyph.data80[row][column], "code=$code row=$row column=$column")
                    assertEquals(on, glyph.data132[row][column])
                }
            }
        }
    }
}
