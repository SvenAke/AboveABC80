package com.aboveware.aboveabc80

actual fun loadNativeLibrary() {
    try {
        System.loadLibrary("aboveabc80")
        System.err.println("NATIVE-LOAD: Successfully loaded 'aboveabc80' via System.loadLibrary")
    } catch (e: Throwable) {
        System.err.println("NATIVE-LOAD: Failed to load 'aboveabc80' via System.loadLibrary: ${e.message}")
        throw e
    }
}
