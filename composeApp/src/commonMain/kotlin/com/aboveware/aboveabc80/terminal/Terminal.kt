package com.aboveware.aboveabc80.terminal

import androidx.compose.ui.graphics.Color

enum class CursorStyle {
    BLOCK, UNDERLINE
}

data class TerminalAttributes(
    val bold: Boolean = false,
    val dim: Boolean = false,
    val underline: Boolean = false,
    val blink: Boolean = false,
    val inverse: Boolean = false,
    val selectiveErase: Boolean = false
)

data class TerminalCell(
    val char: Char = ' ',
    val attr: TerminalAttributes = TerminalAttributes()
)

interface Terminal {
    val screen: Array<Array<TerminalCell>>
    val cursorX: Int
    val cursorY: Int
    val cursorVisible: Boolean
    val cursorStyle: CursorStyle
    val cursorKeysMode: Boolean // false = Normal, true = Application
    val keypadMode: Boolean     // false = Numeric, true = Application
    val newLineMode: Boolean    // false = CR, true = CR+LF
    val screenReverse: Boolean
    var autoRepeatMode: Boolean
    var holdScreen: Boolean
    val backgroundColor: Color
    val keyboardXmlResId: Int
    val keyboard: TerminalKeyboard
    val tabulator: Tabulator
    val conformanceLevel: Int

    val nominalWidth: Int
    val nominalHeight: Int
    val dotStretch: Float

    var onBell: (() -> Unit)?
    var onKeyClick: (() -> Unit)?

    fun triggerClick() {
        onKeyClick?.invoke()
    }

    val isSetupVisible: Boolean
        get() = false

    fun putChar(c: Char)
    fun onKeyEvent(char: Char)
    fun hasChar(): Boolean
    fun getChar(): Int
    fun clearScreen()
    fun clearInputBuffer()
    fun connect()
    fun disconnect()
    fun reset()
    fun toggleSetup() {}
    fun printScreen() {}

    fun getGlyph(c: Char): CharacterSet.Glyph?

    fun getScreenState(): ByteArray
    fun setScreenState(data: ByteArray)

    companion object {
        const val DEFAULT_WIDTH = 80
        const val DEFAULT_HEIGHT = 24
    }
}

class Tabulator {
    val stops = BooleanArray(132)

    init {
        reset()
    }

    fun toggle(pos: Int) {
        if (pos in stops.indices) stops[pos] = !stops[pos]
    }

    fun set(pos: Int, value: Boolean) {
        if (pos in stops.indices) stops[pos] = value
    }

    fun clearAll() {
        stops.fill(false)
    }

    fun reset() {
        clearAll()
        for (i in 8 until stops.size step 8) stops[i] = true
    }

    fun nextTab(pos: Int): Int {
        for (i in (pos + 1) until stops.size) {
            if (stops[i]) return i
        }
        return stops.size - 1
    }
}
