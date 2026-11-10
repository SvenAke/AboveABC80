package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import com.aboveware.aboveabc80.core.DiskController
import java.nio.file.Files
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ABC80VSyncTest {
    @Test
    fun verticalSyncReleasesDisabledHaltAndAllowsSpacePolling() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val lib = NativeLib.getObject()
        lib.freeze()
        try {
            TerminalManager.computerModel = ComputerModel.ABC80
            Assembler.instance.loadRomFiles()
            TKN80.setEnabled(false, lib)
            lib.copyToMemory(0, byteArrayOf(0xc3.toByte(), 0x00, 0xc0.toByte()))
            lib.copyToMemory(0x66, byteArrayOf(0xed.toByte(), 0x45))
            lib.copyToMemory(0xc000, byteArrayOf(
                0x31, 0x00, 0xf0.toByte(), // LD SP,F000h
                0xf3.toByte(), 0x76, // DI; HALT (wait for vertical sync)
                0x3e, 1, 0x32, 0x00, 0xd0.toByte(), // Mark that HALT resumed
                0xdb.toByte(), 0x38, 0xfe.toByte(), 0xa0.toByte(), // IN A,(38h); CP A0h
                0x20, 0xf3.toByte(), // JR NZ back to DI
                0x3e, 2, 0x32, 0x00, 0xd0.toByte(),
                0x18, 0xfe.toByte()
            ))
            lib.copyToMemory(0xd000, byteArrayOf(0))
            lib.startEmulator(3_000_000)
            suspend fun awaitMarker(value: Int) {
                val deadline = System.nanoTime() + 2_000_000_000L
                while ((lib.getMemory()[0xd000].toInt() and 255) != value &&
                    System.nanoTime() < deadline) delay(10)
                assertEquals(value, lib.getMemory()[0xd000].toInt() and 255, lib.getCPUString())
            }
            awaitMarker(1)
            lib.pressKey(0x20)
            try {
                awaitMarker(2)
            } finally {
                lib.releaseKey(0x20)
            }
        } finally {
            lib.freeze()
            lib.releaseAllKeys()
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            lib.resetCpu()
        }
    }

    @Test
    fun abcrezReturnsToBasicOnSpace() = runBlocking {
    val imagePath = System.getenv("ABC80_ABCREZ_DSK") ?: return@runBlocking
    val image = File(imagePath).readBytes()
    val home = Files.createTempDirectory("abc80-abcrez-test").toFile()
    val previousHome = System.getProperty("user.home")
    val previousModel = TerminalManager.computerModel
    val lib = NativeLib.getObject()
    lib.freeze()
    try {
        DiskController.instance.closeForComputerModelChange()
        System.setProperty("user.home", home.absolutePath)
        TerminalManager.computerModel = ComputerModel.ABC80
        Assembler.instance.loadRomFiles()
        TKN80.setEnabled(false, lib)
        check(saveLocalDisk("ABCREZ.DSK", image))
        check(DiskController.instance.loadFromLocal(0, "ABCREZ.DSK") != null)
        lib.startEmulator(3_000_000)
        suspend fun awaitText(text: String) {
            val deadline = System.nanoTime() + 10_000_000_000L
            fun rows() = decodeABC80Screen(lib.getMemory(), false).map { row ->
                row.map { (it.code and 0x7f).toChar() }.joinToString("")
            }
            while (rows().none { it.contains(text) } && System.nanoTime() < deadline) delay(20)
            assertTrue(
                rows().any { it.contains(text) },
                "Waiting for $text:\n${rows().joinToString("\n")}\n${lib.getCPUString()}"
            )
        }
        awaitText("ABC80")
        delay(500)
        pasteABC80Text(lib, "RUN DR0:ABCREZ.BAC\r")
        awaitText("TO QUIT")
        lib.pressKey(0x20)
        try {
            awaitText("ABC80")
        } finally {
            lib.releaseKey(0x20)
            delay(200)
        }
    } finally {
        lib.freeze()
        lib.releaseAllKeys()
        DiskController.instance.closeForComputerModelChange()
        TerminalManager.computerModel = previousModel
        System.setProperty("user.home", previousHome)
        Assembler.instance.loadRomFiles()
        lib.resetCpu()
        home.deleteRecursively()
    }
    }
}
