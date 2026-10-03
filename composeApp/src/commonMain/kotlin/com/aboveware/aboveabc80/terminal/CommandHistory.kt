package com.aboveware.aboveabc80.terminal

class CommandHistory {
    private val lock = Any()
    private val commands = mutableListOf<String>()
    private val currentLine = StringBuilder()
    private var selectedIndex: Int? = null
    private var draft = ""
    private var suppressCapture = false
    private var escapePending = false
    private var escapeSequence = false

    fun record(char: Char) = synchronized(lock) {
        if (suppressCapture) return@synchronized

        if (escapePending) {
            escapePending = false
            escapeSequence = char == '[' || char == 'O'
            if (!escapeSequence) return@synchronized
            return@synchronized
        }
        if (escapeSequence) {
            if (char.code in 0x40..0x7E) escapeSequence = false
            return@synchronized
        }

        when (char) {
            '\u001B' -> escapePending = true
            '\r', '\n' -> {
                if (currentLine.isNotEmpty()) commands += currentLine.toString()
                currentLine.clear()
                selectedIndex = null
                draft = ""
            }
            '\b', '\u007F' -> if (currentLine.isNotEmpty()) {
                currentLine.deleteCharAt(currentLine.lastIndex)
            }
            '\u0015' -> {
                currentLine.clear()
                selectedIndex = null
                draft = ""
            }
            else -> if (char.code >= 0x20) {
                currentLine.append(char)
                selectedIndex = null
                draft = ""
            }
        }
    }

    /**
     * Replaces the current line with the selected history entry, sending the
     * required backspaces and replacement text through [sendInput].
     */
    fun navigate(direction: Int, sendInput: (Char) -> Unit): Boolean {
        require(direction == -1 || direction == 1) { "direction must be -1 (up) or 1 (down)" }

        val replacement = synchronized(lock) {
            if (commands.isEmpty()) return false

            val target = if (direction < 0) {
                val nextIndex = selectedIndex?.let { (it - 1).coerceAtLeast(0) }
                    ?: commands.lastIndex
                if (selectedIndex == null) draft = currentLine.toString()
                selectedIndex = nextIndex
                commands[nextIndex]
            } else {
                val index = selectedIndex ?: return false
                if (index >= commands.lastIndex) {
                    selectedIndex = null
                    draft
                } else {
                    selectedIndex = index + 1
                    commands[index + 1]
                }
            }

            val current = currentLine.toString()
            if (target == current) return true

            currentLine.clear()
            currentLine.append(target)
            suppressCapture = true
            "\b".repeat(current.length) + target
        }

        try {
            replacement.forEach(sendInput)
        } finally {
            synchronized(lock) {
                suppressCapture = false
            }
        }
        return true
    }

}
