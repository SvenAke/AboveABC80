package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SystemDiskTemplateTest {
    @Test
    fun abc800NewDisksAreLimitedTo320KbButAbc80Keeps640Kb() {
        for (model in listOf(ComputerModel.ABC800M, ComputerModel.ABC800C)) {
            assertEquals(listOf("fd2", "abc830", "fd4d"), newDiskFormatNames(model))
        }
        assertEquals(listOf("fd2", "abc830", "fd4d", "abc832"), newDiskFormatNames(ComputerModel.ABC80))
    }

    @Test
    fun abc800BlankDiskCreationRejects640Kb() = runBlocking {
        val previousModel = TerminalManager.computerModel
        try {
            for (model in listOf(ComputerModel.ABC800M, ComputerModel.ABC800C)) {
                TerminalManager.computerModel = model
                assertFailsWith<IllegalArgumentException> { createBlankDisk("abc832", "TEST") }
            }
        } finally {
            TerminalManager.computerModel = previousModel
        }
    }
}
