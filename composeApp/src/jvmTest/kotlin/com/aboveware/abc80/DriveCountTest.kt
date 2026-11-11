package com.aboveware.abc80

import com.aboveware.abc80.core.DiskController
import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class DriveCountTest {
    @Test
    fun driveCountCanBeConfiguredBetween2And8() = runBlocking {
        val home = Files.createTempDirectory("drive-count-test").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val controller = DiskController.instance
        val previousDrives = controller.maxDrives
        System.setProperty("user.home", home.absolutePath)
        try {
            TerminalManager.computerModel = ComputerModel.ABC80
            for (count in 2..8) {
                controller.setDriveCount(count)
                assertEquals(count, controller.maxDrives)
                assertEquals(count.toString(), getPersistedString("abc80_max_drives"))
            }
            // Test unmounting excess drives when reducing drive count
            val image = Abc80FloppyLayout("abc830", 0).create()
            assertTrue(saveLocalDisk("test.dsk", image))
            controller.setDriveCount(8)
            controller.loadFromLocal(7, "test.dsk")
            assertEquals("test.dsk", controller.mountedDisks[7]?.name)

            controller.setDriveCount(4)
            assertEquals(4, controller.maxDrives)
            assertTrue(!controller.mountedDisks.containsKey(7))
        } finally {
            controller.setDriveCount(previousDrives)
            TerminalManager.computerModel = previousModel
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }
}
