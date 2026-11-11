package com.aboveware.abc80

import com.aboveware.abc80.core.AbcDiskImage
import com.aboveware.abc80.core.DiskController
import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.security.MessageDigest
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class TeleDiskImageTest {
    @Test
    fun decodesAllSectorEncodingsAndPreservesPhysicalIds() {
        val raw = Abc80FloppyLayout("abc830", 0).create()
        val image = TeleDiskImage.decode(td0Fixture(raw))
        assertContentEquals(raw, image.rawData)
        assertContentEquals(byteArrayOf(0, 0x42, 1, 1), image.addressMark(0, 0).copyOf(4))
        assertContentEquals(byteArrayOf(0, 0x42, 16, 1), image.addressMark(0, 15).copyOf(4))
        assertContentEquals(byteArrayOf(1, 0, 1, 1), image.addressMark(16, 0).copyOf(4))
        assertContentEquals(raw, AbcDiskImage.createFromData(td0Fixture(raw)).getRawData())
        assertEquals("disks", cloudDownloadFolder("abc80/", null, "ORD.TD0"))
        assertTrue(isDiskImageName("ORD.TD0"))
    }

    @Test
    fun supportsDoubleSidedGeometryAndComments() {
        for (format in listOf("fd4d", "abc832")) {
            val raw = Abc80FloppyLayout(format, 0).create()
            val image = TeleDiskImage.decode(td0Fixture(raw, comment = true))
            assertContentEquals(raw, image.rawData)
            assertContentEquals(byteArrayOf(0, 1, 1, 1), image.addressMark(raw.size / 512, 0).copyOf(4))
        }
    }

    @Test
    fun rejectsTruncationAndCorruptChecksums() {
        val data = td0Fixture(Abc80FloppyLayout("abc830", 0).create())
        for (size in listOf(0, 2, 11, 12, 16, data.size - 5)) {
            assertFailsWith<IllegalArgumentException>("Truncated at $size") {
                TeleDiskImage.decode(data.copyOf(size))
            }
        }
        for (offset in listOf(4, 15, 21, 24)) {
            val corrupt = data.copyOf()
            corrupt[offset] = (corrupt[offset].toInt() xor 1).toByte()
            assertFailsWith<IllegalArgumentException>("Corruption at $offset") { TeleDiskImage.decode(corrupt) }
        }
        val advanced = data.copyOf()
        advanced[0] = 't'.code.toByte()
        advanced[1] = 'd'.code.toByte()
        assertFailsWith<IllegalArgumentException> { TeleDiskImage.decode(advanced) }
    }

    @Test
    fun supportsBothUncompressedEndMarkers() {
        val raw = Abc80FloppyLayout("abc830", 0).create()
        val td0 = td0Fixture(raw)
        assertContentEquals(raw, TeleDiskImage.decode(td0.copyOf(td0.size - 3)).rawData)
        assertFailsWith<IllegalArgumentException> { TeleDiskImage.decode(td0.copyOf(td0.size - 1)) }
    }

    @Test
    fun decodesSyntheticLzhStreamIncludingMatchesAndHuffmanRebuild() {
        // Expected digest was produced by an independent LZH decoder.
        val input = ByteArray(80000) { (it * 73 + 19).toByte() }
        val decoder = TeleDiskLzh(input)
        val decoded = ByteArray(467072) { decoder.readByte().toByte() }
        val digest = MessageDigest.getInstance("SHA-256").digest(decoded)
            .joinToString("") { "%02x".format(it.toInt() and 255) }
        assertEquals("b316ea19f53380f30d71ca07e27f069a51690ff3a58630953e903b65762cd457", digest)
        val truncated = TeleDiskLzh(input.copyOf(1))
        assertFailsWith<IllegalArgumentException> {
            repeat(1000) { truncated.readByte() }
        }
    }

    @Test
    fun controllerReadsAddressMarksAndRestoresThemWithoutLosingWrites() = runBlocking {
        val home = Files.createTempDirectory("td0-controller").toFile()
        val previousHome = System.getProperty("user.home")
        val previousModel = TerminalManager.computerModel
        val lib = NativeLib.getObject()
        lib.freeze(true)
        try {
            System.setProperty("user.home", home.absolutePath)
            DiskController.instance.closeForComputerModelChange()
            TerminalManager.computerModel = ComputerModel.ABC800C
            val raw = Abc80FloppyLayout("abc830", 0).create()
            val td0 = td0Fixture(raw)
            assertTrue(saveLocalDisk("TEST.TD0", td0))
            assertEquals("TEST.TD0", DiskController.instance.loadFromLocal(0, "TEST.TD0"))
            val floppy = lib.bus.floppy
            fun command(vararg bytes: Int) {
                floppy.onWrite(2u, 0u, 0u)
                for (byte in bytes) floppy.onWrite(0u, byte.toUByte(), 0u)
            }
            fun readMark(): ByteArray {
                command(0x10, 0x10, 0, 0)
                assertEquals(0xc9, floppy.onRead(1u).toInt())
                command(2, 0, 0, 0)
                assertEquals(0x49, floppy.onRead(1u).toInt())
                val data = ByteArray(256) { floppy.onRead(0u).toByte() }
                assertEquals(0xc9, floppy.onRead(1u).toInt())
                assertEquals(0, floppy.onRead(0u).toInt())
                return data
            }
            val first = readMark()
            assertContentEquals(byteArrayOf(0, 0x42, 1, 1), first.copyOf(4))
            assertContentEquals(TeleDiskImage.withAddressCrc(first.copyOf(4)), first.copyOf(6))
            assertEquals(2, readMark()[2].toInt())
            command(0x0c, 0, 0, 0)
            repeat(256) { floppy.onWrite(0u, 0x5au, 0u) }
            assertEquals(0, floppy.onRead(0u).toInt())
            val changed = floppy.snapshot(0)
            assertTrue(changed.take(256).all { it == 0x5a.toByte() })
            assertContentEquals(td0, assertNotNull(loadLocalDisk("TEST.TD0")))
            DiskController.instance.closeForComputerModelChange()
            assertNotNull(DiskController.instance.restoreFromLocal(0, "TEST.TD0"))
            assertContentEquals(changed, floppy.snapshot(0))
            assertEquals(0x42, readMark()[1].toInt())
            assertTrue(floppy.mount(0, raw, "RAW.DSK"))
            assertEquals(0, readMark()[1].toInt(), "A raw remount must clear stale TD0 metadata")
            assertTrue(!getComputerModelStorageDir("dr").resolve("0.td0").exists())
            for (format in listOf("fd2", "abc830", "fd4d", "abc832")) {
                assertTrue(floppy.mount(0, Abc80FloppyLayout(format, 0).create(), "RAW.DSK"))
                command(0x0c, 0, 0, 0)
                repeat(256) { floppy.onWrite(0u, it.toUByte(), 0u) }
                assertEquals(0, floppy.onRead(0u).toInt())
                command(3, 0, 0, 0)
                assertContentEquals(ByteArray(256) { it.toByte() }, ByteArray(256) { floppy.onRead(0u).toByte() })
                assertEquals(0, floppy.onRead(0u).toInt())
            }
        } finally {
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

@OptIn(ExperimentalUnsignedTypes::class)
private fun td0Fixture(raw: ByteArray, comment: Boolean = false): ByteArray {
    fun crc(data: ByteArray): Int {
        var result = 0
        for (byte in data) {
            result = result xor ((byte.toInt() and 255) shl 8)
            repeat(8) { result = ((result shl 1) xor if (result and 0x8000 != 0) 0xa097 else 0) and 65535 }
        }
        return result
    }
    val output = ByteArrayOutputStream()
    fun word(value: Int) { output.write(value and 255); output.write(value shr 8) }
    val heads = if (raw.size > 163840) 2 else 1
    val cylinders = if (raw.size == 655360) 80 else 40
    val header = byteArrayOf(0x54, 0x44, 0, 0, 21, 0, 2, if (comment) 0x80.toByte() else 0, 0, heads.toByte())
    output.write(header)
    word(crc(header))
    if (comment) {
        val text = "Synthetic TD0 test".toByteArray()
        val length = byteArrayOf(text.size.toByte(), 0)
        val content = length + ByteArray(6) + text
        word(crc(content))
        output.write(content)
    }
    for (head in 0 until heads) for (cylinder in 0 until cylinders) {
        val numbers = (1..16).toList() + if (cylinder == 19) listOf(1, 2) else emptyList()
        val track = byteArrayOf(numbers.size.toByte(), cylinder.toByte(), head.toByte())
        output.write(track)
        output.write(crc(track) and 255)
        for (number in numbers) {
            val sector = (head * cylinders + cylinder) * 16 + number - 1
            val block = raw.copyOfRange(sector * 256, (sector + 1) * 256)
            val sideId = (if (cylinder == 0 && head == 0) 0x42 else head).toByte()
            output.write(byteArrayOf(cylinder.toByte(), sideId, number.toByte(), 1, 0))
            output.write(crc(block) and 255)
            val repeated = block.indices.all { block[it] == block[it % 2] }
            val packed = when {
                repeated && number % 2 == 0 -> byteArrayOf(1, 128.toByte(), 0, block[0], block[1])
                repeated -> byteArrayOf(2, 1, 128.toByte(), block[0], block[1])
                number % 2 == 0 -> byteArrayOf(0) + block
                else -> byteArrayOf(2, 0, 128.toByte()) + block.copyOfRange(0, 128) +
                    byteArrayOf(0, 128.toByte()) + block.copyOfRange(128, 256)
            }
            word(packed.size)
            output.write(packed)
        }
    }
    output.write(byteArrayOf(255.toByte(), 0, 0, 0))
    return output.toByteArray()
}
