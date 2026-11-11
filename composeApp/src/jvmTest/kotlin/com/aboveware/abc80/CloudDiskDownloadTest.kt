package com.aboveware.abc80

import com.aboveware.abc80.core.AbcDiskImage
import com.aboveware.abc80.core.DiskController
import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class CloudDiskDownloadTest {
    @Test
    fun downloadedImageMountsOnRequestedDriveAndPersistsOnlyAfterSuccess() {
        val home = Files.createTempDirectory("cloud-disk-mount").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val controller = DiskController.instance
        System.setProperty("user.home", home.absolutePath)
        try {
            controller.closeForComputerModelChange()
            for (model in ComputerModel.entries) {
                TerminalManager.computerModel = model
                val image = Abc80FloppyLayout("abc830", 0).create()
                assertTrue(saveLocalDisk("downloaded.dsk", image))
                assertTrue(saveLocalDisk("invalid.dsk", byteArrayOf(1, 2, 3)))
                for (drive in 0 until DiskController.MAX_DRIVES) {
                    mountDownloadedCloudDisk("downloaded.dsk", drive)
                    assertEquals(
                        DiskController.MountedDisk("downloaded.dsk", DiskController.Source.LOCAL),
                        controller.mountedDisks[drive]
                    )
                    assertContentEquals(image, NativeLib.getObject().bus.floppy.snapshot(drive))
                    val preference = DiskController.drivePreferenceKey(model, drive)
                    assertEquals("LOCAL:downloaded.dsk", getPersistedString(preference))
                    val failure = assertFailsWith<IllegalStateException> {
                        mountDownloadedCloudDisk("invalid.dsk", drive)
                    }
                    assertTrue(failure.message!!.contains(model.displayName))
                    assertTrue(failure.message!!.contains("DR$drive:"))
                    assertEquals("LOCAL:downloaded.dsk", getPersistedString(preference))
                    assertEquals("downloaded.dsk", controller.mountedDisks[drive]?.name)
                    assertContentEquals(image, NativeLib.getObject().bus.floppy.snapshot(drive))
                }
                controller.closeForComputerModelChange()
            }
        } finally {
            controller.closeForComputerModelChange()
            System.setProperty("user.home", previousHome)
            TerminalManager.computerModel = previousModel
            home.deleteRecursively()
        }
    }

    @Test
    fun diskImagesUseTheDiskLibraryRegardlessOfCloudFolder() {
        for (path in listOf("abc80/", "abc80/programs/", "abc800c/disks/", "abc80/cassettes/")) {
            assertEquals("disks", cloudDownloadFolder(path, null, "Pascal.DSK"))
            assertEquals("disks", cloudDownloadFolder(path, "tapes", "Pascal.dsk"))
        }
        assertNull(cloudDownloadFolder("abc80/programs/", null, "HELLO.BAS"))
        assertEquals("tapes", cloudDownloadFolder("abc80/cassettes/", null, "tape.wav"))
        assertEquals("tapes", cloudDownloadFolder("abc80/", "tapes", "tape.wav"))
    }

    @Test
    fun downloadedDiskIsByteExactAndVisibleInTheLocalLibrary() = runBlocking {
        val home = Files.createTempDirectory("cloud-disk-download").toFile()
        val previousHome = System.getProperty("user.home")
        val image = Abc80FloppyLayout("abc830", 0).create()
        val existing = Abc80FloppyLayout("fd2", 0).create()
        val client = HttpClient(MockEngine { respond(image) })
        try {
            System.setProperty("user.home", home.absolutePath)
            assertTrue(saveLocalDisk("existing.dsk", existing))
            val folder = assertNotNull(cloudDownloadFolder("abc80/programs/", null, "PASCAL.dsk"))
            FirebaseRestStorageProvider("test", client).downloadToFile(
                "abc80/programs/PASCAL.dsk",
                getLocalFileDirectory(folder).resolve("PASCAL.dsk"),
                image.size.toLong()
            )
            assertEquals(setOf("existing.dsk", "PASCAL.dsk"), listLocalDisks().toSet())
            val saved = assertNotNull(loadLocalDisk("PASCAL.dsk"))
            assertContentEquals(image, saved)
            assertContentEquals(existing, loadLocalDisk("existing.dsk"))
            assertTrue(AbcDiskImage.createFromData(saved).listFiles().isEmpty())
            assertEquals(163840, saved.size)
        } finally {
            client.close()
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }
}
