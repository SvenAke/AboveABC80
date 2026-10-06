package com.aboveware.aboveabc80.keyboard

import kotlin.test.Test
import kotlin.test.assertEquals

class KeyboardEventTest {
    @Test
    fun characterEventsIncludeRelease() {
        val keyboard = Keyboard()
        val pressed = mutableListOf<Char>()
        val released = mutableListOf<Char>()
        keyboard.onCharacter = { pressed.add(it) }
        keyboard.onCharacterReleased = { released.add(it) }
        keyboard.onKeyEvent('A')
        keyboard.onKeyReleaseEvent('A')
        assertEquals(listOf('A'), pressed)
        assertEquals(listOf('A'), released)
    }

    @Test
    fun encodedKeyUsesOneInputPathAndMatchingRelease() {
        val keyboard = Keyboard()
        val down = mutableListOf<String>()
        val up = mutableListOf<String>()
        val characters = mutableListOf<Char>()
        keyboard.onKeyCodes = { codes, _, _, _, _, _, _ -> down.add(codes) }
        keyboard.onKeyUpCodes = { up.add(it) }
        keyboard.onCharacter = { characters.add(it) }
        keyboard.onKeyDown("65", "A")
        keyboard.onKeyUp("65", "A")
        assertEquals(listOf("65"), down)
        assertEquals(listOf("65"), up)
        assertEquals(emptyList(), characters)
    }
}
