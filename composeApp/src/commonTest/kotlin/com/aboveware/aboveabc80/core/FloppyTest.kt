package com.aboveware.aboveabc80.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FloppyTest {
    @Test
    fun clearFilesReclaimsSpaceAndPreservesReservedSystemTracks() {
        val floppy = Floppy()
        val systemSector = ByteArray(128) { 0x42 }
        floppy.writeSector(0, 0, systemSector)
        val emptyFreeSpace = floppy.getFreeSpace()

        assertTrue(floppy.injectFile("TEST", "TXT", ByteArray(1024) { 0x41 }))
        assertTrue(floppy.getFreeSpace() < emptyFreeSpace)

        floppy.clearFiles()

        assertFalse(floppy.listFiles().any { it.isActive() })
        assertEquals(emptyFreeSpace, floppy.getFreeSpace())
        assertTrue(floppy.readSector(0, 0).contentEquals(systemSector))
    }
}
