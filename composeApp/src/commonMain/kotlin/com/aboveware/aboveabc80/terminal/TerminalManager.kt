/**
 * TerminalManager: Manages the active terminal type, colors, and visual effects.
 * Provides a state-aware view of terminal settings.
 */
package com.aboveware.aboveabc80.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class TerminalType {
    ABC80
}

enum class TerminalColor(val color: Color) {
    GREEN(Color(0xFF00FF00)),
    AMBER(Color(0xFFFFB000)),
    WHITE(Color(0xFFFFFFFF)),
    BLUE(Color(0xFFAFEEEE))
}

object TerminalManager {
    enum class OperatingMode { ANSI, VT52 }

    lateinit var cpuSpeed: String
    var nationalReplacement: Boolean by mutableStateOf(false)
    var userPreferredCharacterSetIsDECSupplementalGraphic: Boolean by mutableStateOf(true)
    var operatingMode: OperatingMode by mutableStateOf(OperatingMode.ANSI)
    var currentTerminalType: TerminalType by mutableStateOf(TerminalType.ABC80)
    var terminalColor: TerminalColor by mutableStateOf(TerminalColor.BLUE)
    var scanlineIntensity: Float by mutableStateOf(0.15f)
    var autoUppercase: Boolean by mutableStateOf(false)

    private val abc80Instance by lazy { ABC80() }

    val commandHistory = CommandHistory()

    val activeTerminal: Terminal
        get() = abc80Instance

    fun resetToDefaults() {
        currentTerminalType = TerminalType.ABC80
        terminalColor = TerminalColor.BLUE
        operatingMode = OperatingMode.ANSI
        nationalReplacement = false
        userPreferredCharacterSetIsDECSupplementalGraphic = true
        scanlineIntensity = 0.15f
        autoUppercase = false
    }
}
