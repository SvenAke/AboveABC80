package com.aboveware.aboveabc80.core

import aboveabc80.composeapp.generated.resources.Res
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aboveware.aboveabc80.NativeLib
import com.aboveware.aboveabc80.Abc80Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

class Abc80Debugger {
    companion object {
        val instance = Abc80Debugger()

        internal fun parseListing(text: String): List<SourceLine> {
            val pattern = Regex("""^(.*?)\s*:\s*(\d{3}):(\d{3})\s*:\s*((?:[0-9a-f]{2}\s+)+)(.*)$""")
            return text.lineSequence().mapNotNull { line ->
                val match = pattern.matchEntire(line) ?: return@mapNotNull null
                val address = match.groupValues[2].toInt() * 256 + match.groupValues[3].toInt()
                require(address in 0..65535) { "Ogiltig adress i ABC80-listningen" }
                val label = match.groupValues[1].trim().takeUnless { it.startsWith("0x") }.orEmpty()
                SourceLine(address, match.groupValues[4].trim().uppercase(),
                    label.padEnd(12) + match.groupValues[5], line)
            }.toList().also { require(it.isNotEmpty()) { "ABC80-listningen saknar instruktioner" } }
        }
    }

    data class SourceLine(val address: Int, val hex: String, val code: String, val originalLine: String)

    val lines = mutableStateListOf<SourceLine>()
    private val breakpoints = mutableStateMapOf<Int, NativeLib.MemoryReadWatcher>()
    var currentPc by mutableStateOf(0)
    var isPaused by mutableStateOf(false)
    var onBreakpointHit: (() -> Unit)? = null

    @OptIn(ExperimentalResourceApi::class)
    fun init(scope: CoroutineScope) {
        scope.launch {
            try {
                val listing = parseListing(Res.readBytes("files/disassembler.txt").decodeToString())
                lines.clear()
                lines.addAll(listing)
                updatePc()
            } catch (e: Exception) {
                Abc80Log.wtf("ABC80 debugger: ${e.message}")
                throw e
            }
        }
    }

    fun step(scope: CoroutineScope) {
        scope.launch {
            NativeLib.getObject().step()
            delay(10)
            updatePc()
        }
    }

    fun toggleBreakpoint(address: Int) {
        val lib = NativeLib.getObject()
        val existing = breakpoints.remove(address)
        if (existing != null) {
            lib.removeMemoryReadWatcher(address, existing)
        } else {
            val watcher = object : NativeLib.MemoryReadWatcher {
                override fun onRead(address: Int): Boolean {
                    lib.freeze(true)
                    isPaused = true
                    updatePc()
                    onBreakpointHit?.invoke()
                    return false
                }
            }
            breakpoints[address] = watcher
            lib.addMemoryReadWatcher(address, watcher)
        }
    }

    fun isBreakpoint(address: Int) = breakpoints.containsKey(address)

    fun togglePause() {
        isPaused = !isPaused
        NativeLib.getObject().freeze(isPaused)
        if (isPaused) updatePc()
    }

    fun updatePc() { currentPc = cpu().pc }
    fun getLineForPc(pc: Int) = lines.indexOfFirst { it.address == pc }
}
