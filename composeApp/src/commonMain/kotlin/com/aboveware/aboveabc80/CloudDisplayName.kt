package com.aboveware.aboveabc80

internal fun String.toCloudDisplayName(): String {
    val utf8Replacements = listOf(
        "Ã¥" to "å", "Ã¤" to "ä", "Ã¶" to "ö",
        "Ã\u0085" to "Å", "Ã\u0084" to "Ä", "Ã\u0096" to "Ö",
        "Ã…" to "Å", "Ã„" to "Ä", "Ã–" to "Ö"
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
            else -> char
        }
    }.joinToString("")
}
