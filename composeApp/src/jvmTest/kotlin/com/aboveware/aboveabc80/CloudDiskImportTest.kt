package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class CloudDiskImportTest {
    companion object {
        fun readRecords(image: ByteArray, name: String): ByteArray =
            requireNotNull(Abc80FloppyLayout.readFile(image, name))
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
                Abc80CloudDiskImport.prepare(name, ("OK\n" + "A".repeat(251)).encodeToByteArray())
            }
            assertTrue(requireNotNull(error.message).contains("line 2"))
            assertTrue(requireNotNull(error.message).contains("251"))
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
    fun longBasicLinesArePreservedWithoutWrappingOrTruncation() {
        for (length in listOf(121, 128, 130, 249, 250)) {
            val line = "1950 REM " + "A".repeat(length - 9)
            for (ending in listOf("\n", "\r", "\r\n")) {
                val lines = listOf("10 REM " + "B".repeat(113), line, "1960 END")
                val records = Abc80CloudDiskImport.prepare(
                    "ABCPAINT.BAS", (lines.joinToString(ending) + ending).encodeToByteArray()
                )
                val image = Abc80FloppyLayout.importFiles(
                    Abc80FloppyLayout("abc830", 0).create(), listOf("ABCPAINT.BAS" to records)
                )
                assertContentEquals(records, readRecords(image, "ABCPAINT.BAS"))
                assertContentEquals(
                    (lines.joinToString("\r") + "\r").encodeToByteArray(),
                    records.filter { it != 0.toByte() && it != 3.toByte() }.toByteArray()
                )
                for (offset in records.indices step 253) {
                    val block = records.copyOfRange(offset, offset + 253)
                    val lastCr = block.indexOfLast { it == 13.toByte() }
                    assertTrue(lastCr <= 250, "Two post terminators must fit after CR")
                }
            }
        }
    }

    @Test
    fun detectsBinaryFormatsWithNonstandardExtensionsWithoutChangingTheirNames() {
        val bac = byteArrayOf(0x82.toByte(), 5, 10, 0, 0x1A, 0x17, 1)
        val abs = byteArrayOf(
            0, 1, 0, 0x90.toByte(), 0x6F, 0, 0xFF.toByte(), 0xC9.toByte(),
            0xC9.toByte(), 0, 0, 0, 0x90.toByte(), 0x6F, 0, 0xFF.toByte()
        )
        val files = listOf("INGRAFIK.MRG" to bac, "PROGRAM.DAT" to bac, "CODE.BIN" to abs)
        val image = Abc80FloppyLayout.importFiles(
            Abc80FloppyLayout("abc830", 0).create(),
            files.map { (name, data) -> name to Abc80CloudDiskImport.prepare(name, data) }
        )
        assertEquals(files.map { it.first }.sorted(), Abc80FloppyLayout.list(image)?.map { it.first }?.sorted())
        for ((name, data) in files) {
            assertContentEquals(data.copyOf(253), readRecords(image, name))
        }
        assertFailsWith<IllegalArgumentException> {
            Abc80CloudDiskImport.prepare("BROKEN.MRG", bac.copyOf(6))
        }
        assertContentEquals(
            Abc80CloudDiskImport.prepare("TEXT.BAS", "10 END\n".encodeToByteArray()),
            Abc80CloudDiskImport.prepare("TEXT.MRG", "10 END\n".encodeToByteArray())
        )
    }

    @Test
    fun graphicsDocumentationPreservesEmbeddedDisplayControls() {
        val lines = listOf("P") + List(15) { "\u0017" + "j #!".repeat(13) + "\u0006" }
        val source = lines.joinToString("\n").encodeToByteArray()
        val original = source.copyOf()
        val records = Abc80CloudDiskImport.prepare("INGRAFIK.INF", source)
        val image = Abc80FloppyLayout.importFiles(
            Abc80FloppyLayout("abc830", 0).create(),
            listOf(
                "INGRAFIK.INF" to records,
                "GRAFIK.BAS" to Abc80CloudDiskImport.prepare("GRAFIK.BAS", "10 END\n".encodeToByteArray())
            )
        )
        val stored = readRecords(image, "INGRAFIK.INF")
        assertContentEquals(records, stored)
        assertContentEquals(original, source)
        assertEquals(15, stored.count { it == 0x17.toByte() })
        assertEquals(15, stored.count { it == 0x06.toByte() })
        val recovered = stored.filter { it != 0.toByte() && it != 3.toByte() }.toByteArray()
        assertContentEquals((lines.joinToString("\r") + "\r").encodeToByteArray(), recovered)
        assertEquals(2, Abc80FloppyLayout.list(image)?.size)
        assertFailsWith<IllegalArgumentException> {
            Abc80CloudDiskImport.prepare("INVALID.INF", "TEXT\u0001".encodeToByteArray())
        }
    }

    @Test
    fun textImportStopsAtCtrlZBeforeDecodingOrValidatingPadding() {
        for (charset in listOf(Charsets.UTF_8, Charsets.ISO_8859_1)) {
            for (ending in listOf("", "\n", "\r", "\r\n")) {
                val text = "10 REM ÅÄÖåäö\r\n20 END$ending".toByteArray(charset)
                val original = text + byteArrayOf(0x1A, 0x1A, 0, 0xFF.toByte()) +
                    "NOT TEXT".repeat(30).encodeToByteArray()
                val before = original.copyOf()
                val expected = Abc80CloudDiskImport.prepare("GRAFIK.BAS", text)
                val records = Abc80CloudDiskImport.prepare("GRAFIK.BAS", original)
                assertContentEquals(expected, records)
                assertContentEquals(before, original)
                val image = Abc80FloppyLayout.importFiles(
                    Abc80FloppyLayout("abc830", 0).create(),
                    listOf("GRAFIK.BAS" to records)
                )
                assertContentEquals(expected, readRecords(image, "GRAFIK.BAS"))
            }
        }
        assertContentEquals(
            ByteArray(253),
            Abc80CloudDiskImport.prepare("EMPTY.TXT", byteArrayOf(0x1A, 0xFF.toByte()))
        )
        assertFailsWith<IllegalArgumentException> {
            Abc80CloudDiskImport.prepare("INVALID.TXT", byteArrayOf(0x01, 0x1A))
        }
    }

    @Test
    fun textPostsReserveTwoBytesEvenWhenLinesAlmostFillThePayload() {
        for (lastLength in 88..90) {
            val lines = listOf("A".repeat(80), "B".repeat(80), "C".repeat(lastLength), "NEXT")
            val records = Abc80CloudDiskImport.prepare("BOUND.BAS", lines.joinToString("\n").encodeToByteArray())
            assertEquals(506, records.size)
            if (lastLength == 88) {
                assertContentEquals((lines.take(3).joinToString("\r") + "\r").encodeToByteArray(),
                    records.copyOf(251))
                assertContentEquals(byteArrayOf(3, 3), records.copyOfRange(251, 253))
                assertTrue(records.copyOfRange(253, 506).decodeToString().startsWith("NEXT\r"))
            } else {
                assertContentEquals((lines.take(2).joinToString("\r") + "\r").encodeToByteArray(),
                    records.copyOf(162))
                assertTrue(records.copyOfRange(162, 253).all { it == 3.toByte() })
                assertTrue(records.copyOfRange(253, 506).decodeToString().startsWith(lines[2] + "\rNEXT\r"))
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
