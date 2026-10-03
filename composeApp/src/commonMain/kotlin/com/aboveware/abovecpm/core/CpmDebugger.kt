package com.aboveware.abovecpm.core

import abovecpm.composeapp.generated.resources.Res
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aboveware.abovecpm.NativeLib
import com.aboveware.abovecpm.ZXLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlin.time.Duration.Companion.milliseconds

/**
 * Debugger for CP/M Z80 source code using cpm.txt listing.
 */
class CpmDebugger {
    companion object {
        val instance = CpmDebugger()
    }

    data class SourceLine(
        val address: Int,
        val hex: String,
        val code: String,
        val originalLine: String
    )

    private val sourceMap = mutableMapOf<Int, SourceLine>()
    val lines = mutableStateListOf<SourceLine>()
    private val breakpoints = mutableMapOf<Int, NativeLib.MemoryReadWatcher>()

    var isEnabled by mutableStateOf(false)
    var currentPc by mutableStateOf(0)
    var isPaused by mutableStateOf(false)
    var onBreakpointHit: (() -> Unit)? = null

    @OptIn(ExperimentalResourceApi::class)
    fun init(scope: CoroutineScope) {
        isEnabled = true
        scope.launch(Dispatchers.Default) {
            try {
                val bytes = Res.readBytes("files/cpm.txt")
                val text = bytes.decodeToString()
                parseListing(text)
                ZXLog.terminal("CpmDebugger: Loaded ${sourceMap.size} source lines")
            } catch (e: Exception) {
                ZXLog.wtf("CpmDebugger: Failed to load cpm.txt: ${e.message}")
            }
        }
    }

    private fun parseListing(text: String) {
        sourceMap.clear()
        lines.clear()

        text.lines().forEach { line ->
            if (line.startsWith("[ASSEMBLE]")) {
                val content = line.substring(10)
                val trimmed = content.trim()
                if (trimmed.isEmpty()) return@forEach

                val parts = trimmed.split(Regex("\\s+"))
                if (parts.isEmpty()) return@forEach

                var addr = -1
                var hex = ""
                var codeText: String

                val first = parts[0]
                val isFirstHex =
                    first.length == 4 && first.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }

                if (isFirstHex) {
                    try {
                        addr = first.toInt(16)
                        if (parts.size >= 2 && parts[1].all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
                            hex = parts[1]
                            codeText = trimmed.substringAfter(hex).trim()
                        } else {
                            codeText = trimmed.substringAfter(first).trim()
                        }
                    } catch (_: Exception) {
                        codeText = trimmed
                    }
                } else {
                    codeText = trimmed
                }

                val isLabel =
                    codeText.contains(Regex("^[a-zA-Z0-9_]+:")) || codeText.contains(Regex("\\s+[a-zA-Z0-9_]+:"))
                val isEqu = codeText.contains("EQU", ignoreCase = true)

                if (hex.isNotEmpty() || isLabel || isEqu) {
                    // 1. Separate label, instruction and comment
                    val labelMatch = Regex("^([a-zA-Z0-9_]+:)?\\s*(.*)").find(codeText)
                    val label = labelMatch?.groupValues?.get(1) ?: ""
                    val rest = labelMatch?.groupValues?.get(2) ?: ""

                    val instr = rest.substringBefore(";").trim()
                    val comment = if (rest.contains(";")) rest.substringAfter(";").trim() else ""

                    // 2. Format with fixed columns
                    // Label at 0, Instruction at 12, Comment at 42
                    val sb = StringBuilder()
                    if (label.isNotEmpty()) {
                        sb.append(label)
                    }

                    // Pad to instruction column
                    while (sb.length < 12) sb.append(" ")
                    sb.append(instr)

                    if (comment.isNotEmpty()) {
                        // Pad to comment column
                        while (sb.length < 42) sb.append(" ")
                        sb.append(" ; ").append(comment)
                    }

                    val sourceLine = SourceLine(addr, hex, sb.toString(), line)
                    if (addr != -1 && hex.isNotEmpty()) {
                        sourceMap[addr] = sourceLine
                    }
                    lines.add(sourceLine)
                }
            }
        }
    }

    fun step() {
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            NativeLib.getObject().step()
            delay(10.milliseconds) // Wait for native thread to finish instruction
            updatePc()
        }
    }

    fun toggleBreakpoint(address: Int) {
        val lib = NativeLib.getObject()
        if (breakpoints.containsKey(address)) {
            lib.removeMemoryReadWatcher(address, breakpoints[address]!!)
            breakpoints.remove(address)
        } else {
            val bpWatcher = object : NativeLib.MemoryReadWatcher {
                override fun onRead(address: Int): Boolean {
                    if (breakpoints.containsKey(address)) {
                        lib.freeze(true)
                        isPaused = true
                        updatePc()
                        onBreakpointHit?.invoke()
                    }
                    return false // Keep watcher
                }
            }
            breakpoints[address] = bpWatcher
            lib.addMemoryReadWatcher(address, bpWatcher)
        }
    }

    fun isBreakpoint(address: Int) = breakpoints.containsKey(address)

    fun togglePause() {
        isPaused = !isPaused
        NativeLib.getObject().freeze(isPaused)
        if (isPaused) {
            updatePc()
        }
    }

    fun updatePc() {
        currentPc = cpu().pc
    }

    fun getLineForPc(pc: Int): Int {
        // Find the index of the SourceLine with this address
        return lines.indexOfFirst { it.address == pc }
    }
}
