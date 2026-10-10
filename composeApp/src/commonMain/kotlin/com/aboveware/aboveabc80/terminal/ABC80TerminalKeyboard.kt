package com.aboveware.aboveabc80.terminal

import androidx.compose.ui.input.key.Key
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.abc80TableCode
import com.aboveware.aboveabc80.keyboard.KeyboardLed
import androidx.compose.runtime.mutableStateMapOf

class ABC80TerminalKeyboard(private val terminal: Terminal) : TerminalKeyboard {
    private var shift = false
    private var control = false
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
        when (codes) {
            "0,0" -> { shift = true; return }
            "7,1" -> { control = true; return }
        }
        if (codes.startsWith("abc80:")) {
            val number = codes.removePrefix("abc80:").toIntOrNull()
            val code = number?.let {
                abc80TableCode(it, shift || leds.getValue("upper_case").isOn, control)
            }
            if (code == null) {
                Abc80Log.wtf("Unknown ABC80 keyboard key: $codes")
                return
            }
            terminal.onKeyEvent(code.toChar())
            return
        }
        if (codes.isNotEmpty() && !codes.contains(",")) {
            codes.toIntOrNull()?.let { ascii ->
                terminal.onKeyEvent(ascii.toChar())
                terminal.triggerClick()
            }
        }
    }

    override fun handleKeyRelease(codes: String) {
        when (codes) {
            "0,0" -> shift = false
            "7,1" -> control = false
        }
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
        return if (isShiftActive || leds.getValue("upper_case").isOn) c.uppercaseChar() else c.lowercaseChar()
    }

    override fun stopRepeating() {
    }
}
