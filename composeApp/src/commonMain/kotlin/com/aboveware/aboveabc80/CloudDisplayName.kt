package com.aboveware.aboveabc80

internal fun String.toCloudDisplayName(): String {
    val utf8Replacements = listOf(
        "Õ\u0303" to "ö",
        "O\u0303" to "Ö",
        "o\u0303" to "ö",
        "o\u0308" to "ö",
        "O\u0308" to "Ö",
        "a\u0308" to "ä",
        "A\u0308" to "Ä",
        "a\u030A" to "å",
        "A\u030A" to "Å",
        "Ã¥" to "å", "Ã¤" to "ä", "Ã¶" to "ö",
        "Ãµ" to "ö", "Ã•" to "Ö",
        "Ã\u0085" to "Å", "Ã\u0084" to "Ä", "Ã\u0096" to "Ö",
        "Ã…" to "Å", "Ã„" to "Ä", "Ã–" to "Ö",
        "Ã\u00B6" to "ö", "Ã\u00A5" to "å", "Ã\u00A4" to "ä"
    )
    val decoded = utf8Replacements.fold(this) { name, (encoded, letter) ->
        name.replace(encoded, letter)
    }
    return decoded.map { char ->
        when (char) {
            'σ' -> 'å'
            'Σ' -> 'ä'
            '÷' -> 'ö'
            '┼' -> 'Å'
            '─' -> 'Ä'
            '╓' -> 'Ö'
            'Õ', 'õ' -> 'ö'
            '[' -> 'Ä'
            '\\' -> 'Ö'
            ']' -> 'Å'
            '^' -> 'Ü'
            '{' -> 'ä'
            '|' -> 'ö'
            '}' -> 'å'
            '~' -> 'ü'
            else -> char
        }
    }.filter { it != '\u0303' && it != '\u0308' && it != '\u030A' }.joinToString("")
}
