package com.aboveware.aboveabc80.terminal

import com.aboveware.aboveabc80.R

actual val ABC80_LAYOUT_ID: Int
    get() = try {
        R.xml.abc80
    } catch (e: Exception) {
        0
    }
