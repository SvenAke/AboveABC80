package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.AbcDiskImage
import aboveabc80.composeapp.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AbcDiskImageTest {
    @Test
    fun exportsEntireFilesInAllWritableFormatsIncludingSwedishNames() {
        for (format in listOf("fd2", "abc830", "fd4d")) {
            val records = ByteArray(253 * 45) { (it * 37).toByte() }
            val image = Abc80FloppyLayout.importFiles(
                Abc80FloppyLayout(format, 0).create(),
                listOf("BESÖK.BAS" to records, "SECOND.ABS" to ByteArray(253) { 42 })
            )
            val disk = AbcDiskImage(image)
            assertEquals(listOf("BESÖK", "SECOND"), disk.listFiles().map { it.filename })
            assertContentEquals(records, disk.readFile("BESÖK", "BAS"), format)
            assertContentEquals(ByteArray(253) { 42 }, disk.readFile("SECOND", "ABS"), format)
            assertNull(disk.readFile("MISSING", "BAS"))
            val freeBefore = disk.getFreeSpace()
            val copy = AbcDiskImage(disk.getRawData())
            copy.format()
            assertTrue(copy.listFiles().isEmpty())
            assertTrue(copy.getFreeSpace() > freeBefore)
            assertContentEquals(image, disk.getRawData())
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun exportsBundledSystemFiles() = runBlocking {
        val disk = AbcDiskImage(Res.readBytes("files/system.dsk"))
        for (entry in disk.listFiles()) {
            val data = assertNotNull(disk.readFile(entry.filename, entry.extension))
            assertTrue(data.isNotEmpty(), entry.toString())
            assertEquals(0, data.size % 253)
        }
    }

    @Test
    fun imdImportPreservesSectorIdsAndAbc80Image() {
        val image = Abc80FloppyLayout("fd2", 0).create()
        val bytes = mutableListOf<Byte>()
        bytes.addAll("IMD test\u001a".encodeToByteArray().asIterable())
        val sectorIds = (1..16).reversed()
        repeat(20) { cylinder ->
            bytes.addAll(listOf(0, cylinder, 0, 16, 1).map { it.toByte() })
            bytes.addAll(sectorIds.map { it.toByte() })
            for (id in sectorIds) {
                bytes.add(1)
                val offset = (cylinder * 16 + id - 1) * 256
                bytes.addAll(image.copyOfRange(offset, offset + 256).asIterable())
            }
        }
        assertContentEquals(image, AbcDiskImage.createFromData(bytes.toByteArray()).getRawData())
        assertFailsWith<IllegalArgumentException> {
            AbcDiskImage.createFromData("IMD no tracks\u001a".encodeToByteArray())
        }
    }
}
