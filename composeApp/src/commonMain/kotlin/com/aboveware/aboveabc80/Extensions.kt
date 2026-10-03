package com.aboveware.aboveabc80

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

fun String.tail(length: Int) = this.substring(this.length - length, this.length)

fun Char.isBitSet(bit: Int) = code and (1 shl bit) != 0
fun Char.isBitReset(bit: Int) = code and (1 shl bit) == 0
fun Char.setBit(bit: Int) = Char(code or (1 shl bit))
fun Char.resetBit(bit: Int) = Char(code and (1 shl bit).inv())

fun UByte.isSet(bit: Int) = (0b0000_0001u shl bit and toUInt()) != 0u
fun UByte.isReset(bit: Int) = !isSet(bit)
fun UByte.set(bit: Int) = 0b0000_0001u shl bit or toUInt()
fun UByte.reset(bit: Int) = 0b1111_1110u shl bit and toUInt()

fun Int.isSet(bit: Int) = (0b0000_0001 shl bit and this) != 0
fun Int.isReset(bit: Int) = !isSet(bit)
fun Int.set(bit: Int) = 0b0000_0000 shl bit or this
fun Int.reset(bit: Int) = 0b1111_1110 shl bit and this
fun Int.toBytes(byteOrder: ByteOrder = ByteOrder.BIG_ENDIAN): ByteArray {
    // Int.SIZE_BYTES is 4 in Kotlin
    val buffer = ByteBuffer.allocate(Int.SIZE_BYTES)
    buffer.order(byteOrder)
    buffer.putInt(this)
    return buffer.array()
}

fun String.tab2Space(tabSize: Int = 8): String {
    var textWithSpaces = ""
    split('\t').forEach {
        textWithSpaces += it + " ".repeat(tabSize - it.length % tabSize)
    }
    return textWithSpaces.trimEnd()
}

fun Int.toBinary(len: Int = 8) =
    String.format("0b%" + len + "s", this.toString(2)).replace(" ".toRegex(), "0")

fun Int.toHex(width: Int = 4, case: Char = 'X') = "%0${width}$case".format(this).takeLast(width)
fun Short.toHex(width: Int = 4, case: Char = 'X') = "%0${width}$case".format(this).takeLast(width)
fun Long.toHex(width: Int = 4) = "%0${width}X".format(this)

fun Short.swapBytes(): Short =
    (((this.toInt() and 0xFF) shl 8) or ((this.toInt() ushr 8) and 0xFF)).toShort()

@ExperimentalUnsignedTypes
fun UByte.toBinary() = String.format("%8s", this.toString(2)).replace(" ".toRegex(), "0")

@ExperimentalUnsignedTypes
fun UByte.toHex() = "%02X".format(this.toInt())
fun Byte.toHex() = "%02X".format(this.toInt() and 0xFF)
fun ByteArray.toHex() = joinToString("") { it.toHex() }

const val REGEX_PATTERN: String = "^[A-Za-z0-9.]{1,255}$"
fun String.isValidFilename() = this.matches(REGEX_PATTERN.toRegex())

fun ByteArray.toAscii(address: Int, size: Int = 16): String {
    val ascii = StringBuilder()
    for (index in address until (address + size).coerceAtMost(this.size)) {
        val asChar = this[index].toInt() and 0xFF
        ascii.append(if (asChar in 32..126) asChar.toChar() else '.')
    }
    return ascii.toString()
}

/**
 * Generates a nicely formatted hex dump of the byte array.
 * Includes address, hex values (grouped by 8), and ASCII representation.
 */
fun ByteArray.hexDump(offset: Int = 0, length: Int = this.size - offset): String {
    val builder = StringBuilder()
    val end = (offset + length).coerceAtMost(this.size)
    for (i in offset until end step 16) {
        val lineEnd = (i + 16).coerceAtMost(end)

        // Address (e.g., 0000: )
        builder.append(i.toHex(4)).append(": ")

        // Hex values
        for (j in i until i + 16) {
            if (j < lineEnd) {
                builder.append(this[j].toHex()).append(" ")
            } else {
                builder.append("   ")
            }
            // Extra space between 8-byte groups
            if (j == i + 7) builder.append(" ")
        }

        builder.append(" |")

        // ASCII representation
        for (j in i until lineEnd) {
            val c = this[j].toInt() and 0xFF
            builder.append(if (c in 32..126) c.toChar() else '.')
        }
        builder.append("|")
        if (i + 16 < end) builder.append("\n")
    }
    return builder.toString()
}

fun String.splitFilename(): Pair<String, String> {
    // Om det är en hel filsökväg (t.ex. C:\FOLDER\TEST.TXT), ta bara ut själva filnamnet först
    val cleanName = substringAfterLast('\\').substringAfterLast('/')

    if (!cleanName.contains('.')) {
        return Pair(cleanName, "")
    }

    val name = cleanName.substringBeforeLast('.')
    val ext = cleanName.substringAfterLast('.')

    return Pair(name, ext)
}

fun String.isText() =
    when (substringAfterLast('.', "").lowercase()) {
        "doc",
        "lst",
        "asm",
        "man",
        "bas",
        "tzx",
        "hlp",
        "msg",
        "sub",
        "zzz",
        "hex",
        "for",
        "mac",
        "asc",
        "pdf",
        "txt" -> true

        else -> when (substringBeforeLast('.', "").lowercase()) {
            "catalog" -> true
            else -> false
        }

    }

fun formatSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var s = size.toDouble()
    var unitIndex = 0
    while (s >= 1024.0 && unitIndex < units.size - 1) {
        s /= 1024.0
        unitIndex++
    }
    return String.format(Locale.US, "%.1f %s", s, units[unitIndex])
}
