package com.aboveware.aboveabc80

/** In-memory floppy image (ABC830 layout: 40 tracks, 1 head, 16 sectors of 256 bytes). */
@OptIn(ExperimentalUnsignedTypes::class)
class Abc80FloppyLayout(val name: String, val drive: Int) {
    companion object {
        const val SECTOR_SIZE = 256
        const val SECTORS_PER_TRACK = 16
        const val TRACKS = 40
    }

    private val image = ByteArray(TRACKS * SECTORS_PER_TRACK * SECTOR_SIZE)
    private var position = 0
    private var remaining = 0

    private var formatted = false

    // Bit 0 = byte ready, bit 7 = not ready (reported for a blank, never written disk)
    val status: UByte
        get() = if (formatted) 0x01u else 0x81u

    fun open(): Abc80FloppyLayout = this

    fun close() {}

    fun open(track: Int, sector: Int) {
        val index = (track.coerceIn(0, TRACKS - 1) * SECTORS_PER_TRACK + sector.coerceIn(0, SECTORS_PER_TRACK - 1))
        position = index * SECTOR_SIZE
        remaining = SECTOR_SIZE
    }

    fun read(): Int {
        if (remaining == 0) return 0xFF
        remaining--
        return image[position++].toInt() and 0xFF
    }

    fun write(data: UByte) {
        if (remaining == 0) return
        remaining--
        formatted = true
        image[position++] = data.toByte()
    }
}
