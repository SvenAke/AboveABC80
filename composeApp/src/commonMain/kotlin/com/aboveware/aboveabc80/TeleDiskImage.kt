package com.aboveware.aboveabc80

internal fun isDiskImageName(name: String): Boolean =
    name.substringAfterLast('.', "").lowercase() in setOf("dsk", "td0")

internal class TeleDiskImage private constructor(
    val rawData: ByteArray,
    private val tracks: Map<Pair<Int, Int>, List<ByteArray>>,
    private val cylinders: Int,
    private val heads: Int
) {
    fun addressMark(physicalSector: Int, rotation: Int): ByteArray {
        val cylinder = physicalSector / 16 % cylinders
        val head = physicalSector / (16 * cylinders)
        require(head in 0 until heads) { "TD0 track outside disk" }
        val ids = requireNotNull(tracks[cylinder to head]) { "Missing TD0 track" }
        val id = ids[rotation.mod(ids.size)]
        return withAddressCrc(id)
    }

    companion object {
        fun withAddressCrc(id: ByteArray, doubleDensity: Boolean = true): ByteArray {
            val prefix = if (doubleDensity) byteArrayOf(0xa1.toByte(), 0xa1.toByte(), 0xa1.toByte(), 0xfe.toByte())
                else byteArrayOf(0xfe.toByte())
            val crc = crc16(prefix + id, 0x1021, 0xffff)
            return id + byteArrayOf((crc shr 8).toByte(), crc.toByte())
        }

        fun hasSignature(data: ByteArray): Boolean =
            data.size >= 2 && ((data[0] == 0x54.toByte() && data[1] == 0x44.toByte()) ||
                (data[0] == 0x74.toByte() && data[1] == 0x64.toByte()))

        fun decode(data: ByteArray): TeleDiskImage {
            require(hasSignature(data) && data.size >= 12) { "Invalid TD0 header" }
            val reader = Reader(data)
            require(crc16(data.copyOfRange(0, 10)) == reader.wordAt(10)) { "TD0 header checksum mismatch" }
            require(reader.byteAt(5) and 0x80 == 0) { "Single-density TD0 is not supported" }
            val heads = reader.byteAt(9)
            require(heads in 1..2) { "Unsupported TD0 head count" }
            val compressed = data[0] == 0x74.toByte()
            require(!compressed || reader.byteAt(4) >= 20) { "TD0 1.x compression is not supported" }
            val body: Input = if (compressed) CompressedInput(data) else Reader(data).apply { position = 12 }
            if (reader.byteAt(7) and 0x80 != 0) {
                val crc = body.word()
                val length = body.word()
                val comment = body.take(6 + length)
                require(crc16(byteArrayOf(length.toByte(), (length shr 8).toByte()) + comment) == crc) {
                    "TD0 comment checksum mismatch"
                }
            }
            val tracks = linkedMapOf<Pair<Int, Int>, List<ByteArray>>()
            val sectors = linkedMapOf<Triple<Int, Int, Int>, ByteArray>()
            while (true) {
                val count = body.byte()
                if (count == 255) break
                val cylinder = body.byte()
                val head = body.byte()
                val trackCrc = body.byte()
                require(crc16(byteArrayOf(count.toByte(), cylinder.toByte(), head.toByte())) and 255 == trackCrc) {
                    "TD0 track checksum mismatch"
                }
                require(count in 16..254 && cylinder in 0..79 && head in 0 until heads) {
                    "Only 16-sector, double-density ABC TD0 tracks are supported"
                }
                val ids = List(count) {
                    val id = body.take(4)
                    val flags = body.byte()
                    val sectorCrc = body.byte()
                    require(id[3].toInt() == 1 && flags == 0) {
                        "TD0 sector size or error/deleted/missing-sector flags are not supported"
                    }
                    val number = id[2].toInt() and 255
                    require(number in 1..16) { "Unsupported TD0 sector number" }
                    val packed = Reader(body.take(body.word()))
                    val block = when (val encoding = packed.byte()) {
                        0 -> packed.take(256)
                        1 -> {
                            val repeats = packed.word()
                            val pattern = packed.take(2)
                            require(repeats == 128) { "Invalid TD0 repeated sector length" }
                            ByteArray(256) { pattern[it % 2] }
                        }
                        2 -> {
                            val result = ArrayList<Byte>(256)
                            while (packed.position < packed.data.size) {
                                val code = packed.byte()
                                val repeats = packed.byte()
                                require(code in 0..7 && repeats > 0) { "Invalid TD0 pattern" }
                                val pattern = packed.take(if (code == 0) repeats else 1 shl code)
                                val copies = if (code == 0) 1 else repeats
                                require(result.size + pattern.size * copies <= 256) { "TD0 sector exceeds 256 bytes" }
                                repeat(copies) { result.addAll(pattern.toList()) }
                            }
                            require(result.size == 256) { "Incomplete TD0 sector" }
                            result.toByteArray()
                        }
                        else -> throw IllegalArgumentException("Unsupported TD0 sector encoding $encoding")
                    }
                    require(packed.position == packed.data.size) { "Trailing TD0 sector data" }
                    require(crc16(block) and 255 == sectorCrc) { "TD0 sector checksum mismatch" }
                    val previous = sectors.put(Triple(cylinder, head, number), block)
                    require(previous == null || previous.contentEquals(block)) {
                        "TD0 duplicate sectors with different data are not supported"
                    }
                    id
                }
                require(tracks.put(cylinder to head, ids) == null) { "Duplicate TD0 track" }
            }
            val cylinders = (tracks.keys.maxOfOrNull { it.first } ?: -1) + 1
            require((cylinders == 40 || cylinders == 80 && heads == 2) &&
                tracks.size == cylinders * heads && sectors.size == cylinders * heads * 16) {
                "Unsupported or incomplete TD0 geometry"
            }
            if (!compressed) {
                val remaining = data.size - (body as Reader).position
                require(remaining == 0 || remaining == 3) { "Trailing TD0 data" }
            }
            val raw = ByteArray(cylinders * heads * 16 * 256)
            for ((key, block) in sectors) {
                val (cylinder, head, number) = key
                block.copyInto(raw, ((head * cylinders + cylinder) * 16 + number - 1) * 256)
            }
            return TeleDiskImage(raw, tracks, cylinders, heads)
        }

        private fun crc16(data: ByteArray, polynomial: Int = 0xa097, initial: Int = 0): Int {
            var crc = initial
            for (byte in data) {
                crc = crc xor ((byte.toInt() and 255) shl 8)
                repeat(8) {
                    crc = ((crc shl 1) xor if (crc and 0x8000 != 0) polynomial else 0) and 0xffff
                }
            }
            return crc
        }
    }

    private interface Input {
        fun byte(): Int
        fun word(): Int = byte() or (byte() shl 8)
        fun take(count: Int): ByteArray {
            require(count >= 0) { "Invalid TD0 length" }
            return ByteArray(count) { byte().toByte() }
        }
    }

    private class CompressedInput(data: ByteArray) : Input {
        private val decoder = TeleDiskLzh(data, 12)
        private var count = 0
        override fun byte(): Int {
            require(count++ < 2 * 1024 * 1024) { "TD0 decompressed data exceeds size limit" }
            return decoder.readByte()
        }
    }

    private class Reader(val data: ByteArray) : Input {
        var position = 0
        fun byteAt(at: Int): Int {
            require(at in data.indices) { "Truncated TD0 data" }
            return data[at].toInt() and 255
        }
        fun wordAt(at: Int) = byteAt(at) or (byteAt(at + 1) shl 8)
        override fun byte() = byteAt(position).also { position++ }
        override fun word() = wordAt(position).also { position += 2 }
        override fun take(count: Int): ByteArray {
            require(count >= 0 && position <= data.size - count) { "Truncated TD0 data" }
            return data.copyOfRange(position, position + count).also { position += count }
        }
    }
}
