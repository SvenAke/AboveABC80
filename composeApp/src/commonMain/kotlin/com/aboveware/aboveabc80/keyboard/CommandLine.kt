package com.aboveware.aboveabc80.keyboard

import com.aboveware.aboveabc80.NativeLib
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.core.CutNPaste
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CommandLine {
    companion object {

        val instance = CommandLine()

        enum class Class {
            CLASS_0,
            CLASS_1,
            CLASS_3,
            CLASS_5,
            CLASS_8,
            CLASS_B
        }

        const val NOT_USED = "Not used"
        val tokens = arrayOf(
            0x00 to Pair(NOT_USED, Class.CLASS_0),
            0x01 to Pair(NOT_USED, Class.CLASS_1),
            0x02 to Pair(NOT_USED, Class.CLASS_1),
            0x03 to Pair(NOT_USED, Class.CLASS_1),
            0x04 to Pair(NOT_USED, Class.CLASS_1),
            0x05 to Pair(NOT_USED, Class.CLASS_1),
            0x06 to Pair("PRINT comma", Class.CLASS_1),
            0x07 to Pair("EDIT", Class.CLASS_1),
            0x08 to Pair("", Class.CLASS_1),
            0x09 to Pair("", Class.CLASS_1),
            0x0A to Pair("", Class.CLASS_1),
            0x0B to Pair("", Class.CLASS_1),
            0x0C to Pair("DELETE", Class.CLASS_1),
            0x0D to Pair("ENTER", Class.CLASS_1),
            0x0E to Pair("number", Class.CLASS_1),
            0x0F to Pair("not used", Class.CLASS_1),
            0x10 to Pair("INK control", Class.CLASS_3),
            0x11 to Pair("PAPER control", Class.CLASS_3),
            0x12 to Pair("FLASH control", Class.CLASS_1),
            0x13 to Pair("BRIGHT control", Class.CLASS_3),
            0x14 to Pair("INVERSE control", Class.CLASS_0),
            0x15 to Pair("OVER control", Class.CLASS_1),
            0x16 to Pair("AT control", Class.CLASS_1),
            0x17 to Pair("TAB control", Class.CLASS_1),
            0x18 to Pair(NOT_USED, Class.CLASS_1),
            0x19 to Pair(NOT_USED, Class.CLASS_1),
            0x1A to Pair(NOT_USED, Class.CLASS_1),
            0x1B to Pair(NOT_USED, Class.CLASS_1),
            0x1C to Pair(NOT_USED, Class.CLASS_1),
            0x1D to Pair(NOT_USED, Class.CLASS_1),
            0x1E to Pair(NOT_USED, Class.CLASS_1),
            0x1F to Pair(NOT_USED, Class.CLASS_1),
            0x20 to Pair(" ", Class.CLASS_1),
            0x21 to Pair("!", Class.CLASS_1),
            0x22 to Pair("\"", Class.CLASS_1),
            0x23 to Pair("#", Class.CLASS_1),
            0x24 to Pair("$", Class.CLASS_1),
            0x25 to Pair("%", Class.CLASS_1),
            0x26 to Pair("&", Class.CLASS_1),
            0x27 to Pair("'", Class.CLASS_1),
            0x28 to Pair("(", Class.CLASS_1),
            0x29 to Pair(")", Class.CLASS_1),
            0x2A to Pair("*", Class.CLASS_1),
            0x2B to Pair("+", Class.CLASS_1),
            0x2C to Pair(", Class.CLASS_1),", Class.CLASS_1),
            0x2D to Pair("-", Class.CLASS_1),
            0x2E to Pair(".", Class.CLASS_1),
            0x2F to Pair("/", Class.CLASS_1),
            0x30 to Pair("0", Class.CLASS_1),
            0x31 to Pair("1", Class.CLASS_1),
            0x32 to Pair("2", Class.CLASS_1),
            0x33 to Pair("3", Class.CLASS_1),
            0x34 to Pair("4", Class.CLASS_1),
            0x35 to Pair("5", Class.CLASS_1),
            0x36 to Pair("6", Class.CLASS_1),
            0x37 to Pair("7", Class.CLASS_1),
            0x38 to Pair("8", Class.CLASS_1),
            0x39 to Pair("9", Class.CLASS_1),
            0x3A to Pair(":", Class.CLASS_1),
            0x3B to Pair(";", Class.CLASS_1),
            0x3C to Pair("<", Class.CLASS_1),
            0x3D to Pair("=", Class.CLASS_1),
            0x3E to Pair(">", Class.CLASS_1),
            0x3F to Pair("?", Class.CLASS_1),
            0x40 to Pair("@", Class.CLASS_1),
            0x41 to Pair("A", Class.CLASS_1),
            0x42 to Pair("B", Class.CLASS_1),
            0x43 to Pair("C", Class.CLASS_1),
            0x44 to Pair("D", Class.CLASS_1),
            0x45 to Pair("E", Class.CLASS_1),
            0x46 to Pair("F", Class.CLASS_1),
            0x47 to Pair("G", Class.CLASS_1),
            0x48 to Pair("H", Class.CLASS_1),
            0x49 to Pair("I", Class.CLASS_1),
            0x4A to Pair("J", Class.CLASS_1),
            0x4B to Pair("K", Class.CLASS_1),
            0x4C to Pair("L", Class.CLASS_1),
            0x4D to Pair("M", Class.CLASS_1),
            0x4E to Pair("N", Class.CLASS_1),
            0x4F to Pair("O", Class.CLASS_1),
            0x50 to Pair("P", Class.CLASS_1),
            0x51 to Pair("Q", Class.CLASS_1),
            0x52 to Pair("R", Class.CLASS_1),
            0x53 to Pair("S", Class.CLASS_1),
            0x54 to Pair("T", Class.CLASS_1),
            0x55 to Pair("U", Class.CLASS_1),
            0x56 to Pair("V", Class.CLASS_1),
            0x57 to Pair("W", Class.CLASS_1),
            0x58 to Pair("X", Class.CLASS_1),
            0x59 to Pair("Y", Class.CLASS_1),
            0x5A to Pair("Z", Class.CLASS_1),
            0x5B to Pair("[", Class.CLASS_1),
            0x5C to Pair("/", Class.CLASS_1),
            0x5D to Pair("]", Class.CLASS_1),
            0x5E to Pair("^", Class.CLASS_1),
            0x5F to Pair("_", Class.CLASS_1),
            0x60 to Pair("ukp", Class.CLASS_1),
            0x61 to Pair("a", Class.CLASS_1),
            0x62 to Pair("b", Class.CLASS_1),
            0x63 to Pair("c", Class.CLASS_1),
            0x64 to Pair("d", Class.CLASS_1),
            0x65 to Pair("e", Class.CLASS_1),
            0x66 to Pair("f", Class.CLASS_1),
            0x67 to Pair("g", Class.CLASS_1),
            0x68 to Pair("h", Class.CLASS_1),
            0x69 to Pair("i", Class.CLASS_1),
            0x6A to Pair("j", Class.CLASS_1),
            0x6B to Pair("k", Class.CLASS_1),
            0x6C to Pair("l", Class.CLASS_1),
            0x6D to Pair("m", Class.CLASS_1),
            0x6E to Pair("n", Class.CLASS_1),
            0x6F to Pair("o", Class.CLASS_1),
            0x70 to Pair("p", Class.CLASS_1),
            0x71 to Pair("q", Class.CLASS_1),
            0x72 to Pair("r", Class.CLASS_1),
            0x73 to Pair("s", Class.CLASS_1),
            0x74 to Pair("t", Class.CLASS_1),
            0x75 to Pair("u", Class.CLASS_1),
            0x76 to Pair("v", Class.CLASS_1),
            0x77 to Pair("w", Class.CLASS_1),
            0x78 to Pair("x", Class.CLASS_1),
            0x79 to Pair("y", Class.CLASS_1),
            0x7A to Pair("z", Class.CLASS_1),
            0x7B to Pair("{", Class.CLASS_1),
            0x7C to Pair("|", Class.CLASS_1),
            0x7D to Pair("}", Class.CLASS_1),
            0x7E to Pair("-", Class.CLASS_1),
            0x7F to Pair("©", Class.CLASS_1),
            0x80 to Pair("", Class.CLASS_1),
            0x81 to Pair("", Class.CLASS_1),
            0x82 to Pair("", Class.CLASS_1),
            0x83 to Pair("", Class.CLASS_1),
            0x84 to Pair("", Class.CLASS_1),
            0x85 to Pair("", Class.CLASS_1),
            0x86 to Pair("", Class.CLASS_1),
            0x87 to Pair("", Class.CLASS_1),
            0x88 to Pair("", Class.CLASS_1),
            0x89 to Pair("", Class.CLASS_1),
            0x8A to Pair("", Class.CLASS_1),
            0x8B to Pair("", Class.CLASS_1),
            0x8C to Pair("", Class.CLASS_1),
            0x8D to Pair("", Class.CLASS_1),
            0x8E to Pair("", Class.CLASS_1),
            0x8F to Pair("", Class.CLASS_1),
            0x90 to Pair("(a)", Class.CLASS_1),
            0x91 to Pair("(b)", Class.CLASS_1),
            0x92 to Pair("(c)", Class.CLASS_1),
            0x93 to Pair("(d)", Class.CLASS_1),
            0x94 to Pair("(e)", Class.CLASS_1),
            0x95 to Pair("(f)", Class.CLASS_1),
            0x96 to Pair("(g)", Class.CLASS_1),
            0x97 to Pair("(h)", Class.CLASS_1),
            0x98 to Pair("(i)", Class.CLASS_1),
            0x99 to Pair("(j)", Class.CLASS_1),
            0x9A to Pair("(k)", Class.CLASS_1),
            0x9B to Pair("(l)", Class.CLASS_1),
            0x9C to Pair("(m)", Class.CLASS_1),
            0x9D to Pair("(n)", Class.CLASS_1),
            0x9E to Pair("(o)", Class.CLASS_1),
            0x9F to Pair("(p)", Class.CLASS_1),
            0xA0 to Pair("(q)", Class.CLASS_1),
            0xA1 to Pair("(r)", Class.CLASS_1),
            0xA2 to Pair("(s)", Class.CLASS_1),
            0xA3 to Pair("(t)", Class.CLASS_1),
            0xA4 to Pair("(u)", Class.CLASS_1),
            0xA5 to Pair("RND", Class.CLASS_1),
            0xA6 to Pair("INKEY$", Class.CLASS_1),
            0xA7 to Pair("PI", Class.CLASS_1),
            0xA8 to Pair("FN", Class.CLASS_1),
            0xA9 to Pair("POINT", Class.CLASS_1),
            0xAA to Pair("SCREEN$", Class.CLASS_1),
            0xAB to Pair("ATTR", Class.CLASS_1),
            0xAC to Pair("AT", Class.CLASS_8),
            0xAD to Pair("TAB", Class.CLASS_1),
            0xAE to Pair("VAL$ ", Class.CLASS_1),
            0xAF to Pair("CODE", Class.CLASS_1),
            0xB0 to Pair("VAL ", Class.CLASS_3),
            0xB1 to Pair("LEN", Class.CLASS_1),
            0xB2 to Pair("SIN", Class.CLASS_1),
            0xB3 to Pair("COS", Class.CLASS_1),
            0xB4 to Pair("TAN", Class.CLASS_1),
            0xB5 to Pair("ASN", Class.CLASS_1),
            0xB6 to Pair("ACS", Class.CLASS_1),
            0xB7 to Pair("ATN", Class.CLASS_1),
            0xB8 to Pair("LN", Class.CLASS_1),
            0xB9 to Pair("EXP", Class.CLASS_1),
            0xBA to Pair("INT", Class.CLASS_1),
            0xBB to Pair("SOR", Class.CLASS_1),
            0xBC to Pair("SGN", Class.CLASS_1),
            0xBD to Pair("ABS", Class.CLASS_1),
            0xBE to Pair("PEEK", Class.CLASS_1),
            0xBF to Pair("IN", Class.CLASS_1),
            0xC0 to Pair("USR ", Class.CLASS_3),
            0xC1 to Pair("STR$", Class.CLASS_1),
            0xC2 to Pair("CHR$", Class.CLASS_1),
            0xC3 to Pair("NOT", Class.CLASS_1),
            0xC4 to Pair("BIN", Class.CLASS_1),
            0xC5 to Pair(" OR", Class.CLASS_1),
            0xC6 to Pair(" AND", Class.CLASS_1),
            0xC7 to Pair("<=", Class.CLASS_1),
            0xC8 to Pair(">=", Class.CLASS_1),
            0xC9 to Pair("<>", Class.CLASS_1),
            0xCA to Pair("LINE ", Class.CLASS_1),
            0xCB to Pair("THEN ", Class.CLASS_1),
            0xCC to Pair("TO ", Class.CLASS_1),
            0xCD to Pair("STEP ", Class.CLASS_1),
            0xCE to Pair("DEF FN ", Class.CLASS_1),
            0xCF to Pair("CAT ", Class.CLASS_1),
            0xD0 to Pair("FORMAT ", Class.CLASS_1),
            0xD1 to Pair("MOVE ", Class.CLASS_1),
            0xD2 to Pair("ERASE ", Class.CLASS_1),
            0xD3 to Pair("OPEN #", Class.CLASS_1),
            0xD4 to Pair("CLOSE #", Class.CLASS_1),
            0xD5 to Pair("MERGE ", Class.CLASS_B),
            0xD6 to Pair("VERIFY ", Class.CLASS_B),
            0xD7 to Pair("BEEP ", Class.CLASS_8),
            0xD8 to Pair("CIRCLE ", Class.CLASS_1),
            0xD9 to Pair("INK ", Class.CLASS_3),
            0xDA to Pair("PAPER ", Class.CLASS_3),
            0xDB to Pair("FLASH ", Class.CLASS_1),
            0xDC to Pair("BRIGHT ", Class.CLASS_3),
            0xDD to Pair("INVERSE ", Class.CLASS_1),
            0xDE to Pair("OVER ", Class.CLASS_1),
            0xDF to Pair("OUT ", Class.CLASS_1),
            0xE0 to Pair("LPRINT ", Class.CLASS_1),
            0xE1 to Pair("LLIST ", Class.CLASS_1),
            0xE2 to Pair("STOP ", Class.CLASS_1),
            0xE3 to Pair("READ ", Class.CLASS_1),
            0xE4 to Pair("DATA ", Class.CLASS_1),
            0xE5 to Pair("RESTORE ", Class.CLASS_1),
            0xE6 to Pair("NEW ", Class.CLASS_1),
            0xE7 to Pair("BORDER ", Class.CLASS_3),
            0xE8 to Pair("CONTINUE ", Class.CLASS_1),
            0xE9 to Pair("DIM ", Class.CLASS_1),
            0xEA to Pair("REM ", Class.CLASS_1),
            0xEB to Pair("FOR ", Class.CLASS_1),
            0xEC to Pair("GO TO ", Class.CLASS_1),
            0xED to Pair("GO SUB ", Class.CLASS_1),
            0xEE to Pair("INPUT ", Class.CLASS_1),
            0xEF to Pair("LOAD ", Class.CLASS_B),
            0xF0 to Pair("LIST ", Class.CLASS_1),
            0xF1 to Pair("LET ", Class.CLASS_1),
            0xF2 to Pair("PAUSE ", Class.CLASS_1),
            0xF3 to Pair("NEXT ", Class.CLASS_1),
            0xF4 to Pair("POKE ", Class.CLASS_8),
            0xF5 to Pair("PRINT ", Class.CLASS_5),
            0xF6 to Pair("PLOT ", Class.CLASS_1),
            0xF7 to Pair("RUN ", Class.CLASS_1),
            0xF8 to Pair("SAVE ", Class.CLASS_B),
            0xF9 to Pair("RANDOMIZE ", Class.CLASS_3),
            0xFA to Pair("IF ", Class.CLASS_1),
            0xFB to Pair("CLS ", Class.CLASS_1),
            0xFC to Pair("DRAW ", Class.CLASS_1),
            0xFD to Pair("CLEAR ", Class.CLASS_3),
            0xFE to Pair("RETURN", Class.CLASS_1),
            0xFF to Pair("COPY ", Class.CLASS_1)
        )
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun listen() {
        NativeLib.getObject().addMemoryWriteWatcher(
            "K_CUR",
            object : NativeLib.MemoryWriteWatcher {
                override fun onWrite(address: Int, data: Short, before: Short, changed: Boolean) {
                    // Abc80Log.wtf("${this@CommandLine}")
                }
            })
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    override fun toString(): String {
        val stringBuilder = StringBuilder()
        val commandLineAddress = NativeLib.getObject().peekw("E_LINE")
        val cursorAddress = NativeLib.getObject().peek(NativeLib.getObject().peekw("K_CUR")) + 100
        NativeLib.getObject()[commandLineAddress, commandLineAddress + cursorAddress].forEachIndexed { index, it ->
            val second = tokens[it.toUByte().toInt()].second
            when (second.first.trim()) {
                "ENTER" -> return stringBuilder.toString()
                else -> stringBuilder.append(second)
            }
        }

        return stringBuilder.toString()
    }

    operator fun plusAssign(increment: List<String>) {
        CoroutineScope(Dispatchers.Default).launch {
            CutNPaste.instance.paste(increment) { _, _, _ ->
                //Abc80Log.commandLine("$progress ${ZXSpectrum.commandLine}")
            }
        }
    }

    operator fun plusAssign(increment: String) {
        CoroutineScope(Dispatchers.Default).launch {
            CutNPaste.instance.paste(increment) {
                Abc80Log.commandLine("$instance")
            }
        }
    }
}
