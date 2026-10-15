package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ABC800MBootSmokeTest {
    @Test
    fun bootsAndProcessesBasicInput() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val nativeLib = NativeLib.getObject()
        try {
            TerminalManager.computerModel = ComputerModel.ABC800M
            Assembler.instance.loadRomFiles()
            nativeLib.startEmulator(3_580_000)
            delay(5_000)

            val initialText = nativeLib.getAbc800MTextScreen().decodeToString()
            assertTrue(
                initialText.any { it != ' ' },
                "ABC800M firmware did not render its startup screen"
            )

            nativeLib.sendKey('\r'.code)
            delay(1_000)
            "PRINT 1+1\r".forEach {
                nativeLib.sendKey(it.code)
                delay(50)
            }
            delay(2_000)

            val screenText = nativeLib.getAbc800MTextScreen().decodeToString()
            assertTrue(screenText.contains("PRINT"), "ABC800M did not echo keyboard input")
            assertTrue(screenText.contains("2"), "ABC800M BASIC did not evaluate the input")

            nativeLib.freeze(true)
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
