package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.DiskController
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException

@OptIn(ExperimentalUnsignedTypes::class)
object Abc80CloudDiskImport {
    fun save(drive: Int, files: List<Pair<String, ByteArray>>, clear: Boolean = false) {
        require(files.isNotEmpty()) { "No files selected" }
        Abc80Log.diskette("Importing ${files.size} cloud files to DR$drive: [${files.joinToString { it.first }}]")
        val records = try {
            files.map { (name, data) -> name to prepare(name, data) }
        } catch (e: Exception) {
            Abc80Log.wtf("Cloud import prepare failed for DR$drive: ${e.message}")
            throw e
        }
        val mounted = requireNotNull(DiskController.instance.mountedDisks[drive]) {
            "No floppy disk mounted on DR$drive:"
        }
        NativeLib.getObject().bus.floppy.updateImage(drive, mounted.name) { image ->
            Abc80FloppyLayout.importFiles(image, records, clear)
        }
        Abc80Log.diskette("Saved ${files.size} cloud files to DR$drive: ${mounted.name}")
    }

    internal fun prepare(name: String, data: ByteArray): ByteArray {
        when (name.substringAfterLast('.', "").lowercase()) {
            "bac", "abs" -> {
                require(data.isNotEmpty()) { "File is empty: $name" }
                if (name.endsWith(".bac", true)) {
                    validateBac(name, data, 253)
                } else {
                    validateAbs(name, data)
                }
            }
            else -> return textRecords(name, data)
        }
        return data.copyOf(((data.size + 252) / 253).coerceAtLeast(1) * 253)
    }

    internal fun validateBac(name: String, data: ByteArray, recordSize: Int) {
        require(data.isNotEmpty() && data[0] == 0x82.toByte()) { "Invalid BAC file: $name" }
        var offset = 1
        while (offset < data.size) {
            val length = data[offset].toInt() and 255
            if (length == 1) return
            if (length == 0) {
                offset = (offset / recordSize + 1) * recordSize
            } else {
                require(length >= 3 && offset + length <= data.size &&
                    offset % recordSize + length <= recordSize) { "Incomplete BAC file: $name" }
                offset += length
            }
        }
        throw IllegalArgumentException("BAC file missing end record: $name")
    }

    internal fun validateAbs(name: String, data: ByteArray, recordSize: Int = 253) {
        var offset = 0
        while (offset <= data.size - 7) {
            val type = data[offset].toInt() and 255
            val length = data[offset + 1].toInt() and 255
            if (type == 255) {
                offset = (offset / recordSize + 1) * recordSize
                continue
            }
            require(type == 0 && data[offset + 2] == 0.toByte() &&
                (data[offset + 3].toInt() xor data[offset + 4].toInt()) and 255 == 255 &&
                (data[offset + 5].toInt() xor data[offset + 6].toInt()) and 255 == 255) {
                "Invalid ABS file: $name"
            }
            if (length == 0) return
            require(offset + length + 8 <= data.size &&
                offset % recordSize + length + 8 <= recordSize) { "Incomplete ABS file: $name" }
            val sum = data.copyOfRange(offset + 7, offset + 7 + length).sum() and 255
            require(sum == data[offset + 7 + length].toInt() and 255) { "Incorrect ABS checksum: $name" }
            offset += length + 8
            if (recordSize - offset % recordSize < 8) offset = (offset / recordSize + 1) * recordSize
        }
        throw IllegalArgumentException("ABS file missing end record: $name")
    }

    internal fun encodeText(text: String, context: String = ""): ByteArray = text.map { char ->
        val code = if (char == '¤') 0x24 else abc80CharCode(char)
        if (code == null || (code !in 32..127 && code != 9 && code != 13)) {
            val hexCode = "U+" + char.code.toHex(4)
            val ctxStr = if (context.isNotBlank()) " in $context" else ""
            val detail = "Unsupported character '$char' ($hexCode)$ctxStr"
            Abc80Log.wtf("encodeText: $detail")
            throw IllegalArgumentException(detail)
        }
        code.toByte()
    }.toByteArray()

    private fun textRecords(name: String, data: ByteArray): ByteArray {
        // Legacy text files use Ctrl+Z as EOF; trailing bytes are padding, not text.
        val end = data.indexOf(0x1A.toByte())
        val textData = if (end >= 0) data.copyOf(end) else data
        val decoded = try {
            Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(textData)).toString().removePrefix("\uFEFF")
        } catch (_: CharacterCodingException) {
            textData.toString(Charsets.ISO_8859_1)
        }
        val lines = decoded.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val blocks = mutableListOf<ByteArray>()
        var block = ByteArray(253) { 3 }
        var used = 0
        for ((index, line) in lines.dropLastWhile { it.isEmpty() }.withIndex()) {
            require(line.length <= 120) {
                "$name: line ${index + 1} contains ${line.length} characters (maximum 120)"
            }
            val bytes = encodeText(line + "\r", "$name (line ${index + 1})")
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
