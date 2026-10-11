package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.core.DiskController
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicInteger
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
        check(home.resolve(".aboveabc80").resolve("dr").mkdirs())
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
            val prompts = AtomicInteger()
            val promptWatcher = object : NativeLib.MemoryReadWatcher {
                override fun onRead(address: Int): Boolean {
                    prompts.incrementAndGet()
                    return false
                }
            }
            lib.addMemoryReadWatcher(0x00db, promptWatcher)
            try {
                fun command(text: String) {
                    val before = prompts.get()
                    "$text\r".forEach { lib.sendKey(it.code) }
                    val started = TimeSource.Monotonic.markNow()
                    while (prompts.get() == before && started.elapsedNow().inWholeMilliseconds < 12000) {
                        Thread.sleep(10)
                    }
                    val output = decodeABC80Screen(lib.getMemory(), false).joinToString("\n")
                    assertTrue(prompts.get() > before, "$text did not finish:\n$output")
                    assertFalse(output.contains("ERR"), "$text:\n$output")
                }
                for (format in listOf("fd2", "abc830", "fd4d")) {
                    assertTrue(lib.bus.floppy.mount(1, Abc80FloppyLayout(format, 1).create(), "$format.DSK"))
                    command("NEW")
                    command("10 PRINT 42")
                    command("SAVE DR1:EMPTY.BAS")
                    command("NEW")
                    command("LOAD DR1:EMPTY.BAS")
                    command("LIST")
                    assertTrue(decodeABC80Screen(lib.getMemory(), false).any { it.contains("10 PRINT 42") }, format)
                    val saved = home.resolve(".aboveabc80/dr/1.dsk").readBytes()
                    assertTrue(requireNotNull(Abc80FloppyLayout.list(saved)).any { it.first == "EMPTY.BAS" },
                        "$format must also expose saved files to the dialog")
                    assertTrue(saveLocalDisk("CLOUD.DSK", saved))
                    assertEquals("CLOUD.DSK", controller.loadFromLocal(1, "CLOUD.DSK"))
                    val beforeImport = lib.getMemory()
                    fun word(address: Int) = (beforeImport[address].toInt() and 255) or
                        ((beforeImport[address + 1].toInt() and 255) shl 8)
                    val programStart = word(0xFE1C)
                    val programEnd = word(0xFE1E)
                    Abc80CloudDiskImport.save(1, listOf(
                        "CLOUD.BAS" to ((1..15).joinToString("\n") { "${it * 10} REM MOLN" } +
                            "\n200 PRINT 123\n").encodeToByteArray(),
                        "NOTES.TXT" to "Anteckningar\nAndra raden\n".encodeToByteArray(),
                        "RAW.BIN" to "Text även utan textändelse\n".encodeToByteArray(),
                        "TOKEN.BAC" to CloudDiskImportTest.readRecords(saved, "EMPTY.BAS"),
                        "CODE.ABS" to byteArrayOf(0, 1, 0, 0x90.toByte(), 0x6F, 0, 0xFF.toByte(),
                            0xC9.toByte(), 0xC9.toByte(), 0, 0, 0, 0x90.toByte(), 0x6F, 0, 0xFF.toByte())
                    ))
                    assertContentEquals(
                        beforeImport.copyOfRange(programStart, programEnd),
                        lib.getMemory().copyOfRange(programStart, programEnd),
                        "Import must preserve the program in RAM"
                    )
                    assertContentEquals(loadLocalDisk("CLOUD.DSK"), lib.bus.floppy.snapshot(1))
                    command("NEW")
                    command("LOAD DR1:CLOUD.BAS")
                    command("LIST")
                    assertTrue(decodeABC80Screen(lib.getMemory(), false).any { it.contains("200 PRINT 123") }, format)
                    command("NEW")
                    command("LOAD DR1:TOKEN.BAC")
                    command("LIST")
                    assertTrue(decodeABC80Screen(lib.getMemory(), false).any { it.contains("10 PRINT 42") }, format)
                    command("OPEN \"DR1:NOTES.TXT\" AS FILE 1")
                    command("INPUT #1,A$")
                    command("PRINT A$")
                    assertTrue(decodeABC80Screen(lib.getMemory(), false).any { it.contains("Anteckningar") }, format)
                    command("CLOSE 1")
                    val importedImage = lib.bus.floppy.snapshot(1)
                    val expectedText = "Text {ven utan text{ndelse\r".encodeToByteArray()
                    assertContentEquals(expectedText,
                        CloudDiskImportTest.readRecords(importedImage, "RAW.BIN").copyOf(expectedText.size), format)
                    assertEquals(0xC9.toByte(),
                        CloudDiskImportTest.readRecords(importedImage, "CODE.ABS")[7], format)
                    command("SAVE DR1:AGAIN.BAC")
                    command("NEW")
                    command("LOAD DR1:AGAIN.BAC")
                    command("LIST")
                    assertTrue(decodeABC80Screen(lib.getMemory(), false).any { it.contains("10 PRINT 42") }, format)
                    assertEquals(1, controller.currentDriveIndex)
                    command("NEW")
                    command("LOAD DR1:EMPTY.BAS")
                    command("LIST")
                    assertTrue(decodeABC80Screen(lib.getMemory(), false).any { it.contains("10 PRINT 42") }, format)
                }
            } finally {
                lib.removeMemoryReadWatcher(0x00db, promptWatcher)
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
