package com.aboveware.aboveabc80.terminal

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VT320ComposeTest {
    private lateinit var compose: VT320Compose
    private val context = VT320Compose.Context(
        isDecMultinational = true,
        isNRC = false,
        keyboardLanguage = 0,
        isDataProcessing = false
    )

    @BeforeTest
    fun setup() {
        compose = VT320Compose()
    }

    @Test
    fun testBasicComposition() {
        compose.start()
        assertTrue(compose.isActive)

        // Compose + ' + e -> é
        assertNull(compose.processChar('\'', context))
        assertEquals('é', compose.processChar('e', context))
        assertFalse(compose.isActive)
    }

    @Test
    fun testCapitalComposition() {
        compose.start()
        compose.processChar('\'', context)
        assertEquals('É', compose.processChar('E', context))
    }

    @Test
    fun testSymmetricComposition() {
        // Compose + e + ' -> é (order shouldn't matter)
        compose.start()
        compose.processChar('e', context)
        assertEquals('é', compose.processChar('\'', context))
    }

    @Test
    fun testInvalidSequence() {
        compose.start()
        compose.processChar('x', context)
        assertEquals(
            '\u0000',
            compose.processChar('y', context),
            "Invalid sequence should return null char"
        )
        assertFalse(compose.isActive)
    }

    @Test
    fun testCancel() {
        compose.start()
        compose.processChar('\'', context)
        compose.cancel()
        assertFalse(compose.isActive)
        assertNull(compose.processChar('e', context)) // Should do nothing if not active
    }

    @Test
    fun testVariousSequences() {
        val testCases = mapOf(
            Pair('"', 'a') to 'ä',
            Pair('^', 'o') to 'ô',
            Pair('~', 'n') to 'ñ',
            Pair(',', 'c') to 'ç',
            Pair('o', 'a') to 'å',
            Pair('!', '!') to '¡',
            Pair('?', '?') to '¿',
            Pair('-', 'L') to '£',
            Pair('1', '2') to '½'
        )

        testCases.forEach { (input, expected) ->
            compose.start()
            compose.processChar(input.first, context)
            assertEquals(expected, compose.processChar(input.second, context), "Failed for $input")
        }
    }
}
