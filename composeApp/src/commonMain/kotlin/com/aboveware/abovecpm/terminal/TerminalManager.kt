/**
 * TerminalManager: Manages the active terminal type, colors, and visual effects.
 * Provides a state-aware view of terminal settings.
 */
package com.aboveware.abovecpm.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

enum class TerminalType {
    ADM3A, VT320
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
    private var _nationalReplacement by mutableStateOf(VT320Settings.nationalReplacement)
    var nationalReplacement: Boolean
        get() = _nationalReplacement
        set(value) {
            _nationalReplacement = value
            VT320Settings.nationalReplacement = value
            VT320Settings.save()
        }

    private var _userPreferredCharacterSetIsDECSupplementalGraphic by mutableStateOf(VT320Settings.userPreferredCharacterSet == 0)
    var userPreferredCharacterSetIsDECSupplementalGraphic: Boolean
        get() = _userPreferredCharacterSetIsDECSupplementalGraphic
        set(value) {
            _userPreferredCharacterSetIsDECSupplementalGraphic = value
            VT320Settings.userPreferredCharacterSet = if (value) 0 else 1
            VT320Settings.save()
        }

    private var _operatingMode by mutableStateOf(loadOperatingMode())
    var operatingMode: OperatingMode
        get() = _operatingMode
        set(value) {
            _operatingMode = value
            VT320Settings.operatingMode = value.name
            VT320Settings.save()
        }

    private var _currentTerminalType by mutableStateOf(loadTerminalType())
    var currentTerminalType: TerminalType
        get() = _currentTerminalType
        set(value) {
            _currentTerminalType = value
            VT320Settings.terminalType = value.name
            if (value == TerminalType.ADM3A) operatingMode = OperatingMode.ANSI

            // If the color was never explicitly set, or is the default for the previous terminal,
            // we update it to the default for the new terminal type.
            val savedColor = VT320Settings.terminalColor
            if (savedColor == "" || savedColor == "GREEN" || savedColor == "AMBER" || savedColor == "BLUE") {
                terminalColor =
                    if (value == TerminalType.VT320) TerminalColor.AMBER else TerminalColor.BLUE
            }
            VT320Settings.save()

            // Trigger reset/boot sequence when terminal type changes
            activeTerminal.reset()
        }

    private var _terminalColor by mutableStateOf(loadTerminalColor())
    var terminalColor: TerminalColor
        get() = _terminalColor
        set(value) {
            _terminalColor = value
            VT320Settings.terminalColor = value.name
            VT320Settings.save()
        }

    private var _scanlineIntensity by mutableStateOf(loadScanlineIntensity())
    var scanlineIntensity: Float
        get() = _scanlineIntensity
        set(value) {
            _scanlineIntensity = value
            VT320Settings.scanlineIntensity = value.toString()
            VT320Settings.save()
        }

    private var _autoUppercase by mutableStateOf(VT320Settings.autoUppercase)
    var autoUppercase: Boolean
        get() = _autoUppercase
        set(value) {
            _autoUppercase = value
            VT320Settings.autoUppercase = value
            VT320Settings.save()
        }

    private fun loadOperatingMode(): OperatingMode {
        val saved = VT320Settings.operatingMode
        return try {
            OperatingMode.valueOf(saved)
        } catch (_: Exception) {
            OperatingMode.ANSI
        }
    }

    private fun loadTerminalType(): TerminalType {
        val saved = VT320Settings.terminalType
        return try {
            TerminalType.valueOf(saved)
        } catch (_: Exception) {
            TerminalType.VT320
        }
    }

    private fun loadTerminalColor(): TerminalColor {
        val saved = VT320Settings.terminalColor
        if (saved == "") {
            return if (loadTerminalType() == TerminalType.VT320) TerminalColor.AMBER else TerminalColor.BLUE
        }
        return try {
            TerminalColor.valueOf(saved)
        } catch (_: Exception) {
            if (loadTerminalType() == TerminalType.VT320) TerminalColor.AMBER else TerminalColor.BLUE
        }
    }

    private fun loadScanlineIntensity(): Float {
        val saved = VT320Settings.scanlineIntensity
        return saved.toFloatOrNull() ?: 0.15f
    }

    private val adm3aInstance by lazy { ADM3A() }
    private val vt320Instance by lazy { VT320() }

    val commandHistory = CommandHistory()

    val activeTerminal: Terminal
        get() = when (currentTerminalType) {
            TerminalType.ADM3A -> adm3aInstance
            TerminalType.VT320 -> vt320Instance
        }

    fun resetToDefaults() {
        VT320Settings.clearCache()
        currentTerminalType = TerminalType.VT320
        terminalColor = TerminalColor.AMBER
        operatingMode = OperatingMode.ANSI
        nationalReplacement = false
        userPreferredCharacterSetIsDECSupplementalGraphic = true
        scanlineIntensity = 0.15f
        autoUppercase = false
    }
}
