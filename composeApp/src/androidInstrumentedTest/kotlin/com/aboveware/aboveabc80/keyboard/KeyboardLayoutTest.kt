package com.aboveware.aboveabc80.keyboard

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aboveware.aboveabc80.R
import com.aboveware.aboveabc80.setAndroidContext
import com.aboveware.aboveabc80.terminal.CharacterSet
import com.aboveware.aboveabc80.terminal.CursorStyle
import com.aboveware.aboveabc80.terminal.Tabulator
import com.aboveware.aboveabc80.terminal.Terminal
import com.aboveware.aboveabc80.terminal.TerminalCell
import com.aboveware.aboveabc80.terminal.TerminalKeyboard
import com.aboveware.aboveabc80.terminal.VT320Keyboard
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalUnsignedTypes::class)
class KeyboardLayoutTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        setAndroidContext(context)
    }

    class MockTerminal : Terminal {
        val sentChars = mutableListOf<Char>()
        override val screen: Array<Array<TerminalCell>> = Array(24) { Array(80) { TerminalCell() } }
        override val cursorX: Int = 0
        override val cursorY: Int = 0
        override val cursorVisible: Boolean = true
        override val cursorStyle: CursorStyle = CursorStyle.BLOCK
        override val cursorKeysMode: Boolean = false
        override val keypadMode: Boolean = false
        override val newLineMode: Boolean = false
        override val screenReverse: Boolean = false
        override var autoRepeatMode: Boolean = false
        override var holdScreen: Boolean = false
        override val backgroundColor: Color = Color.Black
        override val keyboardXmlResId: Int = 0
        override lateinit var keyboard: TerminalKeyboard
        override val tabulator: Tabulator = Tabulator()
        override val conformanceLevel: Int = 62
        override val nominalWidth: Int
            get() = 0
        override val nominalHeight: Int
            get() = 0
        override val dotStretch: Float
            get() = 0f
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
    }

    @Test
    fun testNationalKeyboardLayouts() {
        val xmlFields = R.xml::class.java.fields
        val vt320Layouts = xmlFields.filter { it.name.startsWith("vt320_") }

        assertTrue(vt320Layouts.isNotEmpty(), "No vt320 layout XMLs found in R.xml")

        // Map of expected characters for each layout.
        // Format: "layout_name" to Map(KeyCodes to Pair(NormalChar, ShiftedChar))
        val layoutExpectations = mapOf(
            "vt320_swedish" to mapOf(
                "FA" to ('å' to 'Å'), "FB" to ('ä' to 'Ä'), "F2" to ('ö' to 'Ö'),
                "C9" to ('<' to '>'), "BF" to ('`' to '~'), "CB" to ('3' to '#')
            ),
            "vt320_british" to mapOf(
                "CB" to ('3' to '£'), "C5" to ('2' to '"'), "FB" to ('\'' to '@'),
                "BF" to ('`' to '~')
            ),
            "vt320_north_american" to mapOf(
                "CB" to ('3' to '#'), "C5" to ('2' to '@'), "FB" to ('\'' to '"'),
                "BF" to ('`' to '~')
            ),
            "vt320_german_austrian" to mapOf(
                "FA" to ('ü' to 'Ü'), "FB" to ('ä' to 'Ä'), "F2" to ('ö' to 'Ö'),
                "F6" to (']' to '*')
            ),
            "vt320_finnish" to mapOf(
                "FA" to ('å' to 'Å'), "FB" to ('ä' to 'Ä'), "F2" to ('ö' to 'Ö'),
                "C9" to ('<' to '>')
            ),
            "vt320_danish" to mapOf(
                "FA" to ('æ' to 'Æ'), "FB" to ('ø' to 'Ø'), "F2" to ('å' to 'Å'),
                "F6" to (']' to '}')
            ),
            "vt320_norwegian" to mapOf(
                "FA" to ('æ' to 'Æ'), "FB" to ('ø' to 'Ø'), "F2" to ('å' to 'Å'),
                "F6" to (']' to '}')
            ),
            "vt320_french_belgian" to mapOf(
                "C0" to ('&' to '1'), "C5" to ('é' to '2'), "CB" to ('"' to '3'),
                "D0" to ('\'' to '4'), "D6" to ('(' to '5'), "DB" to ('§' to '6'),
                "E0" to ('è' to '7'), "E5" to ('!' to '8'), "EA" to ('ç' to '9'),
                "EF" to ('à' to '0'), "FB" to ('’' to '"')
            ),
            "vt320_flemish" to mapOf(
                "C0" to ('&' to '1'), "C5" to ('é' to '2'), "CB" to ('"' to '3'),
                "D0" to ('\'' to '4'), "D6" to ('(' to '5'), "DB" to ('§' to '6'),
                "E0" to ('è' to '7'), "E5" to ('!' to '8'), "EA" to ('ç' to '9'),
                "EF" to ('à' to '0'), "F9" to (')' to '°'), "FB" to ('ù' to '%'),
                "F7" to ('#' to '@'), "F5" to ('-' to '_'), "C1" to ('a' to 'A'),
                "C6" to ('z' to 'Z'), "C2" to ('q' to 'Q'), "C3" to ('w' to 'W'),
                "F2" to ('m' to 'M'), "FA" to ('^' to '¨'), "F6" to ('$' to '*'),
                "E3" to (',' to '?'), "E8" to (';' to '.'), "ED" to (':' to '/'),
                "C9" to ('<' to '>'), "F3" to ('=' to '+'), "BF" to ('`' to '~')
            ),
            "vt320_canadian_french" to mapOf(
                "FA" to ('à' to 'À'), "FB" to ('è' to 'È'), "F2" to ('é' to 'É'),
                "F6" to ('ù' to 'Ù'), "F7" to ('ç' to 'Ç')
            ),
            "vt320_dutch" to mapOf(
                "CB" to ('3' to '£'), "FB" to ('\'' to '@'), "C5" to ('2' to '"')
            ),
            "vt320_swiss_french" to mapOf(
                "FA" to ('é' to 'ê'), "FB" to ('à' to 'â'), "F2" to ('è' to 'î'),
                "DC" to ('z' to 'Z'), "C3" to ('y' to 'Y')
            ),
            "vt320_swiss_german" to mapOf(
                "FA" to ('ü' to 'è'), "FB" to ('ö' to 'é'), "F2" to ('ä' to 'à'),
                "DC" to ('z' to 'Z'), "C3" to ('y' to 'Y')
            ),
            "vt320_italian" to mapOf(
                "FA" to ('è' to 'é'), "FB" to ('ò' to 'ç'), "F2" to ('à' to '°'),
                "F6" to ('ì' to '^'), "F7" to ('ù' to '§')
            ),
            "vt320_spanish" to mapOf(
                "FA" to ('[' to '{'), "FB" to ('ñ' to 'Ñ'), "F2" to ('\'' to '?'),
                "C9" to ('<' to '>')
            ),
            "vt320_portuguese" to mapOf(
                "FA" to ('«' to '»'), "FB" to ('~' to '^'), "F2" to ('ç' to 'Ç'),
                "C9" to ('<' to '>')
            )
        )

        // Pre-calculate North American mappings to use as baseline for other layouts
        val northAmericanField = vt320Layouts.find { it.name == "vt320_north_american" }
        assertTrue(northAmericanField != null, "vt320_north_american not found")
        val northAmericanResId = northAmericanField.getInt(null)
        val northAmericanMappings = mutableMapOf<String, Pair<Char?, Char?>>()

        run {
            val keyboardViewController = KeyboardViewController(context, northAmericanResId)
            val mockTerminal = MockTerminal()
            val vt320Keyboard = VT320Keyboard(mockTerminal)
            mockTerminal.keyboard = vt320Keyboard
            vt320Keyboard.updateMappings(keyboardViewController.characterMappings)

            keyboardViewController.keys.forEach { key ->
                val codes = key.codes
                if (codes.isEmpty() || codes == "SHIFT" || codes == "CTRL" || codes == "B0") return@forEach

                // Test Normal
                mockTerminal.sentChars.clear()
                vt320Keyboard.handleKeyEvent(
                    codes = codes,
                    label = key.label?.toString(),
                    secondLabel = key.secondLabel?.toString(),
                    thirdLabel = key.thirdLabel?.toString(),
                    fourthLabel = key.fourthLabel?.toString(),
                    fifthLabel = key.fifthLabel?.toString(),
                    sixthLabel = key.sixthLabel?.toString(),
                )
                val normal = mockTerminal.sentChars.lastOrNull()

                // Test Shifted
                vt320Keyboard.handleKeyEvent("SHIFT")
                mockTerminal.sentChars.clear()
                vt320Keyboard.handleKeyEvent(
                    codes = codes,
                    label = key.label?.toString(),
                    secondLabel = key.secondLabel?.toString(),
                    thirdLabel = key.thirdLabel?.toString(),
                    fourthLabel = key.fourthLabel?.toString(),
                    fifthLabel = key.fifthLabel?.toString(),
                    sixthLabel = key.sixthLabel?.toString(),
                )
                val shifted = mockTerminal.sentChars.lastOrNull()
                vt320Keyboard.handleKeyRelease("SHIFT")

                northAmericanMappings[codes] = normal to shifted
            }
        }

        val errors = mutableListOf<String>()

        for (field in vt320Layouts) {
            val resId = field.getInt(null)
            val layoutName = field.name

            val keyboardViewController = KeyboardViewController(context, resId)
            val mockTerminal = MockTerminal()
            val vt320Keyboard = VT320Keyboard(mockTerminal)
            mockTerminal.keyboard = vt320Keyboard

            vt320Keyboard.updateMappings(keyboardViewController.characterMappings)

            // Verify explicit expectations from the array for this layout
            layoutExpectations[layoutName]?.forEach { (codes, expected) ->
                val (expectedNormal, expectedShifted) = expected
                val key = keyboardViewController.keys.find { it.codes == codes }
                if (key == null) {
                    errors.add("Layout $layoutName: Key with codes '$codes' not found")
                    return@forEach
                }

                // 1. Test Normal
                mockTerminal.sentChars.clear()
                vt320Keyboard.handleKeyEvent(
                    codes = key.codes,
                    label = key.label?.toString(),
                    secondLabel = key.secondLabel?.toString(),
                    thirdLabel = key.thirdLabel?.toString(),
                    fourthLabel = key.fourthLabel?.toString(),
                    fifthLabel = key.fifthLabel?.toString(),
                    sixthLabel = key.sixthLabel?.toString(),
                )
                if (expectedNormal != mockTerminal.sentChars.lastOrNull()) {
                    errors.add("Layout $layoutName: Explicit normal expectation for codes '$codes' failed. Expected '$expectedNormal', got '${mockTerminal.sentChars.lastOrNull()}'")
                }

                // 2. Test Shifted
                vt320Keyboard.handleKeyEvent("SHIFT")
                mockTerminal.sentChars.clear()
                vt320Keyboard.handleKeyEvent(
                    codes = key.codes,
                    label = key.label?.toString(),
                    secondLabel = key.secondLabel?.toString(),
                    thirdLabel = key.thirdLabel?.toString(),
                    fourthLabel = key.fourthLabel?.toString(),
                    fifthLabel = key.fifthLabel?.toString(),
                    sixthLabel = key.sixthLabel?.toString(),
                )
                if (expectedShifted != mockTerminal.sentChars.lastOrNull()) {
                    errors.add("Layout $layoutName: Explicit shifted expectation for codes '$codes' failed. Expected '$expectedShifted', got '${mockTerminal.sentChars.lastOrNull()}'")
                }

                // Release SHIFT for next key
                vt320Keyboard.handleKeyRelease("SHIFT")
            }

            // General verification: if not in layoutExpectations, it should match north_american
            keyboardViewController.keys.forEach { key ->
                val codes = key.codes
                if (codes.isEmpty() || codes == "SHIFT" || codes == "CTRL" || codes == "B0") return@forEach

                // Skip keys already explicitly tested for this layout
                if (layoutExpectations[layoutName]?.containsKey(codes) == true) return@forEach

                val northAmericanExpectation = northAmericanMappings[codes] ?: return@forEach

                // Test Normal
                mockTerminal.sentChars.clear()
                vt320Keyboard.handleKeyEvent(
                    codes = codes,
                    label = key.label?.toString(),
                    secondLabel = key.secondLabel?.toString(),
                    thirdLabel = key.thirdLabel?.toString(),
                    fourthLabel = key.fourthLabel?.toString(),
                    fifthLabel = key.fifthLabel?.toString(),
                    sixthLabel = key.sixthLabel?.toString(),
                )
                if (northAmericanExpectation.first != mockTerminal.sentChars.lastOrNull()) {
                    errors.add("Layout $layoutName: Key $codes normal char failed. Expected '${northAmericanExpectation.first}', got '${mockTerminal.sentChars.lastOrNull()}'")
                }

                // Test Shifted
                vt320Keyboard.handleKeyEvent("SHIFT")
                mockTerminal.sentChars.clear()
                vt320Keyboard.handleKeyEvent(
                    codes = codes,
                    label = key.label?.toString(),
                    secondLabel = key.secondLabel?.toString(),
                    thirdLabel = key.thirdLabel?.toString(),
                    fourthLabel = key.fourthLabel?.toString(),
                    fifthLabel = key.fifthLabel?.toString(),
                    sixthLabel = key.sixthLabel?.toString(),
                )
                if (northAmericanExpectation.second != mockTerminal.sentChars.lastOrNull()) {
                    errors.add("Layout $layoutName: Key $codes shifted char failed. Expected '${northAmericanExpectation.second}', got '${mockTerminal.sentChars.lastOrNull()}'")
                }
                vt320Keyboard.handleKeyRelease("SHIFT")
            }
        }

        if (errors.isNotEmpty()) {
            throw AssertionError("Keyboard layout verification failed:\n" + errors.joinToString("\n"))
        }
    }

    @Test
    fun testSpecialKeyLabels() {
        // We test a representative sample of functional, editing and keypad keys
        // format: codes -> (normalLabel to shiftedLabel)
        // note: most functional keys have null as normalLabel and the text as secondLabel due to the swap refactor
        val commonExpectations = mapOf(
            "64" to (null to "F6"),
            "65" to (null to "F7"),
            "66" to (null to "F8"),
            "67" to (null to "F9"),
            "68" to (null to "F10"),
            "71" to (null to "F11"),
            "72" to (null to "F12"),
            "73" to (null to "F13"),
            "74" to (null to "F14"),
            "7C" to (null to "Help"),
            "80" to (null to "F17"),
            "81" to (null to "F18"),
            "82" to (null to "F19"),
            "83" to (null to "F20"),
            "A1" to (null to "PF1"),
            "A2" to (null to "PF2"),
            "A3" to (null to "PF3"),
            "A4" to (null to "PF4"),
            "9D" to (null to "7"),
            "9E" to (null to "8"),
            "9F" to (null to "9"),
            "A0" to (null to "-"),
            "99" to (null to "4"),
            "9A" to (null to "5"),
            "9B" to (null to "6"),
            "9C" to (null to ","),
            "96" to (null to "1"),
            "97" to (null to "2"),
            "98" to (null to "3"),
            "92" to (null to "0"),
            "94" to (null to "."),
            "B0" to (null to "Lock"),
            "CTRL" to (null to "Ctrl"),
            "SHIFT" to (null to "Shift")
        )

        val layoutSpecificExpectations = mapOf(
            "vt320_north_american" to mapOf(
                "8A" to (null to "Find"),
                "8B" to ("Here" to "Insert"),
                "8C" to ("move" to "Re-"),
                "8D" to (null to "Select"),
                "8E" to ("Screen" to "Prev"),
                "8F" to ("Screen" to "Next"),
                "B1" to ("Character" to "Compose"),
                "56" to ("Screen" to "Hold"),
                "57" to ("Screen" to "Print"),
                "58" to (null to "Set Up"),
                "59" to ("Talk" to "Data/"),
                "5A" to (null to "Break"),
                "7D" to (null to "Do")
            ),
            "vt320_flemish" to mapOf(
                "8A" to (null to "Zoek"),
                "8B" to ("In" to "Voeg"),
                "8C" to ("Blok" to "Wis"),
                "8D" to (null to "Sel"),
                "8E" to ("Beeld" to "Vorig"),
                "8F" to ("Beeld" to "Volg"),
                "B1" to ("Teken" to "Samengest"),
                "56" to ("Vast" to "Beeld"),
                "57" to ("Afdruk" to "Scherm"),
                "58" to (null to "Set-Up"),
                "59" to (null to "F4"),
                "5A" to ("Lijn" to "Com"),
                "7D" to (null to "Voer Opdr Uit")
            )
        )

        val layoutsToTest = listOf("vt320_north_american", "vt320_flemish")
        val errors = mutableListOf<String>()

        for (layoutName in layoutsToTest) {
            val field = R.xml::class.java.getField(layoutName)
            val resId = field.getInt(null)
            val keyboardViewController = KeyboardViewController(context, resId)

            val expectations =
                commonExpectations + (layoutSpecificExpectations[layoutName] ?: emptyMap())

            expectations.forEach { (codes, expected) ->
                val keys = keyboardViewController.keys.filter { it.codes == codes }
                if (keys.isEmpty()) {
                    errors.add("Layout $layoutName: Key $codes not found")
                    return@forEach
                }

                val match = keys.any { key ->
                    key.label?.toString() == expected.first && key.secondLabel?.toString() == expected.second
                }

                if (!match) {
                    val actual =
                        keys.joinToString(", ") { "'${it.label?.toString()} / ${it.secondLabel?.toString()}'" }
                    errors.add("Layout $layoutName: Key $codes label mismatch. Expected '${expected.first} / ${expected.second}', got $actual")
                }
            }

            // Special check for Enter/Return (both code 13)
            val keys13 = keyboardViewController.keys.filter { it.codes == "13" }
            val labels13 = keys13.map { it.secondLabel?.toString() }.toSet()
            if (!labels13.contains("Return")) errors.add("Layout $layoutName: Code 13 'Return' key missing")
            val expectedEnterLabel = if (layoutName == "vt320_flemish") "Voer" else "Enter"
            if (!labels13.contains(expectedEnterLabel)) errors.add("Layout $layoutName: Code 13 '$expectedEnterLabel' key missing")
        }

        if (errors.isNotEmpty()) {
            throw AssertionError("Special key label verification failed:\n" + errors.joinToString("\n"))
        }
    }
}
