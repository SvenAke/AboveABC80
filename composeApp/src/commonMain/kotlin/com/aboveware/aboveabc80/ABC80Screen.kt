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

internal const val ABC80_SCREEN_ROWS = 24
internal const val ABC80_SCREEN_COLUMNS = 40

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

internal fun decodeABC80Screen(memory: ByteArray): List<String> {
    require(memory.size >= 0x8000) { "ABC80 screen requires memory through address 0x7fff" }
    return List(ABC80_SCREEN_ROWS) { row ->
        val start = (tkn40[row * 2 + 1] shl 8) or tkn40[row * 2]
        buildString(ABC80_SCREEN_COLUMNS) {
            for (col in 0 until ABC80_SCREEN_COLUMNS) {
                val value = memory[start + col].toInt() and 0x7F
                append(if (value in 0x20..0x7E) value.toChar() else ' ')
            }
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
fun ABC80Screen(nativeLib: NativeLib, modifier: Modifier = Modifier) {
    val charMap = remember { Abc80MonitorCharacterMap() }
    var rows by remember { mutableStateOf(List(ABC80_SCREEN_ROWS) { " ".repeat(ABC80_SCREEN_COLUMNS) }) }
    LaunchedEffect(nativeLib) {
        while (true) {
            rows = decodeABC80Screen(nativeLib.getMemory())
            delay(50)
        }
    }
    val cw = charMap.charWidth
    val ch = charMap.charHeight
    Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Canvas(Modifier.aspectRatio((ABC80_SCREEN_COLUMNS * cw).toFloat() / (ABC80_SCREEN_ROWS * ch)).fillMaxSize()) {
            val px = size.width / (ABC80_SCREEN_COLUMNS * cw)
            val py = size.height / (ABC80_SCREEN_ROWS * ch)
            val pixel = Size(px, py)
            rows.forEachIndexed { r, line ->
                line.forEachIndexed { c, char ->
                    if (char == ' ') return@forEachIndexed
                    charMap.forEach(char.code) { bits, y ->
                        charMap.forEachBit(bits.toInt()) { on, x ->
                            if (on) drawRect(
                                Color(0xFF90EE90),
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