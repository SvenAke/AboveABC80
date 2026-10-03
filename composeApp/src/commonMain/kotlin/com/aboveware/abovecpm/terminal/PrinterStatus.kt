package com.aboveware.abovecpm.terminal

/**
 * Represents the current status of the printer.
 */
enum class PrinterStatus(val text: String, val description: String) {
    READY("Ready", "The printer is ready."),
    NOT_READY("Not Ready", "The printer is not ready."),
    NONE("None", "No printer is connected."),
    AUTO("Auto", "The terminal is in auto print mode."),
    CONTROLLER("Controller", "The terminal is in printer controller mode.")
}
