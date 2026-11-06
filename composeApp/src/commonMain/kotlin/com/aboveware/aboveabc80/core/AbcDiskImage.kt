package com.aboveware.aboveabc80.core

import com.aboveware.aboveabc80.Abc80FloppyLayout

@OptIn(ExperimentalUnsignedTypes::class)
class AbcDiskImage(private var data: ByteArray) {
    companion object {
        fun createFromData(data: ByteArray): AbcDiskImage {
            requireNotNull(Abc80FloppyLayout.list(data)) { "Diskavbildens format stöds inte" }
            return AbcDiskImage(data)
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
            655360 -> "abc832"
            else -> throw IllegalArgumentException("Diskformatet kan inte formateras")
        }
        val existingVolume = Abc80FloppyLayout.readVolumeName(data)
        data = Abc80FloppyLayout(format, 0).create(existingVolume)
    }
    fun listFiles() = requireNotNull(Abc80FloppyLayout.list(data)).map { (name, _) ->
        Entry(name.substringBeforeLast('.', name), name.substringAfterLast('.', ""))
    }
    fun readFile(filename: String, extension: String) =
        Abc80FloppyLayout.readFile(data, if (extension.isEmpty()) filename else "$filename.$extension")
}
