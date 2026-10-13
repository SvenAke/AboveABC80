package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

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
    fun bacLoadRequiresEndRecordAndPreservesCassette() {
        val original = Abc80Cassette.exportBytes()
        val inserted = Abc80Cassette.anyCassette()
        try {
            val name = ByteArray(256).apply {
                fill(-1, 0, 3)
                "BINARY  BAC".encodeToByteArray().copyInto(this, 3)
            }
            val first = block(ByteArray(256).apply {
                this[3] = 0x82.toByte()
                this[4] = 3
                this[5] = 10
                this[255] = 3
            })
            fun tape(terminated: Boolean): ByteArray {
                val last = block(ByteArray(256).apply {
                    this[1] = 1
                    this[3] = 3
                    this[4] = 20
                    this[6] = if (terminated) 1 else 0
                    this[255] = 3
                })
                return block(name) + first + last + ByteArray(Abc80Program.Block.BYTES)
            }
            for (terminated in listOf(false, true)) {
                check(Abc80Cassette.import(tape(terminated).inputStream(), "binary.tape"))
                val file = Abc80Cassette.list().first { it.displayName == "BINARY.BAC" }
                Abc80Cassette.forward()
                val position = Abc80Cassette.list().first { it.current }.position
                val before = Abc80Cassette.exportBytes()
                if (terminated) {
                    Abc80Cassette.validateBacLoad(file)
                } else {
                    val error = assertFailsWith<IllegalArgumentException> {
                        Abc80Cassette.validateBacLoad(file)
                    }
                    assertEquals("BAC file missing end record: BINARY.BAC", error.message)
                }
                assertContentEquals(before, Abc80Cassette.exportBytes())
                assertEquals(position, Abc80Cassette.list().first { it.current }.position)
            }
        } finally {
            if (inserted) {
                check(Abc80Cassette.import(original.inputStream(), "restore.tape"))
            } else {
                Abc80Cassette.eject()
            }
        }
    }

    @Test
    fun previewAcceptsTapeGapWithoutTextEndMarkerButRejectsCorruptData() {
        val original = Abc80Cassette.exportBytes()
        val inserted = Abc80Cassette.anyCassette()
        try {
            val name = ByteArray(256).apply {
                fill(-1, 0, 3)
                "MORSE   BAS".encodeToByteArray().copyInto(this, 3)
            }
            val text = "10 REM " + "X".repeat(244) + "\r"
            val data = block(ByteArray(256).apply {
                text.encodeToByteArray().copyInto(this, 3)
                this[255] = 3
            })
            val tape = block(name) + data + ByteArray(Abc80Program.Block.BYTES)
            check(Abc80Cassette.import(tape.inputStream(), "preview-gap.tape"))
            val file = Abc80Cassette.list().first { it.displayName == "MORSE.BAS" }
            val before = Abc80Cassette.exportBytes()
            assertEquals(text.replace('\r', '\n'), Abc80Cassette.previewBasic(file))
            assertFailsWith<IllegalStateException> { Abc80Cassette.validateBasicLoad(file) }
            assertContentEquals(before, Abc80Cassette.exportBytes())
            assertEquals(file.position, Abc80Cassette.list().first { it.current }.position)

            val corrupt = tape.copyOf().apply {
                this[Abc80Program.Block.BYTES + 293] = (data[293].toInt() xor 1).toByte()
            }
            check(Abc80Cassette.import(corrupt.inputStream(), "preview-corrupt.tape"))
            assertFailsWith<IllegalStateException> { Abc80Cassette.previewBasic(file) }
        } finally {
            if (inserted) {
                check(Abc80Cassette.import(original.inputStream(), "restore.tape"))
            } else {
                Abc80Cassette.eject()
            }
        }
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
            Abc80Cassette.validateBasicLoad(file)
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
