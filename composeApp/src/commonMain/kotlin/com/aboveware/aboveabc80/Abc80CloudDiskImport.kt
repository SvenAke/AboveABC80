package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.DiskController
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException

@OptIn(ExperimentalUnsignedTypes::class)
object Abc80CloudDiskImport {
    fun save(drive: Int, files: List<Pair<String, ByteArray>>, clear: Boolean = false) {
        require(files.isNotEmpty()) { "Inga filer valda" }
        val records = files.map { (name, data) -> name to prepare(name, data) }
        val mounted = requireNotNull(DiskController.instance.mountedDisks[drive]) {
            "Ingen diskett är monterad på DR$drive:"
        }
        require(mounted.source == DiskController.Source.LOCAL) { "Disketten måste vara lokalt sparad" }
        NativeLib.getObject().bus.floppy.updateImage(drive, mounted.name) { image ->
            Abc80FloppyLayout.importFiles(image, records, clear)
        }
        Abc80Log.diskett("Saved ${files.size} cloud files to DR$drive: ${mounted.name}")
    }

    internal fun prepare(name: String, data: ByteArray): ByteArray {
        when (name.substringAfterLast('.', "").lowercase()) {
            "bac", "abs" -> {
                require(data.isNotEmpty()) { "Filen är tom: $name" }
                if (name.endsWith(".bac", true)) {
                    require(data[0] == 0x82.toByte()) { "Ogiltig BAC-fil: $name" }
                    var offset = 1
                    var ended = false
                    while (offset < data.size) {
                        val length = data[offset].toInt() and 255
                        if (length == 1) {
                            ended = true
                            break
                        }
                        if (length == 0) {
                            offset = (offset / 253 + 1) * 253
                        } else {
                            require(length >= 3 && offset + length <= data.size &&
                                offset % 253 + length <= 253) { "Ofullständig BAC-fil: $name" }
                            offset += length
                        }
                    }
                    require(ended) { "BAC-filen saknar slutpost: $name" }
                } else {
                    validateAbs(name, data)
                }
            }
            else -> return textRecords(name, data)
        }
        return data.copyOf(((data.size + 252) / 253).coerceAtLeast(1) * 253)
    }

    private fun validateAbs(name: String, data: ByteArray) {
        var offset = 0
        while (offset <= data.size - 7) {
            val type = data[offset].toInt() and 255
            val length = data[offset + 1].toInt() and 255
            if (type == 255) {
                offset = (offset / 253 + 1) * 253
                continue
            }
            require(type == 0 && data[offset + 2] == 0.toByte() &&
                (data[offset + 3].toInt() xor data[offset + 4].toInt()) and 255 == 255 &&
                (data[offset + 5].toInt() xor data[offset + 6].toInt()) and 255 == 255) {
                "Ogiltig ABS-fil: $name"
            }
            if (length == 0) return
            require(offset + length + 8 <= data.size &&
                offset % 253 + length + 8 <= 253) { "Ofullständig ABS-fil: $name" }
            val sum = data.copyOfRange(offset + 7, offset + 7 + length).sum() and 255
            require(sum == data[offset + 7 + length].toInt() and 255) { "Felaktig ABS-kontrollsumma: $name" }
            offset += length + 8
            if (253 - offset % 253 < 8) offset = (offset / 253 + 1) * 253
        }
        throw IllegalArgumentException("ABS-filen saknar slutpost: $name")
    }

    internal fun encodeText(text: String): ByteArray = text.map { char ->
        val code = if (char == '¤') 0x24 else abc80CharCode(char)
        require(code != null && (code in 32..127 || code == 9 || code == 13)) {
            "Texten innehåller tecken som ABC80 inte stöder"
        }
        code.toByte()
    }.toByteArray()

    private fun textRecords(name: String, data: ByteArray): ByteArray {
        val decoded = try {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(data)).toString().removePrefix("\uFEFF")
        } catch (_: CharacterCodingException) {
            data.toString(Charsets.ISO_8859_1)
        }
        val lines = decoded.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val blocks = mutableListOf<ByteArray>()
        var block = ByteArray(253) { 3 }
        var used = 0
        for ((index, line) in lines.dropLastWhile { it.isEmpty() }.withIndex()) {
            require(line.length <= 120) {
                "$name: rad ${index + 1} innehåller ${line.length} tecken (högst 120)"
            }
            val bytes = encodeText(line + "\r")
            // ROM LOAD needs a post terminator before the final payload byte.
            if (used + bytes.size + 2 > block.size) {
                blocks.add(block)
                block = ByteArray(253) { 3 }
                used = 0
            }
            bytes.copyInto(block, used)
            used += bytes.size
        }
        block.fill(0, used)
        blocks.add(block)
        return blocks.flatMap { it.asIterable() }.toByteArray()
    }
}
