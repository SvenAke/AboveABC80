package com.aboveware.aboveabc80.core

import com.aboveware.aboveabc80.Abc80FloppyLayout
import com.aboveware.aboveabc80.ImdParser

class AbcDiskImage(private var data: ByteArray) {
    companion object {
        fun createFromData(data: ByteArray): AbcDiskImage {
            if (!data.take(3).toByteArray().contentEquals("IMD".encodeToByteArray())) return AbcDiskImage(data)
            val parser = ImdParser(data)
            val tracks = parser.parseAllTracks()
            require(tracks.isNotEmpty()) { "IMD-filen saknar spår" }
            require(tracks.all { it.sectorSize == 256 && it.sectorCount == 16 }) {
                "IMD-filen har inte ABC80-sektorer (16 x 256 byte)"
            }
            val bytes = parser.toRawDsk()
            requireNotNull(Abc80FloppyLayout.list(bytes)) { "IMD-formatet stöds inte av ABC80" }
            return AbcDiskImage(bytes)
        }
    }

    data class Entry(val filename: String, val extension: String)

    fun getRawData() = data.copyOf()
    fun getFreeSpace() = Abc80FloppyLayout.freeSpace(data)
    fun format() {
        val format = when (data.size) {
            81920 -> "fd2"
            163840 -> "abc830"
            327680 -> "fd4d"
            else -> throw IllegalArgumentException("Diskformatet kan inte formateras")
        }
        data = Abc80FloppyLayout(format, 0).create()
    }
    fun listFiles() = requireNotNull(Abc80FloppyLayout.list(data)).map { (name, _) ->
        Entry(name.substringBeforeLast('.', name), name.substringAfterLast('.', ""))
    }
    fun readFile(filename: String, extension: String) =
        Abc80FloppyLayout.readFile(data, if (extension.isEmpty()) filename else "$filename.$extension")
}
