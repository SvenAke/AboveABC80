package com.aboveware.aboveabc80

internal enum class Abc80ExecutableFormat { BAS, BAC, ABS }

internal fun abc80ExecutableFormat(
    name: String,
    data: ByteArray,
    recordSize: Int
): Abc80ExecutableFormat? {
    require(recordSize > 0)
    when (name.substringAfterLast('.', "").uppercase()) {
        "BAS" -> return Abc80ExecutableFormat.BAS
        "BAC" -> return Abc80ExecutableFormat.BAC
        "ABS" -> return Abc80ExecutableFormat.ABS
    }
    if (data.size >= 5 && data[0] == 0x82.toByte()) {
        val length = data[1].toInt() and 255
        if (length == 1 || length in 3 until recordSize && length + 1 <= data.size) {
            return Abc80ExecutableFormat.BAC
        }
    }
    if (data.size >= 7 && data[0] == 0.toByte() && data[2] == 0.toByte() &&
        (data[3].toInt() xor data[4].toInt()) and 255 == 255 &&
        (data[5].toInt() xor data[6].toInt()) and 255 == 255) {
        return Abc80ExecutableFormat.ABS
    }
    val text = StringBuilder()
    for (start in data.indices step recordSize) {
        for (index in start until minOf(start + recordSize, data.size)) {
            val code = data[index].toInt() and 255
            if (code == 0 || code == 3) break
            if (code !in 32..127 && code != 9 && code != 10 && code != 13) return null
            text.append(code.toChar())
        }
    }
    val lines = text.toString().replace('\r', '\n').lines().filter { it.isNotBlank() }
    val numberedLine = Regex("""^\s*(\d{1,5})[ \t]+\S.*$""")
    if (lines.isNotEmpty() && lines.all { line ->
        val match = numberedLine.matchEntire(line)
        match != null && match.groupValues[1].toInt() in 1..65535
    }) return Abc80ExecutableFormat.BAS
    return null
}
