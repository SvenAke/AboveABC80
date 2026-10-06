package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun lettersAndShiftKeepTheirAbc80Mapping() {
        assertEquals(65, abc80CharCode('a'))
        assertEquals(97, abc80CharCode('A', shift = true))
        assertEquals(0x5D, abc80CharCode('Å'))
        assertEquals(0x5B, abc80CharCode('Ä'))
        assertEquals(0x5C, abc80CharCode('Ö'))
    }
}
