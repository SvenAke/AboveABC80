package com.aboveware.abovecpm.terminal

import com.aboveware.abovecpm.R

actual val ADM3A_LAYOUT_ID: Int
    get() = try {
        R.xml.adm3a
    } catch (e: Exception) {
        0
    }

actual fun getVT320LayoutId(index: Int): Int {
    return try {
        when (index) {
            0 -> R.xml.vt320_north_american
            1 -> R.xml.vt320_british
            2 -> R.xml.vt320_flemish            // Belgium(flemish)
            3 -> R.xml.vt320_canadian_french    // Canada(french)
            4 -> R.xml.vt320_danish             // Denmark
            5 -> R.xml.vt320_finnish            // Finland
            6 -> R.xml.vt320_german_austrian    // German/Austria
            7 -> R.xml.vt320_dutch              // Holland
            8 -> R.xml.vt320_italian            // Italy
            9 -> R.xml.vt320_swiss_french       // Switzerland(french)
            10 -> R.xml.vt320_swiss_german      // Switzerland(german)
            11 -> R.xml.vt320_swedish           // Sweden
            12 -> R.xml.vt320_norwegian         // Norge
            13 -> R.xml.vt320_french_belgian    // French/Belgian
            14 -> R.xml.vt320_spanish           // Spain
            15 -> R.xml.vt320_portuguese        // Portugal
            else -> R.xml.vt320
        }
    } catch (e: Exception) {
        try {
            R.xml.vt320
        } catch (e2: Exception) {
            0
        }
    }
}
