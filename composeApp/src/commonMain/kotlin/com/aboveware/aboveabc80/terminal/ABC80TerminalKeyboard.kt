package com.aboveware.aboveabc80.terminal

import androidx.compose.ui.input.key.Key
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.keyboard.KeyboardLed
import androidx.compose.runtime.mutableStateMapOf

class ABC80TerminalKeyboard(private val terminal: Terminal) : TerminalKeyboard {
    override val leds = mutableStateMapOf("upper_case" to KeyboardLed("upper_case"))

    override fun setLed(index: Int, on: Boolean) {
        leds["upper_case"]?.isOn = on
    }

    override fun updateMappings(mappings: Map<Char, Char>) {
    }

    override fun handleKeyEvent(
        codes: String,
        label: String?,
        secondLabel: String?,
        thirdLabel: String?,
        fourthLabel: String?,
        fifthLabel: String?,
        sixthLabel: String?
    ) {
        Abc80Log.keyboard("ABC80TerminalKeyboard: handleKeyEvent codes='$codes' label='$label'")
        if (codes.isNotEmpty() && !codes.contains(",")) {
            codes.toIntOrNull()?.let { ascii ->
                terminal.onKeyEvent(ascii.toChar())
                terminal.triggerClick()
            }
        }
    }

    override fun handleKeyRelease(codes: String) {
    }

    override fun handlePhysicalKeyEvent(
        key: Key,
        type: androidx.compose.ui.input.key.KeyEventType,
        isCtrl: Boolean,
        isShift: Boolean,
        isRepeat: Boolean,
        timeMillis: Long
    ): Boolean {
        if (type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return false
        if (isRepeat && !terminal.autoRepeatMode) return true
        val char = when (key) {
            Key.Enter -> '\r'
            Key.Backspace -> '\u0008'
            Key.Tab -> '\t'
            Key.Escape -> '\u001B'
            Key.DirectionLeft -> '\u0008'
            Key.DirectionDown -> '\u000A'
            Key.DirectionUp -> '\u000B'
            Key.DirectionRight -> '\u000C'
            Key.MoveHome -> '\u001E'
            Key.F1, Key.F3 -> {
                terminal.toggleSetup(); return true
            }

            else -> null
        }

        if (char != null) {
            terminal.onKeyEvent(char)
            terminal.triggerClick()
            return true
        }
        return false
    }

    override fun transformChar(c: Char, isShiftActive: Boolean): Char {
        val useShift = if (c.isLetter()) isShiftActive xor true else isShiftActive
        return if (useShift) c.uppercaseChar() else c.lowercaseChar()
    }

    override fun stopRepeating() {
    }
}
