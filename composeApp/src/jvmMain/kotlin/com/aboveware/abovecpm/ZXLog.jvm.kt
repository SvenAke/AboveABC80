package com.aboveware.abovecpm

actual fun platformLog(tag: String, message: String) {
    if (tag == "WTF") {
        System.err.println("[$tag] $message")
    } else {
        println("[$tag] $message")
    }
}
