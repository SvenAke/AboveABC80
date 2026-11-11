package com.aboveware.abc80

import com.aboveware.abc80.core.DiskController
import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Assume.assumeTrue

@OptIn(ExperimentalUnsignedTypes::class)
class Ord800BootIntegrationTest {
    @Test
    fun bootUserDisk() = runBlocking {
        val source = System.getenv("ABC800_ORD_TD0")
        assumeTrue("Set ABC800_ORD_TD0 to a local ORD 800 v2.4 TD0 image", source != null)
        val original = java.io.File(requireNotNull(source)).readBytes()
        val home = Files.createTempDirectory("ord800-test").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val lib = NativeLib.getObject()
        var resets = 0
        val watcher = object : NativeLib.MemoryWriteWatcher {
            override fun onWrite(address: Int, data: Short, before: Short, changed: Boolean) {
                if (data.toInt() == 0) resets++
            }
        }
        lib.freeze(true)
        try {
            System.setProperty("user.home", home.absolutePath)
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = ComputerModel.ABC800C
            check(saveLocalDisk("ORD.TD0", original))
            check(DiskController.instance.loadFromLocal(0, "ORD.TD0") != null)
            Assembler.instance.loadRomFiles()
            lib.resetCpu()
            lib.addMemoryWriteWatcher(0xff44, watcher)
            lib.startEmulator(3_580_000)
            lib.freeze(false)
            repeat(2) { boot ->
                if (boot == 1) {
                    lib.freeze(true)
                    DiskController.instance.closeForComputerModelChange()
                    check(DiskController.instance.restoreFromLocal(0, "ORD.TD0") != null)
                    Assembler.instance.loadRomFiles()
                    lib.resetCpu()
                    lib.freeze(false)
                }
                delay(12_000)
                assertEquals(boot + 1, resets, "ORD must not reinitialize BASIC")
                val before = decodeABC800CScreen(lib.getAbc800CTextScreen()).joinToString("\n")
                assertTrue(before.contains("katalog") && before.contains("SKRIV"), before)
                "TEST ORD".forEach { lib.sendKey(it.code); delay(100) }
                delay(1000)
                val after = decodeABC800CScreen(lib.getAbc800CTextScreen()).joinToString("\n")
                println("VERIFIED BOOT $boot\n$after")
                assertTrue(after.contains("TEST ORD"), after)
            }
        } finally {
            lib.freeze(true)
            lib.removeMemoryWriteWatcher(0xff44, watcher)
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
