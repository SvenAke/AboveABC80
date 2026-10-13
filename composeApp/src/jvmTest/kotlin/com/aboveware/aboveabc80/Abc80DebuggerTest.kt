package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.core.Abc80Debugger
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class Abc80DebuggerTest {
    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun readsAbc80RomListingWithCorrectAddressesAndInstructions() = runBlocking {
        val lines = Abc80Debugger.parseListing(Res.readBytes("files/disassembler.txt").decodeToString())
        assertTrue(lines.size > 1000)
        assertEquals(0, lines.first().address)
        assertEquals("18 66", lines.first().hex)
        assertTrue(lines.first().code.contains("JR START"))
        assertTrue(lines.any { it.address == 0x00DB })
        val rom = Res.readBytes("files/prom.rom")
        lines.filter { it.address < rom.size }.take(100).forEach { line ->
            assertEquals(rom[line.address].toInt() and 255, line.hex.substringBefore(' ').toInt(16))
        }
        assertFailsWith<IllegalArgumentException> { Abc80Debugger.parseListing("not a listing") }
        Unit
    }
}
