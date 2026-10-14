package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import com.aboveware.aboveabc80.core.DiskController
import java.io.ByteArrayInputStream
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ComputerModelStorageTest {
    @Test
    fun driveMountPreferencesAreIsolatedByComputerModel() {
        assertEquals("drive_ABC80_0", DiskController.drivePreferenceKey(ComputerModel.ABC80, 0))
        assertEquals("drive_ABC800M_0", DiskController.drivePreferenceKey(ComputerModel.ABC800M, 0))
        assertEquals("drive_ABC800C_0", DiskController.drivePreferenceKey(ComputerModel.ABC800C, 0))
        assertEquals("drive_ABC800M_1", DiskController.drivePreferenceKey(ComputerModel.ABC800M, 1))
    }

    @Test
    fun diskImagesAndSavedTapesAreIsolatedByComputerModel() {
        val home = Files.createTempDirectory("computer-model-storage").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val abc80Disk = byteArrayOf(1, 2)
        val abc800mDisk = byteArrayOf(3, 4)
        val abc800cDisk = byteArrayOf(5, 6)
        val abc80Tape = byteArrayOf(7, 8)
        val abc800mTape = byteArrayOf(9, 10)
        try {
            System.setProperty("user.home", home.absolutePath)

            TerminalManager.computerModel = ComputerModel.ABC80
            assertTrue(saveLocalDisk("same.dsk", abc80Disk))
            assertTrue(saveLocalFile("tapes", "same.tap", abc80Tape))

            TerminalManager.computerModel = ComputerModel.ABC800M
            assertTrue(saveLocalDisk("same.dsk", abc800mDisk))
            assertTrue(saveLocalFile("tapes", "same.tap", abc800mTape))
            assertEquals(listOf("same.dsk"), listLocalDisks())
            assertContentEquals(abc800mDisk, loadLocalDisk("same.dsk"))
            assertContentEquals(abc800mTape, loadLocalFile("tapes", "same.tap"))

            TerminalManager.computerModel = ComputerModel.ABC800C
            assertTrue(saveLocalDisk("same.dsk", abc800cDisk))
            assertEquals(listOf("same.dsk"), listLocalDisks())
            assertContentEquals(abc800cDisk, loadLocalDisk("same.dsk"))
            assertNull(loadLocalFile("tapes", "same.tap"))

            val storageRoot = home.resolve(".aboveabc80")
            assertContentEquals(
                abc80Disk,
                storageRoot.resolve("abc80/disks/same.dsk").readBytes()
            )
            assertContentEquals(
                abc80Tape,
                storageRoot.resolve("abc80/tapes/same.tap").readBytes()
            )
            assertContentEquals(
                abc800mDisk,
                storageRoot.resolve("abc800m/disks/same.dsk").readBytes()
            )
            assertContentEquals(
                abc800mTape,
                storageRoot.resolve("abc800m/tapes/same.tap").readBytes()
            )
            assertContentEquals(
                abc800cDisk,
                storageRoot.resolve("abc800c/disks/same.dsk").readBytes()
            )
        } finally {
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }

    @Test
    fun existingAbc80StorageIsMigratedIntoTheModelDirectory() {
        val home = Files.createTempDirectory("legacy-model-storage").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val disk = byteArrayOf(11, 12)
        val tape = byteArrayOf(13, 14)
        val cassette = byteArrayOf(15, 16)
        val driveImage = byteArrayOf(17, 18)
        try {
            System.setProperty("user.home", home.absolutePath)
            TerminalManager.computerModel = ComputerModel.ABC80
            val legacyRoot = home.resolve(".aboveabc80")
            legacyRoot.resolve("disks").mkdirs()
            legacyRoot.resolve("disks/legacy.dsk").writeBytes(disk)
            legacyRoot.resolve("tapes").mkdirs()
            legacyRoot.resolve("tapes/legacy.tap").writeBytes(tape)
            legacyRoot.resolve("cassette").mkdirs()
            legacyRoot.resolve("cassette/current.tape").writeBytes(cassette)
            legacyRoot.resolve("dr").mkdirs()
            legacyRoot.resolve("dr/0.dsk").writeBytes(driveImage)

            assertContentEquals(disk, loadLocalDisk("legacy.dsk"))
            assertContentEquals(tape, loadLocalFile("tapes", "legacy.tap"))
            assertContentEquals(cassette, loadLocalFile("cassette", "current.tape"))
            assertContentEquals(
                driveImage,
                getComputerModelStorageDir("dr").resolve("0.dsk").readBytes()
            )
            assertContentEquals(disk, legacyRoot.resolve("abc80/disks/legacy.dsk").readBytes())
            assertContentEquals(tape, legacyRoot.resolve("abc80/tapes/legacy.tap").readBytes())
            assertContentEquals(
                cassette,
                legacyRoot.resolve("abc80/cassette/current.tape").readBytes()
            )
            assertContentEquals(
                driveImage,
                legacyRoot.resolve("abc80/dr/0.dsk").readBytes()
            )
        } finally {
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }

    @Test
    fun currentCassetteFollowsTheSelectedComputerModel() {
        val home = Files.createTempDirectory("model-cassette-storage").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val abc80Tape = byteArrayOf(21, 22, 23)
        val abc800mTape = byteArrayOf(24, 25, 26)
        try {
            System.setProperty("user.home", home.absolutePath)
            TerminalManager.computerModel = ComputerModel.ABC80
            Abc80Cassette.eject()
            assertTrue(Abc80Cassette.import(ByteArrayInputStream(abc80Tape), "abc80.tap"))

            TerminalManager.computerModel = ComputerModel.ABC800M
            assertContentEquals(byteArrayOf(), Abc80Cassette.exportBytes())
            assertTrue(Abc80Cassette.import(ByteArrayInputStream(abc800mTape), "abc800m.tap"))
            assertContentEquals(abc800mTape, Abc80Cassette.exportBytes())

            TerminalManager.computerModel = ComputerModel.ABC80
            assertContentEquals(abc80Tape, Abc80Cassette.exportBytes())
            assertContentEquals(
                abc80Tape,
                home.resolve(".aboveabc80/abc80/cassette/current.tape")
                    .readBytes()
                    .copyOfRange(8, 11)
            )
            assertContentEquals(
                abc800mTape,
                home.resolve(".aboveabc80/abc800m/cassette/current.tape")
                    .readBytes()
                    .copyOfRange(8, 11)
            )
        } finally {
            Abc80Cassette.eject()
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }
}
