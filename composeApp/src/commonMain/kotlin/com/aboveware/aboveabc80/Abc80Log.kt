package com.aboveware.aboveabc80

/**
 * Enumeration of logging tags used throughout the ZX Spectrum emulator.
 * Each tag has an [enabled] state to control whether logs for that category are processed.
 *
 * @property enabled Whether logging for this specific tag is currently active.
 */
enum class Abc80LogTag(val enabled: Boolean) {
    ASSEMBLE(false),
    COMMAND_LINE(false),
    DEBUG(false),
    DOWNLOAD(false),
    INTERRUPT(false),
    KEYBOARD(true),
    MONITOR(false),
    PORT(false),
    BUS(false),
    DISKETT(true),
    FLOPPY(false),
    PRINTER(false),
    SOUND(false),
    STORAGE(false),
    TERMINAL(true),
    TRACE(false),
    WTF(true),
    Z80(false)
}

/**
 * Centralized logging utility for the aboveabc80 application.
 * This object provides a unified interface for logging across common code,
 * delegating the actual output to platform-specific implementations.
 */
object Abc80Log {
    /**
     * Logs a "What a Terrible Failure" message. Used for conditions that should never happen.
     *
     * @param message The message to log.
     */
    fun wtf(message: String) {
        log(Abc80LogTag.WTF, message)
    }

    /**
     * General logging function. Processes the log if the provided [tag] is enabled.
     *
     * @param tag The category of the log.
     * @param message The message to log.
     */
    fun log(tag: Abc80LogTag, message: String) {
        if (tag.enabled) {
            platformLog(tag.name, message)
        }
    }

    /** Logs messages related to the assembler or disassembler. */
    fun assemble(message: String) = log(Abc80LogTag.ASSEMBLE, message)

    /** Logs keyboard-related events and input processing. */
    fun keyboard(message: String) = log(Abc80LogTag.KEYBOARD, message)

    /** Logs ABC bus traffic. */
    fun bus(message: String) = log(Abc80LogTag.BUS, message)

    /** Logs floppy controller activity. */
    fun floppy(message: String) = log(Abc80LogTag.FLOPPY, message)

    /** Logs I/O port read and write operations. */
    fun port(message: String) = log(Abc80LogTag.PORT, message)

    /** Logs terminal output and control sequences. */
    fun terminal(message: String) = log(Abc80LogTag.TERMINAL, message)

    /** Logs input received from the command line interface. */
    fun commandLine(inputLine: String) = log(Abc80LogTag.COMMAND_LINE, inputLine)

    /** Logs sound generation and audio processing events. */
    fun sound(inputLine: String) = log(Abc80LogTag.SOUND, inputLine)

    /** Logs detailed execution trace information. */
    fun trace(string: String) = log(Abc80LogTag.TRACE, string)

    /**
     * Logs a trace message and explicitly enables logging in the native library.
     * Use this for deep debugging sessions.
     *
     * @param string The message to log.
     */
    fun debug(string: String) {
        log(Abc80LogTag.TRACE, string)
        NativeLib.getObject().enableLogging(true)
    }

    fun diskett(string: String) = log(Abc80LogTag.DISKETT, string)
}

/**
 * Platform-specific logging implementation.
 * This function must be implemented in each target platform (e.g., androidMain, iosMain).
 *
 * @param tag The tag string associated with the log message.
 * @param message The actual log message content.
 */
expect fun platformLog(tag: String, message: String)
