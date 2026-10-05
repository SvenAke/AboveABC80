@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package com.aboveware.aboveabc80

@ExperimentalUnsignedTypes
class Abc80Program : BooleanIterator() {

    internal var endOfTape = false
    internal var filename = ""
    private var program = mutableListOf<Boolean>()
    private val samples = mutableListOf<Sample>()
    val fileBlocks = mutableListOf<Block>()


    private val syncLimit = 255
    private val syncStart = 10

    data class Sample(val value: Boolean, val timeStamp: Long)

    internal fun isNotEmpty() = program.isNotEmpty()
    fun noSamples() = samples.isEmpty()

    fun resample(): Boolean {
        try {

            if (samples.size < syncStart) return false
            // As the ABC80 only writes the 0's at T and 1's at T/2 we need
            // need to resample the 0's to two equal sample at T/2
            // First we need to synchronize
            // We know that the first 256 samples are 0's
            // so it's easy to determine T
            var t = 0L
            for (index in syncStart..syncLimit) {
                val delta = samples[index + 1].timeStamp - samples[index].timeStamp
                t += delta
            }
            t *= 2
            t /= (syncLimit - syncStart)
            t /= 3     // Now we have 2/3's of T

            var index = 1
            program = mutableListOf()
            while (index < samples.size - 1) {
                val delta = samples[index + 1].timeStamp - samples[index].timeStamp
                if (delta > t) {
                    program.add(false)
                } else {
                    program.add(true)
                    ++index
                }
                ++index
            }
            return verify()
        } catch (e: IndexOutOfBoundsException) {
            return false
        }
    }

    fun add(sample: Sample) {
        samples.add(sample)
    }

    internal fun verify(): Boolean {
        val blockBits = Block.BITS
        if (program.size < blockBits) return false
        val nameBlock = NameBlock()
        nameBlock.load(program.subList(0, blockBits))
        fileBlocks.add(nameBlock)
        if (!nameBlock.verify()) return false
        var startIndex = blockBits
        var blockNumber = 0
        while (startIndex < program.size) {
            val dataBlock = DataBlock(blockNumber)
            dataBlock.load(program.subList(startIndex, startIndex + blockBits))
            fileBlocks.add(dataBlock)
            if (!dataBlock.verify()) return false
            startIndex += blockBits
            ++blockNumber
        }
        return true
    }

    @ExperimentalUnsignedTypes
    open class Block {
        companion object {
            const val BYTES = 32 + 8 + 256
            const val BITS = BYTES * 8
        }

        private var marker: UByte = 0x00u
        private var crcLow: UByte = 0x00u
        private var crcHigh: UByte = 0x00u
        private val header = BooleanArray(256) { true }
        private val sync = ubyteArrayOf(0x00u, 0x00u, 0x00u)
        private var stx: UByte = 0x00u
        private var etx: UByte = 0x00u
        private var crc: UShort = 0x00u
        protected val data = UByteArray(256)
        private var index = 0
        private var samples = mutableListOf<Boolean>()

        private fun bit() = samples[index++]

        private fun byte(): UByte {
            var data = 0x00
            for (bit in 0..7) {
                data = data shr 1
                if (bit()) data = data.or(0x80)
            }
            return data.toUByte()
        }

        open fun verify(): Boolean {
            for (element in header) if (element) return false
            if (sync[0] != 0x16u.toUByte()) return false
            if (sync[1] != 0x16u.toUByte()) return false
            if (sync[2] != 0x16u.toUByte()) return false
            if (stx != 0x02u.toUByte()) return false
            if (etx != 0x03u.toUByte()) return false
            var crc16 = 0x0000
            for (element in data)
                crc16 += element.toInt()
            crc16 += etx.toInt()
            return crc16 == crc.toInt()
        }

        fun toByteArray(): ByteArray {
            val array = UByteArray(BYTES) { 0x00u }
            var index = 32
            array[index++] = sync[0]
            array[index++] = sync[1]
            array[index++] = sync[2]
            array[index++] = stx
            for (i in data.indices)
                array[index++] = data[i]
            array[index++] = etx
            array[index++] = crcLow
            array[index] = crcHigh
            return array.toByteArray()
        }

        fun load(samples: MutableList<Boolean>) {
            this.samples = samples
            for (i in header.indices) header[i] = bit()
            sync[0] = byte()
            sync[1] = byte()
            sync[2] = byte()
            stx = byte()
            for (i in data.indices)
                data[i] = byte()
            etx = byte()
            crcLow = byte()
            crcHigh = byte()
            crc = (crcHigh * 256u + crcLow).toUShort() and 0xffffu
            marker = byte()
        }

        fun load(array: ByteArray) {
            for (index in 0..31) {
                val startIndex = index * 8
                for (i in startIndex..startIndex + 7) {
                    header[i] = array[index] != 0x00.toByte()
                }
            }
            index += 32
            sync[0] = array[index++].toUByte()
            sync[1] = array[index++].toUByte()
            sync[2] = array[index++].toUByte()
            stx = array[index++].toUByte()
            for (i in data.indices)
                data[i] = array[index++].toUByte()
            etx = array[index++].toUByte()
            crcLow = array[index++].toUByte()
            crcHigh = array[index++].toUByte()
            crc = (crcHigh * 256u + crcLow).toUShort() and 0xffffu
            marker = array[index].toUByte()
        }

        private fun program(data: UByte) {
            var bits = data.toInt()
            for (index in 0..7) {
                samples.add(bits.and(0x01) == 0x01)
                bits = bits shr 1
            }
        }

        fun program(): Collection<Boolean> {
            samples.addAll(header.toList())
            program(sync[0])
            program(sync[1])
            program(sync[2])
            program(stx)
            for (i in data.indices)
                program(data[i])
            program(etx)
            program(crcLow)
            program(crcHigh)
            program(0x00u)  // todo figure this one out!
            return samples
        }
    }

    @ExperimentalUnsignedTypes
    class NameBlock : Block() {
        var name = ""
        override fun verify(): Boolean {
            if (!super.verify()) return false
            if (data[0] != 0xffu.toUByte()) return false
            if (data[1] != 0xffu.toUByte()) return false
            if (data[2] != 0xffu.toUByte()) return false
            val nameString = StringBuilder()
            for (index in 3..13)
                nameString.append(data[index].toByte().toInt().toChar())
            for (index in 14..255)
                if (data[index] != 0x00u.toUByte()) return false
            name = nameString.toString()
            return true
        }

        override fun toString(): String {
            return "NameBlock(name='$name')"
        }
    }

    @ExperimentalUnsignedTypes
    class DataBlock(private val blockNumber: Int) : Block() {
        override fun verify(): Boolean {
            if (!super.verify()) return false
            if (data[0] != 0x00u.toUByte()) return false
            return blockNumber == (data[2] * 256u + data[1]).toInt()
        }

        override fun toString(): String {
            return "DataBlock(blockNumber=$blockNumber)"
        }
    }

    private var next = 0
    override fun hasNext() = program.size > next
    override fun nextBoolean() = if (hasNext()) program[next++] else false

    fun init(filenameOnStack: String = filename) {
        program.clear()
        samples.clear()
        fileBlocks.clear()
        filename = filenameOnStack
        next = 0
        endOfTape = false
    }

    fun size() = program.size

    fun progress() = next

    fun add(block: Block) {
        program.addAll(block.program())
    }

    fun markError() {
        program.add(false)
    }

    fun markEndOfTape() {
        markError()
        endOfTape = true
    }

    fun name(): String {
        return if (fileBlocks.size != 0 && fileBlocks[0] is NameBlock) {
            (fileBlocks[0] as NameBlock).name
        } else ""
    }
}