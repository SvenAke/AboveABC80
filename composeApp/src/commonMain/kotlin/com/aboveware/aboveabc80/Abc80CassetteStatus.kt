package com.aboveware.aboveabc80

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Observable cassette progress and tape state, written from the emulator thread and shown by the UI. */
object Abc80CassetteStatus {
    enum class Activity { Idle, Reading, Writing }
    enum class EndOfTapeAction { Waiting, Restart, Cancel }

    var missingProgram by mutableStateOf<String?>(null)
        private set

    @Volatile
    var endOfTapeAction = EndOfTapeAction.Cancel
        private set

    var activity by mutableStateOf(Activity.Idle)
        private set
    var visible by mutableStateOf(false)
        private set
    var text by mutableStateOf("")
        private set
    var indeterminate by mutableStateOf(true)
        private set
    var progress by mutableStateOf(0)
        private set
    var max by mutableStateOf(0)
        private set
    var tapeName by mutableStateOf("")
    var hasTape by mutableStateOf(false)

    @Volatile
    var cancelled = false

    fun startReading(filename: String) {
        if (activity == Activity.Reading) return
        activity = Activity.Reading
        show("Loading $filename...")
    }

    fun startWriting(filename: String) {
        if (activity == Activity.Writing) return
        activity = Activity.Writing
        show("Saving $filename...")
    }

    fun show(text: String, indeterminate: Boolean = true, progress: Int = 0, max: Int = 0) {
        this.text = text
        this.indeterminate = indeterminate
        this.progress = progress
        this.max = max
        visible = true
    }

    fun hide() {
        missingProgram = null
        endOfTapeAction = EndOfTapeAction.Cancel
        activity = Activity.Idle
        visible = false
        cancelled = false
    }

    fun cancel() {
        cancelled = true
        missingProgram = null
        endOfTapeAction = EndOfTapeAction.Cancel
    }

    fun programNotFound(filename: String) {
        endOfTapeAction = EndOfTapeAction.Waiting
        missingProgram = filename
    }

    fun restartSearch() {
        missingProgram = null
        endOfTapeAction = EndOfTapeAction.Restart
    }
}
