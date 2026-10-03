package com.aboveware.abovecpm.core

class Tokenizer {

    fun tokenize(line: String): List<String> {
        // First, handle REM comments. Find the first 'REM' that is not inside quotes.
        var remPosition = -1
        var inQuotes = false

        var i = 0
        while (i <= line.length - 3) {
            if (line[i] == '"') {
                inQuotes = !inQuotes
            }

            if (!inQuotes && line.substring(i, i + 3).equals("REM", ignoreCase = true)) {
                val isStartOfLine = i == 0
                val isWordBoundary = isStartOfLine || !line[i - 1].isLetterOrDigit()

                if (isWordBoundary) {
                    remPosition = i
                    break
                }
            }
            i++
        }

        if (remPosition != -1) {
            val codePart = line.take(remPosition)
            val commentPart = line.substring(remPosition + 3)

            val finalTokens = tokenizeInternal(codePart)
                .map { it.trim() } // Trim tokens from the code part
                .filter { it.isNotEmpty() }
                .toMutableList()

            finalTokens.add("REM")
            // Add the comment part as is, without trimming.
            if (commentPart.isNotEmpty()) {
                finalTokens.add(commentPart)
            }
            return finalTokens
        } else {
            // No REM comment found, tokenize and trim the whole line.
            return tokenizeInternal(line)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
    }

    private fun tokenizeInternal(line: String): List<String> {
        val tokens = mutableListOf<String>()
        val delimiters = " ;:)(-+*/"
        var currentToken = StringBuilder()
        var inQuote = false

        for (char in line) {
            when {
                char == '"' -> {
                    if (inQuote) { // Closing quote
                        currentToken.append(char)
                        tokens.add(currentToken.toString())
                        currentToken = StringBuilder()
                    } else { // Opening quote
                        if (currentToken.isNotEmpty()) {
                            tokens.add(currentToken.toString())
                            currentToken = StringBuilder()
                        }
                        currentToken.append(char)
                    }
                    inQuote = !inQuote
                }

                delimiters.contains(char) && !inQuote -> {
                    if (currentToken.isNotEmpty()) {
                        tokens.add(currentToken.toString())
                        currentToken = StringBuilder()
                    }
                    tokens.add(char.toString())
                }

                else -> {
                    currentToken.append(char)
                }
            }
        }

        if (currentToken.isNotEmpty()) {
            tokens.add(currentToken.toString())
        }

        return tokens
    }
}
