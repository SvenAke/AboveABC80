package com.aboveware.aboveabc80

import java.io.ByteArrayOutputStream
import java.io.FilterInputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class SavedCassetteStreamTest {
    private fun waveHeader(dataSize: Int) = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray())
        putInt(dataSize + 36)
        put("WAVEfmt ".toByteArray())
        putInt(16)
        putShort(1)
        putShort(1)
        putInt(44100)
        putInt(88200)
        putShort(2)
        putShort(16)
        put("data".toByteArray())
        putInt(dataSize)
    }.array()

    @Test
    fun decodesSavedWaveUsingSmallStreamReads() {
        val directory = Files.createTempDirectory("saved-cassette-stream").toFile()
        try {
            val pcm = ByteArrayOutputStream()
            var polarity = 10000
            fun edge(samples: Int) {
                repeat(samples) {
                    pcm.write(polarity and 255)
                    pcm.write((polarity shr 8) and 255)
                }
                polarity = -polarity
            }
            fun bit(value: Boolean) {
                if (value) { edge(31); edge(32) } else edge(63)
            }
            fun byte(value: Int) = repeat(8) { bit(value and (1 shl it) != 0) }
            fun block(payload: ByteArray) {
                repeat(512) { bit(false) }
                repeat(3) { byte(0x16) }
                byte(2)
                payload.forEach { byte(it.toInt() and 255) }
                byte(3)
                val sum = 3 + payload.sumOf { it.toInt() and 255 }
                byte(sum and 255)
                byte(sum shr 8)
            }
            block(ByteArray(256).apply {
                this[0] = -1; this[1] = -1; this[2] = -1
                "MUSIK   BAS".toByteArray().copyInto(this, 3)
            })
            repeat(2) { number ->
                block(ByteArray(256).apply {
                    this[1] = number.toByte()
                    this[255] = 3
                })
            }
            repeat(512) { bit(false) }
            val file = directory.resolve("test.wav")
            file.outputStream().use {
                it.write(waveHeader(pcm.size()))
                pcm.writeTo(it)
            }
            val decoder = Abc80CassetteDecoder("Test")
            file.inputStream().use { source ->
                val bounded = object : FilterInputStream(source) {
                    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                        assertTrue(length <= 64 * 1024)
                        return super.read(bytes, offset, length)
                    }
                }
                assertTrue(decoder.decode(bounded))
            }
            assertEquals(listOf("MUSIK   BAS"), decoder.files().map { it.name() })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun scansWaveLargerThanAndroidHeapWithoutAllocatingTheRecording() {
        val directory = Files.createTempDirectory("saved-large-cassette").toFile()
        val dataSize = 286700756 - 44
        try {
            val file = directory.resolve("large.wav")
            RandomAccessFile(file, "rw").use {
                it.write(waveHeader(dataSize))
                it.setLength(dataSize.toLong() + 44)
            }
            val decoder = Abc80CassetteDecoder("Test")
            file.inputStream().use { assertTrue(decoder.decode(it)) }
            assertTrue(decoder.files().isEmpty(), "Silence must not produce a fictitious program")
        } finally {
            directory.deleteRecursively()
        }
    }
}
