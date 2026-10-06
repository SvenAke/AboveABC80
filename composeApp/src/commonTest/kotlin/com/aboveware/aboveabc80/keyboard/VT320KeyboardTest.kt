package com.aboveware.aboveabc80.keyboard

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import com.aboveware.aboveabc80.terminal.CharacterSet
import com.aboveware.aboveabc80.terminal.CursorStyle
import com.aboveware.aboveabc80.terminal.Tabulator
import com.aboveware.aboveabc80.terminal.Terminal
import com.aboveware.aboveabc80.terminal.TerminalCell
import com.aboveware.aboveabc80.terminal.TerminalKeyboard
import com.aboveware.aboveabc80.terminal.VT320Keyboard
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalUnsignedTypes::class)
class VT320KeyboardTest {

    private lateinit var mockTerminal: MockTerminal
    private lateinit var keyboard: VT320Keyboard

    @BeforeTest
    fun setup() {
        mockTerminal = MockTerminal()
        keyboard = VT320Keyboard(mockTerminal)
        mockTerminal.keyboard = keyboard
    }

    class MockTerminal : Terminal {
        val sentChars = mutableListOf<Char>()
        var holdScreenEnabled = false
        var setupToggled = false
        var screenPrinted = false

        override val screen: Array<Array<TerminalCell>> = Array(24) { Array(80) { TerminalCell() } }
        override val cursorX: Int = 0
        override val cursorY: Int = 0
        override val cursorVisible: Boolean = true
        override val cursorStyle: CursorStyle = CursorStyle.BLOCK
        override var cursorKeysMode: Boolean = false
        override var keypadMode: Boolean = false
        override var newLineMode: Boolean = false
        override val screenReverse: Boolean = false
        override var autoRepeatMode: Boolean = false
        override var holdScreen: Boolean
            get() = holdScreenEnabled
            set(value) {
                holdScreenEnabled = value
            }
        override val backgroundColor: Color = Color.Black
        override val keyboardXmlResId: Int = 0
        override lateinit var keyboard: TerminalKeyboard
        override val tabulator: Tabulator = Tabulator()
        override var conformanceLevel: Int = 62

        override val nominalWidth: Int = 80
        override val nominalHeight: Int = 24
        override val dotStretch: Float = 1.0f

        override var onBell: (() -> Unit)? = null
        override var onKeyClick: (() -> Unit)? = null
        override var onKeyInput: ((Char) -> Unit)? = null
        override fun putChar(c: Char) {}
        override fun onKeyEvent(char: Char) {
            sentChars.add(char)
        }

        override fun hasChar(): Boolean = false
        override fun getChar(): Int = -1
        override fun clearScreen() {}
        override fun clearInputBuffer() {}
        override fun connect() {}
        override fun disconnect() {}
        override fun reset() {}
        override fun getGlyph(c: Char): CharacterSet.Glyph? = null
        override fun getScreenState(): ByteArray = byteArrayOf()
        override fun setScreenState(data: ByteArray) {}
        override fun toggleSetup() {
            setupToggled = !setupToggled
        }

        override fun printScreen() {
            screenPrinted = true
        }

        override fun triggerClick() {}
    }

    @Test
    fun testFunctionKeysF6ToF20() {
        val fKeyCodes = mapOf(
            "64" to "\u001B[17~", // F6
            "65" to "\u001B[18~", // F7
            "66" to "\u001B[19~", // F8
            "67" to "\u001B[20~", // F9
            "68" to "\u001B[21~", // F10
            "71" to "\u001B[23~", // F11
            "72" to "\u001B[24~", // F12
            "73" to "\u001B[25~", // F13
            "74" to "\u001B[26~", // F14
            "7C" to "\u001B[28~", // Help (F15)
            "7D" to "\u001B[29~", // Do (F16)
            "80" to "\u001B[31~", // F17
            "81" to "\u001B[32~", // F18
            "82" to "\u001B[33~", // F19
            "83" to "\u001B[34~"  // F20
        )

        fKeyCodes.forEach { (code, expectedSequence) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            val actual = mockTerminal.sentChars.joinToString("")
            assertEquals(expectedSequence, actual, "Failed for code $code at conformance 62")
        }

        // Test F11-F13 with low conformance level (VT52 mode or similar)
        mockTerminal.conformanceLevel = 52

        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("71") // F11 -> ESC
        assertEquals(
            "\u001B",
            mockTerminal.sentChars.joinToString(""),
            "F11 should send ESC at conformance 52"
        )

        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("72") // F12 -> BS
        assertEquals(
            "\u0008",
            mockTerminal.sentChars.joinToString(""),
            "F12 should send BS at conformance 52"
        )

        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("73") // F13 -> LF
        assertEquals(
            "\u000A",
            mockTerminal.sentChars.joinToString(""),
            "F13 should send LF at conformance 52"
        )
    }

    @Test
    fun testPFKeys() {
        // Conformance 62 (VT200/300 mode)
        mockTerminal.conformanceLevel = 62
        val pfKeyCodes62 = mapOf(
            "A1" to "\u001BOP", // PF1
            "A2" to "\u001BOQ", // PF2
            "A3" to "\u001BOR", // PF3
            "A4" to "\u001BOS"  // PF4
        )

        pfKeyCodes62.forEach { (code, expectedSequence) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            val actual = mockTerminal.sentChars.joinToString("")
            assertEquals(expectedSequence, actual, "PF key failed for $code at conformance 62")
        }

        // Conformance 52 (VT52 mode)
        mockTerminal.conformanceLevel = 52
        val pfKeyCodes52 = mapOf(
            "A1" to "\u001BP", // PF1
            "A2" to "\u001BQ", // PF2
            "A3" to "\u001BR"  // PF3
            // PF4 (A4) sends nothing in VT52 mode per VT320Keyboard implementation
        )

        pfKeyCodes52.forEach { (code, expectedSequence) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            val actual = mockTerminal.sentChars.joinToString("")
            assertEquals(expectedSequence, actual, "PF key failed for $code at conformance 52")
        }

        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("A4") // PF4
        assertTrue(mockTerminal.sentChars.isEmpty(), "PF4 should send nothing at conformance 52")
    }

    @Test
    fun testEditingKeys() {
        val editingKeyCodes = mapOf(
            "8A" to "\u001B[1~", // Find
            "8B" to "\u001B[2~", // Insert Here
            "8C" to "\u001B[3~", // Remove
            "8D" to "\u001B[4~", // Select
            "8E" to "\u001B[5~", // Prev Screen
            "8F" to "\u001B[6~"  // Next Screen
        )

        // Conformance 62: sequences sent
        mockTerminal.conformanceLevel = 62
        editingKeyCodes.forEach { (code, expectedSequence) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            val actual = mockTerminal.sentChars.joinToString("")
            assertEquals(expectedSequence, actual, "Editing key failed for $code at conformance 62")
        }

        // Conformance 52: nothing sent
        mockTerminal.conformanceLevel = 52
        editingKeyCodes.forEach { (code, _) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            assertTrue(
                mockTerminal.sentChars.isEmpty(),
                "Editing key $code should send nothing at conformance 52"
            )
        }
    }

    @Test
    fun testNumericKeypad() {
        val keypadCodes = mapOf(
            "92" to ('0' to 'p'),
            "96" to ('1' to 'q'),
            "97" to ('2' to 'r'),
            "98" to ('3' to 's'),
            "99" to ('4' to 't'),
            "9A" to ('5' to 'u'),
            "9B" to ('6' to 'v'),
            "9D" to ('7' to 'w'),
            "9E" to ('8' to 'x'),
            "9F" to ('9' to 'y'),
            "A0" to ('-' to 'm'),
            "9C" to (',' to 'l'),
            "94" to ('.' to 'n')
        )

        // 1. Conformance 62, Numeric Mode (keypadMode = false)
        mockTerminal.conformanceLevel = 62
        mockTerminal.keypadMode = false
        keypadCodes.forEach { (code, chars) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            assertEquals(
                chars.first.toString(),
                mockTerminal.sentChars.joinToString(""),
                "Numeric keypad $code failed in Numeric mode"
            )
        }

        // 2. Conformance 62, Application Mode (keypadMode = true)
        mockTerminal.keypadMode = true
        keypadCodes.forEach { (code, chars) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            assertEquals(
                "\u001BO${chars.second}",
                mockTerminal.sentChars.joinToString(""),
                "Numeric keypad $code failed in Application mode"
            )
        }

        // 3. Conformance 52 (VT52), Numeric Mode
        mockTerminal.conformanceLevel = 52
        mockTerminal.keypadMode = false
        keypadCodes.forEach { (code, chars) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            assertEquals(
                chars.first.toString(),
                mockTerminal.sentChars.joinToString(""),
                "VT52 keypad $code failed in Numeric mode"
            )
        }

        // 4. Conformance 52 (VT52), Application Mode
        mockTerminal.keypadMode = true
        keypadCodes.forEach { (code, chars) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            // VT320Keyboard implementation for VT52: \u001B?$vt52Suffix
            // Except for ',' (ansiSuffix 'l') which sends normal in VT52 per current implementation
            val expected =
                if (chars.second == 'l') chars.first.toString() else "\u001B?${chars.second}"
            assertEquals(
                expected,
                mockTerminal.sentChars.joinToString(""),
                "VT52 keypad $code failed in Application mode"
            )
        }

        // 5. Keypad Enter (code 13, label "Enter")
        mockTerminal.conformanceLevel = 62
        mockTerminal.keypadMode = true
        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("13", label = "Enter")
        assertEquals(
            "\u001BOM",
            mockTerminal.sentChars.joinToString(""),
            "Keypad Enter failed in Application mode"
        )

        mockTerminal.conformanceLevel = 52
        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("13", label = "Enter")
        assertEquals(
            "\u001B?M",
            mockTerminal.sentChars.joinToString(""),
            "Keypad Enter failed in VT52 Application mode"
        )

        mockTerminal.keypadMode = false
        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("13", label = "Enter")
        assertEquals(
            "\r",
            mockTerminal.sentChars.joinToString(""),
            "Keypad Enter failed in Numeric mode"
        )
    }

    @Test
    fun testArrowKeys() {
        val arrowCodes = mapOf(
            "AA" to 'A', // Up
            "A9" to 'B', // Down
            "A8" to 'C', // Right
            "A7" to 'D'  // Left
        )

        // 1. Conformance 62, Normal Cursor Mode
        mockTerminal.conformanceLevel = 62
        mockTerminal.cursorKeysMode = false
        arrowCodes.forEach { (code, suffix) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            assertEquals(
                "\u001B[$suffix",
                mockTerminal.sentChars.joinToString(""),
                "Arrow $code failed in Normal mode"
            )
        }

        // 2. Conformance 62, Application Cursor Mode
        mockTerminal.cursorKeysMode = true
        arrowCodes.forEach { (code, suffix) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            assertEquals(
                "\u001BO$suffix",
                mockTerminal.sentChars.joinToString(""),
                "Arrow $code failed in Application mode"
            )
        }

        // 3. Conformance 52 (VT52)
        mockTerminal.conformanceLevel = 52
        arrowCodes.forEach { (code, suffix) ->
            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(code)
            assertEquals(
                "\u001B$suffix",
                mockTerminal.sentChars.joinToString(""),
                "Arrow $code failed in VT52 mode"
            )
        }
    }

    @Test
    fun testUserDefinedKeys() {
        val udkMappings = mapOf(
            "64" to 17, // F6
            "65" to 18, // F7
            "66" to 19, // F8
            "67" to 20, // F9
            "68" to 21, // F10
            "71" to 23, // F11
            "72" to 24, // F12
            "73" to 25, // F13
            "74" to 26, // F14
            "7C" to 28, // Help
            "7D" to 29, // Do
            "80" to 31, // F17
            "81" to 32, // F18
            "82" to 33, // F19
            "83" to 34  // F20
        )

        udkMappings.forEach { (scanCode, udkIndex) ->
            val testString = "UDK_$udkIndex"

            // Define UDK
            keyboard.defineUdk(udkIndex, testString)

            mockTerminal.sentChars.clear()
            keyboard.handleKeyEvent(scanCode)

            val actual = mockTerminal.sentChars.joinToString("")
            assertEquals(testString, actual, "UDK failed for scan code $scanCode (index $udkIndex)")
        }

        // Test clearing UDKs
        keyboard.clearUdks()
        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("64") // F6
        assertEquals(
            "\u001B[17~",
            mockTerminal.sentChars.joinToString(""),
            "Should return to default sequence after clearing UDKs"
        )
    }

    @Test
    fun testLocalFunctionKeysF1ToF5() {
        // F1: Hold Screen (code "56")
        mockTerminal.holdScreenEnabled = false
        keyboard.handleKeyEvent("56")
        assertTrue(mockTerminal.holdScreenEnabled, "F1 (56) should toggle hold screen")
        assertEquals(
            0x13.toChar(),
            mockTerminal.sentChars.last(),
            "F1 should send XOFF when enabling hold"
        )

        // F2: Print Screen (code "57")
        mockTerminal.screenPrinted = false
        keyboard.handleKeyEvent("57")
        assertTrue(mockTerminal.screenPrinted, "F2 (57) should trigger print screen")

        // F3: Set-Up (code "58")
        mockTerminal.setupToggled = false
        keyboard.handleKeyEvent("58")
        assertTrue(mockTerminal.setupToggled, "F3 (58) should toggle setup")

        // F4: Data/Talk (code "59") - Should do nothing in current implementation
        mockTerminal.sentChars.clear()
        keyboard.handleKeyEvent("59")
        assertTrue(mockTerminal.sentChars.isEmpty(), "F4 (59) should send nothing")

        // F5: Break (code "5A") - Handled by Keyboard.instance.pressBreak() which we can't easily mock here
        // but we can verify it doesn't crash
        keyboard.handleKeyEvent("5A")
    }

    @Test
    fun testPhysicalKeys() {
        // Test F1 (Physical) -> Hold Screen
        mockTerminal.holdScreenEnabled = false
        mockTerminal.sentChars.clear()
        keyboard.handlePhysicalKeyEvent(
            Key.F1,
            KeyEventType.KeyDown,
            isCtrl = false,
            isShift = false,
            isRepeat = false,
            timeMillis = 1000
        )
        assertTrue(mockTerminal.holdScreenEnabled, "Physical F1 should toggle hold screen")
        assertEquals(0x13.toChar(), mockTerminal.sentChars.last(), "Physical F1 should send XOFF")

        // Test F3 (Physical) -> Setup
        mockTerminal.setupToggled = false
        keyboard.handlePhysicalKeyEvent(
            Key.F3,
            KeyEventType.KeyDown,
            isCtrl = false,
            isShift = false,
            isRepeat = false,
            timeMillis = 2000
        )
        assertTrue(mockTerminal.setupToggled, "Physical F3 should toggle setup")

        // Test AltRight (Physical) -> Compose (B1)
        // Since B1 is a sticky key (Compose), we check if the LED for "Compose" turns on
        // and if it signals through some state. In our mock, we can check if it was handled.
        val composeResult = keyboard.handlePhysicalKeyEvent(
            Key.AltRight,
            KeyEventType.KeyDown,
            isCtrl = false,
            isShift = false,
            isRepeat = false,
            timeMillis = 2500
        )
        assertTrue(composeResult, "Physical AltRight should be handled as Compose")

        // Test ScrollLock (Physical) -> Hold Screen
        mockTerminal.holdScreenEnabled = true
        mockTerminal.sentChars.clear()
        keyboard.handlePhysicalKeyEvent(
            Key.ScrollLock,
            KeyEventType.KeyDown,
            isCtrl = false,
            isShift = false,
            isRepeat = false,
            timeMillis = 3000
        )
        assertFalse(
            mockTerminal.holdScreenEnabled,
            "Physical ScrollLock should toggle hold screen off"
        )
        assertEquals(
            0x11.toChar(),
            mockTerminal.sentChars.last(),
            "Physical ScrollLock should send XON when disabling hold"
        )
    }
}
