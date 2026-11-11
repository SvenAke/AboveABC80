package com.aboveware.abc80

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
        val bytes = Res.readBytes("files/system.dsk")
        val files = requireNotNull(Abc80FloppyLayout.list(bytes))
        assertTrue(files.any { it.first == "COPY.ABS" })
    }

    @Test
    fun largeDiskListsEntriesBeyondTheFirstEightDirectorySectors() {
        val layout = Abc80FloppyLayout("abc832", 0)
        val image = layout.create()
        val bitmap = layout.physicalSector(14) * 256
        image[bitmap + 239 + 8] = 2
        val entry = layout.physicalSector(24) * 256 + 16
        image[entry] = 0
        image[entry + 1] = (4 shl 5).toByte()
        "NINTH   BAS".encodeToByteArray().copyInto(image, entry + 4)
        image[entry + 15] = 0xFF.toByte()
        val header = layout.physicalSector(4) * 256
        image.fill(0xFF.toByte(), header, header + 256)
        assertEquals(listOf("NINTH.BAS" to 0), Abc80FloppyLayout.list(image))
    }

    @Test
    fun newlyCreatedLargeDisksHaveCleanDirectoryAccessOverBus() {
        for (name in listOf("abc830", "fd4d", "abc832")) {
            val layout = Abc80FloppyLayout(name, 0)
            val image = layout.create()
            val dirSector = layout.format!!.directorySectors.a
            val file = dirSector / 8
            val sec = dirSector % 8
            // Verify physical sector for dirSector matches offset
            val physicalDirOffset = layout.physicalSector(dirSector) * 256
            assertEquals(0, image[physicalDirOffset].toInt() and 0xFF, "Directory sector $dirSector for $name must start with 0x00")
        }
    }

    @Test
    fun detectsTheAbc830UserFileDirectoryLayout() {
        val image = Abc80FloppyLayout("abc830-ufd", 0).create()
        fun physical(sector: Int) = (sector and 15.inv()) or ((sector * 7) and 15)
        val bitmap = physical(6) * 256
        image[bitmap + 239] = 2
        val entry = physical(16) * 256 + 16
        image[entry] = 3
        image[entry + 1] = 0
        "YRASM   ABS".encodeToByteArray().copyInto(image, entry + 4)
        image[entry + 15] = 0xFF.toByte()
        image[physical(24) * 256 + 4] = 0xFF.toByte()

        assertEquals("abc830-ufd", Abc80FloppyLayout.detectFormatName(image))
        assertEquals(listOf("YRASM.ABS" to 0), Abc80FloppyLayout.list(image))
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

    @Test
    fun diskTypeDeterminesCorrectFloppyType() {
        assertEquals("SS/SD", Abc80FloppyLayout.diskType(Abc80FloppyLayout("fd2", 0).create()))
        assertEquals("SS/DD", Abc80FloppyLayout.diskType(Abc80FloppyLayout("abc830", 0).create()))
        assertEquals("DS/DD", Abc80FloppyLayout.diskType(Abc80FloppyLayout("fd4d", 0).create()))
        assertEquals("DS/QD", Abc80FloppyLayout.diskType(Abc80FloppyLayout("abc832", 0).create()))
    }
}
