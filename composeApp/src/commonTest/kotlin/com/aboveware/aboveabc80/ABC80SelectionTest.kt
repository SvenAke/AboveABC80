package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ABC80SelectionTest {
    @Test
    fun forwardAndReverseSelectionIncludeBothEndpoints() {
        val selection = ABC80Selection()
        assertNull(selection.range)
        selection.start(10)
        selection.extend(3)
        assertEquals(3..10, selection.range)
        selection.start(3)
        selection.extend(10)
        assertEquals(3..10, selection.range)
        selection.clear()
        assertNull(selection.range)
    }

    @Test
    fun copyUsesUnicodeAndRemovesOnlyTrailingRowPadding() {
        val rows = listOf("ABC   ", "][\\}{|", " END  ")
        assertEquals("BC\nÅÄÖåäö\n EN", selectedABC80Text(rows, 1..14))
        assertEquals("C", selectedABC80Text(rows, 2..3))
    }

    @Test
    fun pasteConvertsSwedishTextAndAllLineEndingsBeforeSending() {
        assertEquals(listOf(93, 91, 92, 125, 123, 124, 13, 65, 13, 66, 13),
            abc80PasteCodes("ÅÄÖåäö\r\nA\nB\r"))
        assertFailsWith<IllegalArgumentException> { abc80PasteCodes("ABC€") }
    }
}
