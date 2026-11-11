package com.aboveware.abc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.abc80.core.DiskController
import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
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
    fun playEntersDosAndRunsSpaceWithoutBasicRun() =
        checkDosProgram("SPACE.ABS", "DIRECTORY ENTRIES")

    @Test
    fun playEntersDosAndRunsCopyWithoutBasicRun() =
        checkDosProgram("COPY.ABS", "DOSCOPY 6.0")

    @Test
    fun abc800cPlayEntersDosAndRunsCopy() =
        checkDosProgram("COPY.ABS", "DOSCOPY 1.0", ComputerModel.ABC800C)

    @Test
    fun abc800mPlayEntersDosAndRunsCopy() =
        checkDosProgram("COPY.ABS", "DOSCOPY 1.0", ComputerModel.ABC800M)

    private fun checkDosProgram(
        name: String,
        expectedOutput: String,
        model: ComputerModel = ComputerModel.ABC80
    ) = runBlocking {
        val home = Files.createTempDirectory("abc80-absolute-test").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        System.setProperty("user.home", home.absolutePath)
        TerminalManager.computerModel = model
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
            lib.freeze()
            DiskController.instance.closeForComputerModelChange()
            lib.addMemoryReadWatcher(0x00db, basicPromptWatcher)
            Assembler.instance.loadRomFiles()
            val image = Res.readBytes(if (model.isAbc800) "files/system800.dsk" else "files/system.dsk")
            saveLocalDisk("SYSTEM.DSK", image)
            for (drive in 0 until DiskController.MAX_DRIVES) {
                DiskController.instance.loadFromLocal(drive, "SYSTEM.DSK")
            }
            lib.enableLogging(false)
            lib.startEmulator(model.nominalCpuFrequencyHz)
            fun rows() = if (model == ComputerModel.ABC800C) {
                decodeABC800CScreen(lib.getAbc800CTextScreen())
            } else if (model == ComputerModel.ABC800M) {
                decodeABC800MScreen(lib.getAbc800MTextScreen())
            } else decodeABC80Screen(lib.getMemory())
            suspend fun await(message: String, condition: () -> Boolean) {
                val started = TimeSource.Monotonic.markNow()
                while (!condition() && started.elapsedNow().inWholeMilliseconds < 12000) delay(20)
                assertTrue(condition(), "$message:\n${rows().joinToString("\n")}")
            }
            await("BASIC must boot") { rows().any { it.contains("ABC80") } }
            val bootRows = rows().joinToString("\n")
            assertTrue(!bootRows.contains("Hittar inte filen"), bootRows)
            delay(if (model.isAbc800) 6000 else 250)
            try {
                loadABC80DiskFile(lib, 0, name, run = true)
            } catch (e: IllegalStateException) {
                throw AssertionError("${e.message}\n${rows().joinToString("\n")}", e)
            }
            await("$name must run") {
                rows().any { it.contains(expectedOutput) }
            }
            val output = rows().joinToString("\n")
            assertTrue(output.contains("DR0:$name"), output)
            if (name == "SPACE.ABS") assertTrue(output.contains("SECTORS"), output)
            assertContentEquals(image, lib.bus.floppy.snapshot(0))
            if (model.isAbc800) return@runBlocking
            await("DOS must return to its prompt") { abc80DosPromptVisible(lib) }
            delay(250)
            assertTrue(lib.dosActive, "DOS button must remain visible after SPACE")
            pasteABC80Text(lib, "\u00a4BASIC\r")
            await("The return button command must restore BASIC") {
                val screen = rows()
                screen.indexOfLast { it.trim() == if (model.isAbc800) "ABC800" else "ABC80" } >
                    screen.indexOfLast { it.contains("R E A D Y") }
            }
            if (model == ComputerModel.ABC80) {
                await("BASIC prompt must hide the DOS button") { !lib.dosActive }
            }
            assertTrue(!abc80DosPromptVisible(lib))
            pasteABC80Text(lib, "BYE\r")
            await("Manual BYE must be detected") { abc80DosPromptVisible(lib) }
        } finally {
            native?.freeze()
            native?.removeMemoryReadWatcher(0x00db, basicPromptWatcher)
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            Assembler.instance.loadRomFiles()
            native?.resetCpu()
            home.deleteRecursively()
        }
    }
}
