package com.aboveware.abovecpm.terminal

import com.aboveware.abovecpm.ZXLog
import com.aboveware.abovecpm.printer.VirtualPrinter

internal fun VT320.handleControllerMode(c: Char) {
    // Watch for CSI 4 i (ESC [ 4 i)
    controllerModeSeq += c

    // Forward char to printer
    VirtualPrinter.instance.printChar(c)

    if (controllerModeSeq.endsWith("\u001B[4i") || controllerModeSeq.endsWith("\u009B4i")) {
        printerStatus = PrinterStatus.READY
        controllerModeSeq = ""
        ZXLog.terminal("VT320: Exited Printer Controller Mode")
        sendPrinterTerminator()
    } else if (controllerModeSeq.length > 10) {
        controllerModeSeq = controllerModeSeq.takeLast(5)
    }
}

internal fun VT320.printCurrentLine() {
    val line = screen[cursorY]
    val printer = VirtualPrinter.instance

    // Find last non-empty cell to trim trailing spaces
    val lastIdx = line.findLast { it.char != ' ' }?.let { line.indexOf(it) } ?: -1

    for (i in 0..lastIdx) {
        printer.printChar(line[i].char)
    }
    printer.printChar('\r')
    printer.printChar('\n')
}

internal fun VT320.printScreenInternal() {
    val printer = VirtualPrinter.instance
    val sizeMode = VT320Settings.printerScreenSize

    val startRow = if (sizeMode) topMargin else 0
    val endRow = if (sizeMode) bottomMargin else rows - 1

    for (y in startRow..endRow) {
        val row = screen[y]
        val lastIdx = row.findLast { it.char != ' ' }?.let { row.indexOf(it) } ?: -1

        for (x in 0..lastIdx) {
            printer.printChar(row[x].char)
        }
        printer.printChar('\r')
        printer.printChar('\n')
    }
    sendPrinterTerminator()
}

internal fun VT320.sendPrinterTerminator() {
    val printer = VirtualPrinter.instance
    if (VT320Settings.printTerminator) printer.printChar('\u000C') // FF
}
