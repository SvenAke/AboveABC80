package com.aboveware.abc80

/** Thread-safe observable cassette progress and tape state, written from the emulator thread and polled by the UI. */
object Abc80CassetteStatus {
    enum class Activity { Idle, Reading, Writing }
    enum class EndOfTapeAction { Waiting, Restart, Cancel }

    @Volatile var missingProgram: String? = null
    @Volatile var endOfTapeAction = EndOfTapeAction.Cancel
    @Volatile var activity = Activity.Idle
    @Volatile var visible = false
    @Volatile var text = ""
    @Volatile var indeterminate = true
    @Volatile var progress = 0
    @Volatile var max = 0
    @Volatile var tapeName = ""
    @Volatile var hasTape = false
    @Volatile var cancelled = false

    fun startReading(filename: String) {
        if (activity == Activity.Reading) return
        activity = Activity.Reading
        show("Läser in $filename...")
    }

    fun startWriting(filename: String) {
        if (activity == Activity.Writing) return
        activity = Activity.Writing
        show("Sparar $filename...")
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
