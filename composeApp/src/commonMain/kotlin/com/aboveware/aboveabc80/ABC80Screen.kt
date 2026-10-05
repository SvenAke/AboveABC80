package com.aboveware.aboveabc80

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Same as @color/abc80 on Android: the ABC80 case colour around the screen
internal val ABC80_CASE_COLOR = Color(0xFFDFAD75)
internal val ABC80_SCREEN_COLOR = Color(0xFF57C6F8)

internal const val ABC80_SCREEN_ROWS = 24
internal const val ABC80_SCREEN_COLUMNS = 40

const val SCREEN_ROW = 0xFDF3      // 253:243 Screen row
const val SCREEN_COLUMN = 0xFDF4   // 253:244 Screen column

/** Cursor (row, column) read from system RAM, or null if outside the screen. */
internal fun decodeABC80Cursor(memory: ByteArray, columns: Int = TKN80.columns): Pair<Int, Int>? {
    if (memory.size <= SCREEN_COLUMN) return null
    val row = memory[SCREEN_ROW].toInt() and 0xFF
    val col = memory[SCREEN_COLUMN].toInt() and 0xFF
    return if (row < ABC80_SCREEN_ROWS && col < columns) row to col else null
}

internal fun abc80RowAddress(row: Int) = TKN80.rowAddress(row)

/** Writes the characters 0..255 into screen RAM using the current width, starting at [firstRow]. */
fun writeAllCharactersToScreen(nativeLib: NativeLib, firstRow: Int = 2) {
    val columns = TKN80.columns
    for (i in 0 until 256 step columns) {
        val row = firstRow + i / columns
        if (row >= ABC80_SCREEN_ROWS) break
        val chunk = ByteArray(minOf(columns, 256 - i)) { (i + it).toByte() }
        nativeLib.copyToMemory(abc80RowAddress(row), chunk)
    }
}

internal fun decodeABC80Screen(memory: ByteArray, wide: Boolean = TKN80.enabled): List<String> {
    require(memory.size >= 0x8000) { "ABC80 screen requires memory through address 0x7fff" }
    return List(ABC80_SCREEN_ROWS) { row ->
        val start = TKN80.rowAddress(row, wide)
        val columns = if (wide) 80 else 40
        buildString(columns) {
            for (col in 0 until columns) {
                val value = memory[start + col].toInt() and 0xFF
                append(if (value >= 0x20 && value != 0x7F) value.toChar() else ' ')
            }
        }
    }
}

internal fun decodeABC80ScreenGlyphs(memory: ByteArray, wide: Boolean = TKN80.enabled): List<String> {
    require(memory.size >= 0x8000) { "ABC80 screen requires memory through address 0x7fff" }
    return List(ABC80_SCREEN_ROWS) { row ->
        var graphics = false
        val start = TKN80.rowAddress(row, wide)
        val columns = if (wide) 80 else 40
        buildString(columns) {
            for (col in 0 until columns) {
                val value = memory[start + col].toInt() and 0x7F
                when (value) {
                    0x16 -> { graphics = false; append(' ') }
                    0x17 -> { graphics = true; append(' ') }
                    in 0x20..0x7F -> append(
                        if (graphics) (value or 0x80).toChar()
                        else if (value == 0x7F) ' ' else value.toChar()
                    )
                    else -> append(' ')
                }
            }
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun ABC80Screen(nativeLib: NativeLib, modifier: Modifier = Modifier) {
    val charMap = remember { Abc80MonitorCharacterMap() }
    var rows by remember { mutableStateOf(List(ABC80_SCREEN_ROWS) { " ".repeat(ABC80_SCREEN_COLUMNS) }) }
    var cursor by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var blinkOn by remember { mutableStateOf(true) }
    LaunchedEffect(nativeLib) {
        var ticks = 0
        while (true) {
            val memory = nativeLib.getMemory()
            rows = decodeABC80ScreenGlyphs(memory)
            cursor = decodeABC80Cursor(memory)
            if (++ticks % 10 == 0) blinkOn = !blinkOn
            delay(50)
        }
    }
    val cw = charMap.charWidth
    val ch = charMap.charHeight
    val columns = rows.first().length
    Box(modifier.fillMaxSize().background(ABC80_CASE_COLOR), contentAlignment = Alignment.Center) {
        Canvas(Modifier.aspectRatio((columns * cw).toFloat() / (ABC80_SCREEN_ROWS * ch)).fillMaxSize().background(Color.Black)) {
            val px = size.width / (columns * cw)
            val py = size.height / (ABC80_SCREEN_ROWS * ch)
            val pixel = Size(px, py)
            cursor?.takeIf { blinkOn }?.let { (r, c) ->
                drawRect(ABC80_SCREEN_COLOR, Offset(c * cw * px, r * ch * py), Size(cw * px, ch * py))
            }
            rows.forEachIndexed { r, line ->
                line.forEachIndexed { c, char ->
                    if (char == ' ') return@forEachIndexed
                    charMap.forEach(char.code) { bits, y ->
                        charMap.forEachBit(bits.toInt()) { on, x ->
                            if (on) drawRect(
                                ABC80_SCREEN_COLOR,
                                Offset((c * cw + x) * px, (r * ch + y) * py),
                                pixel
                            )
                        }
                    }
                }
            }
        }
    }
}