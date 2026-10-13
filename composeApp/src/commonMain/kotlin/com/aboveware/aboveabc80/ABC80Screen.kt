package com.aboveware.aboveabc80

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay

/** The ABC80 computer case color (beige/tan) rendered around the monitor display. */
internal val ABC80_CASE_COLOR = Color(0xFFDFAD75)

/** The cyan phosphor foreground color of the ABC80 monitor display. */
internal val ABC80_SCREEN_COLOR = Color(0xFF57C6F8)

/** The fixed number of text rows (24) on the ABC80 screen. */
internal const val ABC80_SCREEN_ROWS = 24

/** The standard number of text columns (40) on the ABC80 screen in normal mode. */
internal const val ABC80_SCREEN_COLUMNS = 40

/** System RAM memory address storing the active screen cursor row (0xFDF3). */
const val SCREEN_ROW = 0xFDF3      // 253:243 Screen row

/** System RAM memory address storing the active screen cursor column (0xFDF4). */
const val SCREEN_COLUMN = 0xFDF4   // 253:244 Screen column

/**
 * Reads and decodes the current cursor position (row, column) from system RAM.
 *
 * @param memory The raw system RAM byte array.
 * @param columns The active screen column count (e.g. 40 in standard mode or 80 in wide mode).
 * @return A [Pair] containing 0-indexed (row, column), or `null` if the cursor is outside valid bounds.
 */
internal fun decodeABC80Cursor(memory: ByteArray, columns: Int = TKN80.columns): Pair<Int, Int>? {
    if (memory.size <= SCREEN_COLUMN) return null
    val row = memory[SCREEN_ROW].toInt() and 0xFF
    val col = memory[SCREEN_COLUMN].toInt() and 0xFF
    return if (row < ABC80_SCREEN_ROWS && col < columns) row to col else null
}

/**
 * Returns the starting RAM address for the specified screen [row].
 *
 * @param row The 0-indexed screen row (0..23).
 * @return The memory offset address corresponding to the start of the row.
 */
internal fun abc80RowAddress(row: Int) = TKN80.rowAddress(row)

/**
 * Writes the full character set (ASCII bytes 0..255) into screen RAM using the current column width.
 *
 * @param nativeLib The native emulator instance providing access to system memory.
 * @param firstRow The 0-indexed row at which to start writing characters (defaults to row 2).
 */
fun writeAllCharactersToScreen(nativeLib: NativeLib, firstRow: Int = 2) {
    val columns = TKN80.columns
    for (i in 0 until 256 step columns) {
        val row = firstRow + i / columns
        if (row >= ABC80_SCREEN_ROWS) break
        val chunk = ByteArray(minOf(columns, 256 - i)) { (i + it).toByte() }
        nativeLib.copyToMemory(abc80RowAddress(row), chunk)
    }
}

/**
 * Decodes the 24 lines of text from screen RAM into plain ASCII strings without graphics processing.
 *
 * @param memory The raw system RAM byte array.
 * @param wide `true` if 80-column wide mode (TKN80) is enabled, `false` for 40-column mode.
 * @return A list of 24 string rows containing plain characters.
 * @throws IllegalArgumentException if memory size is less than 32 KB (0x8000).
 */
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

/**
 * Decodes screen RAM into strings containing glyph characters, evaluating semi-graphics control codes.
 *
 * ABC80 screen memory uses control codes to switch between text and semi-graphics mode on a row.
 * Graphics characters have bit 7 set (`or 0x80`) to differentiate them from standard text characters.
 *
 * @param memory The raw system RAM byte array.
 * @param wide `true` if 80-column wide mode (TKN80) is enabled, `false` for 40-column mode.
 * @return A list of 24 string rows processed for character and graphics glyph rendering.
 * @throws IllegalArgumentException if memory size is less than 32 KB (0x8000).
 */
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
                    0x03, 0x07, 0x16 -> { graphics = false; append(' ') }
                    0x14, 0x17 -> { graphics = true; append(' ') }
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

/**
 * Composable screen component that renders the simulated ABC80 monitor display.
 *
 * Periodically polls the emulator memory to decode and render text and semi-graphics glyphs using
 * [Abc80MonitorCharacterMap], draws a blinking cursor, and supports click and drag text selection.
 *
 * @param nativeLib The native emulator instance providing system memory access.
 * @param modifier The [Modifier] to be applied to the outer layout container.
 * @param selection State holder for the active screen text selection range.
 * @param onRequestFocus Callback triggered when the user interacts with the screen to request input focus.
 */
@OptIn(ExperimentalUnsignedTypes::class)
@Composable
internal fun ABC80Screen(
    nativeLib: NativeLib,
    modifier: Modifier = Modifier,
    selection: ABC80Selection = remember { ABC80Selection() },
    onRequestFocus: () -> Unit = {}
) {
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
    LaunchedEffect(columns) { selection.clear() }
    Box(modifier.fillMaxSize().background(ABC80_CASE_COLOR), contentAlignment = Alignment.Center) {
        Canvas(Modifier.aspectRatio((columns * cw).toFloat() / (ABC80_SCREEN_ROWS * ch))
            .fillMaxSize().background(Color.Black)
            .pointerInput(columns) {
                fun cell(position: Offset): Int {
                    val col = (position.x * columns / size.width).toInt().coerceIn(0, columns - 1)
                    val row = (position.y * ABC80_SCREEN_ROWS / size.height).toInt()
                        .coerceIn(0, ABC80_SCREEN_ROWS - 1)
                    return row * columns + col
                }
                detectDragGestures(
                    onDragStart = { selection.start(cell(it)); onRequestFocus() },
                    onDrag = { change, _ -> selection.extend(cell(change.position)); change.consume() },
                    onDragCancel = { selection.clear() }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { selection.clear(); onRequestFocus() }
            }) {
            val px = size.width / (columns * cw)
            val py = size.height / (ABC80_SCREEN_ROWS * ch)
            val pixel = Size(px, py)
            cursor?.takeIf { blinkOn }?.let { (r, c) ->
                drawRect(ABC80_SCREEN_COLOR, Offset(c * cw * px, r * ch * py), Size(cw * px, ch * py))
            }
            rows.forEachIndexed { r, line ->
                line.forEachIndexed { c, char ->
                    val selected = selection.range?.contains(r * columns + c) == true
                    if (selected) drawRect(
                        ABC80_SCREEN_COLOR, Offset(c * cw * px, r * ch * py), Size(cw * px, ch * py)
                    )
                    if (char == ' ') return@forEachIndexed
                    charMap.forEach(char.code) { bits, y ->
                        charMap.forEachBit(bits.toInt()) { on, x ->
                            if (on) drawRect(
                                if (selected) Color.Black else ABC80_SCREEN_COLOR,
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
