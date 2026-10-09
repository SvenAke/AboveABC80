package com.aboveware.aboveabc80.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aboveware.aboveabc80.R
import com.aboveware.aboveabc80.setAndroidContext
import com.aboveware.aboveabc80.terminal.ABC80TerminalKeyboard
import com.aboveware.aboveabc80.terminal.CharacterSet
import com.aboveware.aboveabc80.terminal.CursorStyle
import com.aboveware.aboveabc80.terminal.Tabulator
import com.aboveware.aboveabc80.terminal.Terminal
import com.aboveware.aboveabc80.terminal.TerminalCell
import com.aboveware.aboveabc80.terminal.TerminalKeyboard
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
