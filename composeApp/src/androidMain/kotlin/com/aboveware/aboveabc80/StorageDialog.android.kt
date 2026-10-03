package com.aboveware.aboveabc80

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeImage(data: ByteArray): ImageBitmap? {
    return try {
        BitmapFactory.decodeByteArray(data, 0, data.size)?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}
