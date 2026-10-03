package com.aboveware.aboveabc80.tape

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aboveware.aboveabc80.ZXLog

class TapeController {
    companion object {
        val instance = TapeController()
    }

    // Data for the reader
    var readerTape by mutableStateOf(byteArrayOf())
    var readerIndex by mutableIntStateOf(0)

    // Data for the punch
    val punchedTape = mutableStateListOf<Byte>()

    var isVisible by mutableStateOf(false)
    var isPunching by mutableStateOf(false)
    var isReading by mutableStateOf(false)

    /**
     * Flag to indicate the emulator thread is blocked waiting for a paper tape to be loaded.
     */
    @Volatile
    var isWaitingForTape = false

    /**
     * Callback triggered when the system attempts to read from the reader
     * but no paper tape is loaded.
     */
    var onRequireTape: (() -> Unit)? = null

    fun punchChar(c: Char) {
        if (!isVisible) {
            isVisible = true
        }
        isPunching = true
        isReading = false
        punchedTape.add(c.code.toByte())
        // Keep only last N chars for animation if it gets too long? 
        // Or just let it grow like the printer.
        ZXLog.terminal(
            "Paper Tape Punch: 0x${
                c.code.toString(16).uppercase()
            } ('${if (c.code >= 32) c else '.'}')"
        )
    }

    fun readChar(): Char {
        // No paper tape loaded: Request one and BLOCK until available or cancelled
        if (readerTape.isEmpty()) {
            isWaitingForTape = true
            onRequireTape?.invoke()

            // Block the emulator thread while waiting for UI selection
            while (isWaitingForTape && readerTape.isEmpty()) {
                Thread.sleep(50)
            }

            // If still empty, user probably cancelled or something went wrong
            if (readerTape.isEmpty()) {
                isWaitingForTape = false
                isVisible = false
                return 0x1A.toChar()
            }
        }

        if (!isVisible) {
            isVisible = true
        }
        isReading = true
        isPunching = false

        if (readerIndex < readerTape.size) {
            val b = readerTape[readerIndex++]
            val char = b.toInt().and(0xFF).toChar()

            // Check if we reached the physical or logical (Ctrl-Z) end
            if (char.code == 0x1A || readerIndex >= readerTape.size) {
                clear() // Automatically hide and reset everything
            }

            return char
        }

        isVisible = false
        return 0x1A.toChar() // Ctrl-Z (EOF)
    }

    fun loadReaderTape(data: ByteArray) {
        readerTape = data
        readerIndex = 0
        isVisible = true
        isReading = true
        isPunching = false
        isWaitingForTape = false // Resume the blocked thread
    }

    fun clear() {
        punchedTape.clear()
        readerTape = byteArrayOf()
        readerIndex = 0
        isVisible = false
        isPunching = false
        isReading = false
    }
}
