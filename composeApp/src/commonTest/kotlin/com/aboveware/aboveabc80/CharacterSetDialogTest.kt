package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CharacterSetDialogTest {
    @Test
    fun chartMatchesReferenceColumnOrder() {
        assertEquals(
            listOf(32, 56, 80, 104),
            (0..3).map { characterChartCode(it, 0) }
        )
        assertEquals(
            listOf(55, 79, 103, 127),
            (0..3).map { characterChartCode(it, 23) }
        )
        assertEquals(
            (32..127).toList(),
            (0..3).flatMap { group -> (0..23).map { characterChartCode(group, it) } }
        )
        assertFailsWith<IllegalArgumentException> { characterChartCode(4, 0) }
        assertFailsWith<IllegalArgumentException> { characterChartCode(0, 24) }
    }
}
