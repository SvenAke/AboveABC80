package com.aboveware.aboveabc80.core

import com.aboveware.aboveabc80.Assembler
import com.aboveware.aboveabc80.NativeLib
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class Utilities {
    companion object {
        val instance = Utilities()
    }

    var isUiTriggeredLoad: Boolean = false

    /**
     * Suspends execution until a specific label in the Z80 memory is reached.
     */
    fun waitForLabel(label: String) {
        if (!Assembler.instance.labels.isDefined(label)) {
            // ZXLog.wtf("waitForLabel: Label $label not found, skipping wait.")
            return
        }

        val latch = CountDownLatch(1)

        fun watcher(): NativeLib.MemoryReadWatcher = object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                latch.countDown()
                return true
            }
        }

        NativeLib.getObject().apply {
            addMemoryReadWatcher(label, watcher())
            try {
                latch.await(300, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                removeMemoryReadWatcher(label, watcher())
                Thread.currentThread().interrupt()
            }
        }
    }
}

fun print2File(
    logFileName: String,
    append: Boolean = true,
    callback: (out: FileOutputStream) -> Unit
) {
    val logFile = File(logFileName)
    val outputStream = FileOutputStream(logFile, append)
    val out = PrintStream(outputStream)
    val oldOut = System.out
    val oldErr = System.err

    try {
        System.setOut(out)
        System.setErr(out)
        callback(outputStream)
    } finally {
        out.close()
        System.setOut(oldOut)
        System.setErr(oldErr)
    }
}