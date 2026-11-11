package com.aboveware.abc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
import com.aboveware.abc80.core.DiskController
import java.nio.file.Files
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import org.jetbrains.compose.resources.ExperimentalResourceApi

class ABC800MBootSmokeTest {
    @OptIn(ExperimentalResourceApi::class, ExperimentalUnsignedTypes::class)
    @Test
    fun missingBasicIniUsesDosDefaultsButMountedBasicIniIsLoaded() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val previousHome = System.getProperty("user.home")
        val home = Files.createTempDirectory("abc800-basicini-test").toFile()
        val controller = DiskController.instance
        System.setProperty("user.home", home.absolutePath)
        TerminalManager.computerModel = ComputerModel.ABC800M
        try {
            for (drive in 0 until DiskController.MAX_DRIVES) controller.clearDrive(drive)

            Assembler.instance.loadRomFiles()
            assertContentEquals(
                byteArrayOf(0x3E, 0x01, 0xB7.toByte()),
                Assembler.instance.memory.copyOfRange(0x6C7C, 0x6C7F)
            )

            val image = Abc80FloppyLayout.importFiles(
                Abc80FloppyLayout("abc830", 0).create(),
                listOf("BASICINI.SYS" to ByteArray(253))
            )
            assertTrue(saveLocalDisk("BASICINI.DSK", image))
            assertTrue(controller.loadFromLocal(0, "BASICINI.DSK") != null)

            Assembler.instance.loadRomFiles()
            assertContentEquals(
                byteArrayOf(0xCD.toByte(), 0xAE.toByte(), 0x6D),
                Assembler.instance.memory.copyOfRange(0x6C7C, 0x6C7F)
            )
        } finally {
            for (drive in 0 until DiskController.MAX_DRIVES) controller.clearDrive(drive)
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            Assembler.instance.loadRomFiles()
            home.deleteRecursively()
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun abc800StartupWithBundledDiskDoesNotReportMissingBasicIni() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val previousHome = System.getProperty("user.home")
        val home = Files.createTempDirectory("abc800-boot-test").toFile()
        val controller = DiskController.instance
        val nativeLib = NativeLib.getObject()
        System.setProperty("user.home", home.absolutePath)
        TerminalManager.computerModel = ComputerModel.ABC800M
        try {
            assertTrue(saveLocalDisk("SYSTEM.DSK", Res.readBytes("files/system.dsk")))
            assertTrue(controller.loadFromLocal(0, "SYSTEM.DSK") != null)
            Assembler.instance.loadRomFiles()
            nativeLib.startEmulator(3_580_000)
            delay(2_000)

            val screen = decodeABC800MScreen(nativeLib.getAbc800MTextScreen()).joinToString("\n")
            assertFalse(screen.contains("HITTAR", ignoreCase = true), screen)
        } finally {
            nativeLib.freeze(true)
            for (drive in 0 until DiskController.MAX_DRIVES) controller.clearDrive(drive)
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            Assembler.instance.loadRomFiles()
            nativeLib.resetCpu()
            nativeLib.freeze(false)
            home.deleteRecursively()
        }
    }

    @Test
    fun bootsAndProcessesBasicInput() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val nativeLib = NativeLib.getObject()
        try {
            TerminalManager.computerModel = ComputerModel.ABC800M
            Assembler.instance.loadRomFiles()
            nativeLib.startEmulator(3_580_000)
            delay(5_000)

            val initialText = nativeLib.getAbc800MTextScreen().decodeToString()
            assertTrue(
                initialText.any { it != ' ' },
                "ABC800M firmware did not render its startup screen"
            )

            nativeLib.sendKey('\r'.code)
            delay(1_000)
            "PRINT 1+1\r".forEach {
                nativeLib.sendKey(it.code)
                delay(50)
            }
            delay(2_000)

            val screenText = nativeLib.getAbc800MTextScreen().decodeToString()
            assertTrue(screenText.contains("PRINT"), "ABC800M did not echo keyboard input")
            assertTrue(screenText.contains("2"), "ABC800M BASIC did not evaluate the input")

            nativeLib.freeze(true)
            Assembler.instance.restart()
            assertTrue(
                nativeLib.getAbc800MTextScreen().all { it == ' '.code.toByte() },
                "ABC800M restart did not clear the text screen"
            )
        } finally {
            nativeLib.freeze()
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            nativeLib.resetCpu()
            nativeLib.freeze(false)
        }
    }
}
