package com.aboveware.aboveabc80

import androidx.compose.ui.input.key.Key

/**
 * Maps a host key press to the 7-bit code the ABC80 keyboard delivers on PIO port A,
 * or null if the key has no ABC80 equivalent.
 */
internal fun abc80KeyCode(key: Key, char: Char?, shift: Boolean): Int? {
    when (key) {
        Key.Enter, Key.NumPadEnter -> return 0x0D
        Key.Backspace, Key.DirectionLeft -> return 0x08
        Key.DirectionRight -> return 0x09
        Key.DirectionDown -> return 0x0A
        Key.DirectionUp -> return 0x0B
        Key.Escape -> return 0x1B
        Key.Delete -> return 0x7F
        Key.Tab -> return 0x09
    }
    return char?.let { abc80CharCode(it, shift) }
}

/** Letters are typed in upper case by default; Shift gives lower case (ABC80 BASIC keywords are upper case). */
internal fun abc80CharCode(char: Char, shift: Boolean = false): Int? = when (char) {
    '\r', '\n' -> 0x0D
    'Å', 'å' -> if (char == 'Å') 0x5D else 0x7D
    'Ä', 'ä' -> if (char == 'Ä') 0x5B else 0x7B
    'Ö', 'ö' -> if (char == 'Ö') 0x5C else 0x7C
    'Ü' -> 0x5E
    'ü' -> 0x7E
    'É' -> 0x40
    'é' -> 0x60
    in 'a'..'z', in 'A'..'Z' -> if (shift) char.lowercaseChar().code else char.uppercaseChar().code
    in ' '..'~' -> char.code
    else -> null
}
