package com.aboveware.aboveabc80

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import aboveabc80.composeapp.generated.resources.Res

/** The ABC80 computer case color (beige/tan) rendered around the monitor display. */
internal val ABC80_CASE_COLOR = Color(0xFFDFAD75)

private val BEZEL_WIDTH = 10.dp

/** Black gap between the bezel and the text area. */
private val SCREEN_MARGIN = 12.dp

/** Draws a bevelled frame outside the draw bounds so the screen looks sunk into the case. */
private fun DrawScope.drawRecessedBezel(b: Float) {
    val w = size.width
    val h = size.height
    val top = Color(0xFF7A552A)
    val left = Color(0xFF9A7240)
    val bottom = Color(0xFFC89A62)
    val right = Color(0xFFE6C08C)
    fun quad(a: Offset, bb: Offset, c: Offset, d: Offset, color: Color) =
        drawPath(Path().apply { moveTo(a.x, a.y); lineTo(bb.x, bb.y); lineTo(c.x, c.y); lineTo(d.x, d.y); close() }, color)
    quad(Offset(-b, -b), Offset(w + b, -b), Offset(w, 0f), Offset(0f, 0f), top)
    quad(Offset(-b, -b), Offset(0f, 0f), Offset(0f, h), Offset(-b, h + b), left)
    quad(Offset(-b, h + b), Offset(0f, h), Offset(w, h), Offset(w + b, h + b), bottom)
    quad(Offset(w + b, -b), Offset(w + b, h + b), Offset(w, h), Offset(w, 0f), right)
}

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
internal fun decodeABC80Cursor(
    memory: ByteArray,
    columns: Int = TKN80.columns,
    visible: Boolean = true
): Pair<Int, Int>? {
    if (!visible || memory.size <= SCREEN_COLUMN) return null
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

internal fun decodeABC800MScreen(characterRam: ByteArray): List<String> {
    require(characterRam.size >= 80 * ABC80_SCREEN_ROWS) {
        "ABC800M screen requires 1920 character bytes"
    }
    return List(ABC80_SCREEN_ROWS) { row ->
        buildString(80) {
            for (column in 0 until 80) {
                append((characterRam[row * 80 + column].toInt() and 0x7f).toChar())
            }
        }
    }
}

internal fun decodeABC800MCursorRasterRanges(rasterRange: Int): List<IntRange> {
    val start = rasterRange and 0xff
    val end = (rasterRange ushr 8) and 0xff
    if (start > 15) return emptyList()
    val last = end.coerceAtMost(15)
    return if (start <= last) {
        listOf(start..last)
    } else {
        listOf(start..15, 0..last)
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
@OptIn(ExperimentalUnsignedTypes::class, ExperimentalResourceApi::class)
@Composable
internal fun ABC80Screen(
    nativeLib: NativeLib,
    modifier: Modifier = Modifier,
    selection: ABC80Selection = remember { ABC80Selection() },
    onRequestFocus: () -> Unit = {}
) {
    val charMap = remember { Abc80MonitorCharacterMap() }
    val model = TerminalManager.computerModel
    val isAbc800M = model == ComputerModel.ABC800M
    val wideScreen = model.isWideScreen(TKN80.enabled)
    val screenColumns = if (wideScreen) 80 else ABC80_SCREEN_COLUMNS
    var abc800Font by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(isAbc800M) {
        abc800Font = if (isAbc800M) Res.readBytes("files/font800.bin") else null
    }
    var rows by remember(model) {
        mutableStateOf(List(ABC80_SCREEN_ROWS) { " ".repeat(screenColumns) })
    }
    var cursor by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cursorRasterRange by remember { mutableStateOf(0x0f00) }
    var blinkOn by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    var returningToBasic by remember { mutableStateOf(false) }
    LaunchedEffect(nativeLib, model, wideScreen) {
        var ticks = 0
        while (true) {
            if (isAbc800M) {
                rows = decodeABC800MScreen(nativeLib.getAbc800MTextScreen())
                val cursorPosition = nativeLib.getAbc800MCursorPosition()
                cursor = if (cursorPosition >= 0) cursorPosition / 80 to cursorPosition % 80 else null
                cursorRasterRange = nativeLib.getAbc800MCursorRasterRange()
            } else {
                val memory = nativeLib.getMemory()
                if (abc80DosPromptVisible(memory)) nativeLib.dosActive = true
                rows = decodeABC80ScreenGlyphs(memory, wideScreen)
                cursor = decodeABC80Cursor(
                    memory,
                    columns = screenColumns,
                    visible = nativeLib.screenCursorVisible
                )
            }
            if (++ticks % 10 == 0) blinkOn = !blinkOn
            delay(50)
        }
    }
    val cw = if (isAbc800M) 6 else charMap.charWidth
    val ch = if (isAbc800M) 16 else charMap.charHeight
    val columns = rows.first().length
    LaunchedEffect(columns) { selection.clear() }
    Box(
        modifier.fillMaxSize().background(model.caseColor).padding(BEZEL_WIDTH),
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.aspectRatio((columns * cw).toFloat() / (ABC80_SCREEN_ROWS * ch))
            .fillMaxSize()) {
        Canvas(Modifier
            .fillMaxSize()
            .drawBehind { drawRecessedBezel(BEZEL_WIDTH.toPx()) }
            .background(model.screenBackgroundColor)
            .pointerInput(columns) {
                fun cell(position: Offset): Int {
                    val m = SCREEN_MARGIN.toPx()
                    val w = size.width - 2 * m
                    val h = size.height - 2 * m
                    val col = ((position.x - m) * columns / w).toInt().coerceIn(0, columns - 1)
                    val row = ((position.y - m) * ABC80_SCREEN_ROWS / h).toInt()
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
            val m = SCREEN_MARGIN.toPx()
            inset(m) {
            val px = size.width / (columns * cw)
            val py = size.height / (ABC80_SCREEN_ROWS * ch)
            val pixel = Size(px, py)
            cursor?.takeIf { blinkOn }?.let { (r, c) ->
                if (isAbc800M) {
                    decodeABC800MCursorRasterRanges(cursorRasterRange).forEach { scanlines ->
                        drawRect(
                            model.screenColor,
                            Offset(c * cw * px, (r * ch + scanlines.first) * py),
                            Size(cw * px, scanlines.count() * py)
                        )
                    }
                } else {
                    drawRect(
                        model.screenColor,
                        Offset(c * cw * px, r * ch * py),
                        Size(cw * px, ch * py)
                    )
                }
            }
            rows.forEachIndexed { r, line ->
                line.forEachIndexed { c, char ->
                    val selected = selection.range?.contains(r * columns + c) == true
                    if (selected) drawRect(
                        model.screenColor, Offset(c * cw * px, r * ch * py), Size(cw * px, ch * py)
                    )
                    if (char == ' ') return@forEachIndexed
                    if (isAbc800M) {
                        val font = abc800Font ?: return@forEachIndexed
                        for (y in 0 until 16) {
                            val bits = font[(char.code and 0x7f) * 16 + y].toInt() and 0x3f
                            for (x in 0 until 6) {
                                if (bits and (1 shl (5 - x)) != 0) drawRect(
                                    if (selected) model.screenBackgroundColor else model.screenColor,
                                    Offset((c * cw + x) * px, (r * ch + y) * py),
                                    pixel
                                )
                            }
                        }
                    } else {
                        charMap.forEach(char.code) { bits, y ->
                            charMap.forEachBit(bits.toInt()) { on, x ->
                                if (on) drawRect(
                                    if (selected) model.screenBackgroundColor else model.screenColor,
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
            if (!isAbc800M && nativeLib.dosActive) {
                TextButton(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    enabled = !returningToBasic,
                    onClick = {
                        returningToBasic = true
                        onRequestFocus()
                        scope.launch {
                            try {
                                pasteABC80Text(nativeLib, "\u00a4BASIC\r")
                            } finally {
                                returningToBasic = false
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.RotateLeft, contentDescription = null)
                    Text("BASIC", modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
    }
}
