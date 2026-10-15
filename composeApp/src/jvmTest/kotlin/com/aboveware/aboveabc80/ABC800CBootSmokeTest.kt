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
            "ddd".forEach {
                nativeLib.sendKey(it.code)
                delay(50)
            }
            suspend fun cursorPhases(samples: Int): Set<Int> {
                val cursorCodes = mutableSetOf<Int>()
                repeat(samples) {
                    val screen = nativeLib.getAbc800CTextScreen()
                    val rows = decodeABC800CScreen(screen)
                    val row = rows.indexOfFirst { it.startsWith("ddd") }
                    assertTrue(row >= 0, "ABC800C did not echo ddd")
                    assertEquals("ddd", rows[row].take(3))
                    cursorCodes.add(screen[row * 40 + 3].toInt() and 0xff)
                    delay(50)
                }
                return cursorCodes
            }
            assertEquals(setOf(0x20, 0xa0), cursorPhases(60), "ABC800C cursor did not initially blink")
            // C-12 deliberately stops blinking after its idle blink counter expires.
            delay(12_000)
            assertEquals(setOf(0xa0), cursorPhases(40), "ABC800C did not preserve the ROM's steady idle cursor")
        } finally {
            nativeLib.freeze(true)
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            nativeLib.resetCpu()
            nativeLib.freeze(false)
        }
    }
}
