package com.aboveware.aboveabc80.core

class Audio {

    companion object {
        val instance = Audio()
    }

    private val earLock = Any()

    fun addEarBit(keyboardScan: UByte): UByte {
        // Bit 6 is EAR input. High (1) by default on a 48K Spectrum.
        // It is pulled Low (0) if either the Tape signal or the CPU EAR output is Low.
        // We ignore MIC output (bit 3) here as it often blocks the ROM loader in emulation.
        val earBit: UByte = if (_hardwareEar && _cpuEar) 0x40u else 0x00u

        // Bits 5 and 7 are always 1 on a 48K Spectrum.
        return (keyboardScan and 0x1Fu) or earBit or 0xA0u
    }

    fun interface OnMicChangedListener {
        fun onMicChanged(mic: Boolean)
    }

    private var onMicChangedListener: OnMicChangedListener? = null

    fun setOnMicChangedListener(onMicChanged: OnMicChangedListener?) {
        onMicChangedListener = onMicChanged
    }

    fun interface OnMicReadListener {
        fun onMicRead(mic: Boolean): Boolean
    }

    private var onMicReadListener: OnMicReadListener = OnMicReadListener { it }

    fun interface OnEarReadListener {
        fun onEarRead(ear: Boolean): Boolean
    }

    /**
     * A dummy implementation of [OnEarReadListener] used for testing or as a default.
     */
    class DummyOnEarReadListener : OnEarReadListener {
        override fun onEarRead(ear: Boolean) = ear
    }

    private var onEarReadListener: OnEarReadListener = DummyOnEarReadListener()

    @Synchronized
    fun setOnEarReadListener(onEarRead: OnEarReadListener) {
        onEarReadListener = onEarRead
    }

    @Synchronized
    fun resetOnEarReadListener() {
        onEarReadListener = DummyOnEarReadListener()
    }

    fun interface OnEarChangedListener {
        fun onEarChanged(level: Float, elapsed: ULong)
    }

    private var onEarChangedListener: OnEarChangedListener? = null

    fun setOnEarChangedListener(onEarChanged: OnEarChangedListener?) {
        onEarChangedListener = onEarChanged
    }

    @Synchronized
    private fun updateSpeaker() {
        // Use Fuse-like levels for the beeper (based on Spectaculator's FAQ)
        // Bit 4 (cpuEar) and Bit 3 (mic) combinations:
        // 0,0 -> 0.0
        // 0,1 -> 0.23  (mic only)
        // 1,0 -> 0.35  (cpuEar only)
        // 1,1 -> 0.50  (both)

        // Note: The current ULA.kt implementation inverts the bits:
        // cpuEar = (data & 0x10) == 0  => true if bit 4 is 0
        // mic = (data & 0x08) == 0     => true if bit 3 is 0
        // So we need to take that into account or just use the boolean values as they are
        // if we assume "true" means "active/high".

        // Real Spectrum levels (normalized to 0.0 .. 1.0):
        val b4 = if (_cpuEar) 1 else 0
        val b3 = if (hardwareMic) 1 else 0

        val level = when ((b4 shl 1) or b3) {
            0 -> 0.0f
            1 -> 0.23f
            2 -> 0.35f
            3 -> 0.50f
            else -> 0.0f
        }

        val tStates = com.aboveware.aboveabc80.NativeLib.getObject().getTStates().toULong()
        onEarChangedListener?.onEarChanged(level, tStates)
    }

    var hardwareMic = true
    var mic: Boolean
        @Synchronized
        get() {
            return hardwareMic
        }
        @Synchronized
        set(value) {
            if (hardwareMic != value) {
                hardwareMic = value
                onMicChangedListener?.onMicChanged(value)
                updateSpeaker()
            }
        }

    private var _hardwareEar = true
    private var _cpuEar = true

    /**
     * Thread-safe access to the EAR state from the tape.
     */
    var ear: Boolean
        @Synchronized
        get() {
            return _hardwareEar
        }
        @Synchronized
        set(value) {
            if (_hardwareEar != value) {
                _hardwareEar = value
                updateSpeaker()
            }
        }

    /**
     * Thread-safe access to the EAR state from the CPU.
     */
    var cpuEar: Boolean
        @Synchronized
        get() {
            return _cpuEar
        }
        @Synchronized
        set(value) {
            if (_cpuEar != value) {
                _cpuEar = value
                updateSpeaker()
            }
        }

    override fun toString() = "Audio(mic=$hardwareMic, ear=$_hardwareEar, cpuEar=$_cpuEar)"
}
