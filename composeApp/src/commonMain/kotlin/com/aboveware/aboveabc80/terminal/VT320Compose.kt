package com.aboveware.aboveabc80.terminal

import com.aboveware.aboveabc80.ZXLog

/**
 * Implements the VT320 Compose key logic.
 * A compose sequence consists of pressing the Compose key, then two characters.
 */
class VT320Compose {
    private var firstChar: Char? = null
    var isActive: Boolean = false
        private set

    data class Context(
        val isDecMultinational: Boolean,
        val isNRC: Boolean,
        val keyboardLanguage: Int,
        val isDataProcessing: Boolean
    )

    fun start(mark: Char? = null) {
        ZXLog.keyboard("VT320Compose: Start${if (mark != null) " with mark '$mark'" else ""}")
        isActive = true
        firstChar = mark
    }

    fun cancel() {
        ZXLog.keyboard("VT320Compose: Cancel")
        isActive = false
        firstChar = null
    }

    /**
     * Processes a character input during Compose mode.
     * Returns the composed character if sequence complete, null if still waiting,
     * or '\u0000' if invalid sequence (and cancels).
     */
    fun processChar(c: Char, context: Context): Char? {
        if (!isActive) return null

        if (c == '\u001B') {
            cancel()
            return null
        }

        if (firstChar == null) {
            ZXLog.keyboard("VT320Compose: First char = '$c' (${c.code})")
            firstChar = c
            return null
        }

        ZXLog.keyboard("VT320Compose: Second char = '$c' (${c.code})")
        val result = compose(firstChar!!, c, context)

        if (result != null) {
            ZXLog.keyboard("VT320Compose: Result = '$result' (${result.code})")
        } else {
            ZXLog.keyboard("VT320Compose: No match for '$firstChar' + '$c'")
        }

        isActive = false
        firstChar = null
        return result ?: '\u0000'
    }

    private fun compose(c1: Char, c2: Char, context: Context): Char? {
        if (context.isNRC) {
            return if (context.isDataProcessing) {
                composeNRCDataProcessing(c1, c2)
            } else {
                composeNRCTypewriter(c1, c2, context.keyboardLanguage)
            }
        }

        // Multinational / ISO Latin-1 (Table 5-1)
        return composeMultinational(c1, c2, context.isDecMultinational)
    }

    private fun composeNRCDataProcessing(c1: Char, c2: Char): Char? {
        // Table 5-3: NRC Sets, Using Data Processing Keys
        // Sorted match
        val s1 = if (c1 < c2) c1 else c2
        val s2 = if (c1 < c2) c2 else c1

        return when {
            s1 == ' ' && s2 == '"' -> '\"'
            s1 == '+' && s2 == '#' -> '#' // Just in case, usually ++
            s1 == '+' && s2 == '+' -> '#'
            s1 == ' ' && s2 == '\'' -> '\''
            s1 == 'A' && s2 == 'A' -> '@'
            s1 == 'a' && s2 == 'a' -> '@'
            s1 == 'A' && s2 == 'a' -> '@'
            s1 == '(' && s2 == '(' -> '['
            s1 == '/' && s2 == '<' -> '\\'
            s1 == ')' && s2 == ')' -> ']'
            s1 == ' ' && s2 == 'ˆ' -> '^'
            s1 == ' ' && s2 == '`' -> '`'
            s1 == '(' && s2 == '-' -> '{'
            s1 == '/' && s2 == '^' -> '|'
            s1 == '^' && s2 == '/' -> '|'
            s1 == ')' && s2 == '-' -> '}'
            s1 == ' ' && s2 == '~' -> '~'
            else -> null
        }
    }

    private fun composeNRCTypewriter(c1: Char, c2: Char, language: Int): Char? {
        // Table 5-2: NRC Sets, Using Typewriter Keys
        // Footnote (*) says: You must type the characters for these sequences in the order shown.

        return when (language) {
            1 -> { // British
                when (c1) {
                    'L' if (c2 == '-' || c2 == '=') -> '£'
                    '`' if c2 == ' ' -> '`'
                    else -> null
                }
            }

            4 -> { // Danish
                when (c1) {
                    '+' if c2 == '+' -> '#'
                    '´' if c2 == ' ' -> '\''
                    'A' if c2 == 'A' -> '@'
                    '`' if c2 == ' ' -> '`'
                    else -> null
                }
            }

            7 -> { // Dutch
                when (c1) {
                    'L' if (c2 == '-' || c2 == '=') -> '£'
                    '\'' if c2 == ' ' -> '\''
                    '1' if c2 == '4' -> '¼'
                    '1' if c2 == '2' -> '½'
                    '3' if c2 == '4' -> '¾'
                    'i' if c2 == 'j' -> 'ÿ'
                    'f' if c2 == '-' -> 'ƒ'
                    '`' if c2 == ' ' -> '`'
                    '\'' if c2 == '\'' -> '´'
                    '"' if c2 == '^' -> '¨'
                    else -> null
                }
            }

            5 -> { // Finnish
                when (c1) {
                    '+' if c2 == '+' -> '#'
                    '\'' if c2 == ' ' -> '\''
                    else -> null
                }
            }

            2, 13 -> { // Flemish and French/Belgian
                when (c1) {
                    'L' if (c2 == '-' || c2 == '=') -> '£'
                    '\'' if c2 == ' ' -> '\''
                    '`' if c2 == ' ' -> '`'
                    else -> null
                }
            }

            3 -> { // French Canadian
                when (c1) {
                    '\'' if c2 == ' ' -> '\''
                    '`' if c2 == 'a' -> 'à'
                    '^' if c2 == 'a' -> 'â'
                    '`' if c2 == 'e' -> 'è'
                    '^' if c2 == 'e' -> 'ê'
                    '^' if c2 == 'i' -> 'î'
                    '^' if c2 == 'o' -> 'ô'
                    '`' if c2 == 'u' -> 'ù'
                    '^' if c2 == 'u' -> 'û'
                    else -> null
                }
            }

            6 -> { // German/Austrian
                when (c1) {
                    '\'' if c2 == ' ' -> '\''
                    '`' if c2 == ' ' -> '`'
                    else -> null
                }
            }

            8 -> { // Italian
                if (c1 == '\'' && c2 == ' ') '\''
                else null
            }

            12 -> { // Norwegian
                when (c1) {
                    '\'' if c2 == ' ' -> '\''
                    '`' if c2 == ' ' -> '`'
                    else -> null
                }
            }

            15 -> { // Portuguese
                if (c1 == '\'' && c2 == ' ') '\''
                else if (c1 == '`' && c2 == ' ') '`'
                else if (c1 == '~' && (c2 == 'A' || c2 == 'a')) if (c2 == 'A') 'Ã' else 'ã'
                else if (c1 == '~' && (c2 == 'O' || c2 == 'o')) if (c2 == 'O') 'Õ' else 'õ'
                else null
            }

            14 -> { // Spanish
                if (c1 == 'L' && (c2 == '-' || c2 == '=')) '£'
                else if (c1 == '\'' && c2 == ' ') '\''
                else if ((c1 == '!' || c1 == 'O' || c1 == '0') && c2 == 'S') '§'
                else if (c1 == '`' && c2 == ' ') '`'
                else if (c1 == '~' && c2 == ' ') '~'
                else null
            }

            11 -> { // Swedish
                when (c1) {
                    '+' if c2 == '+' -> '#'
                    '\'' if (c2 == ' ' || c2 == 'E' || c2 == 'e') -> {
                        when (c2) {
                            ' ' -> '\''
                            'E' -> 'É'
                            'e' -> 'é'
                            else -> null
                        }
                    }

                    else -> null
                }
            }

            9, 10 -> { // Swiss (French) and Swiss (German)
                when (c1) {
                    '\'' if c2 == ' ' -> '\''
                    '^' if (c2 == 'e' || c2 == 'i' || c2 == 'o' || c2 == 'u') -> {
                        when (c2) {
                            'e' -> 'ê'; 'i' -> 'î'; 'o' -> 'ô'; 'u' -> 'û'; else -> null
                        }
                    }

                    '`' if c2 == 'u' -> 'ù'
                    else -> null
                }
            }

            else -> null
        }
    }

    private fun composeMultinational(c1: Char, c2: Char, isDecMultinational: Boolean): Char? {
        // Sort to make matching easier (order shouldn't matter for most)
        val s1 = if (c1 < c2) c1 else c2
        val s2 = if (c1 < c2) c2 else c1

        return when {
            // Table 5-1: Symbols / Special Characters (Sorted by s1)
            s1 == ' ' && s2 == '"' -> '\"'
            s1 == ' ' && s2 == '\'' -> '\''
            s1 == ' ' && s2 == '^' -> '^'
            s1 == ' ' && s2 == '`' -> '`'
            s1 == ' ' && s2 == '~' -> '~'
            s1 == ' ' && s2 == '°' -> '°'
            s1 == ' ' && s2 == '¨' -> '\"'
            s1 == ' ' && s2 == '´' -> '\''
            s1 == ' ' && s2 == 'ˆ' -> '^'
            s1 == ' ' && s2 == '˜' -> '~'

            s1 == '!' && s2 == '!' -> '¡'
            s1 == '!' && s2 == 'P' -> '¶'
            s1 == '!' && s2 == 'S' -> '§'

            s1 == '(' && s2 == '(' -> '['
            s1 == '(' && s2 == '-' -> '{'

            s1 == ')' && s2 == ')' -> ']'
            s1 == ')' && s2 == '-' -> '}'

            s1 == '*' && s2 == 'A' -> 'Å'
            s1 == '*' && s2 == 'a' -> 'å'

            s1 == '+' && s2 == '+' -> '#'
            s1 == '+' && s2 == '-' -> '±'

            s1 == '.' && s2 == '^' -> '·'
            s1 == '.' && s2 == '.' -> '·'

            s1 == '/' && s2 == '/' -> '\\'
            s1 == '/' && s2 == '<' -> '\\'
            s1 == '/' && s2 == 'C' -> '¢'
            s1 == '/' && s2 == 'O' -> 'Ø'
            s1 == '/' && s2 == 'U' -> 'µ'
            s1 == '/' && s2 == '^' -> '|'
            s1 == '/' && s2 == 'o' -> 'ø'

            s1 == '0' && s2 == 'C' -> '©'
            s1 == '0' && s2 == 'S' -> '§'
            s1 == '0' && s2 == 'X' -> '¤'
            s1 == '0' && s2 == '^' -> '°'

            s1 == '1' && s2 == '2' -> '½'
            s1 == '1' && s2 == '4' -> '¼'
            s1 == '1' && s2 == '^' -> '¹'

            s1 == '2' && s2 == '^' -> '²'
            s1 == '3' && s2 == '4' -> '¾'
            s1 == '3' && s2 == '^' -> '³'

            s1 == '<' && s2 == '<' -> '«'
            s1 == '=' && s2 == 'L' -> '£'
            s1 == '=' && s2 == 'Y' -> '¥'
            s1 == '>' && s2 == '>' -> '»'
            s1 == '?' && s2 == '?' -> '¿'

            // ISO Latin-1 specific / Table 5-1 additions
            s1 == ' ' && s2 == ' ' -> '\u00A0' // NBSP
            s1 == '|' && s2 == '|' -> '¦'      // Broken Bar
            s1 == '!' && s2 == '^' -> '¦'      // Broken Bar
            s1 == ',' && s2 == '-' -> '¬'      // Logical Not
            s1 == '-' && s2 == '-' -> '\u00AD' // Soft Hyphen
            s1 == '-' && s2 == '^' -> '¯'      // Macron
            s1 == '-' && s2 == ':' -> '÷'      // Division
            s1 == 'x' && s2 == 'x' -> '×'      // Multiplication
            s1 == '\'' && s2 == '\'' -> '´'    // Acute Mark
            s1 == ',' && s2 == ',' -> '¸'      // Cedilla Mark
            s1 == '"' && s2 == '"' -> '¨'      // Diaeresis Mark
            s1 == 'R' && s2 == 'O' -> '®'      // Registered Trademark

            // Accents: Acute (') = 39, ´ = 180
            (s1 == '\'' || s1 == '´') && s2 == 'A' -> 'Á'
            (s1 == '\'' || s1 == '´') && s2 == 'E' -> 'É'
            (s1 == '\'' || s1 == '´') && s2 == 'I' -> 'Í'
            (s1 == '\'' || s1 == '´') && s2 == 'O' -> 'Ó'
            (s1 == '\'' || s1 == '´') && s2 == 'U' -> 'Ú'
            (s1 == '\'' || s1 == '´') && s2 == 'Y' -> 'Ý'
            (s1 == '\'' || s1 == '´') && s2 == 'a' -> 'á'
            (s1 == '\'' || s1 == '´') && s2 == 'e' -> 'é'
            (s1 == '\'' || s1 == '´') && s2 == 'i' -> 'í'
            (s1 == '\'' || s1 == '´') && s2 == 'o' -> 'ó'
            (s1 == '\'' || s1 == '´') && s2 == 'u' -> 'ú'
            (s1 == '\'' || s1 == '´') && s2 == 'y' -> 'ý'

            // Accents: Diaeresis (") = 34, ¨ = 168
            (s1 == '"' || s1 == '¨') && s2 == 'A' -> 'Ä'
            (s1 == '"' || s1 == '¨') && s2 == 'E' -> 'Ë'
            (s1 == '"' || s1 == '¨') && s2 == 'I' -> 'Ï'
            (s1 == '"' || s1 == '¨') && s2 == 'O' -> 'Ö'
            (s1 == '"' || s1 == '¨') && s2 == 'U' -> 'Ü'
            (s1 == '"' || s1 == '¨') && s2 == 'Y' -> if (isDecMultinational) 'Ÿ' else 'Ý'
            (s1 == '"' || s1 == '¨') && s2 == 'a' -> 'ä'
            (s1 == '"' || s1 == '¨') && s2 == 'e' -> 'ë'
            (s1 == '"' || s1 == '¨') && s2 == 'i' -> 'ï'
            (s1 == '"' || s1 == '¨') && s2 == 'o' -> 'ö'
            (s1 == '"' || s1 == '¨') && s2 == 'u' -> 'ü'
            (s1 == '"' || s1 == '¨') && s2 == 'y' -> 'ÿ'

            // Letters / Mixed
            s1 == '-' && s2 == 'D' -> 'Ð'
            s1 == '-' && s2 == 'L' -> '£'
            s1 == '-' && s2 == 'Y' -> '¥'
            s1 == '-' && s2 == 'd' -> 'ð'

            s1 == ',' && s2 == 'C' -> 'Ç'
            s1 == ',' && s2 == 'c' -> 'ç'

            s1 == 'A' && s2 == 'A' -> '@'
            s1 == 'A' && s2 == 'E' -> 'Æ'
            s1 == 'A' && s2 == 'O' -> 'Å'
            s1 == 'A' && s2 == '`' -> 'À'
            s1 == 'A' && s2 == '^' -> 'Â'
            s1 == 'A' && s2 == '_' -> 'ª'
            s1 == 'A' && s2 == '~' -> 'Ã'
            s1 == 'A' && s2 == '°' -> 'Å'

            s1 == 'C' && s2 == 'O' -> '©'
            s1 == 'C' && s2 == '|' -> '¢'

            s1 == 'E' && s2 == 'O' -> if (isDecMultinational) 'Œ' else null
            s1 == 'E' && s2 == '`' -> 'È'
            s1 == 'E' && s2 == '^' -> 'Ê'

            s1 == 'H' && s2 == 'T' -> 'Þ'
            s1 == 'T' && s2 == 'H' -> 'Þ'

            s1 == 'I' && s2 == '`' -> 'Ì'
            s1 == 'I' && s2 == '^' -> 'Î'

            s1 == 'N' && s2 == '~' -> 'Ñ'

            s1 == 'O' && s2 == 'S' -> '§'
            s1 == 'O' && s2 == 'X' -> '¤'
            s1 == 'O' && s2 == '_' -> 'º'
            s1 == 'O' && s2 == '`' -> 'Ò'
            s1 == 'O' && s2 == '^' -> 'Ô'
            s1 == 'O' && s2 == '~' -> 'Õ'

            s1 == 'U' && s2 == '`' -> 'Ù'
            s1 == 'U' && s2 == '^' -> 'Û'

            s1 == 'Y' && s2 == '\'' -> if (!isDecMultinational) 'Ý' else null
            s1 == 'y' && s2 == '\'' -> if (!isDecMultinational) 'ý' else null

            s1 == 'a' && s2 == 'e' -> 'æ'
            s1 == 'a' && s2 == 'o' -> 'å'
            s1 == 'a' && s2 == '~' -> 'ã'
            s1 == 'a' && s2 == '°' -> 'å'

            s1 == 'e' && s2 == 'o' -> if (isDecMultinational) 'œ' else null

            s1 == 'h' && s2 == 't' -> 'þ'
            s1 == 't' && s2 == 'h' -> 'þ'
            s1 == 'n' && s2 == '~' -> 'ñ'
            s1 == 'o' && s2 == '~' -> 'õ'
            s1 == 's' && s2 == 's' -> 'ß'

            // Grave (`) = 96
            s1 == '`' && s2 == 'a' -> 'à'
            s1 == '`' && s2 == 'e' -> 'è'
            s1 == '`' && s2 == 'i' -> 'ì'
            s1 == '`' && s2 == 'o' -> 'ò'
            s1 == '`' && s2 == 'u' -> 'ù'

            // Circumflex (^) = 94, ˆ = 710
            (s1 == '^' || s1 == 'ˆ') && s2 == 'a' -> 'â'
            (s1 == '^' || s1 == 'ˆ') && s2 == 'e' -> 'ê'
            (s1 == '^' || s1 == 'ˆ') && s2 == 'i' -> 'î'
            (s1 == '^' || s1 == 'ˆ') && s2 == 'o' -> 'ô'
            (s1 == '^' || s1 == 'ˆ') && s2 == 'u' -> 'û'

            // Tilde (~) = 126, ˜ = 732
            (s1 == '~' || s1 == '˜') && s2 == 'A' -> 'Ã'
            (s1 == '~' || s1 == '˜') && s2 == 'N' -> 'Ñ'
            (s1 == '~' || s1 == '˜') && s2 == 'O' -> 'Õ'
            (s1 == '~' || s1 == '˜') && s2 == 'a' -> 'ã'
            (s1 == '~' || s1 == '˜') && s2 == 'n' -> 'ñ'
            (s1 == '~' || s1 == '˜') && s2 == 'o' -> 'õ'

            // Legacy / Extra
            s1 == '$' && s2 == '$' -> '¤'
            s1 == 'c' && s2 == 'o' -> '©'
            s1 == 'r' && s2 == 'o' -> '®'
            s1 == '_' && s2 == '_' -> '¯'
            s1 == ' ' && s2 == ',' -> '¸'

            else -> null
        }
    }
}
