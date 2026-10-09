package com.aboveware.aboveabc80.terminal

import kotlin.test.Test
import kotlin.test.assertEquals

class ABC80TerminalTest {
    @Test
    fun abc80AndHitchCursorAddressingMoveWithoutPrintingParameters() {
        val terminal = ABC80()

        "\u001B=7 HI".forEach(terminal::putChar)

        assertEquals(23, terminal.cursorY)
        assertEquals(2, terminal.cursorX)
        assertEquals('H', terminal.screen[23][0].char)
        assertEquals('I', terminal.screen[23][1].char)

        "\u001BY7 ".forEach(terminal::putChar)

        assertEquals(23, terminal.cursorY)
        assertEquals(0, terminal.cursorX)
        assertEquals('H', terminal.screen[23][0].char)
    }

    @Test
    fun documentedControlCharactersMoveCursorAndClearScreen() {
        val terminal = ABC80()

        "ABC".forEach(terminal::putChar)
        terminal.putChar('\u0008')
        assertEquals(2, terminal.cursorX)

        terminal.putChar('\u000A')
        assertEquals(1, terminal.cursorY)
        terminal.putChar('\u000B')
        assertEquals(0, terminal.cursorY)
        terminal.putChar('\u000C')
        assertEquals(3, terminal.cursorX)
        terminal.putChar('\u000D')
        assertEquals(0, terminal.cursorX)
        terminal.putChar('\u001E')
        assertEquals(0, terminal.cursorX)
        assertEquals(0, terminal.cursorY)

        terminal.putChar('\u001A')

        assertEquals(' ', terminal.screen[0][0].char)
        assertEquals(0, terminal.cursorX)
        assertEquals(0, terminal.cursorY)
    }

    @Test
    fun tabStopsAtLastColumnWithoutWrapping() {
        val terminal = ABC80()
        terminal.putChar('\t')

        assertEquals(8, terminal.cursorX)

        terminal.cursorX = ABC80.WIDTH - 1
        terminal.cursorY = 4

        terminal.putChar('\t')

        assertEquals(ABC80.WIDTH - 1, terminal.cursorX)
        assertEquals(4, terminal.cursorY)
    }

    @Test
    fun bellAndAnswerBackAreHandled() {
        val terminal = ABC80()
        var bellCount = 0
        terminal.onBell = { bellCount++ }

        terminal.putChar('\u0007')
        terminal.putChar('\u0005')

        assertEquals(1, bellCount)
        assertEquals("aboveABC80\r".map(Char::code), buildList {
            while (terminal.hasChar()) add(terminal.getChar())
        })
    }

    @Test
    fun hitchBedroomStatusDoesNotClearRoomDescription() {
        val terminal = ABC80()
        terminal.cursorY = 4
        "It is pitch black.\r\n\r\n".forEach(terminal::putChar)
        val outputCursorY = terminal.cursorY

        "\u001Bj\u001BY  \u001Bp Bedroom\u001Bq\u001Bk".forEach(terminal::putChar)

        assertEquals("It is pitch black.", terminal.screen[4].take(18).joinToString("") { it.char.toString() })
        assertEquals(" Bedroom", terminal.screen[0].take(8).joinToString("") { it.char.toString() })
        assertEquals(true, terminal.screen[0][1].attr.inverse)
        assertEquals(outputCursorY, terminal.cursorY)
    }

    @Test
    fun lineFeedAtBottomScrollsAndEscapeCanRestartIncompleteAddress() {
        val terminal = ABC80()
        terminal.screen[0][0] = TerminalCell('A')
        terminal.screen[1][0] = TerminalCell('B')
        terminal.cursorY = ABC80.HEIGHT - 1

        terminal.putChar('\n')

        assertEquals('B', terminal.screen[0][0].char)
        assertEquals(' ', terminal.screen[ABC80.HEIGHT - 1][0].char)

        "\u001B=\u001BE".forEach(terminal::putChar)

        assertEquals('B', terminal.screen[0][0].char)
        assertEquals(0, terminal.cursorX)
        assertEquals(ABC80.HEIGHT - 1, terminal.cursorY)
    }
}
