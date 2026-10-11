package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class CloudDiskImportTest {
    companion object {
        fun readRecords(image: ByteArray, name: String): ByteArray {
            fun physical(sector: Int) = (sector and 15.inv()) or ((sector * 7) and 15)
            val diskName = Abc80CloudDiskImport.encodeText(
                name.substringBeforeLast('.').uppercase().padEnd(8) +
                    name.substringAfterLast('.', "").uppercase().padEnd(3)
            )
            val entry = (8 until 16).flatMap { sector ->
                (1..15).map { physical(sector) * 256 + it * 16 }
            }.first { image.copyOfRange(it + 4, it + 15).contentEquals(diskName) }
            val headerSector = (image[entry].toInt() and 255) * 8 +
                ((image[entry + 1].toInt() and 255) shr 5)
            val header = physical(headerSector) * 256
            val sectors = mutableListOf<Int>()
            for (offset in 4 until 240 step 2) {
                if (image[header + offset] == 0xFF.toByte()) break
                val start = (image[header + offset].toInt() and 255) * 8 +
                    ((image[header + offset + 1].toInt() and 255) shr 5)
                val count = (image[header + offset + 1].toInt() and 31) + 1
                repeat(count) { sectors.add(start + it) }
            }
            return sectors.drop(1).flatMap { sector ->
                image.copyOfRange(physical(sector) * 256 + 3, (physical(sector) + 1) * 256).asIterable()
            }.toByteArray()
        }
    }

    @Test
    fun importsAllTypesAndPreservesRecords() {
        val image = Abc80FloppyLayout("abc830", 0).create()
        val abs = byteArrayOf(0, 1, 0, 0x90.toByte(), 0x6F, 0, 0xFF.toByte(), 0xC9.toByte(),
            0xC9.toByte(), 0, 0, 0, 0x90.toByte(), 0x6F, 0, 0xFF.toByte())
        val files = listOf(
            "SWEDISH.bas" to "10 PRINT \"ÅÄÖåäö¤\"\r\n20 END\n".encodeToByteArray(),
            "TOKEN.bac" to byteArrayOf(0x82.toByte(), 4, 10, 0, 0, 1),
            "CODE.abs" to abs,
            "NOTES.txt" to (1..20).joinToString("\n") { "Rad $it ${"A".repeat(110)}" }.encodeToByteArray()
        )
        val imported = Abc80FloppyLayout.importFiles(image,
            files.map { it.first to Abc80CloudDiskImport.prepare(it.first, it.second) })
        assertEquals(4, Abc80FloppyLayout.list(imported)?.size)
        for ((name, data) in files) {
            assertContentEquals(Abc80CloudDiskImport.prepare(name, data), readRecords(imported, name), name)
        }
        val bas = readRecords(imported, "SWEDISH.BAS")
        assertTrue(bas.decodeToString().startsWith("10 PRINT \"][\\}{|$\"\r20 END\r"))
        assertContentEquals(files[1].second, readRecords(imported, "TOKEN.BAC").copyOf(files[1].second.size))
        assertContentEquals(abs, readRecords(imported, "CODE.ABS").copyOf(abs.size))
        val original = image.copyOf()
        assertFailsWith<IllegalArgumentException> {
            Abc80FloppyLayout.importFiles(image, listOf("TOOLONGNAME.BAS" to ByteArray(253)))
        }
        assertContentEquals(original, image)
    }

    @Test
    fun importsRepairedSwedishNamesBeforeUppercasingAndEncoding() {
        val original = Abc80FloppyLayout("abc830", 0).create()
        val files = listOf(
            "bes÷k.grf" to "BESÖK.GRF",
            "σΣ÷┼─╓.txt" to "ÅÄÖÅÄÖ.TXT",
            "Ã¥Ã¤Ã¶Ã…Ã„Ã–.bas" to "ÅÄÖÅÄÖ.BAS",
            "Ã\u0085Ã\u0084Ã\u0096.dat" to "ÅÄÖ.DAT"
        )
        var image = original
        for ((encoded, expected) in files) {
            val records = ByteArray(253) { 42 }
            image = Abc80FloppyLayout.importFiles(image, listOf(encoded to records))
            assertContentEquals(records, readRecords(image, expected), encoded)
        }
        assertEquals(files.size, Abc80FloppyLayout.list(image)?.size)
        val replacement = ByteArray(253) { 7 }
        image = Abc80FloppyLayout.importFiles(image, listOf("besök.grf" to replacement))
        assertEquals(files.size, Abc80FloppyLayout.list(image)?.size)
        assertContentEquals(replacement, readRecords(image, "BESÖK.GRF"))
        assertContentEquals(Abc80FloppyLayout("abc830", 0).create(), original)
    }

    @Test
    fun allOrdinaryExtensionsUseTextRecordsWithoutSplittingLines() {
        val image = Abc80FloppyLayout("abc830", 0).create()
        val lines = listOf("A".repeat(120), "B".repeat(120), "C".repeat(120))
        val data = lines.joinToString("\n").encodeToByteArray()
        val files = listOf("TEXT.COM", "DATA.BIN", "README", "PAGE.HTM", "TEXT.TXT", "TEXT.BAS")
        val expected = ByteArray(506)
        (lines[0] + "\r" + lines[1] + "\r").encodeToByteArray().copyInto(expected)
        expected.fill(3, 242, 253)
        (lines[2] + "\r").encodeToByteArray().copyInto(expected, 253)
        val imported = Abc80FloppyLayout.importFiles(image,
            files.map { it to Abc80CloudDiskImport.prepare(it, data) })
        for (name in files) {
            assertContentEquals(expected, readRecords(imported, name), name)
            for (ending in listOf("\n", "\r", "\r\n")) {
                assertContentEquals(expected, Abc80CloudDiskImport.prepare(name,
                    ("\uFEFF" + lines.joinToString(ending) + ending).encodeToByteArray()))
            }
            val error = assertFailsWith<IllegalArgumentException> {
                Abc80CloudDiskImport.prepare(name, ("OK\n" + "A".repeat(121)).encodeToByteArray())
            }
            assertTrue(requireNotNull(error.message).contains("rad 2"))
            assertTrue(requireNotNull(error.message).contains("121"))
        }
        assertContentEquals(ByteArray(253),
            Abc80CloudDiskImport.prepare("EMPTY.DAT", byteArrayOf()))
        for (name in listOf("EMPTY.bAc", "EMPTY.aBs")) {
            assertFailsWith<IllegalArgumentException> {
                Abc80CloudDiskImport.prepare(name, byteArrayOf())
            }
        }
    }

    @Test
    fun replacementAndFullDiskAreAtomic() {
        val image = Abc80FloppyLayout("fd2", 0).create()
        val first = Abc80FloppyLayout.importFiles(image, listOf("ONE.TXT" to ByteArray(506) { 42 }))
        val replacement = Abc80FloppyLayout.importFiles(first, listOf("ONE.TXT" to ByteArray(253) { 7 }))
        assertEquals(1, Abc80FloppyLayout.list(replacement)?.size)
        assertContentEquals(ByteArray(253) { 7 }, readRecords(replacement, "ONE.TXT"))
        assertTrue(Abc80FloppyLayout.freeSpace(replacement) > Abc80FloppyLayout.freeSpace(first))
        val unchanged = first.copyOf()
        assertFailsWith<IllegalArgumentException> {
            Abc80FloppyLayout.importFiles(first, listOf("BIG.TXT" to ByteArray(253 * 400)))
        }
        assertContentEquals(unchanged, first)
        val cleared = Abc80FloppyLayout.importFiles(first, listOf("TWO.TXT" to ByteArray(253)), true)
        assertEquals(listOf("TWO.TXT"), Abc80FloppyLayout.list(cleared)?.map { it.first })
    }

    @Test
    fun invalidFilesAndFailedBatchesDoNotChangeTheDisk() {
        assertFailsWith<IllegalArgumentException> {
            Abc80CloudDiskImport.prepare("BROKEN.BAC", byteArrayOf(0x82.toByte(), 0))
        }
        assertFailsWith<IllegalArgumentException> {
            Abc80CloudDiskImport.prepare("BROKEN.ABS", byteArrayOf(1, 2, 3))
        }
        val image = Abc80FloppyLayout("fd2", 0).create()
        val unchanged = image.copyOf()
        assertFailsWith<IllegalArgumentException> {
            Abc80FloppyLayout.importFiles(image, listOf(
                "SMALL.TXT" to ByteArray(253), "FULL.TXT" to ByteArray(253 * 400)
            ))
        }
        assertContentEquals(unchanged, image)
        val latin1 = Abc80CloudDiskImport.prepare("SWEDISH.TXT", byteArrayOf(0xC5.toByte(), 0xE4.toByte()))
        assertContentEquals("]{\r".encodeToByteArray(), latin1.copyOf(3))
    }
}
