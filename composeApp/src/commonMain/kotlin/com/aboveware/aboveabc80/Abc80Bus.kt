package com.aboveware.aboveabc80

/** ABC bus: ports 0-7 are routed to the card selected by an OUT to port 1. */
@OptIn(ExperimentalUnsignedTypes::class)
class Abc80Bus {
    private val printer = Abc80Printer()
    internal val floppy = Abc80Floppy()

    interface BusInterface {
        fun onWrite(port: UByte, data: UByte, status: UByte)
        fun onRead(port: UByte): UByte
        fun progress(show: Boolean = true, text: String = "") {}
    }

    var currentDevice: BusInterface? = null
        private set

    fun run() {
        floppy.run()
        printer.run()
    }

    fun write(port: Int, data: Int) {
        if (port == 1) {
            currentDevice = when (data) {
                0x2d -> floppy
                0x3C -> printer
                else -> null
            }
        }
        currentDevice?.onWrite(port.toUByte(), data.toUByte(), 0u)
    }

    fun read(port: Int): Int {
        if (port == 7) {
            floppy.reset()
            printer.reset()
            return 0xFF
        }
        return (currentDevice?.onRead(port.toUByte()) ?: 0xffu).toInt()
    }
}
