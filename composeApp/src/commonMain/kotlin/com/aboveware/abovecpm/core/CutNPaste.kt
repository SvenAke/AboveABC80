@file:OptIn(ExperimentalUnsignedTypes::class)

package com.aboveware.abovecpm.core

import com.aboveware.abovecpm.Assembler
import com.aboveware.abovecpm.NativeLib
import com.aboveware.abovecpm.ZXLog
import com.aboveware.abovecpm.keyboard.CommandLine
import com.aboveware.abovecpm.keyboard.Keyboard
import com.aboveware.abovecpm.keyboard.Keys

class CutNPaste {
    companion object {
        val instance = CutNPaste()
    }

    enum class KeyMode {
        Main, MainK, MainShift, Extended, ExtendedShift, ExtendedSymbol,
        LettersSymbolShift, DigitsSymbolShift
    }

    val reservedWords = mapOf(
        " " to Pair(KeyMode.MainK, Keys.KeySpace),
        "ENTER" to Pair(KeyMode.MainK, Keys.KeyEnter),
        "NEW" to Pair(KeyMode.MainK, Keys.KeyA),
        "BORDER" to Pair(KeyMode.MainK, Keys.KeyB),
        "CONT" to Pair(KeyMode.MainK, Keys.KeyC),
        "DIM" to Pair(KeyMode.MainK, Keys.KeyD),
        "REM" to Pair(KeyMode.MainK, Keys.KeyE),
        "FOR" to Pair(KeyMode.MainK, Keys.KeyF),
        "GOTO" to Pair(KeyMode.MainK, Keys.KeyG),
        "GOSUB" to Pair(KeyMode.MainK, Keys.KeyH),
        "INPUT" to Pair(KeyMode.MainK, Keys.KeyI),
        "LOAD" to Pair(KeyMode.MainK, Keys.KeyJ),
        "LIST" to Pair(KeyMode.MainK, Keys.KeyK),
        "LET" to Pair(KeyMode.MainK, Keys.KeyL),
        "PAUSE" to Pair(KeyMode.MainK, Keys.KeyM),
        "NEXT" to Pair(KeyMode.MainK, Keys.KeyN),
        "POKE" to Pair(KeyMode.MainK, Keys.KeyO),
        "PRINT" to Pair(KeyMode.MainK, Keys.KeyP),
        "PLOT" to Pair(KeyMode.MainK, Keys.KeyQ),
        "RUN" to Pair(KeyMode.MainK, Keys.KeyR),
        "SAVE" to Pair(KeyMode.MainK, Keys.KeyS),
        "RANDOMIZE" to Pair(KeyMode.MainK, Keys.KeyT),
        "IF" to Pair(KeyMode.MainK, Keys.KeyU),
        "CLS" to Pair(KeyMode.MainK, Keys.KeyV),
        "DRAW" to Pair(KeyMode.MainK, Keys.KeyW),
        "CLEAR" to Pair(KeyMode.MainK, Keys.KeyX),
        "RETURN" to Pair(KeyMode.MainK, Keys.KeyY),
        "COPY" to Pair(KeyMode.MainK, Keys.KeyZ),

        "A" to Pair(KeyMode.MainShift, Keys.KeyA),
        "B" to Pair(KeyMode.MainShift, Keys.KeyB),
        "C" to Pair(KeyMode.MainShift, Keys.KeyC),
        "D" to Pair(KeyMode.MainShift, Keys.KeyD),
        "E" to Pair(KeyMode.MainShift, Keys.KeyE),
        "F" to Pair(KeyMode.MainShift, Keys.KeyF),
        "G" to Pair(KeyMode.MainShift, Keys.KeyG),
        "H" to Pair(KeyMode.MainShift, Keys.KeyH),
        "I" to Pair(KeyMode.MainShift, Keys.KeyI),
        "J" to Pair(KeyMode.MainShift, Keys.KeyJ),
        "K" to Pair(KeyMode.MainShift, Keys.KeyK),
        "L" to Pair(KeyMode.MainShift, Keys.KeyL),
        "M" to Pair(KeyMode.MainShift, Keys.KeyM),
        "N" to Pair(KeyMode.MainShift, Keys.KeyN),
        "O" to Pair(KeyMode.MainShift, Keys.KeyO),
        "P" to Pair(KeyMode.MainShift, Keys.KeyP),
        "Q" to Pair(KeyMode.MainShift, Keys.KeyQ),
        "R" to Pair(KeyMode.MainShift, Keys.KeyR),
        "S" to Pair(KeyMode.MainShift, Keys.KeyS),
        "T" to Pair(KeyMode.MainShift, Keys.KeyT),
        "U" to Pair(KeyMode.MainShift, Keys.KeyU),
        "V" to Pair(KeyMode.MainShift, Keys.KeyV),
        "W" to Pair(KeyMode.MainShift, Keys.KeyW),
        "X" to Pair(KeyMode.MainShift, Keys.KeyX),
        "Y" to Pair(KeyMode.MainShift, Keys.KeyY),
        "Z" to Pair(KeyMode.MainShift, Keys.KeyZ),

        "a" to Pair(KeyMode.Main, Keys.KeyA),
        "b" to Pair(KeyMode.Main, Keys.KeyB),
        "c" to Pair(KeyMode.Main, Keys.KeyC),
        "d" to Pair(KeyMode.Main, Keys.KeyD),
        "e" to Pair(KeyMode.Main, Keys.KeyE),
        "f" to Pair(KeyMode.Main, Keys.KeyF),
        "g" to Pair(KeyMode.Main, Keys.KeyG),
        "h" to Pair(KeyMode.Main, Keys.KeyH),
        "i" to Pair(KeyMode.Main, Keys.KeyI),
        "j" to Pair(KeyMode.Main, Keys.KeyJ),
        "k" to Pair(KeyMode.Main, Keys.KeyK),
        "l" to Pair(KeyMode.Main, Keys.KeyL),
        "m" to Pair(KeyMode.Main, Keys.KeyM),
        "n" to Pair(KeyMode.Main, Keys.KeyN),
        "o" to Pair(KeyMode.Main, Keys.KeyO),
        "p" to Pair(KeyMode.Main, Keys.KeyP),
        "q" to Pair(KeyMode.Main, Keys.KeyQ),
        "r" to Pair(KeyMode.Main, Keys.KeyR),
        "s" to Pair(KeyMode.Main, Keys.KeyS),
        "t" to Pair(KeyMode.Main, Keys.KeyT),
        "u" to Pair(KeyMode.Main, Keys.KeyU),
        "v" to Pair(KeyMode.Main, Keys.KeyV),
        "w" to Pair(KeyMode.Main, Keys.KeyW),
        "x" to Pair(KeyMode.Main, Keys.KeyX),
        "y" to Pair(KeyMode.Main, Keys.KeyY),
        "z" to Pair(KeyMode.Main, Keys.KeyZ),

        "0" to Pair(KeyMode.MainK, Keys.Key0),
        "1" to Pair(KeyMode.MainK, Keys.Key1),
        "2" to Pair(KeyMode.MainK, Keys.Key2),
        "3" to Pair(KeyMode.MainK, Keys.Key3),
        "4" to Pair(KeyMode.MainK, Keys.Key4),
        "5" to Pair(KeyMode.MainK, Keys.Key5),
        "6" to Pair(KeyMode.MainK, Keys.Key6),
        "7" to Pair(KeyMode.MainK, Keys.Key7),
        "8" to Pair(KeyMode.MainK, Keys.Key8),
        "9" to Pair(KeyMode.MainK, Keys.Key9),

        "READ" to Pair(KeyMode.Extended, Keys.KeyA),
        "BIN" to Pair(KeyMode.Extended, Keys.KeyB),
        "LPRINT" to Pair(KeyMode.Extended, Keys.KeyC),
        "DATA" to Pair(KeyMode.Extended, Keys.KeyD),
        "TAN" to Pair(KeyMode.Extended, Keys.KeyE),
        "SGN" to Pair(KeyMode.Extended, Keys.KeyF),
        "ABS" to Pair(KeyMode.Extended, Keys.KeyG),
        "SQR" to Pair(KeyMode.Extended, Keys.KeyH),
        "CODE" to Pair(KeyMode.Extended, Keys.KeyI),
        "VAL" to Pair(KeyMode.Extended, Keys.KeyJ),
        "LEN" to Pair(KeyMode.Extended, Keys.KeyK),
        "USR" to Pair(KeyMode.Extended, Keys.KeyL),
        "PI" to Pair(KeyMode.Extended, Keys.KeyM),
        "INKEY$" to Pair(KeyMode.Extended, Keys.KeyN),
        "PEEK" to Pair(KeyMode.Extended, Keys.KeyO),
        "TAB" to Pair(KeyMode.Extended, Keys.KeyP),
        "SIN" to Pair(KeyMode.Extended, Keys.KeyQ),
        "INT" to Pair(KeyMode.Extended, Keys.KeyR),
        "RESTORE" to Pair(KeyMode.Extended, Keys.KeyS),
        "RND" to Pair(KeyMode.Extended, Keys.KeyT),
        "CHR$" to Pair(KeyMode.Extended, Keys.KeyU),
        "LLIST" to Pair(KeyMode.Extended, Keys.KeyV),
        "COS" to Pair(KeyMode.Extended, Keys.KeyW),
        "EXP" to Pair(KeyMode.Extended, Keys.KeyX),
        "STR$" to Pair(KeyMode.Extended, Keys.KeyY),
        "LN" to Pair(KeyMode.Extended, Keys.KeyZ),

        "~" to Pair(KeyMode.ExtendedShift, Keys.KeyA),
        "BRIGHT" to Pair(KeyMode.ExtendedShift, Keys.KeyB),
        "PAPER" to Pair(KeyMode.ExtendedShift, Keys.KeyC),
        "\\" to Pair(KeyMode.ExtendedShift, Keys.KeyD),
        "ATN" to Pair(KeyMode.ExtendedShift, Keys.KeyE),
        "{" to Pair(KeyMode.ExtendedShift, Keys.KeyF),
        "}" to Pair(KeyMode.ExtendedShift, Keys.KeyG),
        "CIRCLE" to Pair(KeyMode.ExtendedShift, Keys.KeyH),
        "IN" to Pair(KeyMode.ExtendedShift, Keys.KeyI),
        "VAL$" to Pair(KeyMode.ExtendedShift, Keys.KeyJ),
        "SCREEN$" to Pair(KeyMode.ExtendedShift, Keys.KeyK),
        "ATTR" to Pair(KeyMode.ExtendedShift, Keys.KeyL),
        "INVERSE" to Pair(KeyMode.ExtendedShift, Keys.KeyM),
        "OVER" to Pair(KeyMode.ExtendedShift, Keys.KeyN),
        "OUT" to Pair(KeyMode.ExtendedShift, Keys.KeyO),
        "©" to Pair(KeyMode.ExtendedShift, Keys.KeyP),
        "ASN" to Pair(KeyMode.ExtendedShift, Keys.KeyQ),
        "VERIFY" to Pair(KeyMode.ExtendedShift, Keys.KeyR),
        "|" to Pair(KeyMode.ExtendedShift, Keys.KeyS),
        "MERGE" to Pair(KeyMode.ExtendedShift, Keys.KeyT),
        "]" to Pair(KeyMode.ExtendedShift, Keys.KeyU),
        "FLASH" to Pair(KeyMode.ExtendedShift, Keys.KeyV),
        "ACS" to Pair(KeyMode.ExtendedShift, Keys.KeyW),
        "INK" to Pair(KeyMode.ExtendedShift, Keys.KeyX),
        "[" to Pair(KeyMode.ExtendedShift, Keys.KeyY),
        "BEEP" to Pair(KeyMode.ExtendedShift, Keys.KeyZ),

        "STOP" to Pair(KeyMode.LettersSymbolShift, Keys.KeyA),
        "*" to Pair(KeyMode.LettersSymbolShift, Keys.KeyB),
        "?" to Pair(KeyMode.LettersSymbolShift, Keys.KeyC),
        "STEP" to Pair(KeyMode.LettersSymbolShift, Keys.KeyD),
        ">=" to Pair(KeyMode.LettersSymbolShift, Keys.KeyE),
        "TO" to Pair(KeyMode.LettersSymbolShift, Keys.KeyF),
        "THEN" to Pair(KeyMode.LettersSymbolShift, Keys.KeyG),
        "^" to Pair(KeyMode.LettersSymbolShift, Keys.KeyH),
        "AT" to Pair(KeyMode.LettersSymbolShift, Keys.KeyI),
        "-" to Pair(KeyMode.LettersSymbolShift, Keys.KeyJ),
        "+" to Pair(KeyMode.LettersSymbolShift, Keys.KeyK),
        "=" to Pair(KeyMode.LettersSymbolShift, Keys.KeyL),
        "." to Pair(KeyMode.LettersSymbolShift, Keys.KeyM),
        "," to Pair(KeyMode.LettersSymbolShift, Keys.KeyN),
        ";" to Pair(KeyMode.LettersSymbolShift, Keys.KeyO),
        "\"" to Pair(KeyMode.LettersSymbolShift, Keys.KeyP),
        "<=" to Pair(KeyMode.LettersSymbolShift, Keys.KeyQ),
        "<" to Pair(KeyMode.LettersSymbolShift, Keys.KeyR),
        "NOT" to Pair(KeyMode.LettersSymbolShift, Keys.KeyS),
        ">" to Pair(KeyMode.LettersSymbolShift, Keys.KeyT),
        "OR" to Pair(KeyMode.LettersSymbolShift, Keys.KeyU),
        "/" to Pair(KeyMode.LettersSymbolShift, Keys.KeyV),
        "<>" to Pair(KeyMode.LettersSymbolShift, Keys.KeyW),
        "£" to Pair(KeyMode.LettersSymbolShift, Keys.KeyX),
        "AND" to Pair(KeyMode.LettersSymbolShift, Keys.KeyY),
        ":" to Pair(KeyMode.LettersSymbolShift, Keys.KeyZ),

        "_" to Pair(KeyMode.DigitsSymbolShift, Keys.Key0),
        "!" to Pair(KeyMode.DigitsSymbolShift, Keys.Key1),
        "@" to Pair(KeyMode.DigitsSymbolShift, Keys.Key2),
        "#" to Pair(KeyMode.DigitsSymbolShift, Keys.Key3),
        "$" to Pair(KeyMode.DigitsSymbolShift, Keys.Key4),
        "%" to Pair(KeyMode.DigitsSymbolShift, Keys.Key5),
        "&" to Pair(KeyMode.DigitsSymbolShift, Keys.Key6),
        "'" to Pair(KeyMode.DigitsSymbolShift, Keys.Key7),
        "(" to Pair(KeyMode.DigitsSymbolShift, Keys.Key8),
        ")" to Pair(KeyMode.DigitsSymbolShift, Keys.Key9),

        "FORMAT" to Pair(KeyMode.MainShift, Keys.Key0),
        "DEF FN" to Pair(KeyMode.MainShift, Keys.Key1),
        "FN" to Pair(KeyMode.MainShift, Keys.Key2),
        "LINE" to Pair(KeyMode.ExtendedSymbol, Keys.Key3),
        "OPEN" to Pair(KeyMode.MainShift, Keys.Key4),
        "CLOSE" to Pair(KeyMode.MainShift, Keys.Key5),
        "MOVE" to Pair(KeyMode.MainShift, Keys.Key6),
        "ERASE" to Pair(KeyMode.MainShift, Keys.Key7),
        "POINT" to Pair(KeyMode.MainShift, Keys.Key8),
        "CAT" to Pair(KeyMode.MainShift, Keys.Key9)
    )
    private val space = reservedWords[" "]
    private val rem = reservedWords["REM"]
    private val enter = reservedWords["ENTER"]
    private val quote = "(?=([^\"]*\"[^\"]*\")*[^\"]*$)"
    private val spaceQuote = Regex("\\s$quote")
    var parseError = false

    @OptIn(ExperimentalUnsignedTypes::class)
    fun paste(inputLine: String, progress: (String) -> Unit): String {
        ZXLog.commandLine(inputLine)
        val progressSoFar = StringBuilder()
        var doingRem = false

        if (Assembler.instance.labels.isDefined("OUT_FLASH")) {
            NativeLib.getObject().addMemoryReadWatcher(
                "OUT_FLASH",
                object : NativeLib.MemoryReadWatcher {
                    override fun onRead(address: Int): Boolean {
                        if (cpu().a == '?'.code) parseError =
                            true
                        if (parseError) {
                            ZXLog.wtf("Parse Error ${CommandLine.instance}")
                        }
                        return parseError
                    }
                })
        }

        ZXLog.commandLine(Tokenizer().tokenize(inputLine).toString())
        Tokenizer().tokenize(inputLine).forEach { token ->
            if (doingRem) {
                token.forEach { char ->
                    reservedWords["$char"]?.let { pair ->
                        ZXLog.commandLine("1: $pair $char")
                        progressSoFar.append(char)
                        progress(progressSoFar.toString())
                        send(pair)
                    } ?: run {
                        return "$char -> ?"
                    }
                }
                progressSoFar.append(" ")
                progress(progressSoFar.toString())
                send(space!!)
            } else reservedWords[token]?.let { pair ->
                ZXLog.commandLine("2: $pair $token")
                doingRem = pair == rem
                progressSoFar.append(token).append(" ")
                progress(progressSoFar.toString())
                send(pair)
            } ?: run {
                token.forEach { char ->
                    reservedWords["$char"]?.let { pair ->
                        ZXLog.commandLine("3: $pair $char")
                        progressSoFar.append(char)
                        progress(progressSoFar.toString())
                        send(pair)
                    } ?: run {
                        reservedWords["?"]?.let { send(it) }
                    }
                }
                progressSoFar.append(" ")
                progress(progressSoFar.toString())
                send(space!!)
            }
        }
        val returnValue = CommandLine.instance.toString().trim()
        progressSoFar.clear()
        send(enter!!)
        if (parseError) {
            return "Parse Error ${CommandLine.instance}"
        }
        return returnValue
    }

    private fun send(pair: Pair<KeyMode, Keys>) {
        Keyboard.instance.apply {
            scan()
            when (pair.first) {
                KeyMode.MainShift -> onKeyDown(Keys.KeyCapsShift)
                KeyMode.DigitsSymbolShift -> onKeyDown(Keys.KeySymbolShift)
                KeyMode.LettersSymbolShift -> onKeyDown(Keys.KeySymbolShift)
                KeyMode.ExtendedShift -> {
                    extendedMode()
                    onKeyDown(Keys.KeyCapsShift)
                }

                KeyMode.ExtendedSymbol -> {
                    extendedMode()
                    onKeyDown(Keys.KeySymbolShift)
                }

                KeyMode.Extended -> extendedMode()
                else -> {}
            }
            onKeyDown(pair.second)
            scan()
            onKeyUp(pair.second)
            scan()
            when (pair.first) {
                KeyMode.MainShift -> onKeyUp(Keys.KeyCapsShift)
                KeyMode.DigitsSymbolShift -> onKeyUp(Keys.KeySymbolShift)
                KeyMode.LettersSymbolShift -> onKeyUp(Keys.KeySymbolShift)
                KeyMode.ExtendedShift -> onKeyUp(Keys.KeyCapsShift)
                KeyMode.ExtendedSymbol -> onKeyUp(Keys.KeySymbolShift)
                else -> {}
            }
            // ZXSpectrum.setInputCursor()
            scan()
        }
    }

    private fun extendedMode() {
        Keyboard.instance.apply {
            onKeyDown(Keys.KeyCapsShift)
            onKeyDown(Keys.KeySymbolShift)
            scan()
            onKeyUp(Keys.KeyCapsShift)
            onKeyUp(Keys.KeySymbolShift)
            scan()
        }
    }

    fun paste(program: List<String>, progress: (Int, Int, String) -> Unit) {
        program.forEachIndexed { index, line ->
            paste(line) { pastedSoFar ->
                progress(index, program.size, pastedSoFar)
            }
            if (parseError) return
            if (Assembler.instance.labels.isDefined("EDITOR")) {
                Utilities.instance.waitForLabel("EDITOR")
            } else {
                // In CP/M or systems without EDITOR label, just add a small delay
                // to simulate the time taken to process the line.
                // Thread.sleep(50) 
            }
        }
    }
}