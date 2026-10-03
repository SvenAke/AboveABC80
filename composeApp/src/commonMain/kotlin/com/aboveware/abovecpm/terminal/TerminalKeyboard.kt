package com.aboveware.abovecpm.terminal

import androidx.compose.ui.input.key.Key
import com.aboveware.abovecpm.keyboard.KeyboardLed

interface TerminalKeyboard {
    /**
     * Handles key events from the UI (screen keyboard).
     * @param codes A comma-separated string of "row,bit" or an ASCII code.
     * @param label The primary label of the key.
     * @param secondLabel The secondary label of the key.
     * @param thirdLabel The third label of the key.
     * @param fourthLabel The fourth label of the key.
     * @param fifthLabel The fifth label of the key.
     * @param sixthLabel The sixth label of the key.
     */
    fun handleKeyEvent(
        codes: String,
        label: String? = null,
        secondLabel: String? = null,
        thirdLabel: String? = null,
        fourthLabel: String? = null,
        fifthLabel: String? = null,
        sixthLabel: String? = null
    )

    /**
     * Handles key release events from the UI (screen keyboard).
     */
    fun handleKeyRelease(codes: String)

    /**
     * Handles physical key events.
     * @return true if the event was handled.
     */
    fun handlePhysicalKeyEvent(
        key: Key,
        type: androidx.compose.ui.input.key.KeyEventType,
        isCtrl: Boolean,
        isShift: Boolean,
        isRepeat: Boolean,
        timeMillis: Long
    ): Boolean

    /**
     * Intercepts a character before it is sent to the terminal (e.g. for Compose sequences).
     * @return true if the character was intercepted and should not be sent further.
     */
    fun interceptKeyEvent(char: Char): Boolean = false

    /**
     * Transforms a character based on the current keyboard state (e.g. Caps Lock or Shift Lock).
     */
    fun transformChar(c: Char, isShiftActive: Boolean): Char

    /**
     * Map of LED name to its state.
     */
    val leds: Map<String, KeyboardLed>

    /**
     * Updates character mappings (e.g. from XML layout).
     */
    fun updateMappings(mappings: Map<Char, Char>)

    /**
     * Sets the state of a specific LED by index (1-based, terminal specific).
     */
    fun setLed(index: Int, on: Boolean)

    /**
     * Stops any ongoing auto-repeat logic.
     */
    fun stopRepeating()
}
