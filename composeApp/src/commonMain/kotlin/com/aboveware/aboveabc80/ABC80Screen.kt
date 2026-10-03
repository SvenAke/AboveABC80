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
internal fun decodeABC80Cursor(memory: ByteArray): Pair<Int, Int>? {
    if (memory.size <= SCREEN_COLUMN) return null
    val row = memory[SCREEN_ROW].toInt() and 0xFF
    val col = memory[SCREEN_COLUMN].toInt() and 0xFF
    return if (row < ABC80_SCREEN_ROWS && col < ABC80_SCREEN_COLUMNS) row to col else null
}

// Little-endian (low byte, high byte) start address of each of the 24 rows.
private val tkn40 = intArrayOf(
    0x00, 0x7c, // ROW0
    0x80, 0x7c, // ROW1
    0x00, 0x7d, // ROW2
    0x80, 0x7d, // ROW3
    0x00, 0x7e, // ROW4
    0x80, 0x7e, // ROW5
    0x00, 0x7f, // ROW6
    0x80, 0x7f, // ROW7
    0x28, 0x7c, // ROW8
    0xa8, 0x7c, // ROW9
    0x28, 0x7d, // ROW10
    0xa8, 0x7d, // ROW11
    0x28, 0x7e, // ROW12
    0xa8, 0x7e, // ROW13
    0x28, 0x7f, // ROW14
    0xa8, 0x7f, // ROW15
    0x50, 0x7c, // ROW16
    0xd0, 0x7c, // ROW17
    0x50, 0x7d, // ROW18
    0xd0, 0x7d, // ROW19
    0x50, 0x7e, // ROW20
    0xd0, 0x7e, // ROW21
    0x50, 0x7f, // ROW22
    0xd0, 0x7f  // ROW23
)

internal fun abc80RowAddress(row: Int) = (tkn40[row * 2 + 1] shl 8) or tkn40[row * 2]

/** Writes the characters 0..255 into screen RAM, 40 per row, starting at [firstRow]. */
fun writeAllCharactersToScreen(nativeLib: NativeLib, firstRow: Int = 2) {
    for (i in 0 until 256 step ABC80_SCREEN_COLUMNS) {
        val row = firstRow + i / ABC80_SCREEN_COLUMNS
        if (row >= ABC80_SCREEN_ROWS) break
        val chunk = ByteArray(minOf(ABC80_SCREEN_COLUMNS, 256 - i)) { (i + it).toByte() }
        nativeLib.copyToMemory(abc80RowAddress(row), chunk)
    }
}

internal fun decodeABC80Screen(memory: ByteArray): List<String> {
    require(memory.size >= 0x8000) { "ABC80 screen requires memory through address 0x7fff" }
    return List(ABC80_SCREEN_ROWS) { row ->
        val start = abc80RowAddress(row)
        buildString(ABC80_SCREEN_COLUMNS) {
            for (col in 0 until ABC80_SCREEN_COLUMNS) {
                val value = memory[start + col].toInt() and 0xFF
                append(if (value >= 0x20 && value != 0x7F) value.toChar() else ' ')
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
            rows = decodeABC80Screen(memory)
            cursor = decodeABC80Cursor(memory)
            if (++ticks % 10 == 0) blinkOn = !blinkOn
            delay(50)
        }
    }
    val cw = charMap.charWidth
    val ch = charMap.charHeight
    Box(modifier.fillMaxSize().background(ABC80_CASE_COLOR), contentAlignment = Alignment.Center) {
        Canvas(Modifier.aspectRatio((ABC80_SCREEN_COLUMNS * cw).toFloat() / (ABC80_SCREEN_ROWS * ch)).fillMaxSize().background(Color.Black)) {
            val px = size.width / (ABC80_SCREEN_COLUMNS * cw)
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