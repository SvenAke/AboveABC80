package com.aboveware.abc80.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aboveware.abc80.R
import com.aboveware.abc80.abc80KeyTable
import com.aboveware.abc80.setAndroidContext
import com.aboveware.abc80.terminal.ABC80TerminalKeyboard
import com.aboveware.abc80.terminal.CharacterSet
import com.aboveware.abc80.terminal.CursorStyle
import com.aboveware.abc80.terminal.Tabulator
import com.aboveware.abc80.terminal.Terminal
import com.aboveware.abc80.terminal.TerminalCell
import com.aboveware.abc80.terminal.TerminalKeyboard
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalUnsignedTypes::class)
class KeyboardLayoutTest {

    private lateinit var context: Context

    @Test
    fun abc800LayoutHasSeparateFunctionAndNumericBlocks() {
        val abc80 = KeyboardViewController(context, R.xml.abc80)
        val abc800 = KeyboardViewController(context, R.xml.abc800)
        assertEquals(abc80.keys.size + 22, abc800.keys.size)
        assertTrue(abc800.keys.map { it.codes }.containsAll(abc80.keys.map { it.codes }))
        val functionKeys = abc800.keys.filter { it.codes.startsWith("abc800:pf") }
        assertEquals((1..8).map { "abc800:pf$it" }, functionKeys.map { it.codes })
        val keypad = abc800.keys.filter { it.codes.toIntOrNull() in 32..57 }
        assertEquals(
            listOf("7", "8", "9", "4", "5", "6", "-", "1", "2", "3", "0", "."),
            keypad.map { it.secondLabel.toString() }
        )
        val zero = abc800.keys.single { it.codes == "48" }
        val one = abc800.keys.single { it.codes == "49" }
        val enter = abc800.keys.single { it.codes == "13" }
        assertTrue(zero.width > one.width * 1.8)
        assertEquals(one.height * 2, enter.height)
        assertEquals(setOf("CAPS", "LOCK"), abc800.keys.single { it.ledId == "upper_case" }.let {
            setOf(it.label.toString(), it.secondLabel.toString())
        })
    }

    @Test
    fun abc800LayoutFitsWithoutOverlappingKeysAfterResize() {
        for (width in listOf(480, 1080, 2400)) {
            val layout = KeyboardViewController(context, R.xml.abc800)
            layout.resize(width)
            for ((index, key) in layout.keys.withIndex()) {
                assertTrue(key.x >= 0 && key.y >= 0, "Negative key position: ${key.codes}")
                assertTrue(key.x + key.width <= width, "Key outside width $width: ${key.codes}")
                assertTrue(key.y + key.height <= layout.height, "Key outside height: ${key.codes}")
                val bounds = Rect(key.x, key.y, key.x + key.width, key.y + key.height)
                for (other in layout.keys.drop(index + 1)) {
                    assertFalse(Rect.intersects(bounds, Rect(
                        other.x, other.y, other.x + other.width, other.y + other.height
                    )), "Overlapping keys at width $width: ${key.codes} / ${other.codes}")
                }
            }
            val pf1 = layout.keys.single { it.codes == "abc800:pf1" }
            for (number in listOf(3, 5, 7)) {
                assertEquals(pf1.x, layout.keys.single { it.codes == "abc800:pf$number" }.x)
            }
        }
    }

    @Test
    fun abc800KeypadAndFunctionKeysDeliverMachineCodes() {
        val terminal = MockTerminal()
        val keyboard = ABC80TerminalKeyboard(terminal)
        val layout = KeyboardViewController(context, R.xml.abc800)
        for (key in layout.keys.filter { it.codes.startsWith("abc800:pf") || it.codes.toIntOrNull() != null }) {
            keyboard.handleKeyEvent(key.codes)
            val expected = if (key.codes.startsWith("abc800:pf")) {
                191 + key.codes.removePrefix("abc800:pf").toInt()
            } else {
                key.codes.toInt()
            }
            assertEquals(expected, terminal.sentChars.last().code, key.codes)
        }
    }

    @Test
    fun abc800KeyboardDrawsCreamAndOrangeKeys() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = KeyboardView(context, null, 0, 0).apply {
                keyboardXmlResId = R.xml.abc800
                measure(
                    View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.AT_MOST)
                )
                layout(0, 0, measuredWidth, measuredHeight)
            }
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                fun backgroundPixel(codes: String): Int {
                    val key = view.keyboardViewController.keys.single { it.codes == codes }
                    return bitmap.getPixel(key.x + key.width / 2, key.y + key.height * 3 / 4)
                }
                val cream = backgroundPixel("abc80:38")
                val orange = backgroundPixel("abc800:pf1")
                assertTrue(android.graphics.Color.green(cream) > 150)
                assertTrue(android.graphics.Color.red(orange) > android.graphics.Color.green(orange) * 1.5)
                assertTrue(android.graphics.Color.green(cream) > android.graphics.Color.green(orange))
                for (codes in listOf("45", "46")) {
                    val key = view.keyboardViewController.keys.single { it.codes == codes }
                    val inkRows = (key.height / 8 until key.height * 3 / 4).count { y ->
                        (key.width / 4 until key.width * 3 / 4).any { x ->
                            val pixel = bitmap.getPixel(key.x + x, key.y + y)
                            android.graphics.Color.red(pixel) < 45 &&
                                android.graphics.Color.green(pixel) < 45 &&
                                android.graphics.Color.blue(pixel) < 45
                        }
                    }
                    assertTrue(inkRows in 1..key.height / 3, "Oversized keypad punctuation: $codes")
                }
                context.cacheDir.resolve("abc800-keyboard.png").outputStream().use {
                    assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

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
    fun abc80LayoutDeliversAllFourCodeColumns() {
        val terminal = MockTerminal()
        val terminalKeyboard = ABC80TerminalKeyboard(terminal)
        val keyboard = Keyboard().apply {
            onKeyCodes = terminalKeyboard::handleKeyEvent
            onKeyUpCodes = terminalKeyboard::handleKeyRelease
        }
        val layout = KeyboardViewController(context, R.xml.abc80)
        val shift = layout.keys.first { it.codes == "0,0" }
        val control = layout.keys.single { it.codes == "7,1" }
        val keys = layout.keys.filter { it.codes.startsWith("abc80:") }
        assertEquals(abc80KeyTable.keys - 66, keys.map { it.codes.substringAfter(":").toInt() }.toSet())
        for (key in keys) {
            val codes = abc80KeyTable.getValue(key.codes.substringAfter(":").toInt())
            for ((isShift, isControl, expected) in listOf(
                Triple(false, false, codes.unshifted),
                Triple(true, false, codes.shifted),
                Triple(false, true, codes.controlled),
                Triple(true, true, codes.shiftControlled)
            )) {
                if (isShift) layout.handleKeyPress(keyboard, shift)
                if (isControl) layout.handleKeyPress(keyboard, control)
                val previous = terminal.sentChars.size
                layout.handleKeyPress(keyboard, key)
                assertEquals(previous + 1, terminal.sentChars.size)
                assertEquals(expected, terminal.sentChars.last().code, "key=${key.codes}")
                layout.handleKeyRelease(keyboard, key)
                keyboard.onKeyUp(shift.codes)
                keyboard.onKeyUp(control.codes)
            }
        }
    }

    @Test
    fun abc80StickyControlAtDeliversNulAndResetsAfterUse() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val terminal = MockTerminal()
            val terminalKeyboard = ABC80TerminalKeyboard(terminal)
            val keyboard = Keyboard().apply {
                onKeyCodes = terminalKeyboard::handleKeyEvent
                onKeyUpCodes = terminalKeyboard::handleKeyRelease
            }
            val view = KeyboardView(context, null).apply {
                keyboardXmlResId = R.xml.abc80
                this.keyboard = keyboard
                measure(
                    View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
                )
                layout(0, 0, measuredWidth, measuredHeight)
            }
            fun tap(key: KeyboardViewController.Key) {
                val now = SystemClock.uptimeMillis()
                for (action in listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP)) {
                    val event = MotionEvent.obtain(
                        now, SystemClock.uptimeMillis(), action,
                        key.x + key.width / 2f + view.paddingLeft,
                        key.y + key.height / 2f + view.paddingTop, 0
                    )
                    try {
                        assertTrue(view.onTouchEvent(event))
                    } finally {
                        event.recycle()
                    }
                }
            }
            val control = view.keyboardViewController.keys.single { it.codes == "7,1" }
            val at = view.keyboardViewController.keys.single { it.codes == "abc80:12" }
            assertTrue(control.sticky)
            tap(control)
            assertTrue(control.on)
            assertTrue(terminal.sentChars.isEmpty())
            tap(at)
            assertEquals(listOf(0), terminal.sentChars.map { it.code })
            assertFalse(control.on, "CTRL must reset after the following key")
            tap(at)
            assertEquals(listOf(0, 0x60), terminal.sentChars.map { it.code })
        }
    }

    @Test
    fun abc80MultiTouchDeliversControlAtAndReleasesTheCorrectFinger() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val terminal = MockTerminal()
            val terminalKeyboard = ABC80TerminalKeyboard(terminal)
            val released = mutableListOf<String>()
            val keyboard = Keyboard().apply {
                onKeyCodes = terminalKeyboard::handleKeyEvent
                onKeyUpCodes = {
                    released.add(it)
                    terminalKeyboard.handleKeyRelease(it)
                }
            }
            val view = KeyboardView(context, null).apply {
                keyboardXmlResId = R.xml.abc80
                this.keyboard = keyboard
                setPadding(3, 7, 11, 0)
                measure(
                    View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
                )
                layout(0, 0, measuredWidth, measuredHeight)
            }
            val control = view.keyboardViewController.keys.single { it.codes == "7,1" }
            val at = view.keyboardViewController.keys.single { it.codes == "abc80:12" }
            val downTime = SystemClock.uptimeMillis()
            fun touch(action: Int, keys: List<KeyboardViewController.Key>) {
                val properties = keys.indices.map { index ->
                    MotionEvent.PointerProperties().apply {
                        id = index
                        toolType = MotionEvent.TOOL_TYPE_FINGER
                    }
                }.toTypedArray()
                val coordinates = keys.map { key ->
                    MotionEvent.PointerCoords().apply {
                        x = key.x + key.width / 2f + view.paddingLeft
                        y = key.y + key.height / 2f + view.paddingTop
                        pressure = 1f
                        size = 1f
                    }
                }.toTypedArray()
                val event = MotionEvent.obtain(
                    downTime, SystemClock.uptimeMillis(), action, keys.size,
                    properties, coordinates, 0, 0, 1f, 1f, 0, 0, 0, 0
                )
                try {
                    assertTrue(view.onTouchEvent(event))
                } finally {
                    event.recycle()
                }
            }
            touch(MotionEvent.ACTION_DOWN, listOf(control))
            touch(MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                listOf(control, at))
            assertEquals(listOf(0), terminal.sentChars.map { it.code }, "CTRL + É/@ must send NUL without a move event")
            assertTrue(control.pressed)
            assertTrue(at.pressed)
            touch(MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),
                listOf(control, at))
            assertTrue(control.pressed, "Releasing the second finger must not release CTRL")
            assertFalse(at.pressed)
            assertTrue("abc80:12" in released)
            touch(MotionEvent.ACTION_UP, listOf(control))
            assertFalse(control.pressed)
        }
    }

    @Test
    fun abc80UpperCaseKeyTogglesItsRedLed() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val terminal = MockTerminal()
            val ledMap = ABC80TerminalKeyboard(terminal).leds
            val keyboard = Keyboard().apply {
                leds = ledMap
                onCharacter = terminal::onKeyEvent
            }
            val view = KeyboardView(context, null).apply {
                keyboardXmlResId = R.xml.abc80
                this.keyboard = keyboard
                measure(
                    View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(500, View.MeasureSpec.EXACTLY)
                )
                layout(0, 0, measuredWidth, measuredHeight)
            }
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            try {
                view.draw(Canvas(bitmap))
                val key = view.keyboardViewController.keys.single { it.ledId == "upper_case" }
                assertEquals(setOf("Upper", "Case"), setOf(key.label?.toString(), key.secondLabel?.toString()))
                assertFalse(key.isLed)
                assertEquals(android.graphics.Color.RED, key.ledColor)
                val x = key.x + key.width / 2f + view.paddingLeft
                val y = key.y + key.height / 2f + view.paddingTop
                fun touch(action: Int) {
                    val now = SystemClock.uptimeMillis()
                    val event = MotionEvent.obtain(now, now, action, x, y, 0)
                    try {
                        assertTrue(view.onTouchEvent(event))
                    } finally {
                        event.recycle()
                    }
                }
                touch(MotionEvent.ACTION_DOWN)
                assertTrue(key.pressed)
                assertTrue(android.R.attr.state_pressed in key.currentDrawableState)
                assertTrue(ledMap.getValue("upper_case").isOn)
                touch(MotionEvent.ACTION_UP)
                assertFalse(key.pressed)
                assertTrue(ledMap.getValue("upper_case").isOn)
                touch(MotionEvent.ACTION_DOWN)
                assertTrue(key.pressed)
                assertFalse(ledMap.getValue("upper_case").isOn)
                touch(MotionEvent.ACTION_UP)
                assertFalse(key.pressed)
                assertFalse(ledMap.getValue("upper_case").isOn)
                assertTrue(terminal.sentChars.isEmpty(), "Upper Case must not send Escape")
            } finally {
                bitmap.recycle()
            }
        }
    }
}
