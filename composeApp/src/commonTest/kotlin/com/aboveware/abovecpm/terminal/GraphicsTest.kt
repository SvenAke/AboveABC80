package com.aboveware.abovecpm.terminal

import kotlin.test.Test

class GraphicsTest {
    @Test
    fun printAllCharacterSets() {
        val graphics = Graphics()
        val allSets = graphics.characterSets.all()

        allSets.forEach { set ->
            val name = set::class.simpleName ?: "Unknown"
            print("$name: ")

            // Collect all characters in the set
            val charList = mutableListOf<Char>()
            for (i in 0..0xFF) {
                val glyph = set.getGlyph(i.toChar())
                if (glyph != null && glyph.char != '⸮') { // '⸮' is usually the error glyph
                    charList.add(glyph.char)
                }
            }
            println(charList.joinToString(""))
        }
    }
}
