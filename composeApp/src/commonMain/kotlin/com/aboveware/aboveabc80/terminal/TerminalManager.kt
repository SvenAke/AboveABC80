/**
 * TerminalManager: Manages the active terminal type, colors, and visual effects.
 * Provides a state-aware view of terminal settings.
 */
package com.aboveware.aboveabc80.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.aboveware.aboveabc80.getPersistedString
import com.aboveware.aboveabc80.setPersistedString

enum class TerminalType {
    ABC80
}

enum class ComputerModel(
    val displayName: String,
    private val fixedWideScreen: Boolean?,
    val nominalCpuFrequencyHz: Int,
    val defaultCpuSpeedSetting: Int,
    val screenColor: Color,
    val screenBackgroundColor: Color,
    val caseColor: Color
) {
    ABC80(
        "ABC80",
        null,
        3_000_000,
        3,
        Color(0xFF57C6F8),
        Color.Black,
        Color(0xFFDFAD75)
    ),
    ABC800M(
        "ABC800M",
        true,
        3_580_000,
        4,
        Color(0xFFFFB000),
        Color(0xFF3A2B1A),
        Color(0xFFB17C45)
    ),
    ABC800C(
        "ABC800C",
        false,
        3_580_000,
        4,
        Color(0xFF57C6F8),
        Color.Black,
        Color(0xFFDFAD75)
    );

    fun isWideScreen(abc80WideScreen: Boolean): Boolean =
        fixedWideScreen ?: abc80WideScreen

    fun cpuFrequencyHz(speedSetting: Int): Int =
        if (speedSetting == defaultCpuSpeedSetting) nominalCpuFrequencyHz
        else speedSetting.coerceAtMost(8) * 1_000_000

    fun cpuSpeedLabel(speedSetting: Int): String {
        if (speedSetting == 9) return "MAX"
        val hundredthsOfMHz = cpuFrequencyHz(speedSetting) / 10_000
        val wholeMHz = hundredthsOfMHz / 100
        val fractionalMHz = hundredthsOfMHz % 100
        return if (fractionalMHz == 0) "$wholeMHz.0 MHz"
        else "$wholeMHz.${fractionalMHz.toString().padStart(2, '0')} MHz"
    }

    companion object {
        fun fromStoredValue(value: String): ComputerModel =
            entries.firstOrNull { it.name == value } ?: ABC80
    }
}

enum class TerminalColor(val color: Color) {
    GREEN(Color(0xFF00FF00)),
    AMBER(Color(0xFFFFB000)),
    WHITE(Color(0xFFFFFFFF)),
    BLUE(Color(0xFFAFEEEE))
}

object TerminalManager {
    lateinit var cpuSpeed: String
    var currentTerminalType: TerminalType by mutableStateOf(TerminalType.ABC80)
    private var _computerModel by mutableStateOf(
        ComputerModel.fromStoredValue(getPersistedString("computer_model", ComputerModel.ABC80.name))
    )
    var computerModel: ComputerModel
        get() = _computerModel
        set(value) {
            if (_computerModel != value) {
                _computerModel = value
                setPersistedString("computer_model", value.name)
            }
        }
    var terminalColor: TerminalColor by mutableStateOf(TerminalColor.BLUE)
    var scanlineIntensity: Float by mutableStateOf(0.15f)

    private val abc80Instance by lazy { ABC80() }

    val activeTerminal: Terminal
        get() = abc80Instance

    fun resetToDefaults() {
        currentTerminalType = TerminalType.ABC80
        computerModel = ComputerModel.ABC80
        terminalColor = TerminalColor.BLUE
        scanlineIntensity = 0.15f
    }
}
