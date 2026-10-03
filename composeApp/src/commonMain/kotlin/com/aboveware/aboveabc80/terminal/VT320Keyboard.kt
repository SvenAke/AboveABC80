package com.aboveware.aboveabc80.terminal

import androidx.compose.ui.input.key.Key
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.keyboard.Keyboard
import com.aboveware.aboveabc80.keyboard.KeyboardLed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class VT320Keyboard(private val terminal: Terminal) : TerminalKeyboard {
    override val leds = mapOf(
        "Hold Screen" to KeyboardLed("Hold Screen"),
        "Lock" to KeyboardLed("Lock"),
        "Compose" to KeyboardLed("Compose"),
        "Wait" to KeyboardLed("Wait")
    )

    override fun setLed(index: Int, on: Boolean) {
        when (index) {
            0 -> leds.values.forEach { it.isOn = false }
            1 -> leds["Hold Screen"]?.isOn = on
            2 -> {
                leds["Lock"]?.isOn = on
                TerminalManager.autoUppercase = on
            }

            3 -> leds["Compose"]?.isOn = on
            4 -> leds["Wait"]?.isOn = on
        }
    }

    private var keyMappings = emptyMap<Char, Char>()

    private val shiftMap = mapOf(
        '1' to '!', '2' to '@', '3' to '#', '4' to '$', '5' to '%',
        '6' to '^', '7' to '&', '8' to '*', '9' to '(', '0' to ')',
        '-' to '_', '=' to '+', '[' to '{', ']' to '}', '\\' to '|',
        ';' to ':', '\'' to '"', ',' to '<', '.' to '>', '/' to '?',
        '`' to '~'
    )

    private val udks = mutableMapOf<Int, String>()
    private val compose = VT320Compose()

    private val diacriticalMarks = setOf('\'', '"', '^', '`', '~', '°', '¨', '´', 'ˆ', '˜', '˚')

    override fun updateMappings(mappings: Map<Char, Char>) {
        keyMappings = mappings
    }

    fun defineUdk(key: Int, definition: String) {
        udks[key] = definition
    }

    fun clearUdks() {
        udks.clear()
    }

    internal fun getUdk(key: Int): String? {
        return udks[key]
    }

    private var isShiftActive = false
    private var isCtrlActive = false

    private val pressedPhysicalKeys = mutableSetOf<Key>()
    private val lastPressTimes = mutableMapOf<Key, Long>()
    private var repeatingJob: Job? = null
    private val keyboardScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val nonRepeatableCodes =
        setOf("SHIFT", "CTRL", "B0", "56", "57", "58", "59", "5A", "13", "7C")
    private val nonRepeatableKeys = setOf(
        Key.Enter, Key.NumPadEnter, Key.CapsLock, Key.CtrlLeft, Key.CtrlRight,
        Key.ShiftLeft, Key.ShiftRight, Key.F2, Key.Break,
        Key.F1, Key.F3, Key.ScrollLock
    )

    override fun handleKeyEvent(
        codes: String,
        label: String?,
        secondLabel: String?,
        thirdLabel: String?,
        fourthLabel: String?,
        fifthLabel: String?,
        sixthLabel: String?
    ) {
        Abc80Log.keyboard("VT320Keyboard: handleKeyEvent codes='$codes' label='$label'")
        if (codes.isEmpty()) return

        // 1. Special state keys (Shift, Ctrl, Lock)
        when (codes) {
            "SHIFT" -> {
                isShiftActive = true; return
            }

            "CTRL" -> {
                isCtrlActive = true
                return
            }

            "B0" -> {
                setLed(2, true); return
            }

            "B1" -> {
                if (VT320Settings.composeKey == 1) {
                    compose.start()
                    setLed(3, true)
                }
                return
            }
        }

        // 2. Resolve labels based on data processing setting
        // Note: label and secondLabel (and others) are swapped in the data model.
        // We now receive: label=normal, secondLabel=shifted, thirdLabel=forth(normal alt), fourthLabel=third(shifted alt)
        val isDataProcessing = VT320Settings.terminalKeyMap == 1
        val activeShiftedLabel =
            if (isDataProcessing && !fourthLabel.isNullOrEmpty()) fourthLabel else secondLabel
        val activeNormalLabel =
            if (isDataProcessing && !thirdLabel.isNullOrEmpty()) thirdLabel else label


        // 3. Check for User Defined Keys (UDK)
        val udkKey = when (codes) {
            "64" -> 17; "65" -> 18; "66" -> 19; "67" -> 20; "68" -> 21
            "71" -> 23; "72" -> 24; "73" -> 25; "74" -> 26
            "7C" -> 28; "7D" -> 29
            "80" -> 31; "81" -> 32; "82" -> 33; "83" -> 34
            else -> -1
        }
        if (udkKey != -1) {
            getUdk(udkKey)?.let {
                sendSequence(it)
                return
            }
        }

        // 4. Prioritize Alphanumeric handling if it's a single char and not a known non-alphanumeric code
        val functionalCodes = setOf(
            "AA", "A9", "A8", "A7",                                     // Arrows
            "A1", "A2", "A3", "A4",                                     // PF1-PF4
            "8A", "8B", "8C", "8D", "8E", "8F",                         // Editing keys
            "64", "65", "66", "67", "68", "71", "72", "73", "74",       // F6-F14
            "7C", "7D", "80", "81", "82", "83",                         // Help, Do, F17-F20
            "92", "96", "97", "98", "99", "9A", "9B", "9D", "9E", "9F", // Keypad 0-9
            "A0", "9C", "94",                                           // Keypad - , .
            "13", "09", "1B",                                           // Enter, Tab, Esc
            "56", "57", "58", "59", "5A"                                // F1-F5
        )

        if (compose.isActive && (functionalCodes.contains(codes) || isCtrlActive)) {
            abortCompose()
        }

        // Restart on Compose key (B1) is already handled in the "when (codes)" block above
        // by calling compose.start() which cancels any previous firstChar.

        if (activeShiftedLabel != null && activeShiftedLabel.length == 1 && !functionalCodes.contains(
                codes
            )
        ) {
            val shiftedChar = activeShiftedLabel[0]
            val normalChar = if (!activeNormalLabel.isNullOrEmpty()) {
                activeNormalLabel[0]
            } else {
                if (shiftedChar.isLetter()) shiftedChar.lowercaseChar() else shiftedChar
            }
            sendMappedKey(normalChar, shiftedChar)

            if (terminal.autoRepeatMode && !nonRepeatableCodes.contains(codes)) {
                startRepeating { sendMappedKey(normalChar, shiftedChar) }
            }
            return
        }

        // 4. Function keys and special scan codes
        when (codes) {
            "56" -> { // Hold Screen or Alphanumeric collision
                if (isCtrlActive && activeShiftedLabel == "8") sendMappedKey('8', '8')
                else toggleHoldScreen()
                return
            }

            "57" -> { // Print Screen or Alphanumeric collision
                if (isCtrlActive && activeShiftedLabel == "9") sendMappedKey('9', '9')
                else terminal.printScreen()
                return
            }

            "58" -> { // Setup or Alphanumeric collision
                if (isCtrlActive && activeShiftedLabel == ":") sendMappedKey(':', ':')
                else {
                    stopRepeating(); terminal.toggleSetup()
                }
                return
            }

            "F3" -> { // Collision in XML, but let's allow it to trigger setup if it's the ?/ key (unlikely)
                // Actually, let's NOT map F3 here yet as it might break ?/ key.
                // But if the user says F3 doesn't work, maybe they ARE clicking a key labeled F3.
                // Let's check functionalCodes.
                if (label == "F3") {
                    stopRepeating(); terminal.toggleSetup(); return
                }
            }

            "59" -> return // F4 (Local)
            "5A" -> { // Break
                if (isCtrlActive) {
                    if (terminal is VT320) terminal.triggerAnswerback()
                } else if (isShiftActive) {
                    // Shift-Break: Modem disconnect (not fully implemented, but let's signal it)
                    Abc80Log.terminal("VT320: Shift-Break (Modem Disconnect) triggered")
                } else if (VT320Settings.breakKey == 1) {
                    Abc80Log.terminal("VT320: Break triggered")
                    Keyboard.instance.pressBreak()
                }
                triggerClick()
                return
            }

            "64" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[17~"); return
            } // F6
            "65" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[18~"); return
            } // F7
            "66" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[19~"); return
            } // F8
            "67" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[20~"); return
            } // F9
            "68" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[21~"); return
            } // F10
            "71" -> { // F11
                val seq = if (terminal.conformanceLevel >= 62) "\u001B[23~" else "\u001B"
                sendSequence(seq); return
            }

            "72" -> { // F12
                val seq = if (terminal.conformanceLevel >= 62) "\u001B[24~" else "\u0008"
                sendSequence(seq); return
            }

            "73" -> { // F13
                val seq = if (terminal.conformanceLevel >= 62) "\u001B[25~" else "\u000A"
                sendSequence(seq); return
            }

            "74" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[26~"); return
            } // F14
            "7C" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[28~"); return
            } // Help
            "7D" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[29~"); return
            } // Do
            "80" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[31~"); return
            } // F17
            "81" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[32~"); return
            } // F18
            "82" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[33~"); return
            } // F19
            "83" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[34~"); return
            } // F20

            "8A" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[1~"); return
            } // Find
            "8B" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[2~"); return
            } // Insert Here
            "8C" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[3~"); return
            } // Remove
            "8D" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[4~"); return
            } // Select
            "8E" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[5~"); return
            } // Prev Screen
            "8F" -> {
                if (terminal.conformanceLevel >= 62) sendSequence("\u001B[6~"); return
            } // Next Screen

            "A1" -> {
                sendPFKey('P'); return
            } // PF1
            "A2" -> {
                sendPFKey('Q'); return
            } // PF2
            "A3" -> {
                sendPFKey('R'); return
            } // PF3
            "A4" -> {
                sendPFKey('S'); return
            } // PF4

            "92" -> {
                sendKeypadKey('p', 'p', '0'); return
            } // 0
            "96" -> {
                sendKeypadKey('q', 'q', '1'); return
            } // 1
            "97" -> {
                sendKeypadKey('r', 'r', '2'); return
            } // 2
            "98" -> {
                sendKeypadKey('s', 's', '3'); return
            } // 3
            "99" -> {
                sendKeypadKey('t', 't', '4'); return
            } // 4
            "9A" -> {
                sendKeypadKey('u', 'u', '5'); return
            } // 5
            "9B" -> {
                sendKeypadKey('v', 'v', '6'); return
            } // 6
            "9D" -> {
                sendKeypadKey('w', 'w', '7'); return
            } // 7
            "9E" -> {
                sendKeypadKey('x', 'x', '8'); return
            } // 8
            "9F" -> {
                sendKeypadKey('y', 'y', '9'); return
            } // 9
            "A0" -> {
                sendKeypadKey('m', 'm', '-'); return
            } // -
            "9C" -> {
                sendKeypadKey('l', 'l', ','); return
            } // ,
            "94" -> {
                sendKeypadKey('n', 'n', '.'); return
            } // .

            "13" -> { // Enter or Return
                if (label == "Enter" && terminal.keypadMode) {
                    val seq = if (terminal.conformanceLevel == 52) "\u001B?M" else "\u001BOM"
                    sendSequence(seq)
                } else {
                    val seq = if (terminal.newLineMode) "\r\n" else "\r"
                    sendSequence(seq)
                }
                return
            }

            "AA" -> { // Up
                val seq = when {
                    terminal.conformanceLevel == 52 -> "\u001BA"
                    terminal.cursorKeysMode -> "\u001BOA"
                    else -> "\u001B[A"
                }
                sendSequence(seq); return
            }

            "A9" -> { // Down
                val seq = when {
                    terminal.conformanceLevel == 52 -> "\u001BB"
                    terminal.cursorKeysMode -> "\u001BOB"
                    else -> "\u001B[B"
                }
                sendSequence(seq); return
            }

            "A8" -> { // Right
                val seq = when {
                    terminal.conformanceLevel == 52 -> "\u001BC"
                    terminal.cursorKeysMode -> "\u001BOC"
                    else -> "\u001B[C"
                }
                sendSequence(seq); return
            }

            "A7" -> { // Left
                val seq = when {
                    terminal.conformanceLevel == 52 -> "\u001BD"
                    terminal.cursorKeysMode -> "\u001BOD"
                    else -> "\u001B[D"
                }
                sendSequence(seq); return
            }
        }

        // 5. Fallback: try to interpret code as integer ASCII
        if (!codes.contains(",")) {
            codes.toIntOrNull()?.let { ascii ->
                val char = ascii.toChar()
                val transformed = transformChar(char, isShiftActive)
                sendChar(transformed)
                triggerClick()

                if (terminal.autoRepeatMode && !nonRepeatableCodes.contains(codes)) {
                    startRepeating { sendChar(transformed) }
                }
            }
        }
    }

    override fun handleKeyRelease(codes: String) {
        when (codes) {
            "SHIFT" -> isShiftActive = false
            "CTRL" -> isCtrlActive = false
            "B0" -> setLed(2, false)
        }
        stopRepeating()
    }

    private fun startRepeating(action: () -> Unit) {
        stopRepeating()
        repeatingJob = keyboardScope.launch {
            delay(400.milliseconds)
            while (true) {
                action()
                delay(50.milliseconds)
            }
        }
    }

    override fun stopRepeating() {
        repeatingJob?.cancel()
        repeatingJob = null
    }

    private fun toggleHoldScreen() {
        val newState = !terminal.holdScreen
        terminal.holdScreen = newState
        leds["Hold Screen"]?.isOn = newState
        // Send XOFF (0x13) to hold, XON (0x11) to resume
        terminal.onKeyEvent(if (newState) 0x13.toChar() else 0x11.toChar())
    }

    private fun sendChar(c: Char) {
        var finalChar = c
        // Handle <x] (Backarrow) setting: Delete (127) or Backspace (8)
        if (finalChar.code == 8 || finalChar.code == 127) {
            val isBackspace = VT320Settings.backArrow == 1
            finalChar = if (isBackspace) '\u0008' else '\u007F'
        }

        // If National Replacement Character (NRC) mode is active, map Unicode back to 7-bit NRC codes
        if (terminal is VT320 && TerminalManager.nationalReplacement) {
            val g0 = terminal.graphics.designated[Graphics.CharacterSetIndex.G0]
            if (g0 is CharacterSet.NationalCharacterSet) {
                val code = g0.getCode(c)
                if (code in 0..127) {
                    finalChar = code.toChar()
                }
            }
        }

        terminal.onKeyEvent(finalChar)
    }

    private fun sendMappedKey(normal: Char, shifted: Char): Char {
        val lockActive = TerminalManager.autoUppercase
        val isShiftLock = VT320Settings.lockKey == 1

        val useShift = if (isShiftLock) {
            lockActive || isShiftActive
        } else {
            if (normal.isLetter()) lockActive xor isShiftActive
            else isShiftActive
        }
        var char = if (useShift) shifted else normal

        if (useShift && VT320Settings.commaPoint == 0) {
            if (normal == ',' && shifted == '<') char = ','
            if (normal == '.' && shifted == '>') char = '.'
        }

        if (VT320Settings.angleBracket == 1) {
            if (char == '<') char = '`'
            if (char == '>') char = '~'
        }

        if (VT320Settings.tildeKey == 1) {
            if (char == '`' || char == '~') char = '\u001B'
        }

        if (interceptKeyEvent(char)) return char

        if (isCtrlActive) {
            char = when (val code = char.uppercaseChar().code) {
                in 64..95 -> (code - 64).toChar()
                32, 50 -> 0.toChar()    // Ctrl + Space, Ctrl + 2 -> NUL
                in 48..57 -> char       // Don't transform numbers (except 2, 3-8 above) - wait.
                else -> char
            }

            // Explicit override for common ctrl keys that might not be in 64..95 range or need special handling
            char = when (char.uppercaseChar().code) {
                51 -> 27.toChar()       // Ctrl + 3 -> ESC
                52, 47 -> 28.toChar()   // Ctrl + 4, Ctrl + / -> FS
                53 -> 29.toChar()       // Ctrl + 5 -> GS
                54, 126 -> 30.toChar()  // Ctrl + 6, Ctrl + ~ -> RS
                55, 63 -> 31.toChar()   // Ctrl + 7, Ctrl + ? -> US
                56 -> 127.toChar()      // Ctrl + 8 -> DEL
                else -> char
            }
        }

        sendChar(char)
        triggerClick()
        return char
    }

    override fun handlePhysicalKeyEvent(
        key: Key,
        type: androidx.compose.ui.input.key.KeyEventType,
        isCtrl: Boolean,
        isShift: Boolean,
        isRepeat: Boolean,
        timeMillis: Long
    ): Boolean {
        if (type == androidx.compose.ui.input.key.KeyEventType.KeyUp) {
            pressedPhysicalKeys.remove(key)
            return false
        }

        if (type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return false

        Abc80Log.keyboard("VT320Keyboard: KeyDown: $key (code=${key.keyCode}) repeat=$isRepeat")

        // Handle Hold Screen (F1) and Setup (F3) keys immediately
        if (key == Key.F1 || key == Key.F3 || key == Key(0xF0)) {
            if (!isRepeat) {
                stopRepeating()
                if (compose.isActive) abortCompose()

                if (key == Key.F1) {
                    Abc80Log.terminal("VT320Keyboard: Hold Screen key recognized: $key")
                    toggleHoldScreen()
                } else {
                    Abc80Log.terminal("VT320Keyboard: Setup key recognized: $key")
                    terminal.toggleSetup()
                }
                terminal.triggerClick()
            }
            pressedPhysicalKeys.add(key)
            lastPressTimes[key] = timeMillis
            return true
        }

        val isInternalRepeat = pressedPhysicalKeys.contains(key)
        val lastTime = lastPressTimes[key] ?: 0L
        val isTimeRepeat = (timeMillis > 0 && lastTime > 0 && (timeMillis - lastTime) < 250)

        if (isRepeat || isInternalRepeat || isTimeRepeat) {
            if (!terminal.autoRepeatMode || nonRepeatableKeys.contains(key)) {
                lastPressTimes[key] = timeMillis
                return true
            }
        }

        pressedPhysicalKeys.add(key)
        lastPressTimes[key] = timeMillis

        val willBeHandledExternally = when (key) {
            Key.Backspace, Key.Delete, Key.Escape -> {
                if (compose.isActive) {
                    abortCompose()
                    return true
                }
                false
            }

            Key.Enter, Key.Tab,
            Key.DirectionUp, Key.DirectionDown, Key.DirectionRight, Key.DirectionLeft,
            Key.CapsLock, Key.NumPad0, Key.NumPad1, Key.NumPad2, Key.NumPad3,
            Key.NumPad4, Key.NumPad5, Key.NumPad6, Key.NumPad7, Key.NumPad8,
            Key.NumPad9, Key.NumPadDot, Key.NumPadEnter, Key.NumPadAdd,
            Key.NumPadSubtract, Key.NumPadMultiply,
            Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8,
            Key.F9, Key.F10, Key.F11, Key.F12,
            Key(0xF0), Key.Break, Key.ScrollLock -> {
                if (compose.isActive) abortCompose()
                false
            }

            else -> {
                if (compose.isActive && isCtrl) abortCompose()
                true
            }
        }

        if (willBeHandledExternally && (isRepeat || isInternalRepeat)) {
            if (!terminal.autoRepeatMode) return true
        }

        val seq = when (key) {
            Key.Enter -> "\r"
            Key.Backspace -> if (VT320Settings.backArrow == 1) "\u0008" else "\u007F"
            Key.Delete -> "\u001B[3~"
            Key.Tab -> "\t"
            Key.Escape -> "\u001B"
            Key.DirectionUp -> if (terminal.cursorKeysMode) "\u001BOA" else "\u001B[A"
            Key.DirectionDown -> if (terminal.cursorKeysMode) "\u001BOB" else "\u001B[B"
            Key.DirectionRight -> if (terminal.cursorKeysMode) "\u001BOC" else "\u001B[C"
            Key.DirectionLeft -> if (terminal.cursorKeysMode) "\u001BOD" else "\u001B[D"
            Key.ScrollLock -> {
                toggleHoldScreen(); return true
            }

            Key.CapsLock -> {
                setLed(2, !leds["Lock"]!!.isOn); return true
            }

            Key.NumPad0 -> if (terminal.keypadMode) "\u001BOp" else transformChar(
                '0',
                isShift
            ).toString()

            Key.NumPad1 -> if (terminal.keypadMode) "\u001BOq" else transformChar(
                '1',
                isShift
            ).toString()

            Key.NumPad2 -> if (terminal.keypadMode) "\u001BOr" else transformChar(
                '2',
                isShift
            ).toString()

            Key.NumPad3 -> if (terminal.keypadMode) "\u001BOs" else transformChar(
                '3',
                isShift
            ).toString()

            Key.NumPad4 -> if (terminal.keypadMode) "\u001BOt" else transformChar(
                '4',
                isShift
            ).toString()

            Key.NumPad5 -> if (terminal.keypadMode) "\u001BOu" else transformChar(
                '5',
                isShift
            ).toString()

            Key.NumPad6 -> if (terminal.keypadMode) "\u001BOv" else transformChar(
                '6',
                isShift
            ).toString()

            Key.NumPad7 -> if (terminal.keypadMode) "\u001BOw" else transformChar(
                '7',
                isShift
            ).toString()

            Key.NumPad8 -> if (terminal.keypadMode) "\u001BOx" else transformChar(
                '8',
                isShift
            ).toString()

            Key.NumPad9 -> if (terminal.keypadMode) "\u001BOy" else transformChar(
                '9',
                isShift
            ).toString()

            Key.NumPadDot -> if (terminal.keypadMode) "\u001BOn" else transformChar(
                '.',
                isShift
            ).toString()

            Key.NumPadEnter -> if (terminal.keypadMode) "\u001BOM" else "\r"
            Key.NumPadAdd -> if (terminal.keypadMode) "\u001BOl" else ","
            Key.NumPadSubtract -> if (terminal.keypadMode) "\u001BOm" else "-"
            Key.NumPadMultiply -> if (terminal.keypadMode) "\u001BOw" else "*"
            Key.F6 -> "\u001B[17~"
            Key.F7 -> "\u001B[18~"
            Key.F8 -> "\u001B[19~"
            Key.F9 -> "\u001B[20~"
            Key.F10 -> "\u001B[21~"
            Key.F11 -> if (terminal.conformanceLevel >= 62) "\u001B[23~" else "\u001B"
            Key.F12 -> if (terminal.conformanceLevel >= 62) "\u001B[24~" else "\u0008"
            Key.F2 -> {
                terminal.printScreen(); terminal.triggerClick(); return true
            }

            Key.AltRight -> {
                if (VT320Settings.composeKey == 1) {
                    handleKeyEvent("B1")
                }
                return true
            } // AltGr maps to Compose
            Key.Break -> {
                if (isCtrl) {
                    if (terminal is VT320) {
                        terminal.triggerAnswerback(); terminal.triggerClick(); return true
                    }
                } else if (isShift) {
                    Abc80Log.terminal("VT320: Shift-Break (Modem Disconnect) triggered")
                    terminal.triggerClick(); return true
                } else if (VT320Settings.breakKey == 1) {
                    Keyboard.instance.pressBreak(); terminal.triggerClick(); return true
                }
                null
            }

            else -> null
        }

        if (seq != null) {
            sendSequence(seq); return true
        }
        return false
    }

    override fun transformChar(c: Char, isShiftActive: Boolean): Char {
        var result = c
        if (VT320Settings.commaPoint == 0) {
            if (result == '<') result = ','
            if (result == '>') result = '.'
        }
        if (VT320Settings.angleBracket == 1) {
            if (result == '<') result = '`'
            if (result == '>') result = '~'
        }
        if (VT320Settings.tildeKey == 1) {
            if (result == '`' || result == '~') result = '\u001B'
        }

        val lockActive = TerminalManager.autoUppercase
        if (!lockActive) return result

        val isShiftLock = VT320Settings.lockKey == 1
        return if (isShiftLock) {
            if (isShiftActive) result
            else if (result.isLetter()) result.uppercaseChar()
            else shiftMap[result] ?: result
        } else {
            if (result.isLetter()) {
                if (isShiftActive) result.lowercaseChar() else result.uppercaseChar()
            } else result
        }
    }

    override fun interceptKeyEvent(char: Char): Boolean {
        if (compose.isActive) {
            // Any Ctrl-other key combination cancels the sequence and performs their usual function.
            if (isCtrlActive) {
                abortCompose()
                return false
            }

            // Cancel on Backspace (8), Delete (127) or ESC (27)
            if (char.code == 8 || char.code == 127 || char.code == 27) {
                abortCompose()
                return true
            }

            // Restart on another diacritical mark
            if (diacriticalMarks.contains(char)) {
                abortCompose()
                compose.start(char)
                setLed(3, true)
                return true
            }

            val context = VT320Compose.Context(
                isDecMultinational = TerminalManager.userPreferredCharacterSetIsDECSupplementalGraphic,
                isNRC = TerminalManager.nationalReplacement,
                keyboardLanguage = (terminal as? VT320)?.currentKeyboardLanguage ?: 0,
                isDataProcessing = VT320Settings.terminalKeyMap == 1
            )
            val composed = compose.processChar(char, context)
            if (composed != null) {
                if (composed != '\u0000') {
                    sendChar(composed)
                } else {
                    // Invalid sequence - ring the bell
                    terminal.onBell?.invoke()
                }
                setLed(3, false) // Compose LED off
            }
            return true
        } else {
            // Start a two-stroke sequence on a diacritical mark (only for typewriter keys and if Compose is enabled)
            if (VT320Settings.composeKey == 1 && VT320Settings.terminalKeyMap == 0 && diacriticalMarks.contains(
                    char
                )
            ) {
                compose.start(char)
                setLed(3, true)
                return true
            }
        }
        return false
    }

    private fun abortCompose() {
        if (compose.isActive) {
            Abc80Log.keyboard("VT320Keyboard: Aborting Compose sequence")
            compose.cancel()
            setLed(3, false)
            terminal.onBell?.invoke()
        }
    }

    private fun triggerClick() {
        terminal.triggerClick()
    }

    private fun sendSequence(seq: String) {
        seq.forEach { terminal.onKeyEvent(it) }
        triggerClick()
    }

    private fun sendPFKey(char: Char) {
        if (terminal.conformanceLevel == 52 && char == 'S') return
        val seq = if (terminal.conformanceLevel == 52) "\u001B$char" else "\u001BO$char"
        sendSequence(seq)
    }

    private fun sendKeypadKey(ansiSuffix: Char, vt52Suffix: Char, normal: Char) {
        if (terminal.conformanceLevel == 52 && (ansiSuffix == 'l' || ansiSuffix == 'S')) {
            sendSequence(normal.toString()); return
        }
        val seq = when {
            terminal.conformanceLevel == 52 -> if (terminal.keypadMode) "\u001B?$vt52Suffix" else normal.toString()
            terminal.keypadMode -> "\u001BO$ansiSuffix"
            else -> normal.toString()
        }
        sendSequence(seq)
    }
}
