package com.aboveware.aboveabc80.terminal

import com.aboveware.aboveabc80.getPersistedString
import com.aboveware.aboveabc80.setPersistedMap

/**
 * VT320Settings: Handles terminal settings with an in-memory cache.
 * Settings take effect immediately (runtime) but are only written to persistent 
 * storage when save() is called.
 */
object VT320Settings {
    private const val PREFIX = "vt320_"
    private val memoryCache = mutableMapOf<String, String>()
    private val lock = Any()

    internal fun clearCache() = synchronized(lock) {
        memoryCache.clear()
    }

    internal fun getString(key: String, defaultValue: String): String = synchronized(lock) {
        val cached = memoryCache[key]
        if (cached != null) return cached
        val persisted = getPersistedString(PREFIX + key, defaultValue)
        memoryCache[key] = persisted
        return persisted
    }

    internal fun setString(key: String, value: String) = synchronized(lock) {
        memoryCache[key] = value
    }

    internal fun getInt(key: String, defaultValue: Int): Int {
        val strValue = getString(key, defaultValue.toString())
        return strValue.toIntOrNull() ?: defaultValue
    }

    internal fun setInt(key: String, value: Int) = synchronized(lock) {
        memoryCache[key] = value.toString()
    }

    internal fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val strValue = getString(key, defaultValue.toString())
        return strValue.toBoolean()
    }

    internal fun setBoolean(key: String, value: Boolean) = synchronized(lock) {
        memoryCache[key] = value.toString()
    }

    // --- Specific Settings Properties ---

    var nationalReplacement: Boolean
        get() = getBoolean("national_replacement", false)
        set(value) = setBoolean("national_replacement", value)

    var userPreferredCharacterSet: Int
        get() = getInt("userPreferredCharacterSet", 0)
        set(value) = setInt("userPreferredCharacterSet", value)

    var operatingMode: String
        get() = getString("operating_mode", "ANSI")
        set(value) = setString("operating_mode", value)

    var terminalType: String
        get() = getString("terminal_type", "VT320")
        set(value) = setString("terminal_type", value)

    var terminalColor: String
        get() = getString("terminal_color", "")
        set(value) = setString("terminal_color", value)

    var scanlineIntensity: String
        get() = getString("scanline_intensity", "0.15")
        set(value) = setString("scanline_intensity", value)

    var autoUppercase: Boolean
        get() = getBoolean("auto_uppercase", false)
        set(value) = setBoolean("auto_uppercase", value)

    var keyboardLanguage: Int
        get() = getInt("keyboardLanguage", 0)
        set(value) = setInt("keyboardLanguage", value)

    var keyClick: Int
        get() = getInt("keyClick", 1)
        set(value) = setInt("keyClick", value)

    var columnMode: Int
        get() = getInt("columnMode", 0)
        set(value) = setInt("columnMode", value)

    var controlsMode: Int
        get() = getInt("controlsMode", 0)
        set(value) = setInt("controlsMode", value)

    var wrapMode: Boolean
        get() = getInt("wrapMode", 0) == 1
        set(value) = setInt("wrapMode", if (value) 1 else 0)

    var autoRepeat: Boolean
        get() = getInt("autoRepeat", 1) == 1
        set(value) = setInt("autoRepeat", if (value) 1 else 0)

    var scrollMode: Boolean
        get() = getInt("scrollMode", 0) == 1
        set(value) = setInt("scrollMode", if (value) 1 else 0)

    var statusDisplay: Int
        get() = getInt("statusDisplay", 1)
        set(value) = setInt("statusDisplay", value)

    var screenDisplayType: Int
        get() = getInt("screenDisplayType", 0)
        set(value) = setInt("screenDisplayType", value)

    var textCursor: Int
        get() = getInt("textCursor", 0)
        set(value) = setInt("textCursor", value)

    var cursorStyle: Int
        get() = getInt("cursorStyle", 0)
        set(value) = setInt("cursorStyle", value)

    var userFeaturesSetting: Int
        get() = getInt("userFeaturesSetting", 0)
        set(value) = setInt("userFeaturesSetting", value)

    var cursorKeysMode: Int
        get() = getInt("cursorKeysMode", 0)
        set(value) = setInt("cursorKeysMode", value)

    var keypadMode: Int
        get() = getInt("keypadMode", 0)
        set(value) = setInt("keypadMode", value)

    var newLineMode: Int
        get() = getInt("newLineMode", 0)
        set(value) = setInt("newLineMode", value)

    var nationalOnly: Int
        get() = getInt("National Only", 0)
        set(value) = setInt("National Only", value)

    var characterSetMode: Int
        get() = getInt("characterSetMode", 1)
        set(value) = setInt("characterSetMode", value)

    var userDefinedKeys: Int
        get() = getInt("userDefinedKeys", 0)
        set(value) = setInt("userDefinedKeys", value)

    var terminalModeId: Int
        get() = getInt("terminalModeId", 0)
        set(value) = setInt("terminalModeId", value)

    var printerToHost: Int
        get() = getInt("printerToHost", 1)
        set(value) = setInt("printerToHost", value)

    var onLineLocal: Boolean
        get() = getInt("OnLineLocal", 0) == 1
        set(value) = setInt("OnLineLocal", if (value) 1 else 0)

    var localEcho: Boolean
        get() = getInt("localEcho", 0) == 1
        set(value) = setInt("localEcho", if (value) 1 else 0)

    var answerBack: String
        get() = getString("answerBack", "")
        set(value) = setString("answerBack", value)

    var printerScreenSize: Boolean
        get() = getInt("printerScreenSize", 0) == 1
        set(value) = setInt("printerScreenSize", if (value) 1 else 0)

    var printTerminator: Boolean
        get() = getInt("printTerminator", 0) == 1
        set(value) = setInt("printTerminator", if (value) 1 else 0)

    var marginBell: Boolean
        get() = getInt("marginBell", 1) == 1
        set(value) = setInt("marginBell", if (value) 1 else 0)

    var warningBell: Boolean
        get() = getInt("warningBell", 1) == 1
        set(value) = setInt("warningBell", if (value) 1 else 0)

    var terminalKeyMap: Int
        get() = getInt("terminalKeyMap", 0)
        set(value) = setInt("terminalKeyMap", value)

    var composeKey: Int
        get() = getInt("composeKey", 1)
        set(value) = setInt("composeKey", value)

    var backArrow: Int
        get() = getInt("backArrow", 0)
        set(value) = setInt("backArrow", value)

    var lockKey: Int
        get() = getInt("lockKey", 0)
        set(value) = setInt("lockKey", value)

    var commaPoint: Int
        get() = getInt("commaPoint", 0)
        set(value) = setInt("commaPoint", value)

    var angleBracket: Int
        get() = getInt("angleBracket", 0)
        set(value) = setInt("angleBracket", value)

    var tildeKey: Int
        get() = getInt("tildeKey", 0)
        set(value) = setInt("tildeKey", value)

    var breakKey: Int
        get() = getInt("breakKey", 1)
        set(value) = setInt("breakKey", value)

    var setUpLanguage: Int
        get() = getInt("SetUpLanguage", 0)
        set(value) = setInt("SetUpLanguage", value)

    var printerMode: Int
        get() = getInt("printerMode", 0)
        set(value) = setInt("printerMode", value)

    var bitsFormat: Int
        get() = getInt("bitsFormat", 0)
        set(value) = setInt("bitsFormat", value)

    var autoAnswerBack: Int
        get() = getInt("autoAnswerBack", 0)
        set(value) = setInt("autoAnswerBack", value)

    var disconnectDelay: Int
        get() = getInt("disconnectDelay", 0)
        set(value) = setInt("disconnectDelay", value)

    var hostPort: Int
        get() = getInt("port", 0)
        set(value) = setInt("port", value)

    var printedDataType: Int
        get() = getInt("printedDataType", 0)
        set(value) = setInt("printedDataType", value)

    var printerBitsFormat: Int
        get() = getInt("printerBitsFormat", 1)
        set(value) = setInt("printerBitsFormat", value)

    var printerSpeed: Int
        get() = getInt("printerSpeed", 7)
        set(value) = setInt("printerSpeed", value)

    var printerStopBits: Int
        get() = getInt("printerStopBits", 0)
        set(value) = setInt("printerStopBits", value)

    var receiveSpeed: Int
        get() = getInt("receive", 8)
        set(value) = setInt("receive", value)

    var transmitSpeed: Int
        get() = getInt("transmit", 8)
        set(value) = setInt("transmit", value)

    var stopBits: Int
        get() = getInt("stopBits", 0)
        set(value) = setInt("stopBits", value)

    var terminalTransmitSpeed: Int
        get() = getInt("terminalTransmitSpeed", 0)
        set(value) = setInt("terminalTransmitSpeed", value)

    var printerXOff: Int
        get() = getInt("printerXOff", 0)
        set(value) = setInt("printerXOff", value)

    var xOff: Int
        get() = getInt("xOff", 0) // Default to "XOFF at 64" (index 0)
        set(value) = setInt("xOff", value)

    /**
     * Persists all currently cached settings to long-term storage.
     */
    fun save() {
        val map = synchronized(lock) {
            memoryCache.mapKeys { PREFIX + it.key }
        }
        setPersistedMap(map)
    }

    /**
     * Reverts all in-memory changes by clearing the cache.
     * Next get() will fetch from persistent storage.
     */
    fun recall() = synchronized(lock) {
        memoryCache.clear()
    }

    fun restoreDefaults() = synchronized(lock) {
        memoryCache.clear()
    }
}
