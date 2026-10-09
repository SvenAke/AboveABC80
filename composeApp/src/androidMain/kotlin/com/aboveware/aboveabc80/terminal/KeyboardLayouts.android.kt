package com.aboveware.aboveabc80.terminal

import com.aboveware.aboveabc80.R

actual val ADM3A_LAYOUT_ID: Int
    get() = try {
        R.xml.adm3a
    } catch (e: Exception) {
        0
    }

