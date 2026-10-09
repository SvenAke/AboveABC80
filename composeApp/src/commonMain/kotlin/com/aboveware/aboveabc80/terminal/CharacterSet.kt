package com.aboveware.aboveabc80.terminal

import com.aboveware.aboveabc80.Abc80MonitorCharacterMap

object CharacterSet {
    @OptIn(ExperimentalUnsignedTypes::class)
    class Glyph(val char: Char, characterMap: Abc80MonitorCharacterMap) {
        val glyphWidth80 = characterMap.charWidth
        val height = characterMap.charHeight
        val data80 = Array(height) { BooleanArray(glyphWidth80) }
        val data132 = Array(height) { BooleanArray(glyphWidth80) }

        init {
            characterMap.forEach(char.code) { bits, row ->
                characterMap.forEachBit(bits.toInt()) { on, column ->
                    data80[row][column] = on
                    data132[row][column] = on
                }
            }
        }
    }

    class ABC80CharacterSet {
        private val characterMap = Abc80MonitorCharacterMap()
        private val glyphs = Array(256) { code -> Glyph(code.toChar(), characterMap) }

        fun getGlyph(char: Char): Glyph? = glyphs.getOrNull(char.code)
    }
}
