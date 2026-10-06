package com.aboveware.aboveabc80.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.core.BIOS

private const val adm3aAnswerBack = "aboveCMP\r"

class ADM3A : Terminal {
    companion object {
        const val WIDTH = Terminal.DEFAULT_WIDTH
        const val HEIGHT = Terminal.DEFAULT_HEIGHT
    }

    override var screen by mutableStateOf(Array(HEIGHT) { Array(WIDTH) { TerminalCell() } })
    override var cursorX by mutableIntStateOf(0)
    override var cursorY by mutableIntStateOf(0)
    internal var _cursorVisible by mutableStateOf(true)
    override val cursorVisible: Boolean get() = _cursorVisible && escState == 0
    override var cursorStyle by mutableStateOf(CursorStyle.BLOCK)
    override val cursorKeysMode: Boolean = false
    override val keypadMode: Boolean = false
    override val newLineMode: Boolean = false
    override val screenReverse: Boolean = false
    override var autoRepeatMode: Boolean by mutableStateOf(true)
    override var holdScreen: Boolean by mutableStateOf(false)
    override val backgroundColor: Color = Color(0xFF6D9BC3)
    override val keyboardXmlResId: Int get() = ADM3A_LAYOUT_ID
    override val keyboard: TerminalKeyboard = ADM3AKeyboard(this)
    override val tabulator: Tabulator = Tabulator()
    override val conformanceLevel: Int = 0

    override val nominalWidth: Int = 7
    override val nominalHeight: Int = 9
    override val dotStretch: Float = 2.0f

    override var onBell: (() -> Unit)? = null
    override var onKeyClick: (() -> Unit)? = null
    override var onKeyInput: ((Char) -> Unit)? = null

    private val inputBuffer = mutableListOf<Int>()
    private val lock = Any()
    private var escState = 0
    private var savedCursorX = 0
    private var savedCursorY = 0
    private var inverseVideo = false

    init {
        clearScreen()
    }

    private var isConnected = false

    override fun connect() {
        if (isConnected) return
        isConnected = true
    }

    override fun disconnect() {
        if (!isConnected) return
        isConnected = false
    }

    override fun reset() {
        val wasConnected = isConnected
        clearScreen()
        cursorX = 0
        cursorY = 0
        escState = 0
        savedCursorX = 0
        savedCursorY = 0
        inverseVideo = false
        disconnect()
        if (wasConnected) connect()
    }

    override fun toggleSetup() {
        Abc80Log.terminal("ADM3A: toggleSetup called (not implemented for ADM3A)")
    }

    override fun getGlyph(c: Char): CharacterSet.Glyph? = if (CharacterSet.isLoaded()) {
        CharacterSet.characterSets.adm3aCharacterSet.getGlyph(c)
    } else {
        CharacterSet.getGlyph(c)
    }

    override fun getScreenState(): ByteArray {
        val buffer = ByteArray(WIDTH * HEIGHT * 3 + 4)
        var offset = 0
        // Cursor and state
        buffer[offset++] = (cursorX and 0xFF).toByte()
        buffer[offset++] = (cursorY and 0xFF).toByte()
        buffer[offset++] = (escState and 0xFF).toByte()
        offset++ // padding

        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val cell = screen[y][x]
                buffer[offset++] = (cell.char.code and 0xFF).toByte()
                buffer[offset++] = ((cell.char.code shr 8) and 0xFF).toByte()

                var attrMask = 0
                if (cell.attr.bold) attrMask = attrMask or 0x01
                if (cell.attr.dim) attrMask = attrMask or 0x02
                if (cell.attr.underline) attrMask = attrMask or 0x04
                if (cell.attr.blink) attrMask = attrMask or 0x08
                if (cell.attr.inverse) attrMask = attrMask or 0x10
                buffer[offset++] = attrMask.toByte()
            }
        }
        return buffer
    }

    override fun setScreenState(data: ByteArray) {
        if (data.size < WIDTH * HEIGHT * 3 + 4) return
        var offset = 0
        cursorX = data[offset++].toInt() and 0xFF
        cursorY = data[offset++].toInt() and 0xFF
        escState = data[offset++].toInt() and 0xFF
        offset++

        val newScreen = Array(HEIGHT) { Array(WIDTH) { TerminalCell() } }
        for (y in 0 until HEIGHT) {
            for (x in 0 until WIDTH) {
                val charCode =
                    (data[offset++].toInt() and 0xFF) or ((data[offset++].toInt() and 0xFF) shl 8)
                val attrMask = data[offset++].toInt() and 0xFF

                val attr = TerminalAttributes(
                    bold = (attrMask and 0x01) != 0,
                    dim = (attrMask and 0x02) != 0,
                    underline = (attrMask and 0x04) != 0,
                    blink = (attrMask and 0x08) != 0,
                    inverse = (attrMask and 0x10) != 0
                )
                newScreen[y][x] = TerminalCell(charCode.toChar(), attr)
            }
        }
        screen = newScreen
    }

    override fun clearInputBuffer() {
        synchronized(lock) {
            inputBuffer.clear()
        }
    }

    override fun hasChar(): Boolean = synchronized(lock) {
        inputBuffer.isNotEmpty()
    }

    override fun getChar(): Int = synchronized(lock) {
        if (inputBuffer.isNotEmpty()) {
            val char = inputBuffer.removeAt(0)
            Abc80Log.keyboard("ADM3A read: ${char.toChar()} (${char.toString(16)})")
            char
        } else {
            0x00
        }
    }

    override fun onKeyEvent(char: Char) {
        onKeyInput?.let { it(char); return }
        if (!BIOS.instance.isTransientProgramRunning) {
            TerminalManager.commandHistory.record(char)
        }
        synchronized(lock) {
            if (char.code == 0x05) { // ENQ - Answer Back
                Abc80Log.keyboard("ADM3A: Answer Back triggered")
                adm3aAnswerBack.forEach { inputBuffer.add(it.code) }
            } else {
                Abc80Log.keyboard("ADM3A: onKeyEvent $char")
                inputBuffer.add(char.code)
            }
        }
    }

    override fun putChar(c: Char) {
        val charCode = c.code and 0x7F
        val cleanChar = charCode.toChar()

        if (escState > 0) {
            handleEscape(cleanChar)
            return
        }

        when (charCode) {
            0x05 -> { // ENQ - Answer Back
                onKeyEvent(0x05.toChar())
            }

            0x07 -> { // Bell
                onBell?.invoke()
            }

            0x08 -> { // Backspace (Cursor Left)
                if (cursorX > 0) cursorX--
            }

            0x09 -> { // Horizontal Tab
                cursorX = tabulator.nextTab(cursorX).coerceAtMost(WIDTH - 1)
            }

            0x0A -> { // Line Feed (Cursor Down)
                if (cursorY < HEIGHT - 1) {
                    cursorY++
                } else {
                    scrollUp()
                }
            }

            0x0B -> { // Vertical Tab (Cursor Up)
                if (cursorY > 0) cursorY--
            }

            0x0C -> { // Form Feed (Cursor Right)
                if (cursorX < WIDTH - 1) cursorX++
            }

            0x0D -> { // Carriage Return
                cursorX = 0
            }

            0x1A -> { // Clear Screen and Home
                clearScreen()
            }

            0x1B -> { // ESC
                escState = 1
            }

            0x1E -> { // Home Cursor
                cursorX = 0
                cursorY = 0
            }

            else -> {
                if (charCode in 32..126) {
                    val newScreen = screen.copyOf()
                    val newRow = newScreen[cursorY].copyOf()
                    newRow[cursorX] = TerminalCell(
                        cleanChar,
                        TerminalAttributes(inverse = inverseVideo)
                    )
                    newScreen[cursorY] = newRow
                    screen = newScreen

                    cursorX++
                    if (cursorX >= WIDTH) {
                        cursorX = 0
                        if (cursorY < HEIGHT - 1) {
                            cursorY++
                        } else {
                            scrollUp()
                        }
                    }
                }
            }
        }
    }

    private fun handleEscape(c: Char) {
        if (c.code == 0x1B) {
            escState = 1
            return
        }

        when (escState) {
            1 -> {
                escState = when (c) {
                    '=' -> 2
                    'Y' -> 4
                    'j' -> {
                        savedCursorX = cursorX
                        savedCursorY = cursorY
                        0
                    }
                    'k' -> {
                        cursorX = savedCursorX
                        cursorY = savedCursorY
                        0
                    }
                    'p' -> {
                        inverseVideo = true
                        0
                    }
                    'q' -> {
                        inverseVideo = false
                        0
                    }
                    else -> 0
                }
            }

            2 -> { // Row
                if (c.code in 32..(HEIGHT + 31)) {
                    cursorY = c.code - 32
                    escState = 3
                } else {
                    escState = 0
                }
            }

            3 -> { // Col
                if (c.code in 32..(WIDTH + 31)) cursorX = c.code - 32
                escState = 0
            }

            4 -> {
                if (c.code in 32..(HEIGHT + 31)) {
                    cursorY = c.code - 32
                    escState = 5
                } else {
                    escState = 0
                }
            }

            5 -> {
                if (c.code in 32..(WIDTH + 31)) cursorX = c.code - 32
                escState = 0
            }
        }
    }

    private fun scrollUp() {
        val newScreen = Array(HEIGHT) { y ->
            if (y < HEIGHT - 1) {
                screen[y + 1]
            } else {
                Array(WIDTH) { TerminalCell() }
            }
        }
        screen = newScreen
    }

    override fun clearScreen() {
        screen = Array(HEIGHT) { Array(WIDTH) { TerminalCell() } }
        cursorX = 0
        cursorY = 0
    }
}
