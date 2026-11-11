package com.aboveware.abc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.abc80.core.DiskController
import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalResourceApi::class, ExperimentalUnsignedTypes::class)
class ABC800CDiskTest {
    @Test
    fun basicReadsMountedDriveZero() = runBlocking {
        val home = Files.createTempDirectory("abc800c-disk-test").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        System.setProperty("user.home", home.absolutePath)
        val lib = NativeLib.getObject()
        try {
            lib.freeze(true)
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = ComputerModel.ABC800C
            Assembler.instance.loadRomFiles()
            assertTrue(lib.bus.floppy.mount(0, Res.readBytes("files/system.dsk"), "SYSTEM.DSK"))
            lib.startEmulator(3_580_000)
            lib.freeze(false)
            delay(5_000)
            val before = DiskController.instance.lastAccessTime
            "LOAD DR0:COPY.ABS\r".forEach {
                lib.sendKey(it.code)
                delay(50)
            }
            delay(5_000)
            val rows = decodeABC800CScreen(lib.getAbc800CTextScreen())
            assertTrue(
                DiskController.instance.lastAccessTime > before,
                "ABC800C did not read DR0:\n${rows.joinToString("\n")}"
            )
            assertTrue(rows.none { it.contains("Error") }, rows.joinToString("\n"))
        } finally {
            lib.freeze(true)
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            Assembler.instance.loadRomFiles()
            lib.resetCpu()
            lib.freeze(false)
            home.deleteRecursively()
        }
    }
}
