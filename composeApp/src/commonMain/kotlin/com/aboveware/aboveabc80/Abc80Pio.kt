@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package com.aboveware.aboveabc80

/** Z80-PIO port B of the ABC80 (cassette interface). Port A is the keyboard and is handled natively. */
@OptIn(ExperimentalUnsignedTypes::class)
class Abc80Pio {
    val B = Port()

    class Port {
        private var interruptVector: UByte = 0x00u

        private enum class Mode(val mode: UByte) {
            Output(0x00u),
            Input(0x40u),
            BiDirectional(0x80u),
            BitControl(0xc0u)
        }

        private var mode = Mode.Input
        private var interruptControl: UByte = 0x00u
        private var maskFollows = false
        private var portDirectionMaskFollows = false
        private var portInterruptMask: UByte = 0x00u
        private var portDirectionMask: UByte = 0x00u
        private var port: UByte = 0x00u
        private var interruptFF = false

        private val enableInterrupt: UByte = 0x80u
        private val andLogic: UByte = 0x40u
        private val activeHigh: UByte = 0x20u
        private val maskFollowsBit: UByte = 0x10u

        val isInterruptEnabled: Boolean
            get() = interruptControl.and(enableInterrupt) != 0x00u.toUByte()
        private val isAndLogic: Boolean get() = interruptControl.and(andLogic) != 0x00u.toUByte()
        private val isActiveHigh: Boolean get() = interruptControl.and(activeHigh) != 0x00u.toUByte()

        var data: UByte
            get() = port
            set(value) {
                if (mode == Mode.BitControl) {
                    port = port.and(portDirectionMask).or(value.and(portDirectionMask.inv()))
                    return
                }
                if (mode == Mode.Input) return
                port = value
            }

        private fun externalData(value: UByte) {
            if (mode == Mode.BitControl) {
                port = port.and(portDirectionMask.inv()).or(value.and(portDirectionMask))
            }
            if (mode == Mode.Input) {
                port = value
            }
            check4Interrupt()
        }

        /** Bit 7 is the tape input, inverted: true means a high level on the tape. */
        var cassette: Boolean
            get() = port.and(0x80u) == 0x80.toUByte()
            set(value) = externalData(if (value) port.and(0x7Fu) else port.or(0x80u))

        fun write(value: UByte) {
            if (portDirectionMaskFollows) {
                portDirectionMask = value
                portDirectionMaskFollows = false
                return
            }
            if (maskFollows) {
                portInterruptMask = value
                maskFollows = false
                return
            }
            when {
                value.and(0x0fu) == 0x03u.toUByte() -> {
                    interruptFF = false
                    NativeLib.getObject().requestCassetteInterrupt(-1)
                    interruptControl = if (value.and(enableInterrupt) == 0x00u.toUByte())
                        interruptControl.and(enableInterrupt.inv())
                    else interruptControl.or(enableInterrupt)
                }
                value.and(0x01u) == 0x00u.toUByte() -> interruptVector = value.and(0xfeu)
                value.and(0x0fu) == 0x0fu.toUByte() -> {
                    mode = when (value.and(0xc0u)) {
                        Mode.Output.mode -> Mode.Output
                        Mode.Input.mode -> Mode.Input
                        Mode.BiDirectional.mode -> Mode.BiDirectional
                        else -> Mode.BitControl
                    }
                    portDirectionMaskFollows = mode == Mode.BitControl
                }
                value.and(0x0fu) == 0x07u.toUByte() -> {
                    interruptControl = value.and(0xf0u)
                    maskFollows = value.and(maskFollowsBit) != 0x00u.toUByte()
                }
            }
        }

        private fun check4Interrupt() {
            if (!isInterruptEnabled) return
            interruptFF = if (isActiveHigh) {
                if (isAndLogic) data.or(portInterruptMask) == 0xffu.toUByte()
                else data.and(portInterruptMask.inv()) != 0x00u.toUByte()
            } else {
                if (isAndLogic) data.and(portInterruptMask.inv()) == 0x00u.toUByte()
                else data.or(portInterruptMask) != 0xffu.toUByte()
            }
            if (interruptFF && mode == Mode.BitControl) {
                NativeLib.getObject().requestCassetteInterrupt(interruptVector.toInt())
            }
        }
    }
}
