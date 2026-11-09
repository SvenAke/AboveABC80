package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.core.DiskController
import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalResourceApi::class, ExperimentalUnsignedTypes::class)
class ABC800CloudDiskIntegrationTest {
    @Test
    fun basicFindsImportedProgramsAcrossDirectorySectors() = runBlocking {
        val home = Files.createTempDirectory("abc800-cloud-disk").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val lib = NativeLib.getObject()
        lib.freeze()
        try {
            System.setProperty("user.home", home.absolutePath)
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = ComputerModel.ABC800M
            assertTrue(saveLocalDisk("SYSTEM.DSK", Res.readBytes("files/system800.dsk")))
            assertTrue(DiskController.instance.loadFromLocal(0, "SYSTEM.DSK") != null)
            val names = (1..84).map { "TEST${it.toString().padStart(4, '0')}.BAS" } +
                listOf("GROTERA.BAS", "\u00d6GON.BAS")
            val image = Abc80FloppyLayout.importFiles(
                Abc80FloppyLayout("fd4d", 1).create(),
                names.map { name -> name to Abc80CloudDiskImport.prepare(name,
                    "10 PRINT \"CLOUD IMPORT OK\"\n20 END\n".encodeToByteArray()) }
            )
            assertTrue(saveLocalDisk("CLOUD.DSK", image))
            assertTrue(DiskController.instance.loadFromLocal(1, "CLOUD.DSK") != null)
            Assembler.instance.loadRomFiles()
            lib.startEmulator(3_580_000)
            delay(6000)
            for (name in listOf("TEST0001.BAS", "TEST0084.BAS", "GROTERA.BAS", "\u00d6GON.BAS")) {
                for (char in "RUN DR1:$name\r") {
                    lib.sendKey(requireNotNull(abc80CharCode(char)))
                    delay(70)
                }
                val deadline = System.nanoTime() + 10_000_000_000L
                var screen: String
                do {
                    delay(50)
                    screen = decodeABC800MScreen(lib.getAbc800MTextScreen()).joinToString("\n")
                } while (!screen.contains("CLOUD IMPORT OK") &&
                    !screen.contains("Hittar ej") && !screen.contains("Error") &&
                    System.nanoTime() < deadline)
                assertTrue(screen.contains("CLOUD IMPORT OK"), "$name:\n$screen")
                pasteABC80Text(lib, "PRINT CHR$(12)\r")
                delay(500)
            }
        } finally {
            lib.freeze()
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            Assembler.instance.loadRomFiles()
            lib.resetCpu()
            home.deleteRecursively()
        }
    }
}
