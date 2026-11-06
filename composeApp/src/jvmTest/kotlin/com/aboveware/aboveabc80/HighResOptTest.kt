package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class HighResOptTest {
    @Test
    fun highResolutionOptionPortReadReturnsControlLatch() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val native = NativeLib.getObject()
        native.freeze(true)
        try {
            TerminalManager.computerModel = ComputerModel.ABC800M
            Assembler.instance.loadRomFiles()
            native.resetCpu()
            native.outPort(0x06, 0x80)
            assertEquals(0x80, native.inPort(0x06))
            native.outPort(0x07, 0xAA)
            assertEquals(0xAA, native.inPort(0x07))
        } finally {
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            native.resetCpu()
            native.freeze(false)
        }
    }
}
