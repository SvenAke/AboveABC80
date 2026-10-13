package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class Abc80ExecutableFileTest {
    @Test
    fun detectsBasicWithOtherExtensionsOnDiskAndCassette() {
        for (recordSize in listOf(252, 253)) {
            val data = ByteArray(recordSize * 2) { 3 }.apply {
                "10 PRINT \"HELLO\"\r".encodeToByteArray().copyInto(this)
                "20 END\r\u0000".encodeToByteArray().copyInto(this, recordSize)
            }
            for (name in listOf("DRAW.GRF", "CODE.txt", "NOEXT")) {
                assertEquals(Abc80ExecutableFormat.BAS, abc80ExecutableFormat(name, data, recordSize))
            }
        }
    }

    @Test
    fun detectsBinarySignaturesWithOtherExtensions() {
        val bac = byteArrayOf(0x82.toByte(), 3, 10, 0, 1)
        val abs = byteArrayOf(0, 1, 0, 0x90.toByte(), 0x6f, 0, -1, 0, 0)
        for (recordSize in listOf(252, 253)) {
            assertEquals(Abc80ExecutableFormat.BAC, abc80ExecutableFormat("CODE.GRF", bac, recordSize))
            assertEquals(Abc80ExecutableFormat.ABS, abc80ExecutableFormat("CODE.GRF", abs, recordSize))
        }
    }

    @Test
    fun doesNotTreatOrdinaryTextOrArbitraryBinaryAsExecutable() {
        for (data in listOf(
            byteArrayOf(),
            "Instructions\rUse LOAD to open a program\r".encodeToByteArray(),
            "10 PRINT \"HELLO\"\rThis is not a program\r".encodeToByteArray(),
            byteArrayOf(0x82.toByte(), 2, 0, 0, 0),
            byteArrayOf(0, 1, 0, 1, 1, 0, 0)
        )) {
            assertNull(abc80ExecutableFormat("DATA.GRF", data, 252))
        }
    }

    @Test
    fun keepsKnownExtensionsExecutableEvenWhenIncomplete() {
        for (format in Abc80ExecutableFormat.entries) {
            assertEquals(format, abc80ExecutableFormat("FILE.${format.name.lowercase()}", byteArrayOf(), 253))
        }
    }
}
