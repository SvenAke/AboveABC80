package com.aboveware.abc80

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException

/** Decodes cloud text for display, including the Swedish ABC character positions. */
internal fun ByteArray.toCloudPreviewText(): String {
    val decoded = try {
        Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(this)).toString().removePrefix("\uFEFF")
    } catch (_: CharacterCodingException) {
        toString(Charsets.ISO_8859_1)
    }
    return decoded.map { char ->
        when (char) {
            '[' -> '\u00C4'
            '\\' -> '\u00D6'
            ']' -> '\u00C5'
            '^' -> '\u00DC'
            '{' -> '\u00E4'
            '|' -> '\u00F6'
            '}' -> '\u00E5'
            '~' -> '\u00FC'
            else -> char
        }
    }.joinToString("")
}
