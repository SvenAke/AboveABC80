package com.aboveware.abc80

actual fun loadNativeLibrary() {
    try {
        System.loadLibrary("abc80")
        System.err.println("NATIVE-LOAD: Successfully loaded 'abc80' via System.loadLibrary")
    } catch (e: Throwable) {
        System.err.println("NATIVE-LOAD: Failed to load 'abc80' via System.loadLibrary: ${e.message}")
        throw e
    }
}
