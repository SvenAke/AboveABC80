package com.aboveware.abovecpm.terminal

import com.aboveware.abovecpm.ZXLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal fun VT320.clearInputBufferInternal() {
    synchronized(lock) {
        inputBuffer.clear()
        inputSeq = ""
    }
}

internal fun VT320.hasCharInput(): Boolean = synchronized(lock) { inputBuffer.isNotEmpty() }

internal fun VT320.getCharInput(): Int = synchronized(lock) {
    if (inputBuffer.isNotEmpty()) inputBuffer.removeAt(0) else 0
}

internal fun VT320.sendResponse(seq: String) {
    synchronized(lock) {
        val response = if (eightBitControls) {
            seq.replace("\u001B[", "\u009B")
                .replace("\u001BO", "\u008F")
                .replace("\u001BN", "\u008E")
                .replace("\u001BP", "\u0090")
                .replace("\u001B\\", "\u009C")
                .replace("\u001BD", "\u0084")
                .replace("\u001BE", "\u0085")
                .replace("\u001BM", "\u008D")
                .replace("\u001BH", "\u0088")
        } else seq
        response.forEach { inputBuffer.add(it.code) }
    }
}

internal fun VT320.handleKeyEvent(char: Char) {
    if (keyboardLocked && !setup.isVisible) return

    synchronized(lock) {
        escFlushJob?.cancel()
        escFlushJob = null
        // A new ESC restarts the escape sequence state machine
        if (char == '\u001B') inputSeq = ""
        inputSeq += char

        // 1. Check for Setup key sequence (\u001B[28~) or fallback Ctrl-B (ascii 2)
        if (inputSeq == VTSetup.SETUP_KEY || (inputSeq.length == 1 && inputSeq[0].code == 2)) {
            ZXLog.terminal("VT320: Setup trigger detected via inputSeq")
            toggleSetup()
            inputSeq = ""
            return
        }

        // 2. Handle input if Setup is active
        if (setup.isVisible) {
            if (setup.handleInput(inputSeq)) {
                inputSeq = ""
            } else if (!setup.isPartialSequence(inputSeq)) {
                // Clear if it's definitely not a partial escape sequence to prevent buffer pollution
                inputSeq = ""
            }
            return
        }

        // 3. Check for Local mode (behaves like a typewriter, no host communication)
        if (VT320Settings.onLineLocal) {
            inputSeq.forEach { putChar(it) }
            inputSeq = ""
            updateStatusLine()
            return
        }

        // 4. Normal CP/M input handling with prefix buffering
        if (inputSeq.startsWith("\u001B")) {
            if (inputSeq.startsWith("\u001B[")) {
                val last = inputSeq.last()
                // Terminators for CSI are 0x40-0x7E.
                if (last.code !in 0x40..0x7E && inputSeq.length < 10) {
                    return
                }
            } else if (inputSeq.length < 2) {
                // Lone Esc key: deliver it if no further characters follow
                escFlushJob = terminalScope.launch {
                    delay(60.milliseconds)
                    synchronized(lock) {
                        if (inputSeq == "\u001B") flush()
                    }
                }
                return
            }
        }

        flush()
    }
}

internal fun VT320.flush() {
    synchronized(lock) {
        if (inputSeq.isEmpty()) return

        val toSend = if (eightBitControls) {
            inputSeq.replace("\u001B[", "\u009B")
                .replace("\u001BO", "\u008F")
                .replace("\u001BN", "\u008E")
                .replace("\u001BP", "\u0090")
                .replace("\u001B\\", "\u009C")
                .replace("\u001BD", "\u0084")
                .replace("\u001BE", "\u0085")
                .replace("\u001BM", "\u008D")
                .replace("\u001BH", "\u0088")
        } else inputSeq

        toSend.forEach { c ->
            inputBuffer.add(c.code)

            if (localEcho)
                putChar(c)
        }
        inputSeq = ""
    }
}

internal fun VT320.triggerAnswerback() {
    val msg = VT320Settings.answerBack
    ZXLog.terminal("VT320: Triggering answerback: $msg")
    msg.forEach { handleKeyEvent(it) }
}

internal fun VT320.triggerMarginBell() {
    if (VT320Settings.marginBell) onBell?.invoke()
}

internal fun VT320.triggerWarningBell() {
    if (VT320Settings.warningBell) onBell?.invoke()
}
