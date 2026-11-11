package com.aboveware.abc80

/** TeleDisk's 4 KiB LZ window and adaptive Huffman stream, without an uncompressed size field. */
internal class TeleDiskLzh(private val input: ByteArray, start: Int = 0) {
    private val alphabet = 314
    private val treeSize = alphabet * 2 - 1
    private val root = treeSize - 1
    private val frequency = IntArray(treeSize + 1)
    private val child = IntArray(treeSize)
    private val parent = IntArray(treeSize + alphabet)
    private val history = ByteArray(4096) { 0x20 }
    private var historyPosition = 4096 - 60
    private var bitPosition = start * 8
    private var remainingMatch = 0
    private var matchPosition = 0

    init {
        require(start in 0..input.size) { "Invalid TD0 compressed data offset" }
        for (i in 0 until alphabet) {
            frequency[i] = 1
            child[i] = treeSize + i
            parent[treeSize + i] = i
        }
        var left = 0
        for (node in alphabet..root) {
            frequency[node] = frequency[left] + frequency[left + 1]
            child[node] = left
            parent[left] = node
            parent[left + 1] = node
            left += 2
        }
        frequency[treeSize] = 65535
    }

    private fun bit(): Int {
        require(bitPosition / 8 < input.size) { "Truncated TD0 compressed stream" }
        val result = (input[bitPosition / 8].toInt() ushr (7 - bitPosition % 8)) and 1
        bitPosition++
        return result
    }

    private fun bits(count: Int): Int {
        var value = 0
        repeat(count) { value = (value shl 1) or bit() }
        return value
    }

    private fun rebuild() {
        var leaves = 0
        for (node in 0 until treeSize) {
            if (child[node] >= treeSize) {
                frequency[leaves] = (frequency[node] + 1) / 2
                child[leaves++] = child[node]
            }
        }
        var left = 0
        for (node in alphabet..root) {
            val weight = frequency[left] + frequency[left + 1]
            var insertion = node
            while (insertion > 0 && frequency[insertion - 1] > weight) insertion--
            for (at in node downTo insertion + 1) {
                frequency[at] = frequency[at - 1]
                child[at] = child[at - 1]
            }
            frequency[insertion] = weight
            child[insertion] = left
            left += 2
        }
        for (node in 0 until treeSize) {
            val first = child[node]
            parent[first] = node
            if (first < treeSize) parent[first + 1] = node
        }
    }

    private fun update(symbol: Int) {
        if (frequency[root] == 0x8000) rebuild()
        var node = parent[treeSize + symbol]
        do {
            val weight = ++frequency[node]
            var target = node + 1
            if (weight > frequency[target]) {
                while (weight > frequency[target]) target++
                target--
                frequency[node] = frequency[target]
                frequency[target] = weight
                val a = child[node]
                val b = child[target]
                child[node] = b
                child[target] = a
                parent[a] = target
                parent[b] = node
                if (a < treeSize) parent[a + 1] = target
                if (b < treeSize) parent[b + 1] = node
                node = target
            }
            node = parent[node]
        } while (node != 0)
    }

    private fun distance(): Int {
        val prefix = bits(8)
        val (group, length) = when {
            prefix < 32 -> 0 to 3
            prefix < 80 -> (1 + (prefix - 32) / 16) to 4
            prefix < 144 -> (4 + (prefix - 80) / 8) to 5
            prefix < 192 -> (12 + (prefix - 144) / 4) to 6
            prefix < 240 -> (24 + (prefix - 192) / 2) to 7
            else -> (48 + prefix - 240) to 8
        }
        return (group shl 6) or (((prefix shl (length - 2)) or bits(length - 2)) and 63)
    }

    fun readByte(): Int {
        if (remainingMatch == 0) {
            var node = child[root]
            while (node < treeSize) node = child[node + bit()]
            val symbol = node - treeSize
            update(symbol)
            if (symbol < 256) return record(symbol)
            remainingMatch = symbol - 253
            matchPosition = (historyPosition - distance() - 1) and 4095
        }
        val value = history[matchPosition].toInt() and 255
        matchPosition = (matchPosition + 1) and 4095
        remainingMatch--
        return record(value)
    }

    private fun record(value: Int): Int {
        history[historyPosition] = value.toByte()
        historyPosition = (historyPosition + 1) and 4095
        return value
    }
}
