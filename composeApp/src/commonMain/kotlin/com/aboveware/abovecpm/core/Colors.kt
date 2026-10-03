package com.aboveware.abovecpm.core

class Color(val initialColorBits: UInt) {

    abstract class Cache<Key, Value>(val extra: Int = 0) {
        private val cache = mutableMapOf<Key, Value>()
        operator fun get(key: Key): Value = cache[key] ?: run {
            create(key).also { cache[key] = it }
        }

        abstract fun create(key: Key): Value

        operator fun set(key: Key, value: Value) {
            cache[key] = value
        }
    }

    companion object {
        private val cachedColor = object : Cache<UInt, Color>() {
            override fun create(key: UInt) = Color(key)
        }

        operator fun get(initialColorBits: UInt) = cachedColor[initialColorBits]

        // ZX Spectrum standard colors (0-7)
        val black = Color[0x00u]
        val blue = Color[0x01u]
        val red = Color[0x02u]
        val magenta = Color[0x03u]
        val green = Color[0x04u]
        val cyan = Color[0x05u]
        val yellow = Color[0x06u]
        val white = Color[0x07u]

        // ZX Spectrum bright colors (8-15)
        val bright_black = Color[0x08u]
        val bright_blue = Color[0x09u]
        val bright_red = Color[0x0Au]
        val bright_magenta = Color[0x0Bu]
        val bright_green = Color[0x0Cu]
        val bright_cyan = Color[0x0Du]
        val bright_yellow = Color[0x0Eu]
        val bright_white = Color[0x0Fu]
    }
}
