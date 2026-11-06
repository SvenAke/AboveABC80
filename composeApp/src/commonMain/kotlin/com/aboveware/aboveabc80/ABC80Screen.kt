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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.graphics.Canvas as ImageCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
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
import androidx.compose.ui.graphics.BlendMode
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

internal const val ABC800M_HIGH_RESOLUTION_WIDTH = 256
internal const val ABC800M_HIGH_RESOLUTION_HEIGHT = 240
internal const val ABC800M_HIGH_RESOLUTION_BYTES_PER_ROW = 64
internal const val ABC800M_RASTER_WIDTH = 768
internal const val ABC800M_RASTER_HEIGHT = 480
internal const val ABC800M_TEXT_WIDTH = 80 * 6
internal const val ABC800M_TEXT_HEIGHT = 24 * 16
internal const val ABC800M_HIGH_RESOLUTION_DISPLAY_WIDTH =
    ABC800M_HIGH_RESOLUTION_WIDTH * 2f * ABC800M_TEXT_WIDTH / ABC800M_RASTER_WIDTH
internal const val ABC800M_HIGH_RESOLUTION_LEFT =
    115f * ABC800M_TEXT_WIDTH / ABC800M_RASTER_WIDTH
internal const val ABC800M_HIGH_RESOLUTION_DISPLAY_HEIGHT =
    ABC800M_HIGH_RESOLUTION_HEIGHT.toFloat() * ABC800M_TEXT_HEIGHT / ABC800M_RASTER_HEIGHT
internal const val ABC800M_HIGH_RESOLUTION_TOP_SCALE =
    ABC800M_TEXT_HEIGHT.toFloat() / ABC800M_RASTER_HEIGHT

internal fun decodeABC800MHighResolutionScreen(videoRam: ByteArray): ByteArray {
    val requiredSize = ABC800M_HIGH_RESOLUTION_BYTES_PER_ROW * ABC800M_HIGH_RESOLUTION_HEIGHT
    require(videoRam.size >= requiredSize) {
        "ABC800M high-resolution screen requires $requiredSize video bytes"
    }
    return ByteArray(ABC800M_HIGH_RESOLUTION_WIDTH * ABC800M_HIGH_RESOLUTION_HEIGHT).also { pixels ->
        for (y in 0 until ABC800M_HIGH_RESOLUTION_HEIGHT) {
            val rowStart = y * ABC800M_HIGH_RESOLUTION_BYTES_PER_ROW
            val pixelRow = y * ABC800M_HIGH_RESOLUTION_WIDTH
            for (byteIndex in 0 until ABC800M_HIGH_RESOLUTION_BYTES_PER_ROW) {
                val packed = videoRam[rowStart + byteIndex].toInt() and 0xff
                for (pixelIndex in 0 until 4) {
                    pixels[pixelRow + byteIndex * 4 + pixelIndex] =
                        ((packed ushr (6 - pixelIndex * 2)) and 0x03).toByte()
                }
            }
        }
    }
}

internal fun decodeABC800MHighResolutionScreen(
    videoRam: ByteArray,
    control: Int,
    colorProm: ByteArray
): ByteArray {
    require(colorProm.size == 512) { "ABC800M graphics color PROM must be 512 bytes" }
    val paletteOffset = ((control ushr 8) and 0x7f) * 4
    return decodeABC800MHighResolutionScreen(videoRam).also { pixels ->
        for (index in pixels.indices) {
            pixels[index] = if (colorProm[paletteOffset + pixels[index].toInt()].toInt() and 7 != 0) 1 else 0
        }
    }
}

internal const val ABC800C_HIGH_RESOLUTION_WIDTH = 240
internal const val ABC800C_HIGH_RESOLUTION_HEIGHT = 240
internal const val ABC800C_TEXT_CELL_WIDTH = 12
internal const val ABC800C_TEXT_CELL_HEIGHT = 20
internal const val ABC800C_DISPLAY_ASPECT_RATIO = 4f / 3f

internal val ABC800C_PALETTE = listOf(
    Color.Black,
    Color.Red,
    Color.Green,
    Color.Yellow,
    Color.Blue,
    Color.Magenta,
    Color.Cyan,
    Color.White
)

private enum class ABC800CCharType {
    ALPHANUMERIC,
    CONTIGUOUS,
    SEPARATED
}

private fun expandABC800CSixBitRow(bits: Int): Int {
    var expanded = 0
    for (bit in 0 until 6) {
        if (bits and (1 shl (5 - bit)) == 0) continue
        val shift = (5 - bit) * 2
        expanded = expanded or (0b11 shl shift)
    }
    return expanded
}

private fun abc800CGlyphRow(font: ByteArray, code: Int, rasterRow: Int, doubleHeight: Boolean, bottomHalf: Boolean): Int {
    if (rasterRow !in 0 until ABC800C_TEXT_CELL_HEIGHT) return 0
    val teletextRow = if (doubleHeight) {
        rasterRow / 2 + if (bottomHalf) 10 else 0
    } else rasterRow
    // VUM SE has nine glyph rows followed by padding, not sixteen glyph rows.
    // Fit those rows below one blank row in the ten-row teletext character cell.
    val sourceRow = teletextRow / 2 - 1
    if (sourceRow !in 0..8) return 0
    val bits = font[(code and 0x7f) * 16 + sourceRow].toInt() and 0x3f
    return expandABC800CSixBitRow(bits)
}

private fun abc800CGraphicsRow(code: Int, rasterRow: Int, separated: Boolean): Int {
    var rowBits = 0
    when (rasterRow / 2) {
        0, 1 -> {
            if (code and 0x01 != 0) rowBits = rowBits or 0xfc0
            if (code and 0x02 != 0) rowBits = rowBits or 0x03f
            if (separated) rowBits = rowBits and 0x3cf
        }
        2 -> if (!separated) {
            if (code and 0x01 != 0) rowBits = rowBits or 0xfc0
            if (code and 0x02 != 0) rowBits = rowBits or 0x03f
        }
        3, 4, 5 -> {
            if (code and 0x04 != 0) rowBits = rowBits or 0xfc0
            if (code and 0x08 != 0) rowBits = rowBits or 0x03f
            if (separated) rowBits = rowBits and 0x3cf
        }
        6 -> if (!separated) {
            if (code and 0x04 != 0) rowBits = rowBits or 0xfc0
            if (code and 0x08 != 0) rowBits = rowBits or 0x03f
        }
        7, 8 -> {
            if (code and 0x10 != 0) rowBits = rowBits or 0xfc0
            if (code and 0x40 != 0) rowBits = rowBits or 0x03f
            if (separated) rowBits = rowBits and 0x3cf
        }
        9 -> if (!separated) {
            if (code and 0x10 != 0) rowBits = rowBits or 0xfc0
            if (code and 0x40 != 0) rowBits = rowBits or 0x03f
        }
    }
    return rowBits
}

internal fun renderABC800CTextBitmap(characterRam: ByteArray, font: ByteArray, flashVisible: Boolean): ImageBitmap {
    require(characterRam.size >= 40 * ABC80_SCREEN_ROWS) {
        "ABC800C screen requires 960 character bytes"
    }
    val bitmap = ImageBitmap(40 * ABC800C_TEXT_CELL_WIDTH, 24 * ABC800C_TEXT_CELL_HEIGHT)
    val canvas = ImageCanvas(bitmap)
    val paint = Paint()
    val bottomHalfRows = BooleanArray(ABC80_SCREEN_ROWS)
    for (row in 0 until ABC80_SCREEN_ROWS) {
        val sourceRow = if (bottomHalfRows[row] && row > 0) row - 1 else row
        var fg = 7
        var bg = 0
        var graphics = false
        var separated = false
        var flash = false
        var holdGraphics = false
        var heldChar = 0x20
        var nextCharType = ABC800CCharType.ALPHANUMERIC
        var heldCharType = ABC800CCharType.ALPHANUMERIC
        var doubleHeight = false
        var rowStartsDoubleHeight = false
        for (column in 0 until 40) {
            val rawCode = characterRam[sourceRow * 40 + column].toInt() and 0xff
            val controlCode = rawCode and 0x7f
            val previousFg = fg
            val previousFlash = flash
            val previousGraphics = graphics
            val previousHoldGraphics = holdGraphics
            val previousDoubleHeight = doubleHeight
            var currentCharType = nextCharType
            var displayCode = controlCode
            if (controlCode < 0x20) {
                when (controlCode) {
                    in 0x01..0x07 -> {
                        graphics = false
                        fg = controlCode and 0x07
                        nextCharType = ABC800CCharType.ALPHANUMERIC
                    }
                    0x08 -> flash = true
                    0x09 -> flash = false
                    0x0c, 0x0d -> {
                        doubleHeight = controlCode == 0x0d
                        if (doubleHeight) rowStartsDoubleHeight = true
                    }
                    in 0x11..0x17 -> {
                        graphics = true
                        fg = controlCode and 0x07
                        nextCharType = if (separated) {
                            ABC800CCharType.SEPARATED
                        } else {
                            ABC800CCharType.CONTIGUOUS
                        }
                    }
                    0x18 -> fg = bg
                    0x19 -> {
                        separated = false
                        nextCharType = if (graphics) {
                            ABC800CCharType.CONTIGUOUS
                        } else {
                            ABC800CCharType.ALPHANUMERIC
                        }
                    }
                    0x1a -> {
                        separated = true
                        nextCharType = if (graphics) {
                            ABC800CCharType.SEPARATED
                        } else {
                            ABC800CCharType.ALPHANUMERIC
                        }
                    }
                    0x1c -> bg = 0
                    0x1d -> bg = fg
                    0x1e -> holdGraphics = true
                    0x1f -> holdGraphics = false
                }
                if (previousGraphics && (previousHoldGraphics || holdGraphics) && doubleHeight == previousDoubleHeight) {
                    displayCode = heldChar
                    if (displayCode in 0x40..0x5f) displayCode = 0x20
                    currentCharType = heldCharType
                } else {
                    heldChar = 0x20
                    heldCharType = ABC800CCharType.ALPHANUMERIC
                    displayCode = 0x20
                }
            } else if (graphics && controlCode and 0x20 != 0) {
                heldChar = controlCode
                heldCharType = currentCharType
            } else if (!graphics) {
                heldChar = 0x20
                heldCharType = ABC800CCharType.ALPHANUMERIC
            }
            if (previousFlash && !flashVisible) displayCode = 0x20
            if (bottomHalfRows[row] && !doubleHeight) displayCode = 0x20
            val foregroundIndex = previousFg and 0x07
            val backgroundIndex = bg and 0x07
            val cellLeft = column * ABC800C_TEXT_CELL_WIDTH
            val cellTop = row * ABC800C_TEXT_CELL_HEIGHT
            for (rasterRow in 0 until ABC800C_TEXT_CELL_HEIGHT) {
                val pattern = if (currentCharType == ABC800CCharType.ALPHANUMERIC || displayCode and 0x20 == 0) {
                    abc800CGlyphRow(
                        font,
                        displayCode,
                        rasterRow,
                        doubleHeight = previousDoubleHeight,
                        bottomHalf = bottomHalfRows[row]
                    )
                } else {
                    abc800CGraphicsRow(
                        displayCode,
                        rasterRow,
                        separated = currentCharType == ABC800CCharType.SEPARATED
                    )
                }
                var bit = 11
                while (bit >= 0) {
                    val pixelOn = pattern and (1 shl bit) != 0
                    var colorIndex = if (pixelOn) foregroundIndex else backgroundIndex
                    if (rawCode and 0x80 != 0) colorIndex = colorIndex xor 0x07
                    val x = cellLeft + (11 - bit)
                    paint.color = ABC800C_PALETTE[colorIndex]
                    canvas.drawRect(
                        Rect(
                            x.toFloat(),
                            (cellTop + rasterRow).toFloat(),
                            (x + 1).toFloat(),
                            (cellTop + rasterRow + 1).toFloat()
                        ),
                        paint
                    )
                    bit--
                }
            }
        }
        if (!bottomHalfRows[row] && rowStartsDoubleHeight && row + 1 < ABC80_SCREEN_ROWS) {
            bottomHalfRows[row + 1] = true
        }
    }
    return bitmap
}

internal fun decodeABC800CScreen(characterRam: ByteArray): List<String> {
    require(characterRam.size >= 40 * ABC80_SCREEN_ROWS) {
        "ABC800C screen requires 960 character bytes"
    }
    return List(ABC80_SCREEN_ROWS) { row ->
        buildString(40) {
            for (column in 0 until 40) {
                append((characterRam[row * 40 + column].toInt() and 0x7f).toChar())
            }
        }
    }
}

internal fun decodeABC800CHighResolutionScreen(videoRam: ByteArray, control: Int, colorProm: ByteArray): ByteArray {
    val requiredSize = ABC800M_HIGH_RESOLUTION_BYTES_PER_ROW * ABC800C_HIGH_RESOLUTION_HEIGHT
    require(videoRam.size >= requiredSize) {
        "ABC800C high-resolution screen requires $requiredSize video bytes"
    }
    require(colorProm.size == 512) { "ABC800C graphics color PROM must be 512 bytes" }
    val paletteOffset = ((control ushr 8) and 0x7f) * 4
    return ByteArray(ABC800C_HIGH_RESOLUTION_WIDTH * ABC800C_HIGH_RESOLUTION_HEIGHT).also { pixels ->
        for (y in 0 until ABC800C_HIGH_RESOLUTION_HEIGHT) {
            val rowStart = y * ABC800M_HIGH_RESOLUTION_BYTES_PER_ROW
            for (x in 0 until ABC800C_HIGH_RESOLUTION_WIDTH) {
                val packed = videoRam[rowStart + x / 4].toInt() and 0xff
                val sourcePixel = (packed ushr (6 - (x % 4) * 2)) and 0x03
                pixels[y * ABC800C_HIGH_RESOLUTION_WIDTH + x] =
                    (colorProm[paletteOffset + sourcePixel].toInt() and 0x0f).toByte()
            }
        }
    }
}

internal fun renderABC800CDisplay(
    text: ImageBitmap,
    videoRam: ByteArray,
    control: Int,
    colorProm: ByteArray
): ImageBitmap {
    require(text.width == 480 && text.height == 480) { "ABC800C text raster must be 480x480" }
    val graphics = decodeABC800CHighResolutionScreen(videoRam, control, colorProm)
    val textVisible = control and 0x8000 == 0
    val textPixels = if (textVisible) text.toPixelMap() else null
    val bitmap = ImageBitmap(480, 480)
    val canvas = ImageCanvas(bitmap)
    val paint = Paint().apply { color = Color.Black }
    canvas.drawRect(Rect(0f, 0f, 480f, 480f), paint)
    if (textVisible) canvas.drawImage(text, Offset.Zero, Paint())
    val firstLine = control and 0xff
    for (y in firstLine until minOf(480, firstLine + 480)) {
        val sourceRow = (y - firstLine) / 2
        fun overlayColor(x: Int): Int {
            val entry = graphics[sourceRow * ABC800C_HIGH_RESOLUTION_WIDTH + x / 2].toInt()
            val color = entry and 7
            if (color == 0) return 0
            // HRU-II bit 3 preserves non-black text; cleared bit 3 makes graphics opaque.
            return if (entry and 8 == 0 || textPixels == null || textPixels[x, y] == Color.Black) color else 0
        }
        var x = 0
        while (x < 480) {
            val color = overlayColor(x)
            val start = x++
            while (x < 480 && overlayColor(x) == color) x++
            if (color != 0) {
                paint.color = ABC800C_PALETTE[color]
                canvas.drawRect(Rect(start.toFloat(), y.toFloat(), x.toFloat(), (y + 1).toFloat()), paint)
            }
        }
    }
    return bitmap
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
    val isAbc800C = model == ComputerModel.ABC800C
    val isAbc800 = model.isAbc800
    val wideScreen = model.isWideScreen(TKN80.enabled)
    val screenColumns = if (wideScreen) 80 else ABC80_SCREEN_COLUMNS
    var abc800Font by remember { mutableStateOf<ByteArray?>(null) }
    var abc800HrColorProm by remember { mutableStateOf<ByteArray?>(null) }
    LaunchedEffect(model) {
        abc800Font = if (isAbc800) Res.readBytes("files/font800.bin") else null
        abc800HrColorProm = if (isAbc800) Res.readBytes("files/hru800c.bin") else null
    }
    var rows by remember(model) {
        mutableStateOf(List(ABC80_SCREEN_ROWS) { " ".repeat(screenColumns) })
    }
    var cursor by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var cursorRasterRange by remember { mutableStateOf(0x0f00) }
    var blinkOn by remember { mutableStateOf(true) }
    var highResolutionBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var abc800cTextBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var abc800cDisplayBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var highResolutionControl by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    var returningToBasic by remember { mutableStateOf(false) }
    LaunchedEffect(nativeLib, model, wideScreen) {
        var ticks = 0
        var previousHighResolutionVideoRam: ByteArray? = null
        var previousABC800MControl: Int? = null
        var previousABC800MProm: ByteArray? = null
        var previousABC800CControl: Int? = null
        var previousABC800CCharacterRam: ByteArray? = null
        var previousABC800CFlashVisible: Boolean? = null
        var previousABC800CTextBitmap: ImageBitmap? = null
        while (true) {
            if (isAbc800M) {
                abc800cTextBitmap = null
                abc800cDisplayBitmap = null
                rows = decodeABC800MScreen(nativeLib.getAbc800MTextScreen())
                val cursorPosition = nativeLib.getAbc800MCursorPosition()
                cursor = if (cursorPosition >= 0) cursorPosition / 80 to cursorPosition % 80 else null
                cursorRasterRange = nativeLib.getAbc800MCursorRasterRange()
                val videoRam = nativeLib.getAbc800MHighResolutionScreen()
                highResolutionControl = nativeLib.getAbc800MHighResolutionControl()
                val prom = abc800HrColorProm
                if (previousHighResolutionVideoRam?.contentEquals(videoRam) != true ||
                    previousABC800MControl != highResolutionControl ||
                    previousABC800MProm !== prom
                ) {
                    previousHighResolutionVideoRam = videoRam
                    previousABC800MControl = highResolutionControl
                    previousABC800MProm = prom
                    if (prom != null) {
                        val pixels = decodeABC800MHighResolutionScreen(videoRam, highResolutionControl, prom)
                        val bitmap = ImageBitmap(
                            ABC800M_HIGH_RESOLUTION_WIDTH,
                            ABC800M_HIGH_RESOLUTION_HEIGHT
                        )
                        val canvas = ImageCanvas(bitmap)
                        val backgroundPaint = Paint().apply { color = model.screenBackgroundColor }
                        canvas.drawRect(
                            Rect(
                                0f,
                                0f,
                                ABC800M_HIGH_RESOLUTION_WIDTH.toFloat(),
                                ABC800M_HIGH_RESOLUTION_HEIGHT.toFloat()
                            ),
                            backgroundPaint
                        )
                        val foregroundPaint = Paint().apply { color = model.screenColor }
                        for (y in 0 until ABC800M_HIGH_RESOLUTION_HEIGHT) {
                            val rowOffset = y * ABC800M_HIGH_RESOLUTION_WIDTH
                            var x = 0
                            while (x < ABC800M_HIGH_RESOLUTION_WIDTH) {
                                if (pixels[rowOffset + x] == 0.toByte()) {
                                    x++
                                    continue
                                }
                                val start = x
                                while (
                                    x < ABC800M_HIGH_RESOLUTION_WIDTH &&
                                    pixels[rowOffset + x] != 0.toByte()
                                ) {
                                    x++
                                }
                                canvas.drawRect(
                                    start.toFloat(),
                                    y.toFloat(),
                                    x.toFloat(),
                                    (y + 1).toFloat(),
                                    foregroundPaint
                                )
                            }
                        }
                        highResolutionBitmap = bitmap
                    } else {
                        highResolutionBitmap = null
                    }
                }
            } else if (isAbc800C) {
                highResolutionBitmap = null
                val characterRam = nativeLib.getAbc800CTextScreen()
                rows = decodeABC800CScreen(characterRam)
                cursor = null
                val videoRam = nativeLib.getAbc800MHighResolutionScreen()
                val control = nativeLib.getAbc800MHighResolutionControl()
                highResolutionControl = control
                val flashVisible = ticks / 10 % 6 >= 3
                if (abc800Font != null &&
                    (previousABC800CCharacterRam?.contentEquals(characterRam) != true ||
                        abc800cTextBitmap == null ||
                        previousABC800CFlashVisible != flashVisible)
                ) {
                    previousABC800CCharacterRam = characterRam
                    previousABC800CFlashVisible = flashVisible
                    abc800cTextBitmap = renderABC800CTextBitmap(characterRam, abc800Font!!, flashVisible)
                }
                if (previousHighResolutionVideoRam?.contentEquals(videoRam) != true ||
                    previousABC800CControl != control ||
                    previousABC800CTextBitmap !== abc800cTextBitmap ||
                    abc800cDisplayBitmap == null
                ) {
                    val text = abc800cTextBitmap
                    val prom = abc800HrColorProm
                    if (text != null && prom != null) {
                        abc800cDisplayBitmap = renderABC800CDisplay(text, videoRam, control, prom)
                        previousHighResolutionVideoRam = videoRam
                        previousABC800CControl = control
                        previousABC800CTextBitmap = text
                    }
                }
            } else {
                highResolutionBitmap = null
                abc800cTextBitmap = null
                abc800cDisplayBitmap = null
                highResolutionControl = 0
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
    val cw = when {
        isAbc800M -> 6
        isAbc800C -> ABC800C_TEXT_CELL_WIDTH
        else -> charMap.charWidth
    }
    val ch = when {
        isAbc800M -> 16
        isAbc800C -> ABC800C_TEXT_CELL_HEIGHT
        else -> charMap.charHeight
    }
    val columns = rows.firstOrNull()?.length ?: if (wideScreen) 80 else 40
    LaunchedEffect(columns) { selection.clear() }
    Box(
        modifier.fillMaxSize().background(model.caseColor).padding(BEZEL_WIDTH),
        contentAlignment = Alignment.Center
    ) {
        // The ABC800C's 480x480 raster uses non-square pixels on a 4:3 monitor.
        val displayAspectRatio = if (isAbc800C) ABC800C_DISPLAY_ASPECT_RATIO
            else (columns * cw).toFloat() / (ABC80_SCREEN_ROWS * ch)
        Box(Modifier.aspectRatio(displayAspectRatio)
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
            val pixel = Size(px + 0.5f, py + 0.5f)
            if (isAbc800M) {
                highResolutionBitmap?.let { bitmap ->
                    drawImage(
                        image = bitmap,
                        dstOffset = IntOffset(
                            (ABC800M_HIGH_RESOLUTION_LEFT * px).toInt(),
                            ((highResolutionControl and 0xff) * ABC800M_HIGH_RESOLUTION_TOP_SCALE * py).toInt()
                        ),
                        dstSize = IntSize(
                            (ABC800M_HIGH_RESOLUTION_DISPLAY_WIDTH * px).toInt(),
                            (ABC800M_HIGH_RESOLUTION_DISPLAY_HEIGHT * py).toInt()
                        )
                    )
                }
            }
            val abc800TextVisible = !isAbc800 || (highResolutionControl and 0x8000) == 0
            cursor?.takeIf { blinkOn && abc800TextVisible }?.let { (r, c) ->
                if (isAbc800M) {
                    decodeABC800MCursorRasterRanges(cursorRasterRange).forEach { scanlines ->
                        drawRect(
                            model.screenColor,
                            Offset(c * cw * px, (r * ch + scanlines.first) * py),
                            Size(cw * px + 0.5f, scanlines.count() * py + 0.5f)
                        )
                    }
                } else {
                    drawRect(
                        model.screenColor,
                        Offset(c * cw * px, r * ch * py),
                        Size(cw * px + 0.5f, ch * py + 0.5f)
                    )
                }
            }
            if (isAbc800C) {
                abc800cDisplayBitmap?.let { bitmap ->
                    drawImage(
                        image = bitmap,
                        dstSize = IntSize(
                            (columns * cw * px).toInt(),
                            (ABC80_SCREEN_ROWS * ch * py).toInt()
                        )
                    )
                }
                selection.range?.forEach { cell ->
                    val row = cell / columns
                    val column = cell % columns
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(column * cw * px, row * ch * py),
                        size = Size(cw * px + 0.5f, ch * py + 0.5f),
                        blendMode = BlendMode.Difference
                    )
                }
            } else if (abc800TextVisible) rows.forEachIndexed { r, line ->
                line.forEachIndexed { c, char ->
                    val selected = selection.range?.contains(r * columns + c) == true
                    if (selected) drawRect(
                        model.screenColor, Offset(c * cw * px, r * ch * py), Size(cw * px + 0.5f, ch * py + 0.5f)
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
            val scanlineIntensity = TerminalManager.scanlineIntensity
            if (scanlineIntensity > 0f) {
                val totalScanlines = ABC80_SCREEN_ROWS * ch
                val scanlineHeight = py * 0.4f
                val scanlineAlpha = (scanlineIntensity * 1.5f).coerceIn(0f, 0.9f)
                val darkColor = Color.Black.copy(alpha = scanlineAlpha)
                for (y in 0 until totalScanlines) {
                    drawRect(
                        color = darkColor,
                        topLeft = Offset(0f, (y + 0.6f) * py),
                        size = Size(size.width, scanlineHeight)
                    )
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
