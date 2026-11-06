package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import java.security.MessageDigest

class DosRomSettingTest {
    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun switchesBetweenFloppyDosAndUfd20Rom() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val previousRom = getPersistedString("abc80_dos_rom", "dos.rom")
        val native = NativeLib.getObject()
        native.freeze(true)
        try {
            TerminalManager.computerModel = ComputerModel.ABC80

            // 1. Test "dos.rom"
            setPersistedString("abc80_dos_rom", "dos.rom")
            Assembler.instance.loadRomFiles()
            val expectedDos = Res.readBytes("files/dos.rom")
            val memoryDos = native.getMemory().copyOfRange(0x6000, 0x6000 + expectedDos.size)
            assertContentEquals(expectedDos, memoryDos)

            // 2. Test "ufdv20.rom"
            setPersistedString("abc80_dos_rom", "ufdv20.rom")
            Assembler.instance.loadRomFiles()
            val expectedUfd20 = Res.readBytes("files/ufdv20.rom")
            val memoryUfd20 = Assembler.instance.memory.copyOfRange(0x6000, 0x6000 + expectedUfd20.size)
            assertContentEquals(expectedUfd20, memoryUfd20)
        } finally {
            setPersistedString("abc80_dos_rom", previousRom)
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            native.resetCpu()
            native.freeze(false)
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun compareDosRoms() = runBlocking {
        val dos = Res.readBytes("files/dos.rom")
        val ufd = Res.readBytes("files/ufdv20.rom")
        assertEquals(4096, dos.size)
        assertEquals(4096, ufd.size)
        assertFalse(dos.contentEquals(ufd))
        assertContentEquals(
            byteArrayOf(
                0xa6.toByte(), 0xb3.toByte(), 0xa9.toByte(), 0x58, 0x77, 0x14, 0xf8.toByte(),
                0xdb.toByte(), 0x80.toByte(), 0x7c, 0x05, 0xbe.toByte(), 0xe6.toByte(), 0xc7.toByte(),
                0x1c, 0x06, 0x84.toByte(), 0x36, 0x37, 0x44
            ),
            MessageDigest.getInstance("SHA-1").digest(dos)
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun listFilesInSystemDsk() = runBlocking {
        val bytes = Res.readBytes("files/system.dsk")
        val files = Abc80FloppyLayout.list(bytes)
        println("System.dsk files: ${files?.map { it.first }}")
    }
}
