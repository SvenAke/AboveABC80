package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.core.DiskController
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalResourceApi::class)
class FloppyLoadIntegrationTest {
    @Test
    fun mountingWhileDiskIsReadPreservesEntireImage() {
        val home = Files.createTempDirectory("abc80-floppy-mount-test").toFile()
        val previousHome = System.getProperty("user.home")
        System.setProperty("user.home", home.absolutePath)
        val floppy = Abc80Floppy()
        val running = AtomicBoolean(true)
        val readFailure = AtomicReference<Throwable?>(null)
        val reader = Thread {
            while (running.get()) {
                floppy.onWrite(2u, 0u, 0u)
                for (value in listOf(1, 0, 0, 0)) floppy.onWrite(0u, value.toUByte(), 0u)
                repeat(256) { floppy.onRead(0u) }
            }
        }
        reader.setUncaughtExceptionHandler { _, failure -> readFailure.set(failure) }
        try {
            val image = ByteArray(655360) { (it * 17).toByte() }
            reader.start()
            repeat(20) { assertTrue(floppy.mount(0, image, "TEST.DSK")) }
            running.set(false)
            reader.join(5000)
            assertFalse(reader.isAlive)
            assertNull(readFailure.get(), "Mounting must not close a file while a sector is being read")
            assertContentEquals(image, home.resolve(".aboveabc80/dr/0.dsk").readBytes())
        } finally {
            running.set(false)
            reader.join(5000)
            floppy.disks.close()
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }

    @Test
    fun basicLoadsFromBothMountedDisksAndRejectsInvalidImages() = runBlocking {
        val home = Files.createTempDirectory("abc80-floppy-test").toFile()
        val previousHome = System.getProperty("user.home")
        System.setProperty("user.home", home.absolutePath)
        var native: NativeLib? = null
        try {
            val lib = NativeLib.getObject()
            native = lib
            val memory = ByteArray(0x10000)
            Res.readBytes("files/prom.rom").copyInto(memory)
            Res.readBytes("files/dos.rom").copyInto(memory, 0x6000)
            Res.readBytes("files/printer.rom").copyInto(memory, 0x7800)
            lib.copyToMemory(0, memory)
            val image = Res.readBytes("files/system.dsk")
            lib.enableLogging(false)
            lib.startEmulator(3_000_000)
            Thread.sleep(1000)
            val controller = DiskController.instance
            assertTrue(saveLocalDisk("SYSTEM.DSK", image))
            assertTrue(saveLocalDisk("SECOND.DSK", image))
            assertEquals("SYSTEM.DSK", controller.loadFromLocal(0, "SYSTEM.DSK"))
            assertEquals("SECOND.DSK", controller.loadFromLocal(1, "SECOND.DSK"))
            val mounted = lib.bus.floppy.disks[0]
            assertTrue(saveLocalDisk("INVALID.DSK", image.copyOf(163072)))
            assertEquals(null, controller.loadFromLocal(0, "INVALID.DSK"))
            assertEquals("SYSTEM.DSK", controller.mountedDisks[0]?.name)
            assertSame(mounted, lib.bus.floppy.disks[0])
            for (drive in 0..1) {
                val before = decodeABC80Screen(lib.getMemory(), false).count { it.trim() == "ABC80" }
                "LOAD DR$drive:COPY.ABS\r".forEach { lib.sendKey(it.code) }
                val started = TimeSource.Monotonic.markNow()
                var rows = decodeABC80Screen(lib.getMemory(), false)
                while (started.elapsedNow().inWholeMilliseconds < 12000) {
                    rows = decodeABC80Screen(lib.getMemory(), false)
                    if (rows.any { it.contains("ERR") } ||
                        rows.count { it.trim() == "ABC80" } > before) break
                    Thread.sleep(10)
                }
                val output = rows.joinToString("\n")
                assertFalse(output.contains("ERR") || output.contains("RST"), output)
                assertTrue(rows.count { it.trim() == "ABC80" } > before, output)
            }
        } finally {
            native?.freeze()
            for (drive in 0..1) DiskController.instance.clearDrive(drive)
            native?.bus?.floppy?.disks?.close()
            System.setProperty("user.home", previousHome)
            home.deleteRecursively()
        }
    }
}
