package com.aboveware.abc80

import com.aboveware.abc80.core.AbcDiskImage
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class PascalRecoveryTest {
    @Test
    fun recoverArchiveIntoVerifiedDiskImage() {
        val directory = File("C:\\Users\\svena\\.aboveabc80\\abc800c\\disks")
        val source = File(directory, "PASCAL.dsk")
        val original = source.readBytes()
        val backup = File(directory, "PASCAL-original-20261017.tar")
        val extracted = File(directory, "PASCAL-recovered-20261017")
        check(!backup.exists() && !extracted.exists()) { "Recovery output already exists" }
        fun field(offset: Int, size: Int) =
            original.copyOfRange(offset, offset + size).toString(Charsets.US_ASCII).trim('\u0000', ' ')
        data class Header(val offset: Int, val name: String, val size: Int)
        val headers = (0..original.size - 512 step 512).mapNotNull { offset ->
            val name = field(offset, 100)
            val size = field(offset + 124, 12).toIntOrNull(8)
            val checksum = field(offset + 148, 8).toIntOrNull(8)
            val computed = (0 until 512).sumOf {
                if (it in 148..155) 32 else original[offset + it].toInt() and 255
            }
            if (name.matches(Regex("[A-Za-z0-9_]{1,8}\\.pas")) &&
                size != null && size > 0 && checksum == computed
            ) Header(offset, name.uppercase(), size) else null
        }
        assertEquals(11, headers.size)
        val report = mutableListOf(
            "PASCAL recovery 2026-10-17",
            "Source: TAR archive stored as PASCAL.dsk; damaged headers and payloads.",
            "Only checksum-valid named TAR headers were recovered.",
            "Unidentified data without intact headers was not assigned invented filenames.",
            "Files without a CP/M EOF marker may be incomplete or contain unrelated data.",
            "This disk contains source files, not a Pascal compiler or bootable Pascal system.",
            ""
        )
        val files = headers.mapIndexed { index, header ->
            val start = header.offset + 512
            val limit = headers.getOrNull(index + 1)?.offset ?: original.size
            val end = minOf(start + header.size, limit)
            check(end > start)
            val payload = original.copyOfRange(start, end)
            val status = if (end < start + header.size) {
                "DAMAGED: declared payload overlaps next valid header; clipped"
            } else if (payload.any { it == 0x1a.toByte() }) {
                "CP/M EOF found; completeness not guaranteed"
            } else {
                "WARNING: no CP/M EOF; may be incomplete or corrupted"
            }
            report.add("${header.name}: ${payload.size}/${header.size} bytes. $status")
            header.name to payload
        }
        val records = files.map { (name, bytes) ->
            name to bytes.copyOf(((bytes.size + 252) / 253) * 253)
        }
        val image = Abc80FloppyLayout.importFiles(Abc80FloppyLayout("abc830", 0).create(), records)
        val disk = AbcDiskImage.createFromData(image)
        assertEquals(files.map { it.first }.toSet(), disk.listFiles().map {
            "${it.filename}.${it.extension}"
        }.toSet())
        records.forEach { (name, bytes) ->
            assertContentEquals(bytes, disk.readFile(name.substringBeforeLast('.'), "PAS"), name)
        }
        assertTrue(extracted.mkdir())
        files.forEach { (name, bytes) -> File(extracted, name).writeBytes(bytes) }
        File(extracted, "RECOVERY.txt").writeText(report.joinToString("\r\n"))
        backup.writeBytes(original)
        assertContentEquals(original, backup.readBytes())
        source.writeBytes(image)
        assertContentEquals(image, source.readBytes())
        println(report.joinToString("\n"))
        println("Verified ${files.size} files in ${image.size}-byte ABC830 disk image.")
    }
}
