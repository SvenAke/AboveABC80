package com.aboveware.aboveabc80

internal class ABC80CommandHistory {
    private val commands = mutableListOf<String>()
    private var index: Int? = null
    private var draft = ""
    private val input = StringBuilder()
    @Volatile var atPrompt = false

    @Synchronized
    fun record(code: Int) {
        if (!atPrompt) return
        when (code) {
            13 -> { submit(input.toString()); input.clear() }
            8, 127 -> if (input.isNotEmpty()) input.deleteCharAt(input.lastIndex)
            in 32..126 -> input.append(code.toChar())
        }
    }

    @Synchronized
    fun submit(line: String) {
        if (!atPrompt) return
        if (line.isNotBlank() && commands.lastOrNull() != line) commands.add(line)
        index = null
        draft = ""
        atPrompt = false
    }

    @Synchronized
    fun recall(direction: Int, current: String): String? {
        require(direction == -1 || direction == 1)
        if (!atPrompt || commands.isEmpty()) return null
        if (direction < 0) {
            if (index == null) draft = current
            index = index?.let { (it - 1).coerceAtLeast(0) } ?: commands.lastIndex
        } else {
            val selected = index ?: return null
            if (selected == commands.lastIndex) {
                index = null
                return draft
            }
            index = selected + 1
        }
        return commands[requireNotNull(index)]
    }
}

internal fun abc80PromptLine(memory: ByteArray): String? {
    val cursor = decodeABC80Cursor(memory) ?: return null
    val rows = decodeABC80Screen(memory)
    var firstRow = cursor.first
    while (firstRow > 0 && rows[firstRow - 1].last() != ' ') firstRow--
    return buildString {
        for (row in firstRow..cursor.first) {
            append(if (row == cursor.first) rows[row].take(cursor.second) else rows[row].trimEnd())
        }
    }
}
