package com.aboveware.abovecpm

import abovecpm.composeapp.generated.resources.Res
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.aboveware.abovecpm.Assembler.Companion.opcodeList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.io.BufferedReader
import java.io.IOException
import java.util.Locale
import java.util.TreeSet

class Assembler {
    @OptIn(ExperimentalResourceApi::class)
    suspend fun loadRomSymbols() = withContext(Dispatchers.Default) {
        try {
            // Zero memory first
            NativeLib.getObject().setRam(0, ByteArray(0x10000))

            val asm48kBytes = Res.readBytes("files/cpm.z80")
            val opcodesBytes = Res.readBytes("files/opcode.lst")

            val asm48kReader = asm48kBytes.decodeToString().reader().buffered()
            val opcodeReader = opcodesBytes.decodeToString().reader().buffered()

            opcodeList = opcodesBytes.decodeToString().lines().toTypedArray()

            assemble(opcodeReader, asm48kReader)
            NativeLib.getObject().copyToMemory(0, memory)

        } catch (e: Exception) {
            ZXLog.wtf("Kunde inte ladda ROM-symboler: ${e.message}")
            throw e
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    @Composable
    fun AsmFileReader() {
        LaunchedEffect(Unit) {
            try {
                loadRomSymbols()
                com.aboveware.abovecpm.core.BIOS.instance.connect()
                NativeLib.getObject().enableLogging(true)
                val cpuSpeedMHz = getPersistedString(
                    "cpu_speed_mhz",
                    (Constants.CPU_FREQUENCY / 1_000_000).toString()
                ).toIntOrNull()?.coerceIn(1, 9) ?: (Constants.CPU_FREQUENCY / 1_000_000)
                NativeLib.getObject().setFastSpeed(cpuSpeedMHz == 9)
                NativeLib.getObject().startEmulator(cpuSpeedMHz.coerceAtMost(8) * 1_000_000)
            } catch (e: Exception) {
                ZXLog.wtf("Kunde inte starta emulatorn: ${e.message}")
            }
        }
    }

    companion object {
        val instance = Assembler()
        private const val EQU = "EQU"
        private const val HIGH = ".HIGH."
        private const val LOW = ".LOW."
        private const val QUOTE = "(?=([^\"]*\"[^\"]*\")*[^\"]*$)(?=([^']*'[^']*')*[^']*$)"
        private val spaceQuote = Regex("\\s$QUOTE")

        // private val comment = Regex(";$QUOTE")
        private val commaQuote = Regex(",$QUOTE")
        private val operations = Regex("[+\\-*]")
        private val spaces = Regex("\\s+")
        var opcodeList: Array<String> = arrayOf()
    }

    private var maxPc = 0
    var memory = ByteArray(0x10000)
    private val touchedMemory = BooleanArray(0x10000)
    private val macros = Macros()
    private var secondPass = false

    open class Opcode(private val tokens: MutableList<String>) {
        private val bytes = tokens[0].toInt()
        private var arguments = 0

        override fun toString() = tokens.toString()
        fun arguments() = arguments

        open fun hex(pc: Int, first: String, second: String) = mutableListOf<String>().apply {
            val buffer = StringBuilder()
            for (index in 1 until bytes + 1) {
                buffer.append(tokens[index])
                if (buffer.length == 8) {
                    add(buffer.toString())
                    buffer.clear()
                }
            }
            if (buffer.isNotEmpty()) {
                add(buffer.toString())
            }
        }

        private fun argument(index: Int) =
            if (tokens.size > bytes + index) tokens[bytes + index] else ""

        fun first() = argument(2)
        fun second() = argument(3)
        open fun assemble(pc: Int, first: String, second: String) = pc + bytes

        init {
            arguments = tokens.count { it == "XX" || it == "$+2" }
        }
    }

    inner class Labels {
        var labels = mutableMapOf<String, Int>()
        private var double = mutableSetOf<String>()
        fun put(label: String, pc: Int) {
            labels[label.remove(":").remove(HIGH).remove(LOW)] = pc
        }

        fun double(label: String) {
            double.add(label)
        }

        fun forEach(action: (Map.Entry<String, Int>) -> Unit) = labels.forEach { action(it) }
        fun label(pc: Int) =
            if (labels.values.contains(pc)) labels.keys.first { pc == labels[it] } else pc.toHex()

        fun dump() {
            val resolvedList = mutableListOf<String>()
            val unresolvedList = mutableListOf<String>()
            val doubleDefinedList = mutableListOf<String>()
            val resolved = StringBuilder()
            val unresolved = StringBuilder()
            val doubleDefined = StringBuilder()
            var resolvedCounter = 0
            var unresolvedCounter = 0
            var doubleDefinedCount = 0
            val labelSize = "%21.20s"
            for (key in TreeSet(labels.keys)) {
                labels[key]?.apply {
                    if (this == -1) {
                        unresolved.append(labelSize.format(key))
                        ++unresolvedCounter
                        if (unresolvedCounter == 6) {
                            unresolvedCounter = 0
                            unresolvedList.add(unresolved.toString())
                            unresolved.clear()
                        }
                    } else if (this < 0) {
                        doubleDefined.append(labelSize.format(key))
                        ++doubleDefinedCount
                        if (doubleDefinedCount == 6) {
                            doubleDefinedCount = 0
                            doubleDefinedList.add(doubleDefined.toString())
                            doubleDefined.clear()
                        }
                    } else {
                        resolved.append(labelSize.format(key))
                        resolved.append(" : ")
                        resolved.append(asWord(this))
                        ++resolvedCounter
                        if (resolvedCounter == 4) {
                            resolvedCounter = 0
                            resolvedList.add(resolved.toString())
                            resolved.clear()
                        }
                    }
                }
            }
            for (label in double) {
                doubleDefined.append(labelSize.format(label))
                ++doubleDefinedCount
                if (doubleDefinedCount == 6) {
                    doubleDefinedCount = 0
                    doubleDefinedList.add(doubleDefined.toString())
                    doubleDefined.clear()
                }
            }
            if (resolved.isNotEmpty()) resolvedList.add(resolved.toString())
            if (unresolved.isNotEmpty()) unresolvedList.add(unresolved.toString())
            if (doubleDefined.isNotEmpty()) doubleDefinedList.add(doubleDefined.toString())
            resolvedList.forEach {
                ZXLog.assemble("LABELS $it")
            }
            unresolvedList.forEach {
                ZXLog.assemble("ULABELS $it")
            }
            doubleDefinedList.forEach {
                ZXLog.assemble("DLABELS $it")
            }
        }

        operator fun contains(key: String): Boolean {
            return labels.containsKey(key.remove(HIGH).remove(LOW))
        }

        operator fun get(key: String): Int {
            val labelKey = key.remove(HIGH).remove(LOW)
            val value = labels[labelKey] ?: run {
                if (secondPass) {
                    ZXLog.wtf("Label $labelKey not found!")
                }
                return 0
            }
            if (key.contains(HIGH)) {
                return value and 0xFF00 shr 8
            }
            return if (key.contains(LOW)) {
                value and 0x00FF
            } else value
        }

        operator fun get(key: Int) = if (labels.containsValue(key))
            labels.keys.first { key == labels[it] }
        else
            "0x${key.toHex()}"

        fun isDefined(label: String) = contains(label)
    }

    var labels = Labels()

    internal inner class Line(val line: String, val lineNo: Int, labels: Labels, val pc: Int) {
        var label = ""
        var opcode = ""
        var first = ""
        var second = ""
        private var lineComment = ""
        private val evaluator = Evaluator()
        override fun toString(): String {
            return line
        }

        private fun second(line: String): String {
            if (line.isEmpty()) {
                return line
            }
            val regex = line.split(spaceQuote).toTypedArray()
            second = (if (line.contains(",")) line else regex[0]).trim()
            return line
        }

        private fun first(line: String): String {
            if (line.isEmpty()) {
                return line
            }
            // Regexp fucks up if we got odd number of ' or "
            val singleQuote = line.count { it == '\'' }
            val doubleQuote = line.count { it == '"' }
            var regex = line.split(commaQuote).map { it.trim() }
            if (regex[0].contains(Regex("\\s"))) regex = line.split(spaceQuote)
            if (singleQuote % 2 == 1) {
                regex = line.remove("'").split(commaQuote)
            }
            if (doubleQuote % 2 == 1) {
                regex = line.remove("\"").split(commaQuote)
            }
            if (line.contains("AF'")) {
                regex = mutableListOf("AF", "AF'")
            }
            if (!line.contains(",")) {
                regex = listOf(line.split(spaceQuote)[0])
            }
            first = regex[0].split(spaceQuote)[0].trim()
            return if (regex.size == 1 || regex[1].isEmpty()) {
                ""
            } else {
                var s = ""
                for (index in 1 until regex.size) {
                    s += "${regex[index]}, "
                }
                s.removeSuffix(", ")
            }
        }

        private fun opcode(line: String): String {
            if (line.isEmpty()) {
                return line
            }
            val opcodes = spaces.split(line)
            opcode = opcodes[0].trim().uppercase()
            if (opcodes.size == 1) {
                return ""
            }
            return line.substring(opcode.length + 1).trim()
        }

        private fun label(line: String): String {
            label = ""
            if (line.isEmpty() || Character.isWhitespace(line[0])) {
                return line.trim()
            }
            val fixedLine = line.replace(':', ' ')
            val labelLength = fixedLine.indexOfFirst { Character.isWhitespace(it) }
            if (labelLength != -1) {
                label = fixedLine.take(labelLength).trim()
                return line.substringAfter(label).trim().removePrefix(":").trim()
            }
            label = line.trim()
            return ""
        }

        /**
         * Find and remove any comments.
         */
        private fun removeComment(line: String): String {
            line.split(";").forEachIndexed { index, comment ->
                if (index == 1) {
                    lineComment = ";$comment"
                    return line.substring(0, line.indexOf(lineComment))
                }
            }
            return line
        }

        val isEmpty: Boolean
            get() = opcode.isEmpty()

        private fun isPossible(arguments: Int, expression: String, firstOrSecond: String): Boolean {
            return if (arguments == 0 && expression == "" && firstOrSecond == "") {
                true
            } else when (expression) {
                "N", "N_", "NN" -> evaluator.evaluate(pc, firstOrSecond)
                "(N)", "(NN)" -> firstOrSecond.isSurrounded('(', ')') && evaluator.evaluate(
                    pc, firstOrSecond.removeSurrounding("(", ")")
                )

                "(IX+N)", "(IY+N)" -> isIndexRegisters(expression, firstOrSecond)
                "$+2" -> evaluator.relative(pc, firstOrSecond)
                else -> compare(firstOrSecond.uppercase(), expression)
            }
        }

        private fun isIndexRegisters(expression: String, firstOrSecond: String) =
            if (firstOrSecond.length > 3 && firstOrSecond.isSurrounded(
                    '(',
                    ')'
                ) && firstOrSecond.startsWith(
                    expression.take(3)
                )
            ) {
                if (firstOrSecond[3] == '+') {
                    evaluator.evaluate(
                        pc,
                        firstOrSecond.substring(4, firstOrSecond.length - 1)
                    ) && evaluator.setIndexRegister()
                } else if (firstOrSecond[3] == '-') {
                    val result = evaluator.evaluate(
                        pc,
                        firstOrSecond.substring(3, firstOrSecond.length - 1)
                    ) && evaluator.setIndexRegister()
                    evaluator.result = evaluator.result.tail(4)
                    result
                } else {
                    evaluator.evaluate(pc, "0") && evaluator.setIndexRegister()
                }
            } else false


        private fun compare(firstOrSecond: String, expression: String): Boolean {
            val eval = Evaluator()
            if (eval.evaluate(pc, firstOrSecond)) {
                val first = eval.result
                if (eval.evaluate(pc, expression) && first == eval.result) {
                    evaluator.result = eval.result
                    return true
                }
            }
            return firstOrSecond == expression
        }

        fun isFirst(expression: Opcode) =
            isPossible(expression.arguments(), expression.first(), first)

        fun isSecond(expression: Opcode) =
            isPossible(expression.arguments(), expression.second(), second)

        fun hex(hex: String): String {
            return when {
                hex.contains("XXXX") -> hex.replace(
                    "XXXX", evaluator.result.substring(2, 4) + evaluator.result.substring(0, 2)
                )

                hex.contains("XX") -> when {
                    evaluator.result.length < 2 -> {
                        ZXLog.assemble("'$first' '$second' $line")
                        ""
                    }

                    evaluator.result.length > 2 -> hex.replace(
                        "XX", evaluator.result.substring(2, 4)
                    )

                    else -> hex.replace("XX", evaluator.result.substring(0, 2))
                }

                else -> hex
                // else -> String.format("%04X", pc) + " " + hex
            }
        }

        val isPseudo: Boolean
            get() = pseudoOpcodes.containsKey(opcode)

        init {
            var assembler = removeComment(line)
            assembler = label(assembler)
            assembler = opcode(assembler)
            assembler = first(assembler)
            second(assembler)
            if (label.isNotEmpty()) {
                if (!secondPass && labels.isDefined(label)) {
                    labels.double(label)
                }
                labels.put(label, pc)
            }
        }
    }

    internal inner class Evaluator {
        var result = ""
        private var reserved = mutableListOf(
            "A",
            "B",
            "C",
            "D",
            "E",
            "F",
            "L",
            "H",
            "AF",
            "BC",
            "DE",
            "HL",
            "IX",
            "IY",
            "NC",
            "NZ",
            "Z",
            "SP",
            "P",
            "I",
            "PE",
            "PO",
            "M",
            "(HL)",
            "(DE)"
        )
        private var indexRegister = false
        private fun removeSpaces(expression: String): String {
            // Glitch for single character
            if (expression.length == 1) {
                return expression
            }
            val tokens = expression.split(spaceQuote).toTypedArray()
            val withoutSpaces = StringBuilder()
            for (token in tokens) {
                if (token.isEmpty()) {
                    continue
                }
                withoutSpaces.append(token)
            }
            return withoutSpaces.toString()
        }

        fun evaluate(pc: Int, expression: String): Boolean {
            if (!indexRegister) {
                result = ""
            }
            removeSpaces(expression.trim()).apply {
                if (isEmpty() || startsWith("IX+") || startsWith("IY+")) {
                    return false
                }
                if (reserved.contains(this.uppercase())) {
                    return false
                }
                if (isSurrounded('"') || isSurrounded('\'')) {
                    val builder = StringBuilder()
                    for (index in 1 until length - 1) {
                        builder.append(asByte(this[index]))
                    }
                    result = if (indexRegister) {
                        builder.toString().substring(0, 2) + result.substring(2)
                    } else {
                        builder.toString()
                    }
                    return true
                }
                val parsed = StringBuilder()
                val terms = split(operations).toTypedArray()
                var term = if (terms[0].isEmpty()) 1 else 0
                var index = 0
                while (index < length) {
                    when (this[index]) {
                        '+' -> parsed.append(this[index++])
                        '-' -> parsed.append(this[index++])
                        '*' -> parsed.append(this[index++])
                        '%' -> parsed.append(this[index++])
                    }
                    val result = evaluateInteger(pc, terms[term])
                    if (!result.first) {
                        return false
                    }
                    parsed.append(result.second)
                    index += terms[term++].length - 1
                    ++index
                }
                result = if (indexRegister) {
                    "${asByte(eval(parsed.toString()))}${result.substring(2)}"
                } else {
                    asWord(eval(parsed.toString()))
                }
                return true
            }
        }

        fun evaluateInteger(pc: Int, expression: String): Pair<Boolean, Int> {
            expression.apply {
                if (this == "$") {
                    return Pair(true, pc)
                }
                if (labels.contains(this)) {
                    return Pair(true, labels[this])
                }
                return if (isSurrounded('\'') && length == 3) {
                    Pair(true, removeSurrounding("'").first().code)
                } else try {
                    Pair(
                        true,
                        if (startsWith("$") || startsWith("0X") || startsWith("0x")) substring(
                            if (startsWith(
                                    "$"
                                )
                            ) 1 else 2
                        ).toInt(
                            16
                        )
                        else when (last()) {
                            'h', 'H' -> substring(0, length - 1).toInt(16)
                            'b', 'B' -> substring(0, length - 1).toInt(2)
                            else -> toInt()
                        }
                    )
                } catch (_: NumberFormatException) {
                    result = "0000"
                    // Check if possible address formatted as XXXX:YYYY
                    val colonIndex = indexOf(":")
                    if (colonIndex != -1) {
                        try {
                            val high = substring(0, colonIndex).toInt()
                            val low = substring(colonIndex + 1).toInt()
                            return Pair(true, high * 256 + low)
                        } catch (_: NumberFormatException) {
                            // Ignore 'n' continue
                        }
                    }
                    Pair(isPossibleLabel(this), pc)
                }
            }
        }

        private fun isCPMCharacter(char: Char): Boolean {
            return Character.isLetterOrDigit(char) || char in listOf(
                '_',
                '?',
                '#',
                '.',
                '\\',
                ']',
                '[',
                '|',
                '}',
                '{'
            )
        }

        private fun isPossibleLabel(expression: String) =
            if (!reserved.contains(expression) && !expression.uppercase(Locale.ROOT)
                    .startsWith("0X") && expression.indexOfFirst { !isCPMCharacter(it) } == -1
            ) !secondPass else false

        private fun eval(str: String): Int {
            return object : Any() {
                var pos = -1
                var ch = 0
                fun nextChar() {
                    ch = if (++pos < str.length) str[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) {
                        nextChar()
                    }
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Int {
                    nextChar()
                    val x = parseExpression()
                    if (pos < str.length) {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }
                    return x
                }

                // Grammar:
                // result = term | result `+` term | result `-` term
                // term = factor | term `*` factor | term `/` factor
                // factor = `+` factor | `-` factor | `(` result `)`
                //        | number | functionName factor | factor `^` factor
                fun parseExpression(): Int {
                    var x = parseTerm()
                    while (true) {
                        when {
                            eat('+'.code) -> x += parseTerm() // addition
                            eat('-'.code) -> x -= parseTerm() // subtraction
                            eat('*'.code) -> x *= parseTerm() // multiplication
                            eat('%'.code) -> x %= parseTerm() // multiplication
                            else -> return x
                        }
                    }
                }

                fun parseTerm(): Int {
                    var x = parseFactor()
                    while (true) {
                        when {
                            eat('*'.code) -> x *= parseFactor() // multiplication
                            eat('/'.code) -> x /= parseFactor() // division
                            else -> {
                                return x
                            }
                        }
                    }
                }

                fun parseFactor(): Int {
                    if (eat('+'.code)) {
                        return parseFactor() // unary plus
                    }
                    if (eat('-'.code)) {
                        return -parseFactor() // unary minus
                    }
                    var x = 0
                    val startPos = pos
                    if (eat('('.code)) { // parentheses
                        x = parseExpression()
                        eat(')'.code)
                    } else if (ch >= '0'.code && ch <= '9'.code) { // numbers
                        while (ch >= '0'.code && ch <= '9'.code) {
                            nextChar()
                        }
                        try {
                            x = str.substring(startPos, pos).toInt()
                        } catch (_: NumberFormatException) {
                            nextChar()
                        }
                    } else {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }
                    return x
                }
            }.parse()
        }

        fun relative(pc: Int, relative: String): Boolean {
            if (labels.contains(relative)) {
                return computeRelative(pc, labels[relative])
            }
            return if (relative.startsWith("$") || relative.uppercase(Locale.ROOT)
                    .startsWith("0X")
            ) {
                try {
                    if (relative == "$") {
                        computeRelative(pc, pc)
                    } else {
                        val remove = if (relative.uppercase(Locale.ROOT).startsWith("0X")) 0 else 1
                        val offset = evaluateInteger(
                            pc,
                            relative.substring(remove)
                        ).second + if (relative.startsWith("$")) pc else 0
                        computeRelative(pc, offset)
                    }
                } catch (_: NumberFormatException) {
                    false
                }
            } else isPossibleLabel(relative)
        }

        private fun computeRelative(pc: Int, relative: Int): Boolean {
            var address = relative - (pc + 2)
            if (address > -129 && address < 129) {
                if (address < 0) {
                    address += 0x100
                }
                result = asWord(address)
                return true
            }
            if (secondPass) {
                result = asWord(0)
            }
            return !secondPass
        }

        fun setIndexRegister(): Boolean {
            indexRegister = true
            return true
        }
    }

    private fun asByte(value: Char) = asByte(value.code)
    private fun asByte(value: Int) = String.format("%02X", value)
    private fun asWord(value: Int) = String.format("%04X", value)

    internal inner class Opcodes {
        private var opcodes = HashMap<String, MutableList<Opcode>>()
        fun add(pseudo: Org) {
            opcodes[pseudo.key()] = pseudo.get()
        }

        /**
         * Load all opcodes into a "smart structure
         */
        fun loadOpcodes(lineReader: BufferedReader) {
            try {
                while (lineReader.ready()) {
                    var tokens = mutableListOf<String>()
                    // Fix any faulty spaces by removing all empty tokens
                    for (token in lineReader.readLine().split(Regex("[ ,]"))) {
                        if (token.trim().isNotEmpty()) {
                            tokens.add(token.trim())
                        }
                    }
                    val bytes = tokens[0].toInt()
                    var key = tokens[bytes + 1].uppercase()
                    // Glitch! missing XX in opcodes for all $+2's
                    if (tokens[tokens.size - 1] == "$+2") {
                        key = tokens[bytes].uppercase()
                        tokens = if (tokens.size == 5) {
                            mutableListOf(
                                tokens[0].trim(),
                                tokens[1].trim(),
                                "XX",
                                tokens[2].trim(),
                                tokens[3],
                                tokens[4]
                            )
                        } else {
                            mutableListOf(
                                tokens[0].trim(), tokens[1].trim(), "XX", tokens[2], tokens[3]
                            )
                        }
                    }
                    add(tokens, key)

                    // Aliases!
                    isInA(tokens, key)
                    isOutA(tokens, key)
                    isAf(tokens, key)
                    isJr(tokens, key)
                    isJpCall(tokens, key)
                    isAddAdcA(tokens, key)
                    isRet(tokens, key)
                }
            } catch (e: Exception) {
                ZXLog.assemble("OPCODE ${e.message}")
            }
        }

        private fun isRet(tokens: MutableList<String>, key: String) {
            if (tokens.size == 4 && key == "RET") {
                val alias = key + tokens[3]
                add(mutableListOf(tokens[0], tokens[1], alias), alias)
            }
        }

        private fun isAddAdcA(tokens: MutableList<String>, key: String) {
            if (key == "ADD" || key == "ADC") {
                if (tokens.size == 7 && tokens[5] == "A") {
                    add(
                        mutableListOf(
                            tokens[0], tokens[1], tokens[2], tokens[3], tokens[4], tokens[6]
                        ), key
                    )
                }
                // ADD == ADD A,
                if (tokens.size == 6 && tokens[4] == "A" && tokens[5] == "N") {
                    add(
                        mutableListOf(
                            tokens[0], tokens[1], tokens[2], tokens[3], tokens[5], ""
                        ), key
                    )
                }
                // ADD == ADD A,
                if (tokens.size == 5 && tokens[3] == "A") {
                    add(mutableListOf(tokens[0], tokens[1], tokens[2], tokens[4]), key)
                }
            }
        }

        private fun isJpCall(tokens: MutableList<String>, key: String) {
            if (tokens.size == 7 && (key == "JP" || key == "CALL")) {
                val alias = key + tokens[5]
                add(
                    mutableListOf(
                        tokens[0], tokens[1], tokens[2], tokens[3], alias, tokens[6]
                    ), alias
                )
            }
        }

        private fun isJr(tokens: MutableList<String>, key: String) {
            if (tokens.size == 6 && key == "JR") {
                val alias = tokens[3] + tokens[4]
                add(
                    mutableListOf(tokens[0], tokens[1], tokens[2], alias, tokens[5]), alias
                )
            }
        }

        private fun isAf(tokens: MutableList<String>, key: String) {
            if (tokens.size == 5 && key == "EX" && tokens[3] == "AF") {
                add(mutableListOf(tokens[0], tokens[1], tokens[2], tokens[3]), key)
            }
        }

        private fun isOutA(tokens: MutableList<String>, key: String) {
            if (tokens.size == 6 && key == "OUT" && tokens[4] == "(N)" && tokens[5] == "A") {
                val alias = "OUT"
                add(mutableListOf(tokens[0], tokens[1], tokens[2], alias, "NN"), alias)
            }
        }

        private fun isInA(tokens: MutableList<String>, key: String) {
            if (tokens.size == 6 && key == "IN" && tokens[4] == "A" && tokens[5] == "(N)") {
                val alias = "INP"
                add(mutableListOf(tokens[0], tokens[1], tokens[2], alias, "NN"), alias)
            }
        }

        private fun add(tokens: MutableList<String>, key: String) {
            if (!opcodes.containsKey(key)) {
                opcodes[key] = mutableListOf()
            }
            opcodes[key]!!.add(Opcode(tokens))
        }

        fun containsKey(opcode: String): Boolean {
            return opcodes.containsKey(opcode)
        }

        operator fun get(opcode: String): MutableList<Opcode> {
            return opcodes[opcode]!!
        }

        fun addAll(opcodes: Opcodes) {
            opcodes.opcodes.forEach {
                this.opcodes[it.key] = it.value
            }
        }
    }

    private val opcodes: Opcodes = Opcodes()
    private val pseudoOpcodes: Opcodes = Opcodes()
    private fun assemble(opcodes: BufferedReader, asm48k: BufferedReader) {
        try {
            setup(opcodes)
            secondPass = false
            touchedMemory.fill(false)
            asm48k.mark(1000000)
            try {
                assemble(asm48k)
            } catch (_: Exception) {
            }
            asm48k.reset()
            secondPass = true
            try {
                assemble(asm48k)
            } catch (_: Exception) {
            }
            if (ZXLogTag.ASSEMBLE.enabled) {
                labels.dump()
                dumpIntelHex()
            }
        } catch (e: IOException) {
            ZXLog.wtf("${e.message}")
        }
    }

    private fun setup(opcodeStream: BufferedReader) {
        opcodes.loadOpcodes(opcodeStream)
        pseudoOpcodes.add(End())
        pseudoOpcodes.add(Title())
        pseudoOpcodes.add(Org())
        pseudoOpcodes.add(Defm())
        pseudoOpcodes.add(Dm())
        pseudoOpcodes.add(Defs())
        pseudoOpcodes.add(Ds())
        pseudoOpcodes.add(Defw())
        pseudoOpcodes.add(Dw())
        pseudoOpcodes.add(Defb())
        pseudoOpcodes.add(Db())
        pseudoOpcodes.add(Equ())
        opcodes.addAll(pseudoOpcodes)
    }

    @Throws(IOException::class)
    private fun assemble(lineReader: BufferedReader) {
        var pc = 0
        var lineNo = 0
        maxPc = 0
        while (lineReader.ready()) {
            val line = Line(lineReader.readLine(), ++lineNo, labels, pc)
            if (line.isEmpty) {
                if (secondPass && ZXLogTag.ASSEMBLE.enabled) ZXLog.assemble(
                    String.format(
                        "%20.20s %s",
                        " ",
                        line
                    )
                )
                continue
            }
            when {
                macros.isCollecting -> macros.add(line)
                macros.contains(line.opcode) -> {
                    for (macroLine in macros.lines(line)) {
                        pc = assemble(pc, macroLine)
                    }
                }

                line.opcode == "MACRO" -> macros.add(line)
                line.opcode == EQU -> {
                    labels.put(line.label, assemble(pc, line))
                }

                else -> pc = assemble(pc, line)
            }
        }
    }

    /**
     * Holds all compiled source code lines
     */
    private val sourceCode = mutableMapOf<Int, MutableList<Pair<String, String>>>()

    /**
     * Save the assembled line in the collection
     */
    fun addSourceCode(pc: Int, args: String, line: String = "") {
        if (!sourceCode.contains(pc)) {
            sourceCode[pc] = mutableListOf()
        }
        sourceCode[pc]!!.add(Pair(args, line.tab2Space(4)))
    }

    /**
     * We now has a line to assemble that is formatted as:
     * operation whitespaces [optional extra]
     */
    private fun assemble(pc: Int, line: Line): Int {
        val opcode = line.opcode.uppercase()
        if (opcodes.containsKey(opcode)) {
            val candidates = opcodes[opcode]
            for (candidate in candidates) {
                if (line.isPseudo || line.isFirst(candidate) && line.isSecond(candidate)) {
                    if (secondPass) {
                        var first = true
                        var memoryPc = pc
                        for (hex in candidate.hex(pc, line.first, line.second)) {
                            if (first) {
                                val args = line.hex(hex)
                                memoryPc = add2memory(pc, args)
                                // val len = if (line.first.length > 15) line.first.length else 15
                                val format = "%s %-15.15s %s"
                                if (ZXLogTag.ASSEMBLE.enabled) ZXLog.assemble(
                                    String.format(format, pc.toHex(), args, line)
                                )
                                addSourceCode(pc, args, line.toString())
                                first = false
                            } else {
                                memoryPc = add2memory(memoryPc, hex)
                                if (ZXLogTag.ASSEMBLE.enabled) ZXLog.assemble(
                                    String.format(
                                        "     %-15.15s",
                                        hex
                                    )
                                )
                                addSourceCode(pc, hex)
                            }
                        }
                        if (first && ZXLogTag.ASSEMBLE.enabled) ZXLog.assemble(
                            String.format(
                                "%20.20s %s",
                                " ",
                                line
                            )
                        )
                    }
                    return candidate.assemble(pc, line.first, line.second)
                }
            }
            if (secondPass) {
                ZXLog.assemble("NO_MATCH $line")
            }
        } else {
            if (secondPass) {
                ZXLog.assemble("UNKNOWN $line")
            }
        }
        return pc
    }

    private fun add2memory(pc: Int, args: String): Int {
        var pointer = pc
        for (index in args.indices step 2) {
            val value = args.substring(index, index + 2).toInt(16).toByte()
            memory[pointer] = value
            touchedMemory[pointer] = true
            pointer++
        }
        if (maxPc < pointer) maxPc = pointer
        return pointer
    }

    private fun dumpIntelHex() {
        val records = mutableListOf<String>()
        var start = -1

        for (i in 0 until 0x10000) {
            if (touchedMemory[i]) {
                if (start == -1) start = i

                // Max 16 bytes per record, or break if next byte not touched
                if (i - start == 15 || (i + 1 < 0x10000 && !touchedMemory[i + 1])) {
                    records.add(toIntelHexRecord(start, memory.copyOfRange(start, i + 1)))
                    start = -1
                }
            }
        }
        records.add(":00000001FF") // End of File record

        ZXLog.terminal("--- INTEL HEX DUMP START ---")
        records.forEach { ZXLog.terminal(it) }
        ZXLog.terminal("--- INTEL HEX DUMP END ---")
    }

    private fun toIntelHexRecord(address: Int, data: ByteArray): String {
        val sb = StringBuilder(":")
        val len = data.size
        sb.append(len.toHex(2))
        sb.append(address.toHex(4))
        sb.append("00") // Data record type

        var checksum = len + (address shr 8) + (address and 0xFF)
        for (b in data) {
            val value = b.toInt() and 0xFF
            sb.append(value.toHex(2))
            checksum += value
        }

        val finalChecksum = (0x100 - (checksum and 0xFF)) and 0xFF
        sb.append(finalChecksum.toHex(2))
        return sb.toString().uppercase()
    }

    private inner class Macros {
        operator fun contains(macro: String): Boolean {
            return macros.containsKey(macro)
        }

        fun lines(line: Line): MutableList<Line> {
            return macros[line.opcode]!!.lines(line)
        }

        private inner class Macro(val macro: String) {
            val parameters = mutableListOf<String>()
            private val macros = mutableListOf<String>()
            fun add(line: Line) {
                macros.add(line.line)
            }

            fun lines(line: Line): MutableList<Line> {
                val parameters =
                    line.line.substring(line.opcode.length + line.line.indexOf(line.opcode)).trim()
                        .split(Regex(",$QUOTE"))
                        .toTypedArray()
                val lines = mutableListOf<Line>()
                for (macroLine in macros) {
                    lines.add(
                        Line(
                            substitute(macroLine, parameters), line.lineNo, labels, line.pc
                        )
                    )
                }
                return lines
            }

            private fun substitute(macroLine: String, parameters: Array<String>): String {
                var result = macroLine
                if (parameters.size == this.parameters.size) {
                    for (index in parameters.indices) {
                        result = result.replace(this.parameters[index], parameters[index])
                    }
                }
                return result
            }
        }

        private var current: Macro? = null
        var macros = HashMap<String, Macro?>()
        fun add(line: Line) {
            if (isCollecting) {
                if (line.opcode == "ENDM") {
                    macros[current!!.macro] = current
                    current = null
                } else {
                    current!!.add(line)
                }
            } else {
                current = Macro(line.label)
                for (parameter in line.line.substring(line.line.indexOf("MACRO") + 5).split("&")
                    .toTypedArray()) {
                    if (parameter.trim().isNotEmpty()) {
                        current!!.parameters.add(
                            "&" + parameter.remove(",").trim()
                        )
                    }
                }
            }
        }

        val isCollecting: Boolean
            get() = current != null
    }

    open inner class Org(val key: String = "ORG") : Opcode(mutableListOf("0", "YY", "NN")) {
        fun get(): MutableList<Opcode> {
            val opcodes = mutableListOf<Opcode>()
            opcodes.add(this)
            return opcodes
        }

        fun key() = key

        override fun assemble(pc: Int, first: String, second: String): Int {
            return Evaluator().evaluateInteger(pc, first).second
        }
    }

    private open inner class Defm(key: String = "DEFM") : Org(key) {

        override fun hex(pc: Int, first: String, second: String): MutableList<String> {
            var value = first
            if (second.isNotEmpty()) {
                value += ",$second"
            }
            val buffer = StringBuilder()
            for (token in value.split(commaQuote)) {
                val chars = token.trim()
                if (chars[0] == '\"') {
                    val length = stringIncrement(chars.length - 2) + 1
                    var index = 1
                    while (index < length) {
                        if (chars[index] == '\\' && index + 1 < length) {
                            ++index
                            buffer.append(quoted(chars[index]))
                        } else {
                            buffer.append(asByte(chars[index]))
                        }
                        ++index
                    }
                } else {
                    val evaluator = Evaluator()
                    if (evaluator.evaluate(pc, token)) {
                        buffer.append(getValue(evaluator))
                    }
                }
            }
            return formatHexOutput(buffer)
        }

        open fun getChar(chars: ByteArray, index: Int): Byte {
            return if (chars.size - 1 > index) chars[index] else 0
        }

        open fun getValue(evaluator: Evaluator): String {
            return evaluator.result.substring(2)
        }

        override fun assemble(pc: Int, first: String, second: String): Int {
            var value = first
            if (second.isNotEmpty()) {
                value += ",$second"
            }
            var length = 0
            for (token in value.split(commaQuote)) {
                try {
                    length += if (token.trim()[0] == '\"') {
                        stringIncrement(token.trim().escapedLength())
                    } else {
                        increment()
                    }
                } catch (_: StringIndexOutOfBoundsException) {
                    increment()
                }
            }
            return pc + length
        }

        open fun stringIncrement(increment: Int): Int {
            return increment
        }

        open fun increment(): Int {
            return 1
        }
    }

    private fun formatHexOutput(buffer: StringBuilder): MutableList<String> {
        val hexes = mutableListOf<String>()
        val line = StringBuilder()
        for (element in buffer) {
            line.append(element)
            if (line.length == 8) {
                hexes.add(line.toString())
                line.clear()
            }
        }
        if (line.isNotEmpty()) {
            hexes.add(line.toString())
        }
        return hexes
    }

    private fun quoted(char: Char) = when (char) {
        'r' -> "0D"
        'b' -> "07"
        't' -> "09"
        'n' -> "0A"
        '\\' -> asByte('\\')
        else -> asByte(char)
    }

    private inner class Dm : Defm("DM")
    private open inner class Defs(key: String = "DEFS") : Defm(key) {

        override fun getValue(evaluator: Evaluator): String {
            val length = Evaluator().evaluateInteger(0, evaluator.result + "H").second
            val builder = StringBuilder()
            repeat(length) {
                builder.append("00")
            }
            return builder.toString()
        }

        override fun assemble(pc: Int, first: String, second: String): Int {
            var value = first
            if (second.isNotEmpty()) {
                value += ",$second"
            }
            var length = 0
            for (token in value.split(commaQuote)) {
                val c = token.trim()[0]
                length += if (c == '\"' || c == '\'') {
                    stringIncrement(token.trim().escapedLength())
                } else {
                    Evaluator().evaluateInteger(pc, token).second
                }
            }
            return pc + length
        }
    }

    private inner class Ds : Defs("DS")
    private open inner class Defb(key: String = "DEFB") : Defm(key) {

        override fun hex(pc: Int, first: String, second: String): MutableList<String> {
            var value = first
            if (second.isNotEmpty()) {
                value += ",$second"
            }
            val buffer = StringBuilder()
            for (token in value.split(commaQuote)) {
                val chars = token.trim()
                if (chars[0] == '\'') {
                    val evaluator = Evaluator()
                    if (evaluator.evaluate(pc, token)) {
                        buffer.append(evaluator.result.tail(2))
                    }
                } else {
                    val evaluator = Evaluator()
                    if (evaluator.evaluate(pc, token)) {
                        buffer.append(evaluator.result.tail(2))
                    }
                }
            }
            return formatHexOutput(buffer)
        }

        override fun assemble(pc: Int, first: String, second: String): Int {
            var value = first
            if (second.isNotEmpty()) {
                value += ",$second"
            }
            return pc + value.split(commaQuote).size
        }
    }

    private inner class Db : Defb("DB")
    private open inner class Defw(key: String = "DEFW") : Defm(key) {

        override fun getChar(chars: ByteArray, index: Int): Byte {
            return if (chars.size - 1 > index) chars[index] else 0
        }

        override fun getValue(evaluator: Evaluator): String {
            return "${evaluator.result.substring(2, 4)}${evaluator.result.substring(0, 2)}"
        }

        override fun stringIncrement(increment: Int): Int {
            return increment + increment % 2
        }

        override fun increment(): Int {
            return 2
        }
    }

    private inner class Dw : Defw("DW")

    private inner class Equ : Org(EQU) {
        override fun assemble(pc: Int, first: String, second: String): Int {
            return Evaluator().evaluateInteger(pc, first).second
        }
    }

    private inner class End : Org("END") {
        override fun assemble(pc: Int, first: String, second: String): Int {
            return pc
        }
    }

    private inner class Title : Org("TITLE") {
        override fun assemble(pc: Int, first: String, second: String): Int {
            return pc
        }
    }

    private fun String.isSurrounded(first: Char, last: Char = first) =
        last() == last && first() == first

    private fun String.remove(string: String) = replace(string, "")

    private fun String.escapedLength(): Int {
        var index = 1
        var len = 0
        while (index < length - 1) {
            ++len
            if (this[index] == '\\' && index + 1 < length) {
                ++index
                when (this[index]) {
                    'r' -> ++index
                    'b' -> ++index
                    't' -> ++index
                    'n' -> ++index
                    '\\' -> ++index
                }
            } else ++index
        }
        return len
    }
}

fun String.toOpcode() = opcodeList.find {
    it.substring(5).substringBefore(",").substringBefore("$").trim() == this
}?.substring(2, 4)?.toInt(16)!!
