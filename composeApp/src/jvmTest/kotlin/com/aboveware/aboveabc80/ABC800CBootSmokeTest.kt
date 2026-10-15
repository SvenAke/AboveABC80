package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ABC800CBootSmokeTest {
    @Test
    fun bootsIntoFortyColumnScreenAndReportsInvalidBasicInput() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val nativeLib = NativeLib.getObject()
        try {
            TerminalManager.computerModel = ComputerModel.ABC800C
            Assembler.instance.loadRomFiles()
            nativeLib.startEmulator(3_580_000)
            delay(5_000)

            val initialScreen = nativeLib.getAbc800CTextScreen()
            assertEquals(40 * 24, initialScreen.size)
            val initialRows = initialScreen.decodeToString().chunked(40)
            assertTrue(
                initialRows[0].startsWith("ABC800"),
                "ABC800C firmware did not render its banner at the top left:\n${initialRows.joinToString("\n")}"
            )

            "123123".forEach {
                nativeLib.sendKey(it.code)
                delay(50)
            }
            delay(500)
            val numericRows = nativeLib.getAbc800CTextScreen().decodeToString().chunked(40)
            assertTrue(
                numericRows[1].startsWith("123123"),
                "ABC800C did not place input below the banner at column zero:\n${numericRows.joinToString("\n")}"
            )
            repeat(6) {
                nativeLib.sendKey(0x08)
                delay(50)
            }
            "qweqwe\r".forEach {
                nativeLib.sendKey(it.code)
                delay(50)
            }
            delay(2_000)
            val inputRows = nativeLib.getAbc800CTextScreen().decodeToString().chunked(40)
            val commandRow = inputRows.indexOfFirst { it.contains("qweqwe") }
            assertEquals(1, commandRow, "ABC800C moved the input away from the row below the banner")
            assertTrue(inputRows[commandRow].startsWith("qweqwe"))
            assertTrue(
                inputRows.sumOf { Regex("qweqwe").findAll(it).count() } == 1,
                "ABC800C echoed the input more than once:\n${inputRows.joinToString("\n")}"
            )
            assertTrue(
                inputRows[commandRow + 1].startsWith("Error"),
                "ABC800C did not show a BASIC error after Return:\n${inputRows.joinToString("\n")}"
            )
        } finally {
            nativeLib.freeze(true)
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            nativeLib.resetCpu()
            nativeLib.freeze(false)
        }
    }
}
