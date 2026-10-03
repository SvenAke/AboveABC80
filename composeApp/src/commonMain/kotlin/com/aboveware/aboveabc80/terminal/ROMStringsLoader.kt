package com.aboveware.aboveabc80.terminal

import aboveabc80.composeapp.generated.resources.Res
import com.aboveware.aboveabc80.Abc80Log
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Loads and parses VT320 setup strings from the ROM file.
 */
object ROMStringsLoader {
    private var romData: ByteArray? = null

    // Starting offsets for each language in 23-054E7.bin
    private const val OFFSET_EN = 566
    private const val OFFSET_FR = 2602
    private const val OFFSET_DE = 4833

    /**
     * Loads and parses VT320 setup strings from the ROM file.
     */
    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        try {
            romData = Res.readBytes("files/23-054E7.bin")
            Abc80Log.terminal("ROM strings loaded (size=${romData?.size}).")
        } catch (e: Exception) {
            Abc80Log.wtf("Failed to load ROM strings: ${e.message}")
        }
    }

    /**
     * A map of our internal keys to (offset, length) in the ROM for EN, FR, DE.
     * This is a subset of strings found by analyzing the ROM.
     */
    private val romMappings = mapOf(
        "Keyboard" to arrayOf(intArrayOf(566, 8), intArrayOf(2602, 7), intArrayOf(4833, 8)),
        "Transmit" to arrayOf(intArrayOf(575, 8), intArrayOf(2627, 6), intArrayOf(4854, 6)),
        "Receive=" to arrayOf(intArrayOf(584, 8), intArrayOf(2619, 7), intArrayOf(4861, 8)),
        "Set-Up" to arrayOf(intArrayOf(661, 6), intArrayOf(2730, 7), intArrayOf(5014, 5)),
        "Display" to arrayOf(intArrayOf(667, 7), intArrayOf(2837, 5), intArrayOf(5022, 7)),
        "General" to arrayOf(intArrayOf(725, 7), intArrayOf(2864, 7), intArrayOf(5057, 10)),
        "Printer" to arrayOf(intArrayOf(600, 7), intArrayOf(2634, 10), intArrayOf(4940, 7)),
        "On-Line" to arrayOf(intArrayOf(804, 7), intArrayOf(3015, 8), intArrayOf(5152, 6)),
        "Local" to arrayOf(intArrayOf(697, 5), intArrayOf(2880, 5), intArrayOf(5148, 5))
    )

    fun translate(key: String, langIndex: Int): String {
        val data = romData ?: return key
        val mapping = romMappings[key] ?: return key
        val (offset, length) = mapping.getOrElse(langIndex) { mapping[0] }

        return extractString(data, offset, length)
    }

    private fun extractString(data: ByteArray, offset: Int, length: Int): String {
        if (offset + length > data.size) return ""
        val bytes = data.sliceArray(offset until offset + length)

        // VT320 ROM uses Latin-1 mostly, but some chars have high bit set for attributes.
        // We strip the high bit for simplicity if it's not a valid Latin-1 char.
        return bytes.map {
            val b = it.toInt() and 0xFF
            (if (b in 128..<160) (b and 0x7F) else b).toChar()
        }.joinToString("").trim()
    }
}
