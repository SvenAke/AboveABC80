package com.aboveware.abovecpm.terminal

import androidx.compose.ui.input.key.Key
import com.aboveware.abovecpm.ZXLog
import com.aboveware.abovecpm.keyboard.KeyboardLed

class ADM3AKeyboard(private val terminal: Terminal) : TerminalKeyboard {
    override val leds = emptyMap<String, KeyboardLed>()

    override fun setLed(index: Int, on: Boolean) {
        // ADM3A has no LEDs
    }

    override fun updateMappings(mappings: Map<Char, Char>) {
        // Not used for ADM3A
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
        ZXLog.keyboard("ADM3AKeyboard: handleKeyEvent codes='$codes' label='$label'")
        if (codes.isNotEmpty() && !codes.contains(",")) {
            codes.toIntOrNull()?.let { ascii ->
                // HERE IS key (Answer Back) is traditionally CTRL-E (0x05)
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
            Key.Backspace -> '\u0008' // Ctrl-H (BS)
            Key.Tab -> '\t'
            Key.Escape -> '\u001B'
            Key.DirectionLeft -> '\u0008'  // ADM-3A: Ctrl-H
            Key.DirectionDown -> '\u000A'  // ADM-3A: Ctrl-J (LF)
            Key.DirectionUp -> '\u000B'    // ADM-3A: Ctrl-K (VT)
            Key.DirectionRight -> '\u000C' // ADM-3A: Ctrl-L (FF)
            Key.MoveHome -> '\u001E'       // ADM-3A: Home
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
