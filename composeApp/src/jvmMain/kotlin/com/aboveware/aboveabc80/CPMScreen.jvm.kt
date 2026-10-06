package com.aboveware.aboveabc80

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.key.KeyEvent
import com.aboveware.aboveabc80.keyboard.Keyboard
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo

actual fun createPlatformBitmap(width: Int, height: Int): Any {
    val bitmap = Bitmap()
    bitmap.allocPixels(ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.PREMUL))
    return bitmap
}

actual fun Any.toImageBitmap(): ImageBitmap {
    return Image.makeFromBitmap(this as Bitmap).toComposeImageBitmap()
}

@Composable
actual fun PlatformKeyboard(keyboard: Keyboard, modifier: Modifier) {
}

actual fun getRepeatCount(event: KeyEvent): Int {
    return 0
}

actual fun getEventTime(event: KeyEvent): Long {
    // java.awt.event.KeyEvent has getWhen()
    // Native event in skiko jvm is SkikoKeyEvent which doesn't directly expose when.
    // Use System.nanoTime for monotonic stability
    return System.nanoTime() / 1_000_000L
}

actual fun triggerKeyClick(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
}

actual fun triggerBell(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    java.awt.Toolkit.getDefaultToolkit().beep()
}
