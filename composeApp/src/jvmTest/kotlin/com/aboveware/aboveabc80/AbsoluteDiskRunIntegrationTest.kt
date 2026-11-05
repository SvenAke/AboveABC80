package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

@OptIn(ExperimentalUnsignedTypes::class)
class AbsoluteDiskRunIntegrationTest {
    @Test
    fun playEntersDosAndRunsSpaceWithoutBasicRun() = runBlocking {
        val home = Files.createTempDirectory("abc80-absolute-test").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        System.setProperty("user.home", home.absolutePath)
        TerminalManager.computerModel = ComputerModel.ABC80
        var native: NativeLib? = null
        val basicPromptWatcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                native?.dosActive = false
                return false
            }
        }
        try {
            home.resolve(".aboveabc80").resolve("dr").mkdirs()
            val lib = NativeLib.getObject()
            native = lib
            lib.addMemoryReadWatcher(0x00db, basicPromptWatcher)
            val memory = ByteArray(0x10000)
            Res.readBytes("files/prom.rom").copyInto(memory)
            Res.readBytes("files/dos.rom").copyInto(memory, 0x6000)
            Res.readBytes("files/printer.rom").copyInto(memory, 0x7800)
            lib.copyToMemory(0, memory)
            val image = Res.readBytes("files/system.dsk")
            saveLocalDisk("SYSTEM.DSK", image)
            for (drive in 0 until DiskController.MAX_DRIVES) {
                DiskController.instance.loadFromLocal(drive, "SYSTEM.DSK")
            }
            lib.enableLogging(false)
            lib.startEmulator(3_000_000)
            suspend fun await(message: String, condition: () -> Boolean) {
                val started = TimeSource.Monotonic.markNow()
                while (!condition() && started.elapsedNow().inWholeMilliseconds < 12000) delay(20)
                assertTrue(condition(), "$message:\n${decodeABC80Screen(lib.getMemory()).joinToString("\n")}")
            }
            await("BASIC must boot") { decodeABC80Screen(lib.getMemory()).any { it.contains("ABC80") } }
            val bootRows = decodeABC80Screen(lib.getMemory()).joinToString("\n")
            assertTrue(!bootRows.contains("Hittar inte filen"), bootRows)
            delay(250)
            loadABC80DiskFile(lib, 0, "SPACE.ABS", run = true)
            await("SPACE must show free space") {
                decodeABC80Screen(lib.getMemory()).any { it.contains("DIRECTORY ENTRIES") }
            }
            val output = decodeABC80Screen(lib.getMemory()).joinToString("\n")
            assertTrue(output.contains("DR0:SPACE.ABS"), output)
            assertTrue(output.contains("SECTORS"), output)
            assertContentEquals(image, lib.bus.floppy.snapshot(0))
            await("DOS must return to its prompt") { abc80DosPromptVisible(lib.getMemory()) }
            delay(250)
            assertTrue(lib.dosActive, "DOS button must remain visible after SPACE")
            pasteABC80Text(lib, "\u00a4BASIC\r")
            await("The return button command must restore BASIC") {
                val rows = decodeABC80Screen(lib.getMemory())
                rows.indexOfLast { it.trim() == "ABC80" } >
                    rows.indexOfLast { it.contains("R E A D Y") }
            }
            await("BASIC prompt must hide the DOS button") { !lib.dosActive }
            assertTrue(!abc80DosPromptVisible(lib.getMemory()))
            pasteABC80Text(lib, "BYE\r")
            await("Manual BYE must be detected") { abc80DosPromptVisible(lib.getMemory()) }
        } finally {
            native?.freeze()
            native?.removeMemoryReadWatcher(0x00db, basicPromptWatcher)
            for (drive in 0 until DiskController.MAX_DRIVES) DiskController.instance.clearDrive(drive)
            native?.bus?.floppy?.disks?.close()
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }
}
