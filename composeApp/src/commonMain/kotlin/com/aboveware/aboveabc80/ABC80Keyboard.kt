package com.aboveware.aboveabc80

import androidx.compose.ui.input.key.Key
import java.nio.charset.StandardCharsets

/**
 * Maps a host key press to the 7-bit code the ABC80 keyboard delivers on PIO port A,
 * or null if the key has no ABC80 equivalent.
 */
internal fun abc80KeyCode(key: Key, char: Char?, shift: Boolean, control: Boolean = false): Int? {
    val effectiveShift = shift || (char?.isUpperCase() == true)
    val symbolNumber = when (char) {
        'É', 'é', '`', '@' -> 12
        'Å', 'å' -> 30
        'Ü', 'ü' -> 31
        'Ö', 'ö' -> 47
        'Ä', 'ä' -> 48
        '<', '>', '\u007F' -> if (key == Key.Delete) null else 13
        '¤' -> 4
        else -> if (char != null && char.code >= 32 && !char.isLetter()) {
            abc80KeyTable.entries.firstOrNull {
                it.value.unshifted == char.code || it.value.shifted == char.code
            }?.key
        } else null
    }
    if (symbolNumber != null) return abc80TableCode(symbolNumber, effectiveShift, control)
    val number = when (key) {
        Key.One -> 1
        Key.Two -> 2
        Key.Three -> 3
        Key.Four -> 4
        Key.Five -> 5
        Key.Six -> 6
        Key.Seven -> 7
        Key.Eight -> 8
        Key.Nine -> 9
        Key.Zero -> 10
        Key.Minus -> 11
        Key.Equals, Key.Grave -> 12
        Key.Q -> 20
        Key.W -> 21
        Key.E -> 22
        Key.R -> 23
        Key.T -> 24
        Key.Y -> 25
        Key.U -> 26
        Key.I -> 27
        Key.O -> 28
        Key.P -> 29
        Key.LeftBracket -> 30
        Key.RightBracket -> 31
        Key.A -> 38
        Key.S -> 39
        Key.D -> 40
        Key.F -> 41
        Key.G -> 42
        Key.H -> 43
        Key.J -> 44
        Key.K -> 45
        Key.L -> 46
        Key.Semicolon -> 47
        Key.Apostrophe -> 48
        Key.Backslash -> 49
        Key.Z -> 56
        Key.X -> 57
        Key.C -> 58
        Key.V -> 59
        Key.B -> 60
        Key.N -> 61
        Key.M -> 62
        Key.Comma -> 63
        Key.Period -> 64
        Key.Spacebar -> 74
        else -> null
    }
    if (number != null) return abc80TableCode(number, effectiveShift, control)
    when (key) {
        Key.Enter, Key.NumPadEnter -> return 0x0D
        Key.Backspace, Key.DirectionLeft -> return 0x08
        Key.DirectionRight -> return 0x09
        Key.DirectionDown -> return 0x0A
        Key.DirectionUp -> return 0x0B
        Key.Escape -> return 0x1B
        Key.Delete -> return 0x7F
        Key.Tab -> return abc80TableCode(66, shift, control)
    }
    val normalized = char?.let { abc80CharCode(it)?.toChar()?.lowercaseChar()?.code }
    val entry = abc80KeyTable.values.firstOrNull {
        it.unshifted == normalized || it.shifted == normalized
    }
    return entry?.code(shift, control) ?: char?.let { abc80CharCode(it, shift) }
}

/** Converts Unicode input/paste to ABC80 codes; physical modifiers use the key table. */
internal fun abc80CharCode(char: Char, shift: Boolean = false): Int? = when(char) {
    '\r', '\n' -> 0x0D
    '\u00a4' -> 0x24
    in '\u0000'..'\u001F', '\u007F' -> char.code
    'Å', 'å' -> if (char == 'Å') 0x5D else 0x7D
    'Ä', 'ä' -> if (char == 'Ä') 0x5B else 0x7B
    'Ö', 'ö' -> if (char == 'Ö') 0x5C else 0x7C
    'Ü' -> 0x5E
    'ü' -> 0x7E
    'É' -> 0x40
    'é' -> 0x60
    in 'a'..'z', in 'A'..'Z' -> if (shift) char.uppercaseChar().code else char.code
    in ' '..'~' -> char.code
    else -> null
}

internal fun abc800FunctionKeyCode(number: Int, shift: Boolean, control: Boolean): Int {
    require(number in 1..8)
    return 0xC0 + number - 1 + (if (shift) 0x10 else 0) + (if (control) 0x20 else 0)
}

internal fun abc800PhysicalKeyCode(key: Key, char: Char?, shift: Boolean, control: Boolean): Int? {
    val function = when (key) {
        Key.F1 -> 1
        Key.F2 -> 2
        Key.F3 -> 3
        Key.F4 -> 4
        Key.F5 -> 5
        Key.F6 -> 6
        Key.F7 -> 7
        Key.F8 -> 8
        else -> null
    }
    return if (function != null) abc800FunctionKeyCode(function, shift, control)
        else char?.let { abc800mKeyCode(it, control) }
}

internal fun abc800mKeyCode(char: Char, control: Boolean = false): Int? {
    val code = if (control) when (val value = char.code) {
        in 97..122 -> value - 96
        in 65..90 -> value - 64
        '['.code -> 0x1b
        ']'.code -> 0x1d
        '\\'.code -> 0x1c
        '^'.code -> 0x1e
        '_'.code -> 0x1f
        else -> value
    } else {
        char.code
    }
    if (code in 0..0x7f) return abc80CharCode(code.toChar())
    return abc80CharCode(char)
}
