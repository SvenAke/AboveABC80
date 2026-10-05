package com.aboveware.aboveabc80

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object TKN80 {
    var enabled by mutableStateOf(false)
        private set

    val columns get() = if (enabled) 80 else 40

    fun rowAddress(row: Int, wide: Boolean = enabled): Int {
        require(row in 0 until ABC80_SCREEN_ROWS)
        return if (wide) 0x5800 + (row % 8) * 0x100 + (row / 8) * 80
        else 0x7c00 + (row % 8) * 0x80 + (row / 8) * 40
    }

    fun run(lib: NativeLib) {
        setEnabled(getPersistedString("tkn80_start", "false").toBoolean(), lib)
        lib.addMemoryReadWatcher(0x0276, object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                for (row in 0 until ABC80_SCREEN_ROWS) {
                    lib.copyToMemory(rowAddress(row, !enabled), ByteArray(if (enabled) 40 else 80))
                }
                return false
            }
        })
    }

    fun setEnabled(wide: Boolean, lib: NativeLib = NativeLib.getObject()) {
        for (row in 0 until ABC80_SCREEN_ROWS) {
            val address = rowAddress(row, wide)
            lib.patchMemory(0x0374 + row * 2, address and 0xff)
            lib.patchMemory(0x0375 + row * 2, address shr 8)
        }
        val width = if (wide) 80 else 40
        for (address in intArrayOf(0x01d8, 0x0211, 0x024e, 0x02de, 0x033c)) {
            lib.patchMemory(address, width)
        }
        lib.patchMemory(0x026f, width - 1)
        enabled = wide
    }
}
