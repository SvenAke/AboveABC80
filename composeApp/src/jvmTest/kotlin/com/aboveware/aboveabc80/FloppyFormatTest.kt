package com.aboveware.aboveabc80

import aboveabc80.composeapp.generated.resources.Res
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class FloppyFormatTest {
    @Test
    fun listingDecodesAllSwedishLettersWithoutModifyingTheImage() {
        val image = Abc80FloppyLayout.importFiles(
            Abc80FloppyLayout("abc830", 0).create(),
            listOf("SWEDISH.BAS" to ByteArray(253))
        )
        val size = requireNotNull(Abc80FloppyLayout.list(image)).single().second
        fun physical(sector: Int) = (sector and 15.inv()) or ((sector * 7) and 15)
        val nameOffset = physical(8) * 256 + 16 + 4
        "][\\}{|  BAS".encodeToByteArray().copyInto(image, nameOffset)
        val before = image.copyOf()
        assertEquals(listOf("ÅÄÖåäö.BAS" to size), Abc80FloppyLayout.list(image))
        assertContentEquals(before, image)
        assertEquals("BESÖK.GRF", "BES\\K.GRF".toAndroid())
        assertEquals("besök.grf", "bes|k.grf".toAndroid())
    }

    @OptIn(ExperimentalResourceApi::class)
    @Test
    fun bundledSystemDirectoryStillContainsCopy() = runBlocking {
        val files = requireNotNull(Abc80FloppyLayout.list(Res.readBytes("files/system.dsk")))
        assertTrue(files.any { it.first == "COPY.ABS" })
    }

    @Test
    fun largeDiskListsEntriesBeyondTheFirstEightDirectorySectors() {
        val image = Abc80FloppyLayout("abc832", 0).create()
        image[2 * 256 + 239 + 8] = 2
        val entry = 24 * 256 + 16
        image[entry] = 4
        image[entry + 1] = 0
        "NINTH   BAS".encodeToByteArray().copyInto(image, entry + 4)
        image.fill(0xFF.toByte(), 128 * 256, 129 * 256)
        assertEquals(listOf("NINTH.BAS" to 0), Abc80FloppyLayout.list(image))
    }

    @Test
    fun allEmptyFormatsHaveEmptyDirectoriesAndPhysicalAllocationMaps() {
        for (name in listOf("fd2", "abc830", "fd4d", "abc832")) {
            val layout = Abc80FloppyLayout(name, 0)
            val format = requireNotNull(layout.format)
            val image = layout.create()
            assertEquals(format.sectorFormat.size, image.size, name)
            assertEquals(emptyList(), Abc80FloppyLayout.list(image), name)
            val physicalBitmapSector = (format.systemSector and 15.inv()) or
                ((format.systemSector * 7) and 15)
            val bitmap = physicalBitmapSector * 256
            assertEquals(format.clusterShift, image[bitmap + 255].toInt(), name)
            assertTrue(image[bitmap].toInt() != 0, "$name must reserve its system clusters")
            assertTrue((32 until 200).any { image[bitmap + it] == 0.toByte() },
                "$name must have free clusters")
        }
    }
}
