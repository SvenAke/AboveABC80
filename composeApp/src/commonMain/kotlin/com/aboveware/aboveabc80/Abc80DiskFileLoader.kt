package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.core.DiskController
import kotlinx.coroutines.delay
import kotlin.time.TimeSource

internal fun abc80DosPromptVisible(memory: ByteArray): Boolean {
    val rows = decodeABC80Screen(memory)
    val dosPrompt = rows.indexOfLast { it.contains("R E A D Y") }
    return dosPrompt >= 0 && dosPrompt > rows.indexOfLast { it.trim() == "ABC80" }
}

internal suspend fun runABC80AbsoluteFile(nativeLib: NativeLib, name: String) {
    fun dosReady() = abc80DosPromptVisible(nativeLib.getMemory())
    if (!dosReady()) {
        pasteABC80Text(nativeLib, "BYE\r")
        val started = TimeSource.Monotonic.markNow()
        while (!dosReady() && started.elapsedNow().inWholeMilliseconds < 12000) delay(20)
        check(dosReady()) { "DOS did not start. Mount a system disk containing CMDINT.SYS." }
    }
    nativeLib.dosActive = true
    pasteABC80Text(nativeLib, "$name\r")
}

internal suspend fun loadABC80DiskFile(nativeLib: NativeLib, drive: Int, name: String, run: Boolean) {
    val floppy = checkNotNull(DiskController.instance.getFloppy(drive)) { "No disk in DR$drive:" }
    val data = checkNotNull(floppy.readFile(name.substringBeforeLast('.', name), name.substringAfterLast('.', ""))) {
        "Could not read DR$drive:$name"
    }
    if (abc80ExecutableFormat(name, data, 253) == Abc80ExecutableFormat.ABS) {
        require(run) { "ABS files must be executed from DOS, not loaded from BASIC" }
        Abc80CloudDiskImport.validateAbs(name, data)
        runABC80AbsoluteFile(nativeLib, "DR$drive:$name")
    } else {
        pasteABC80Text(nativeLib, "${if (run) "RUN" else "LOAD"} DR$drive:$name\r")
    }
}
