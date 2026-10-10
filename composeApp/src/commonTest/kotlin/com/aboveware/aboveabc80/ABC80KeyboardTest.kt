package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import androidx.compose.ui.input.key.Key
import com.aboveware.aboveabc80.terminal.ABC80

class ABC80KeyboardTest {
    @Test
    fun controlCharactersRetainTheirKeyboardCodes() {
        assertEquals(3, abc80CharCode('\u0003'))
        assertEquals(9, abc80CharCode('\t'))
        assertEquals(27, abc80CharCode('\u001B'))
        assertEquals(127, abc80CharCode('\u007F'))
        assertEquals(13, abc80CharCode('\n'))
    }

    @Test
    fun pastePreservesLetterCaseAndSwedishCodes() {
        assertEquals(97, abc80CharCode('a'))
        assertEquals(65, abc80CharCode('A', shift = true))
        assertEquals(0x5D, abc80CharCode('Å'))
        assertEquals(0x5B, abc80CharCode('Ä'))
        assertEquals(0x5C, abc80CharCode('Ö'))
    }

    @Test
    fun codeTableMatchesManualWithCorrectedE() {
        val expected = """
            1 31 21 31 21
            2 32 22 32 22
            3 33 23 33 23
            4 34 24 34 24
            5 35 25 35 25
            6 36 26 36 26
            7 37 2F 37 2F
            8 38 28 38 28
            9 39 29 39 29
            10 30 3D 30 3D
            11 2B 3F 2B 3F
            12 60 40 00 00
            13 3C 7F 7F 7F
            20 71 51 11 11
            21 77 57 17 17
            22 65 45 05 05
            23 72 52 12 12
            24 74 54 14 14
            25 79 59 19 19
            26 75 55 15 15
            27 69 49 09 09
            28 6F 4F 0F 1F
            29 70 50 10 00
            30 7D 5D 1D 1D
            31 7E 5E 1E 1E
            32 0D 0D 0D 0D
            38 61 41 01 01
            39 73 53 13 13
            40 64 44 04 04
            41 66 46 06 06
            42 67 47 07 07
            43 68 48 08 08
            44 6A 4A 0A 0A
            45 6B 4B 0B 1B
            46 6C 4C 0C 0C
            47 7C 5C 1C 1C
            48 7B 5B 1B 1B
            49 27 2A 27 2A
            50 08 08 08 08
            56 7A 5A 1A 1A
            57 78 58 18 18
            58 63 43 03 03
            59 76 56 16 16
            60 62 42 02 02
            61 6E 4E 0E 1E
            62 6D 4D 0D 1D
            63 2C 3B 2C 3B
            64 2E 3A 2E 3A
            65 2D 5F 1F 5F
            66 09 09 1F 1F
            67 09 09 09 09
            74 20 20 20 20
        """.trimIndent().lines().associate { line ->
            val fields = line.split(" ")
            fields[0].toInt() to fields.drop(1).map { it.toInt(16) }
        }
        assertEquals(expected.keys, abc80KeyTable.keys)
        for ((number, codes) in expected) {
            assertEquals(codes[0], abc80TableCode(number, false, false))
            assertEquals(codes[1], abc80TableCode(number, true, false))
            assertEquals(codes[2], abc80TableCode(number, false, true))
            assertEquals(codes[3], abc80TableCode(number, true, true))
        }
    }

    @Test
    fun physicalKeysUseTheFourHardwareModes() {
        assertEquals(0x61, abc80KeyCode(Key.A, 'a', false))
        assertEquals(0x41, abc80KeyCode(Key.A, 'A', true))
        assertEquals(0x01, abc80KeyCode(Key.A, '\u0001', false, true))
        assertEquals(0x01, abc80KeyCode(Key.A, '\u0001', true, true))
        assertEquals(0x1F, abc80KeyCode(Key.O, 'O', true, true))
        assertEquals(0x00, abc80KeyCode(Key.P, 'P', true, true))
        assertEquals(0x1B, abc80KeyCode(Key.K, 'K', true, true))
        assertEquals(0x1E, abc80KeyCode(Key.N, 'N', true, true))
        assertEquals(0x1D, abc80KeyCode(Key.M, 'M', true, true))
        assertEquals(0x16, abc80KeyCode(Key.V, '\u0016', false, true))
        assertEquals(0x00, abc80KeyCode(Key.Unknown, 'É', false, true))
        assertEquals(0x1F, abc80KeyCode(Key.Tab, '\t', false, true))
    }

    @Test
    fun screenKeyboardDeliversEveryTableEntryWithoutRecodingControlBytes() {
        val terminal = ABC80()
        val sent = mutableListOf<Int>()
        terminal.onKeyInput = { sent.add(it.code) }
        val keyboard = terminal.keyboard
        for ((number, codes) in abc80KeyTable) {
            for ((shift, control, expected) in listOf(
                Triple(false, false, codes.unshifted),
                Triple(true, false, codes.shifted),
                Triple(false, true, codes.controlled),
                Triple(true, true, codes.shiftControlled)
            )) {
                if (shift) keyboard.handleKeyEvent("0,0")
                if (control) keyboard.handleKeyEvent("7,1")
                keyboard.handleKeyEvent("abc80:$number")
                assertEquals(expected, sent.last(), "key=$number shift=$shift control=$control")
                keyboard.handleKeyRelease("abc80:$number")
                keyboard.handleKeyRelease("0,0")
                keyboard.handleKeyRelease("7,1")
            }
        }
        assertEquals(abc80KeyTable.size * 4, sent.size)
        keyboard.setLed(1, true)
        keyboard.handleKeyEvent("abc80:38")
        assertEquals(0x41, sent.last())
        keyboard.setLed(1, false)
        keyboard.handleKeyEvent("abc80:38")
        assertEquals(0x61, sent.last())
    }
}
