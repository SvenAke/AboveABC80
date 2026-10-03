/**
 * PrinterManager: Manages the active printer type and shared printer settings.
 */
package com.aboveware.abovecpm.printer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aboveware.abovecpm.getPersistedString
import com.aboveware.abovecpm.setPersistedString

enum class PrinterType(val displayName: String) {
    EPSON_MX80("Epson MX-80"),
    EPSON_FX80("Epson FX-80"),
    DIABLO_630("Diablo 630")
}

enum class InternationalCharset {
    USA, FRANCE, GERMANY, UK, DENMARK, SWEDEN, ITALY, SPAIN
}

object PrinterManager {
    private var _currentPrinterType by mutableStateOf(loadInitialPrinterType())

    var currentPrinterType: PrinterType
        get() = _currentPrinterType
        set(value) {
            if (_currentPrinterType != value) {
                _currentPrinterType = value
                setPersistedString("printer_type", value.name)
            }
        }

    val mx80Settings = Mx80Settings()
    val diablo630Settings = Diablo630Settings()

    fun resetToDefaults() {
        currentPrinterType = PrinterType.EPSON_MX80
        mx80Settings.resetToDefaults()
        diablo630Settings.resetToDefaults()
    }

    private fun loadInitialPrinterType(): PrinterType {
        val savedName = getPersistedString("printer_type", PrinterType.EPSON_MX80.name)
        return try {
            PrinterType.valueOf(savedName)
        } catch (_: Exception) {
            PrinterType.EPSON_MX80
        }
    }
}

class Mx80Settings {
    val sw1 = List(8) { i -> DipSwitch(1, i + 1, getSw1Description(i + 1)) }
    val sw2 = List(4) { i -> DipSwitch(2, i + 1, getSw2Description(i + 1)) }

    private fun getSw1Description(n: Int) = when (n) {
        1, 2, 3 -> "Char Set $n"
        4 -> "Line Spacing (ON:1/8\", OFF:1/6\")"
        5 -> "Slashed Zero"
        6 -> "Internal CG"
        7 -> "Page Length (ON:12\", OFF:11\")"
        8 -> "Default Italic"
        else -> ""
    }

    private fun getSw2Description(n: Int) = when (n) {
        1 -> "Auto Line Feed (ON: LF, OFF: CR)"
        2 -> "Bell Enabled"
        3 -> "Paper Out Sensor"
        4 -> "8-bit Data"
        else -> ""
    }

    // Helper to get raw state for drivers
    fun isSw1On(n: Int) = sw1[n - 1].isOn
    fun isSw2On(n: Int) = sw2[n - 1].isOn

    fun getInternationalCharset(): InternationalCharset {
        val bits = (if (isSw1On(1)) 1 else 0) or
                (if (isSw1On(2)) 2 else 0) or
                (if (isSw1On(3)) 4 else 0)
        return when (bits) {
            0 -> InternationalCharset.USA
            1 -> InternationalCharset.FRANCE
            2 -> InternationalCharset.GERMANY
            3 -> InternationalCharset.UK
            4 -> InternationalCharset.DENMARK
            5 -> InternationalCharset.SWEDEN
            6 -> InternationalCharset.ITALY
            7 -> InternationalCharset.SPAIN
            else -> InternationalCharset.USA
        }
    }

    fun resetToDefaults() {
        sw1.forEach { it.resetToDefault() }
        sw2.forEach { it.resetToDefault() }
    }

    fun isSwedish() = getInternationalCharset() == InternationalCharset.SWEDEN
    fun isUSA() = getInternationalCharset() == InternationalCharset.USA
    fun isFrance() = getInternationalCharset() == InternationalCharset.FRANCE
    fun isGermany() = getInternationalCharset() == InternationalCharset.GERMANY
    fun isUK() = getInternationalCharset() == InternationalCharset.UK
    fun isDenmark() = getInternationalCharset() == InternationalCharset.DENMARK
    fun isItaly() = getInternationalCharset() == InternationalCharset.ITALY
    fun isSpain() = getInternationalCharset() == InternationalCharset.SPAIN

    fun getCurrentExplanation(bank: Int, num: Int): String {
        return when (bank) {
            1 -> when (num) {
                1, 2, 3 -> "Character Set: ${getInternationalCharset().name}"
                4 -> "Line Spacing: ${if (isSw1On(4)) "1/8 inch" else "1/6 inch"}"
                5 -> "Zero Character: ${if (isSw1On(5)) "Slashed (Ø)" else "Normal (0)"}"
                6 -> "Character Generator: ${if (isSw1On(6)) "Internal" else "External/ROM"}"
                7 -> "Page Length: ${if (isSw1On(7)) "12 inches" else "11 inches"}"
                8 -> "Italic Mode: ${if (isSw1On(8)) "Enabled" else "Disabled"}"
                else -> ""
            }

            2 -> when (num) {
                1 -> "Auto Line Feed: ${if (isSw2On(1)) "Enabled (CR+LF)" else "Disabled (CR only)"}"
                2 -> "Buzzer (Bell): ${if (isSw2On(2)) "Enabled" else "Disabled"}"
                3 -> "Paper Out Sensor: ${if (isSw2On(3)) "Active" else "Inactive"}"
                4 -> "Data Length: ${if (isSw2On(4)) "8-bit" else "7-bit"}"
                else -> ""
            }

            else -> ""
        }
    }
}

class Diablo630Settings {
    val sw1 = List(8) { i ->
        DipSwitch(
            3,
            i + 1,
            getSwDescription(i + 1)
        )
    } // Use bank 3 to differentiate persistence

    private fun getSwDescription(n: Int) = when (n) {
        1 -> "Auto Line Feed"
        2 -> "Paper Out Sensor"
        3 -> "Bidirectional Print"
        4 -> "Form Length (ON:12\", OFF:11\")"
        5, 6 -> "Baud Rate"
        7, 8 -> "Parity"
        else -> ""
    }

    fun isSwOn(n: Int) = sw1[n - 1].isOn

    fun getCurrentExplanation(num: Int): String {
        return when (num) {
            1 -> "Auto Line Feed: ${if (isSwOn(1)) "Enabled (CR+LF)" else "Disabled (CR only)"}"
            2 -> "Paper Out Sensor: ${if (isSwOn(2)) "Active" else "Inactive"}"
            3 -> "Bidirectional Print: ${if (isSwOn(3)) "Enabled" else "Disabled"}"
            4 -> "Form Length: ${if (isSwOn(4)) "12 inches" else "11 inches"}"
            else -> "Switch $num changed"
        }
    }

    fun resetToDefaults() {
        sw1.forEach { it.resetToDefault() }
    }
}

class DipSwitch(val bank: Int, val num: Int, val description: String) {
    private val persistenceKey = "mx80_sw${bank}_$num"
    private val defaultOn = (num == 1 && bank == 2)

    private var _isOn by mutableStateOf(
        getPersistedString(persistenceKey, if (defaultOn) "true" else "false") == "true"
    )

    var isOn: Boolean
        get() = _isOn
        set(value) {
            if (_isOn != value) {
                _isOn = value
                setPersistedString(persistenceKey, value.toString())
                // Re-sync the active driver immediately
                VirtualPrinter.instance.syncDriver()
            }
        }

    fun resetToDefault() {
        isOn = defaultOn
    }
}
