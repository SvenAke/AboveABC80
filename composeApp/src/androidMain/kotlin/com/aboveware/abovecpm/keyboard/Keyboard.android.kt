@file:OptIn(ExperimentalUnsignedTypes::class)

package com.aboveware.abovecpm.keyboard

import android.view.KeyEvent

/**
 * Android-specific extensions for the common Keyboard class.
 */
fun Keyboard.onKeyDown(key: KeyboardViewController.Key) {
    if (handleLabelLogic(key, true)) return
    onKeyDown(
        key.codes,
        key.label?.toString(),
        key.secondLabel?.toString(),
        key.thirdLabel?.toString(),
        key.fourthLabel?.toString(),
        key.fifthLabel?.toString(),
        key.sixthLabel?.toString()
    )
}

fun Keyboard.onKeyUp(key: KeyboardViewController.Key) {
    if (handleLabelLogic(key, false)) return
    onKeyUp(
        key.codes,
        key.label?.toString(),
        key.secondLabel?.toString(),
        key.thirdLabel?.toString(),
        key.fourthLabel?.toString(),
        key.fifthLabel?.toString(),
        key.sixthLabel?.toString()
    )
}

private fun Keyboard.handleLabelLogic(key: KeyboardViewController.Key, isDown: Boolean): Boolean {
    val label = key.label?.toString() ?: ""
    val secondLabel = key.secondLabel?.toString() ?: ""

    if (label.isEmpty()) return false

    val upperLabel = label.uppercase()

    when (upperLabel) {
        "REPEAT" -> {
            isRepeatActive = isDown
            return true
        }

        "BREAK" -> {
            if (isDown) pressBreak()
            return true
        }

        "RETURN", "ENTER" -> {
            if (isDown) onKeyEvent(13.toChar()) else onKeyReleaseEvent(13.toChar())
            return true
        }

        "TAB" -> {
            if (isDown) onKeyEvent(9.toChar()) else onKeyReleaseEvent(9.toChar())
            return true
        }

        "ESC", "ESCAPE" -> {
            if (isDown) onKeyEvent(27.toChar()) else onKeyReleaseEvent(27.toChar())
            return true
        }

        "SPACE" -> {
            if (isDown) onKeyEvent(' ') else onKeyReleaseEvent(' ')
            return true
        }

        "BACKSPACE", "DELETE", "DEL" -> {
            if (isDown) onKeyEvent(8.toChar()) else onKeyReleaseEvent(8.toChar())
            return true
        }

        "SHIFT" -> {
            if (isDown) onKeyDown(Keys.KeyCapsShift) else onKeyUp(Keys.KeyCapsShift)
            return true
        }

        "CTRL" -> {
            if (isDown) onKeyDown(Keys.KeySymbolShift) else onKeyUp(Keys.KeySymbolShift)
            return true
        }
    }

    if (label.length == 1 && key.codes.isNotEmpty() && !key.codes.contains(",")) {
        // Let the terminal keyboard handle single character keys via codes and labels
        return false
    }

    return false
}

private val KEY_CODE_MAP = mapOf(
    KeyEvent.KEYCODE_SHIFT_LEFT to Keys.KeyCapsShift,
    KeyEvent.KEYCODE_Z to Keys.KeyZ, KeyEvent.KEYCODE_X to Keys.KeyX,
    KeyEvent.KEYCODE_C to Keys.KeyC, KeyEvent.KEYCODE_V to Keys.KeyV,
    KeyEvent.KEYCODE_A to Keys.KeyA, KeyEvent.KEYCODE_S to Keys.KeyS,
    KeyEvent.KEYCODE_D to Keys.KeyD, KeyEvent.KEYCODE_F to Keys.KeyF,
    KeyEvent.KEYCODE_G to Keys.KeyG, KeyEvent.KEYCODE_Q to Keys.KeyQ,
    KeyEvent.KEYCODE_W to Keys.KeyW, KeyEvent.KEYCODE_E to Keys.KeyE,
    KeyEvent.KEYCODE_R to Keys.KeyR, KeyEvent.KEYCODE_T to Keys.KeyT,
    KeyEvent.KEYCODE_1 to Keys.Key1, KeyEvent.KEYCODE_2 to Keys.Key2,
    KeyEvent.KEYCODE_3 to Keys.Key3, KeyEvent.KEYCODE_4 to Keys.Key4,
    KeyEvent.KEYCODE_5 to Keys.Key5, KeyEvent.KEYCODE_0 to Keys.Key0,
    KeyEvent.KEYCODE_9 to Keys.Key9, KeyEvent.KEYCODE_8 to Keys.Key8,
    KeyEvent.KEYCODE_7 to Keys.Key7, KeyEvent.KEYCODE_6 to Keys.Key6,
    KeyEvent.KEYCODE_P to Keys.KeyP, KeyEvent.KEYCODE_O to Keys.KeyO,
    KeyEvent.KEYCODE_I to Keys.KeyI, KeyEvent.KEYCODE_U to Keys.KeyU,
    KeyEvent.KEYCODE_Y to Keys.KeyY, KeyEvent.KEYCODE_ENTER to Keys.KeyEnter,
    KeyEvent.KEYCODE_L to Keys.KeyL, KeyEvent.KEYCODE_K to Keys.KeyK,
    KeyEvent.KEYCODE_J to Keys.KeyJ, KeyEvent.KEYCODE_H to Keys.KeyH,
    KeyEvent.KEYCODE_SPACE to Keys.KeySpace, KeyEvent.KEYCODE_ALT_LEFT to Keys.KeySymbolShift,
    KeyEvent.KEYCODE_M to Keys.KeyM, KeyEvent.KEYCODE_N to Keys.KeyN,
    KeyEvent.KEYCODE_B to Keys.KeyB
)

fun Keyboard.translate(keyCode: Int) = KEY_CODE_MAP[keyCode] ?: Keys.KeyNone

fun Keyboard.onKeyDown(keyCode: Int): Keys {
    val zxKey = translate(keyCode)
    onKeyDown(zxKey)
    return zxKey
}

fun Keyboard.onKeyUp(keyCode: Int): Keys {
    val zxKey = translate(keyCode)
    onKeyUp(zxKey)
    return zxKey
}
