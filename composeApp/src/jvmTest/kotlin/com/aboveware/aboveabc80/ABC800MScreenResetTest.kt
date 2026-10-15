package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ABC800MScreenResetTest {
    @Test
    fun restartingClearsTextScreen() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val nativeLib = NativeLib.getObject()
        try {
            TerminalManager.computerModel = ComputerModel.ABC800M
            Assembler.instance.loadRomFiles()
            nativeLib.startEmulator(3_580_000)
            delay(5_000)

            nativeLib.freeze(true)
            assertTrue(
                nativeLib.getAbc800MTextScreen().any { it != ' '.code.toByte() },
                "ABC800M did not display startup text before reset"
            )

            Assembler.instance.restart()
            assertTrue(
                nativeLib.getAbc800MTextScreen().all { it == ' '.code.toByte() },
                "ABC800M restart did not clear the text screen"
            )
        } finally {
            nativeLib.freeze()
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            nativeLib.resetCpu()
            nativeLib.freeze(false)
        }
    }
}
