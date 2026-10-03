package com.aboveware.abovecpm

import android.graphics.Bitmap
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import com.aboveware.abovecpm.keyboard.Keyboard
import com.aboveware.abovecpm.keyboard.KeyboardView
import com.aboveware.abovecpm.terminal.TerminalManager

actual fun createPlatformBitmap(width: Int, height: Int): Any {
    return createBitmap(width, height)
}

actual fun Any.toImageBitmap(): ImageBitmap {
    return (this as Bitmap).asImageBitmap()
}

@OptIn(ExperimentalUnsignedTypes::class)
@Composable
actual fun PlatformKeyboard(keyboard: Keyboard, modifier: Modifier) {
    val activeTerminal = TerminalManager.activeTerminal
    val layoutId = activeTerminal.keyboardXmlResId

    AndroidView(
        modifier = modifier.padding(8.dp),
        factory = { context ->
            KeyboardView(context, null).apply {
                visibility = View.VISIBLE
                keyboardXmlResId = layoutId
                this.keyboard = keyboard
                onMappingsLoaded = { mappings ->
                    activeTerminal.keyboard.updateMappings(mappings)
                }
            }
        },
        update = { view ->
            if (view.keyboardXmlResId != layoutId) {
                view.keyboardXmlResId = layoutId
            }
        }
    )
}

@Preview(widthDp = 400, heightDp = 200, showBackground = true, backgroundColor = 0xFF444444)
@Composable
fun KeyboardPreview() {
    Box(modifier = Modifier.fillMaxSize()) {
        PlatformKeyboard(Keyboard.instance, Modifier.fillMaxWidth())
    }
}

actual fun getRepeatCount(event: KeyEvent): Int {
    return event.nativeKeyEvent.repeatCount
}

actual fun getEventTime(event: KeyEvent): Long {
    return event.nativeKeyEvent.eventTime
}

actual fun triggerKeyClick(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
    playKeyClick()
}

actual fun triggerBell(haptic: androidx.compose.ui.hapticfeedback.HapticFeedback) {
    // For a bell, we can use a more intense haptic or a sound.
    // Let's use standard haptic for now, or ToneGenerator if sound is preferred.
    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
}
