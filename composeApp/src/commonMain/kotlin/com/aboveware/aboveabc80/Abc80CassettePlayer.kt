package com.aboveware.aboveabc80

/** The ABC80 cassette recorder: PIO port B on I/O ports 0x39-0x3B plus hooks into the monitor ROM. */
@OptIn(ExperimentalUnsignedTypes::class)
class Abc80CassettePlayer {
    private val portData = 0x3A
    private val portControlA = 0x39
    private val portControlB = 0x3B
    private val cassetteWrite = 0x40
    private val cassetteMotor = 0x20

    private val tape = Abc80CassetteTape()
    private var reading = false

    private var motorOn = false
        set(on) {
            if (on != field) {
                if (on) {
                    // The ROM disables PIO interrupts between read blocks; that is not SAVE.
                    reading = pioB.isInterruptEnabled
                    tape.start()
                } else {
                    tape.close()
                    reading = false
                }
            }
            field = on
            if (!on) tape.stop()
        }

    private val pioB get() = NativeLib.getObject().pio.B

    private fun hook(address: Int, action: () -> Unit) {
        NativeLib.getObject().addMemoryReadWatcher(address, object : NativeLib.MemoryReadWatcher {
            override fun onRead(address: Int): Boolean {
                action()
                return false
            }
        })
    }

    fun run() {
        val lib = NativeLib.getObject()
        // Cassette interrupt service routine
        hook(0x0594) { tape.onCassetteInterrupt() }
        // Skip the initial 3-second cassette delay
        hook(0x03da) {
            lib.setRegisterB(1)
            lib.setRegisterHL(1)
        }
        // The requested file has been found
        hook(0x04e2) { tape.found() }
        hook(0x04e5) { tape.found() }
        hook(0x053f) { lib.setZeroFlag(Abc80CassetteStatus.cancelled) }
        // Shortens the bit delay when loading to nothing
        lib.patchMemory(5 * 256 + 159, 0x01)
    }

    fun write(port: Int, data: Int) {
        when (port) {
            portControlB -> pioB.write(data.toUByte())
            portControlA -> {}
            portData -> {
                pioB.data = data.toUByte()
                motorOn = data and cassetteMotor != 0
                val isSet = data and cassetteWrite == 0
                if (reading) {
                    if (pioB.isInterruptEnabled && motorOn && Abc80Cassette.anyCassette()) tape.play()
                } else {
                    if (isSet) pioB.cassette = true
                    if (motorOn && Abc80Cassette.anyCassette()) tape.add(isSet, NativeLib.getObject().getTStates())
                }
            }
        }
    }

    fun read(port: Int): Int =
        if (port == portData) {
            if (reading && pioB.isInterruptEnabled) tape.read().toInt() else pioB.data.toInt()
        } else 0xFF
}
