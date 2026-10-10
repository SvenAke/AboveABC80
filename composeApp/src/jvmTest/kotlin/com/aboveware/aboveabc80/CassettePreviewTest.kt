package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalUnsignedTypes::class)
class CassettePreviewTest {
    private fun block(data: ByteArray): ByteArray {
        val result = ByteArray(Abc80Program.Block.BYTES)
        byteArrayOf(0x16, 0x16, 0x16, 2).copyInto(result, 32)
        data.copyInto(result, 36)
        result[292] = 3
        val checksum = 3 + data.sumOf { it.toInt() and 255 }
        result[293] = checksum.toByte()
        result[294] = (checksum shr 8).toByte()
        return result
    }

    @Test
    fun previewReadsMultipleBlocksAndPreservesTapeBytesAndPosition() {
        val original = Abc80Cassette.exportBytes()
        val inserted = Abc80Cassette.anyCassette()
        try {
            val name = ByteArray(256).apply {
                fill(-1, 0, 3)
                "TEST    BAS".encodeToByteArray().copyInto(this, 3)
            }
            val text = "10 PRINT \"[\\]\"\r20 REM ${"X".repeat(300)}\r"
            val blocks = (text + "\u0000").encodeToByteArray().asList().chunked(252)
                .mapIndexed { index, bytes ->
                    block(ByteArray(256).apply {
                        this[1] = index.toByte()
                        this[2] = (index shr 8).toByte()
                        bytes.toByteArray().copyInto(this, 3)
                        this[255] = 3
                    })
                }
            val tape = block(name) + blocks.flatMap { it.asList() }.toByteArray() +
                ByteArray(Abc80Program.Block.BYTES)
            check(Abc80Cassette.import(tape.inputStream(), "preview.tape"))
            val file = Abc80Cassette.list().first { it.displayName == "TEST.BAS" }
            Abc80Cassette.forward()
            val before = Abc80Cassette.exportBytes()
            assertEquals(
                text.replace("[\\]", "ÄÖÅ").replace('\r', '\n'),
                Abc80Cassette.previewBasic(file)
            )
            kotlin.test.assertContentEquals(before, Abc80Cassette.exportBytes())
        } finally {
            if (inserted) {
                check(Abc80Cassette.import(original.inputStream(), "restore.tape"))
            } else {
                Abc80Cassette.eject()
            }
        }
    }
}
