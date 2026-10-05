package com.aboveware.aboveabc80

import kotlin.time.TimeSource

@ExperimentalUnsignedTypes
@OptIn(ExperimentalUnsignedTypes::class, ExperimentalStdlibApi::class)
class Abc80CassetteTape {

    private var foundProgram = false
    private val program = Abc80Program()

    private class StateMachine {
        enum class State {
            WAIT, READCASSETTEPORT, RESETCASSETTEPORT, FIRSTTHIRD, SECONDTHIRD, READ
        }

        private var state = State.WAIT

        fun interrupt() {
            state = State.READCASSETTEPORT
        }

        fun next(): State {
            state = when (state) {
                State.WAIT -> State.WAIT
                State.READCASSETTEPORT -> State.RESETCASSETTEPORT
                State.RESETCASSETTEPORT -> State.FIRSTTHIRD
                State.FIRSTTHIRD -> State.SECONDTHIRD
                State.SECONDTHIRD -> State.READ
                State.READ -> State.WAIT
            }
            return state
        }
    }

    private var stateMachine = StateMachine()

    fun start() {
        Abc80Log.tape("start")
        foundProgram = false
        program.init(filenameOnStack().eightPointThree())
    }

    fun close() {
        Abc80Log.tape("close")
        if (program.resample())
            Abc80Cassette.save(program)
        hide()
    }

    private fun hide() {
        Abc80CassetteStatus.hide()
    }

    private fun add(sample: Abc80Program.Sample) {
        Abc80CassetteStatus.startWriting(program.filename)
        program.add(sample)
    }

    fun add(value: Boolean, runtime: Long) {
        add(Abc80Program.Sample(value, runtime))
    }

    fun stop() {
        Abc80Log.tape("stop")
    }

    fun play() {
        Abc80Log.tape("play")
        Abc80CassetteStatus.startReading(program.filename.toAndroid())
        if (!program.isNotEmpty() || program.hasNext()) {
            NativeLib.getObject().pio.B.cassette = true
        }
        if (program.isNotEmpty() && !foundProgram && !program.hasNext()) {
            if (program.verify()) {
                NativeLib.getObject().pio.B.cassette = true
                program.init()
            } else {
                NativeLib.getObject().pio.B.cassette = false
            }
        }
        stateMachine.next()
    }

    fun read(): UByte {
        if (stateMachine.next() == StateMachine.State.READ) {
            if (program.hasNext())
                NativeLib.getObject().pio.B.cassette = program.next()
            if (foundProgram && (program.progress() % 256) == 0) {
                val message = "Loading ${program.filename.toAndroid()}..."
                progress(message, program.progress(), program.size())
            }
        }
        return NativeLib.getObject().pio.B.data
    }

    fun onCassetteInterrupt() {
        val requestedName = filenameOnStack()
        var fileInfo = Abc80Cassette.load(requestedName, program)
        while (program.endOfTape && !foundProgram && !Abc80CassetteStatus.cancelled) {
            Abc80CassetteStatus.programNotFound(program.filename.toAndroid())
            // Keep the ROM's pending read intact while the user decides whether to rewind.
            while (Abc80CassetteStatus.endOfTapeAction == Abc80CassetteStatus.EndOfTapeAction.Waiting &&
                !Abc80CassetteStatus.cancelled) {
                Thread.sleep(10)
            }
            if (Abc80CassetteStatus.endOfTapeAction != Abc80CassetteStatus.EndOfTapeAction.Restart)
                break
            Abc80Cassette.rewind()
            program.init(requestedName.eightPointThree())
            fileInfo = Abc80Cassette.load(requestedName, program)
        }
        if (fileInfo.valid) {
            val eightPointThree = fileInfo.name.eightPointThree()
            foundProgram = program.filename.startsWith(".") ||
                    eightPointThree == program.filename
            val message = if (foundProgram) {
                program.filename = eightPointThree
                "Loading $eightPointThree..."
            }
            else
                "Winding (found $eightPointThree)"
            progress(message, !foundProgram, program.size())
            if (!foundProgram) {
                // Pause the emulator thread so no cassette bits or ROM timeouts advance.
                val started = TimeSource.Monotonic.markNow()
                while (started.elapsedNow().inWholeMilliseconds < 1000 &&
                    !Abc80CassetteStatus.cancelled) {
                    Thread.sleep(10)
                }
            }
        }
        if(program.endOfTape)
            progress("Winding", true, 0)
        stateMachine.interrupt()
    }

    private fun progress(message: String, progress: Int, max: Int) {
        Abc80CassetteStatus.show(message, false, progress, max)
    }

    private fun progress(message: String, indeterminate: Boolean, max: Int) {
        Abc80CassetteStatus.show(message, indeterminate, 0, max)
    }

    /**
     * We've found the file we're searching for. We should not continue to
     * respond to interrupts
     */
    fun found() {
        foundProgram = true
    }

    /** An ABC80 file name is 11 consecutive letters, digits, spaces or the national characters [ \ ]. */
    private fun filenameOnStack(): String {
        val sp = NativeLib.getObject().stackPointer()
        val memory = NativeLib.getObject().getMemory()
        val end = minOf(sp + 1024, 0xFFFF)
        val stack = StringBuilder()
        for (address in sp..end) stack.append((memory[address].toInt() and 0xFF).toChar())
        for (start in 0..stack.length - 11) {
            if ((start until start + 11).all { abc80Character(stack[it]) })
                return stack.substring(start, start + 11)
        }
        return ""
    }

    private fun abc80Character(c: Char) = (c in 'A'..'Z') || (c in '0'..'9') || c == ' ' || c == '[' || c == '\\' || c == ']'
}