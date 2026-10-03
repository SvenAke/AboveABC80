package com.aboveware.aboveabc80

import android.util.Log

actual fun platformLog(tag: String, message: String) {
    try {
        if (tag == "WTF") {
            Log.e("Abc80Log", "[$tag] $message")
        } else {
            Log.d("Abc80Log", "[$tag] $message")
        }
    } catch (e: Exception) {
        // Fallback for tests where android.util.Log is not mocked
        if (tag == "WTF") {
            System.err.println("[$tag] $message")
        } else {
            println("[$tag] $message")
        }
    }
}
