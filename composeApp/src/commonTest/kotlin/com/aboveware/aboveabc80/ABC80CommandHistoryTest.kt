package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ABC80CommandHistoryTest {
    @Test
    fun recallsCommandsAndRestoresDraftOnlyAtPrompt() {
        val history = ABC80CommandHistory()
        for (line in listOf("LOAD DR1:CHECK.BAS", "RUN")) {
            history.atPrompt = true
            history.submit(line)
        }
        assertNull(history.recall(-1, ""))
        history.atPrompt = true
        assertEquals("RUN", history.recall(-1, "LIST"))
        assertEquals("LOAD DR1:CHECK.BAS", history.recall(-1, "RUN"))
        assertEquals("LOAD DR1:CHECK.BAS", history.recall(-1, "LOAD DR1:CHECK.BAS"))
        assertEquals("RUN", history.recall(1, "LOAD DR1:CHECK.BAS"))
        assertEquals("LIST", history.recall(1, "RUN"))
        assertNull(history.recall(1, "LIST"))
    }

    @Test
    fun capturesPromptTextWithSwedishAbcCodes() {
        val memory = ByteArray(65536)
        "ABC80".encodeToByteArray().copyInto(memory, TKN80.rowAddress(1))
        "LOAD BES\\K.GRF".encodeToByteArray().copyInto(memory, TKN80.rowAddress(2))
        memory[SCREEN_ROW] = 2
        memory[SCREEN_COLUMN] = 13
        assertEquals("LOAD BES\\K.GR", abc80PromptLine(memory))
    }
}
