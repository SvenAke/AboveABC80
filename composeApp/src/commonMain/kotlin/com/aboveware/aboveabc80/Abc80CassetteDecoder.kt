package com.aboveware.aboveabc80

import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.InputStream
import kotlin.math.exp

/*
 * Decode ABC80 cassette files
 *
 * This is not needed anymore:
 * Convert to single channel 16-bit with appropriate endianness first:
 *   sox *.wav --endian big -c 1 -S *.raw
 */
@OptIn(ExperimentalUnsignedTypes::class)
@ExperimentalUnsignedTypes
class Abc80CassetteDecoder(progressText: String) {

    private val frequency = 44100
    private val thresh = 5300f
    private val cutoff = 0f
    private var k = exp(-cutoff / frequency)
    private var xTime = frequency / 700 // Samples /baud
    private var sinceStart = 0
    private var captured = false
    private var state = false
    private var previousTime = 0
    private var lastSample = 0
    private var isWave = false

    class File(inBlocks: List<ByteArray>, inRawBuffers: List<ByteArray>) {
        private var blocks = inBlocks
        private var rawBuffers = inRawBuffers
        fun name(): String {
            val nameString = StringBuilder()
            for (index in 0..10)
                nameString.append(blocks[0][index].toInt().toChar())
            return nameString.toString()
        }

        fun data(): ByteArray {
            val result = mutableListOf<Byte>()
            // Return all data blocks without the first name block and the last end block
            blocks.subList(1, blocks.size - 1).forEach { result.addAll(it.toList()) }
            return result.toByteArray()
        }

        fun rawData(): ByteArray {
            val result = mutableListOf<Byte>()
            rawBuffers.subList(0, blocks.size).forEach { result.addAll(it.toList()) }
            return result.toByteArray()
        }

        /**
         * Validate that we actually have decoded a header block, some data blocks
         * and an end block
         *
         * Header block:
         *  Has 11 character as the filename and the rest is zeros
         */
        fun validate(): Boolean {
            if (blocks.size < 3) {
                Abc80Log.cassette("To few blocks decoded")
                return false
            }
            val nameBlock = Abc80Program.NameBlock()
            nameBlock.load(rawBuffers[0])
            if (!nameBlock.verify()) {
                Abc80Log.cassette("Name block error")
                return false
            }
            for (index in 1 until blocks.size) {
                val dataBlock = Abc80Program.DataBlock(index - 1)
                dataBlock.load(rawBuffers[index])
                if (!dataBlock.verify()) {
                    blocks = blocks.subList(0, index)
                    rawBuffers = rawBuffers.subList(0, index)
                    Abc80Log.cassette("Data block ${index - 1} error")
                    return true
                }
            }
            return true
        }
    }

    private val decoder = Decoder(mutableListOf(), progressText)

    fun files() = decoder.files

    fun decode(cassette: InputStream): Boolean {
        try {
            DataInputStream(BufferedInputStream(cassette)).use { stream ->
                var time = 0
                var value = 0f
                val channels = skipIfWave(stream)
                while (true) {
                    val sample = readShort(stream)
                    value = highPassFilter(value, sample, time++)
                    if (channels == 2) readShort(stream)
                }
            }
        } catch (e: EOFException) {
            Abc80Log.trace("$e")
            decoder.save()
        }
        return true
    }

    /**
     * Skip header if this is a wav-file. Return the # of channels in the
     * wav-file or 1 for raw file
     */
    private fun skipIfWave(stream: DataInputStream): Int {
        stream.mark(256)
        val bytes = ByteArray(8)
        stream.read(bytes, 0, 4)        // Skip ChunkSize
        if (bytes[0].toInt().toChar() == 'R' && bytes[1].toInt().toChar() == 'I' &&
            bytes[2].toInt().toChar() == 'F' && bytes[3].toInt().toChar() == 'F'
        ) {
            isWave = true
            stream.readInt()
            stream.read(bytes, 0, 8)
            if (bytes[0].toInt().toChar() == 'W' && bytes[1].toInt().toChar() == 'A' &&
                bytes[2].toInt().toChar() == 'V' && bytes[3].toInt().toChar() == 'E' &&
                bytes[4].toInt().toChar() == 'f' && bytes[5].toInt().toChar() == 'm' &&
                bytes[6].toInt().toChar() == 't' && bytes[7].toInt().toChar() == ' '
            ) {
                stream.read(bytes, 0, 4)        // Skip Chunk1Size
                stream.read(bytes, 0, 2)        // Skip AudioFormat
                val channels = readShort(stream)
                val sampleRate = readInt(stream)
                readInt(stream)                 // Skip blockRate
                readShort(stream)               // Skip blockAlign
                readShort(stream)               // Skip bitsPerSample
                stream.read(bytes, 0, 4)
                if (bytes[0].toInt().toChar() == 'd' && bytes[1].toInt().toChar() == 'a' &&
                    bytes[2].toInt().toChar() == 't' && bytes[3].toInt().toChar() == 'a'
                ) {
                    stream.read(bytes, 0, 4)        // Skip Chunk2Size
                    k = exp(-cutoff / sampleRate)
                    xTime = sampleRate / 700        // Samples /baud
                    isWave = true
                    return channels
                }
            }
        }
        isWave = false
        stream.reset()
        return 1
    }

    private fun readShort(stream: DataInputStream) =
        if (isWave) Integer.reverseBytes(stream.readShort().toInt()).and(0xffff0000.toInt()).shr(16)
        else stream.readShort().toInt()

    private fun readInt(stream: DataInputStream) =
        Integer.reverseBytes(stream.readInt())

    private fun highPassFilter(value: Float, sample: Int, time: Int): Float {
        val nextValue = (value * k) + (sample - lastSample)
        lastSample = sample
        if (nextValue >= thresh && !state) {
            state = true
            flankDetected(time - previousTime)
            previousTime = time

        } else if (nextValue <= -thresh && state) {
            state = false
            flankDetected(time - previousTime)
            previousTime = time
        }
        return nextValue
    }

    private fun flankDetected(time: Int) {
        sinceStart += time
        when {
            sinceStart < xTime / 4 -> {
            }
            sinceStart < xTime * 3 / 4 -> captured = true
            sinceStart < xTime * 5 / 4 -> {
                decoder.save(captured)
                sinceStart = 0
                captured = false
            }
            else -> sinceStart = 0
        }
    }

    enum class State {
        Leader,
        Sync,
        Stx,
        Data,
        Etx,
        CheckSum
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    class Decoder(val files: MutableList<File>, private val progressText: String) {
        private var state = State.Leader
        private var counter = 0
        private var bits = 0
        private var data = 0
        private var block = 0
        private var buffers = mutableListOf<ByteArray>()
        private var rawBuffers = mutableListOf<ByteArray>()
        private val buf = ByteArray(256)
        private val rawData = mutableListOf<Byte>()
        private val checkSum = ByteArray(2)

        fun save(bit: Boolean) {
            data = (data shr 1) + if (bit) 128 else 0 /* Big endian bit order... */
            bits++

            when (state) {
                State.Leader -> {
                    counter++
                    if (bit) {
                        counter = 0
                    }
                    if (counter > 128) {   /* After 128 zeroes start looking for sync */
                        state = State.Sync
                    }
                }

                State.Sync -> {
                    if (data == 0x16) {
                        rawData.add(data.toByte())
                        state = State.Stx /* Sync acquired */
                        bits = 0
                    }
                }

                State.Stx -> {
                    if (bits == 8) {
                        bits = 0
                        counter = 0
                        when (data) {
                            0x02 -> {
                                rawData.add(data.toByte())
                                state = State.Data
                            }

                            0x16 -> {
                                rawData.add(data.toByte())
                                /* Got another SYNC */
                            }

                            else -> {
                                // printf("Got %02x when expecting SYNC or STX\n", data)
                                //output_block(null, null)
                                state = State.Leader /* ERROR */
                            }
                        }
                    }
                }

                State.Data -> {
                    if (bits == 8) {
                        bits = 0
                        rawData.add(data.toByte())
                        buf[counter++] = data.toByte()
                        if (counter == 256) {
                            state = State.Etx
                        }
                    }
                }

                State.Etx -> {
                    if (bits == 8) {
                        bits = 0
                        counter = 0
                        if (data == 0x03) {
                            rawData.add(data.toByte())
                            state = State.CheckSum
                        } else {
                            //printf("Got %02x when expecting ETX\n", data)
                            //output_block(null, null)
                            state = State.Leader /* ERROR */
                        }
                    }
                }

                State.CheckSum -> {
                    if (bits == 8) {
                        bits = 0
                        rawData.add(data.toByte())
                        checkSum[counter++] = data.toByte()
                        if (counter == 2) {
                            save(buf, checkSum, rawData)
                            counter = 0
                            state = State.Leader
                        }
                    }
                }
            }
        }

        private fun save(data: ByteArray, checkSum: ByteArray, rawData: MutableList<Byte>) {
            if (isValidCheckSum(data, checkSum)) {
                rawData.addAll(0, ByteArray(32) { 0 }.toMutableList())
                rawData.add(0x00)
                if (isHeaderBlock(data)) {
                    if (buffers.isNotEmpty())
                        save()
                    buffers.add(data.copyOfRange(3, data.size))
                    rawBuffers.add(rawData.toByteArray())
                    rawData.clear()
                    block = 0
                }
                if (isDataBlock(data)) {
                    buffers.add(data.copyOfRange(3, data.size))
                    rawBuffers.add(rawData.toByteArray())
                    rawData.clear()
                    ++block
                }
            }
        }

        fun ByteArray.int(index: Int) = this[index].toUByte().toInt() + (this[index + 1].toUByte().toInt() shl 8)

        private fun isValidCheckSum(data: ByteArray, readCheckSum: ByteArray): Boolean {
            var checkSum = 0x03u
            data.forEach { checkSum += it.toUByte() }
            return checkSum.and(65535u).toInt() == readCheckSum.int(0)
        }

        private fun isDataBlock(data: ByteArray) = data[0] == 0x00.toByte() && data.int(1) == block

        private fun isHeaderBlock(data: ByteArray) =
            data[0] == 0xff.toByte() && data[1] == 0xff.toByte() && data[2] == 0xff.toByte()

        fun save() {
            val file = File(buffers, rawBuffers)
            val valid = file.validate()
            val text = if (valid) {
                files.add(file)
                "$progressText ${file.name().eightPointThree()}"
            } else {
                "Format error ${file.name().eightPointThree()}"
            }
            Abc80CassetteStatus.show(text)
            buffers = mutableListOf()
            rawBuffers = mutableListOf()
        }
    }
}