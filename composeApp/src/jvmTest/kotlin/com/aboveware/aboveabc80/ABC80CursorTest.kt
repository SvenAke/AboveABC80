package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ABC80CursorTest {
    @Test
    fun visibilityDoesNotChangeCursorPositionOrScreenMemory() {
        val memory = ByteArray(0x10000).apply {
            this[SCREEN_ROW] = 3
            this[SCREEN_COLUMN] = 12
        }
        assertEquals(3 to 12, decodeABC80Cursor(memory, 40))
        assertNull(decodeABC80Cursor(memory, 40, visible = false))
        assertEquals(3 to 12, decodeABC80Cursor(memory, 40, visible = true))
    }

    @Test
    fun cursorOutsideVisibleRowsOrColumnsIsHidden() {
        val memory = ByteArray(0x10000)
        memory[SCREEN_ROW] = 24
        assertNull(decodeABC80Cursor(memory, 40))
        memory[SCREEN_ROW] = 0
        memory[SCREEN_COLUMN] = 40
        assertNull(decodeABC80Cursor(memory, 40))
        assertEquals(0 to 40, decodeABC80Cursor(memory, 80))
    }
}
