package com.aboveware.abovecpm.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CommandHistoryTest {
    @Test
    fun recordsSubmittedLinesAndNavigatesUpAndDown() {
        val history = CommandHistory()
        "first\rsecond\r".forEach(history::record)

        val sent = StringBuilder()
        assertTrue(history.navigate(-1) { sent.append(it) })
        assertEquals("second", sent.toString())

        sent.clear()
        assertTrue(history.navigate(-1) { sent.append(it) })
        assertEquals("\b".repeat(6) + "first", sent.toString())

        sent.clear()
        assertTrue(history.navigate(1) { sent.append(it) })
        assertEquals("\b".repeat(5) + "second", sent.toString())

        sent.clear()
        assertTrue(history.navigate(1) { sent.append(it) })
        assertEquals("\b".repeat(6), sent.toString())
        assertFalse(history.navigate(1) { sent.append(it) })
    }

    @Test
    fun preservesDraftAndTracksEditedInput() {
        val history = CommandHistory()
        "stored\rpartial".forEach(history::record)

        val sent = StringBuilder()
        assertTrue(history.navigate(-1) { sent.append(it) })
        assertEquals("\b".repeat(7) + "stored", sent.toString())

        sent.clear()
        assertTrue(history.navigate(1) { sent.append(it) })
        assertEquals("\b".repeat(6) + "partial", sent.toString())
    }

    @Test
    fun ignoresControlSequencesAndAppliesBackspaceBeforeSaving() {
        val history = CommandHistory()
        "abc\u007Fd\u001B[A\r".forEach(history::record)

        val sent = StringBuilder()
        assertTrue(history.navigate(-1) { sent.append(it) })
        assertEquals("abd", sent.toString())
    }
}
