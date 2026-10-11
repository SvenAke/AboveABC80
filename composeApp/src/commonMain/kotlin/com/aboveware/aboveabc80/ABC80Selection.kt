package com.aboveware.aboveabc80

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

internal class ABC80Selection {
    private var anchor by mutableStateOf<Int?>(null)
    private var end by mutableStateOf<Int?>(null)

    val range: IntRange?
        get() {
            val first = anchor ?: return null
            val last = end ?: return null
            return minOf(first, last)..maxOf(first, last)
        }

    fun start(cell: Int) {
        anchor = cell
        end = cell
    }

    fun extend(cell: Int) {
        if (anchor != null) end = cell
    }

    fun clear() {
        anchor = null
        end = null
    }
}

internal fun selectedABC80Text(rows: List<String>, range: IntRange): String {
    val columns = rows.first().length
    return (range.first / columns..range.last / columns).joinToString("\n") { row ->
        val start = if (row == range.first / columns) range.first % columns else 0
        val end = if (row == range.last / columns) range.last % columns + 1 else columns
        rows[row].substring(start, end).trimEnd().toAndroid()
    }
}

internal fun abc80PasteCodes(text: String): List<Int> =
    text.replace("\r\n", "\r").replace('\n', '\r').map { char ->
        requireNotNull(abc80CharCode(char)) {
            "Texten innehåller tecken som ABC80 inte stöder: U+${char.code.toString(16).uppercase()}"
        }
    }

internal suspend fun pasteABC80Text(nativeLib: NativeLib, text: String) {
    val codes = abc80PasteCodes(text)
    for (code in codes) {
        nativeLib.sendKey(code)
        delay(if (code == 13) 120 else 20)
    }
}
