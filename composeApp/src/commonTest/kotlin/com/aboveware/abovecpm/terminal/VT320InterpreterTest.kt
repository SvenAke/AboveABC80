package com.aboveware.abovecpm.terminal

import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VT320InterpreterTest {

    companion object {
        private val lock = Any()
    }

    @BeforeTest
    fun setup() = synchronized(lock) {
        if (!CharacterSet.isLoaded()) {
            val paths = listOf(
                "src/commonMain/composeResources/files/",
                "composeApp/src/commonMain/composeResources/files/",
                "../composeApp/src/commonMain/composeResources/files/"
            )
            var mainRom: ByteArray? = null
            var vt52Rom: ByteArray? = null

            for (path in paths) {
                val f1 = File(path + "23-054E7.bin")
                val f2 = File(path + "vt52rom.bin")
                if (f1.exists() && f2.exists()) {
                    mainRom = f1.readBytes()
                    vt52Rom = f2.readBytes()
                    break
                }
            }

            if (mainRom == null) {
                File(".").walkTopDown().forEach { file ->
                    if (file.name == "23-054E7.bin") {
                        mainRom = file.readBytes()
                        val vt52 = File(file.parent, "vt52rom.bin")
                        if (vt52.exists()) vt52Rom = vt52.readBytes()
                    }
                }
            }

            if (mainRom != null && vt52Rom != null) {
                CharacterSet.initForTests(mainRom, vt52Rom)
            }
        }
        VT320Settings.restoreDefaults()
        VT320Settings.nationalOnly = 0
        VT320Settings.characterSetMode = 1
        VT320Settings.nationalReplacement = false
        VT320Settings.operatingMode = "ANSI"
        TerminalManager.operatingMode = TerminalManager.OperatingMode.ANSI
        TerminalManager.autoUppercase = false
        VT320Settings.terminalType = "VT320"
    }

    private fun VT320.putChars(s: String) {
        s.forEach { putChar(it) }
    }

    private fun readResponse(vt: VT320): String {
        val sb = StringBuilder()
        while (vt.hasChar()) {
            sb.append(vt.getChar().toChar())
        }
        return sb.toString()
    }

    @Test
    fun testESC_Hash_LineAttributes() = synchronized(lock) {
        val vt = VT320()
        vt.cursorY = 2

        // DECDHL - Double Height Line (Top)
        vt.putChars("\u001B#3")
        assertEquals(VT320.LineAttribute.DOUBLE_HEIGHT_TOP, vt.lineAttributes[2])

        // DECDHL - Double Height Line (Bottom)
        vt.putChars("\u001B#4")
        assertEquals(VT320.LineAttribute.DOUBLE_HEIGHT_BOTTOM, vt.lineAttributes[2])

        // DECDWL - Double Width Line
        vt.putChars("\u001B#6")
        assertEquals(VT320.LineAttribute.DOUBLE_WIDTH, vt.lineAttributes[2])

        // DECSWL - Single Width Line
        vt.putChars("\u001B#5")
        assertEquals(VT320.LineAttribute.NORMAL, vt.lineAttributes[2])
    }

    @Test
    fun testCSI_TBC_TabClear() = synchronized(lock) {
        val vt = VT320()
        // Set a tab at col 10
        vt.cursorX = 10
        vt.putChars("\u001BH") // ESC H - HTS
        assertTrue(vt.tabulator.stops[10])

        // TBC 0 - Clear tab at current column
        vt.cursorX = 10
        vt.putChars("\u001B[0g")
        assertFalse(vt.tabulator.stops[10])

        // Restore tab
        vt.putChars("\u001BH")
        assertTrue(vt.tabulator.stops[10])

        // TBC 3 - Clear all tabs
        vt.putChars("\u001B[3g")
        for (i in 0 until 132) {
            assertFalse(vt.tabulator.stops[i], "Tab at $i should be cleared")
        }
    }

    @Test
    fun testCSI_DA_DeviceAttributes() = synchronized(lock) {
        val vt = VT320()

        // Primary DA (CSI c)
        vt.putChars("\u001B[c")
        val resp = readResponse(vt)
        assertTrue(resp.startsWith("\u001B[?63;")) // VT320 response

        // Secondary DA (CSI > c)
        vt.putChars("\u001B[>c")
        val respSec = readResponse(vt)
        assertEquals("\u001B[>1;10;0c", respSec)

        // Third-level DA (CSI = c)
        vt.putChars("\u001B[=c")
        val respThird = readResponse(vt)
        assertEquals("\u001BP!|00000000\u001B\\", respThird)
    }

    @Test
    fun testCSI_DECLL_LEDs() = synchronized(lock) {
        val vt = VT320()
        // Ps = 1: LED 1 on
        vt.putChars("\u001B[1q")
    }

    @Test
    fun testCSI_DECSCUSR_CursorStyle() = synchronized(lock) {
        val vt = VT320()

        // Block (1 or 2 or 0)
        vt.putChars("\u001B[1 q")
        assertEquals(CursorStyle.BLOCK, vt.cursorStyle)

        // Underline (3 or 4)
        vt.putChars("\u001B[3 q")
        assertEquals(CursorStyle.UNDERLINE, vt.cursorStyle)
    }

    @Test
    fun testCSI_DSR_CursorPosition() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("\u001B[5;12H") // Move to row 5, col 12

        vt.putChars("\u001B[6n") // DSR 6
        val resp = readResponse(vt)
        assertEquals("\u001B[5;12R", resp)
    }

    @Test
    fun testCSI_DECCOLM_ColumnMode() = synchronized(lock) {
        val vt = VT320()
        vt.allow80or132Mode = true

        // 132 columns
        vt.putChars("\u001B[?3h")
        assertEquals(132, vt.columns)

        // 80 columns
        vt.putChars("\u001B[?3l")
        assertEquals(80, vt.columns)
    }

    @Test
    fun testCSI_DECSCLM_ScrollMode() = synchronized(lock) {
        val vt = VT320()

        // Smooth scroll
        vt.putChars("\u001B[?4h")
        assertTrue(vt.smoothScroll)
        assertTrue(VT320Settings.scrollMode)

        // Jump scroll
        vt.putChars("\u001B[?4l")
        assertFalse(vt.smoothScroll)
        assertFalse(VT320Settings.scrollMode)
    }

    @Test
    fun testCSI_DECSCNM_ScreenMode() = synchronized(lock) {
        val vt = VT320()

        // Reverse
        vt.putChars("\u001B[?5h")
        assertTrue(vt.screenReverse)
        assertEquals(1, VT320Settings.screenDisplayType)

        // Normal
        vt.putChars("\u001B[?5l")
        assertFalse(vt.screenReverse)
        assertEquals(0, VT320Settings.screenDisplayType)
    }

    @Test
    fun testCSI_DECAWN_AutoWrap() = synchronized(lock) {
        val vt = VT320()

        // On
        vt.putChars("\u001B[?7h")
        assertTrue(vt.autoWrap)

        // Off
        vt.putChars("\u001B[?7l")
        assertFalse(vt.autoWrap)
    }

    @Test
    fun testCSI_DECARM_AutoRepeat() = synchronized(lock) {
        val vt = VT320()

        // On
        vt.putChars("\u001B[?8h")
        assertTrue(vt.autoRepeatMode)
        assertTrue(VT320Settings.autoRepeat)

        // Off
        vt.putChars("\u001B[?8l")
        assertFalse(vt.autoRepeatMode)
        assertFalse(VT320Settings.autoRepeat)
    }

    @Test
    fun testCSI_DECTCEM_TextCursor() = synchronized(lock) {
        val vt = VT320()

        // Visible
        vt.putChars("\u001B[?25h")
        assertTrue(vt.cursorVisible)

        // Hidden
        vt.putChars("\u001B[?25l")
        assertFalse(vt.cursorVisible)
    }

    @Test
    fun testCSI_SRM_LocalEcho() = synchronized(lock) {
        val vt = VT320()

        // Set (Local echo off)
        vt.putChars("\u001B[12h")
        assertFalse(vt.localEcho)
        assertFalse(VT320Settings.localEcho)

        // Reset (Local echo on)
        vt.putChars("\u001B[12l")
        assertTrue(vt.localEcho)
        assertTrue(VT320Settings.localEcho)
    }

    @Test
    fun testCSI_DECAUPSS_UserPreferredSet() = synchronized(lock) {
        val vt = VT320()

        // 1. Assign ISO Latin-1 (1)
        vt.putChars("\u001B[1&u")
        assertEquals(1, VT320Settings.userPreferredCharacterSet)
        assertFalse(TerminalManager.userPreferredCharacterSetIsDECSupplementalGraphic)

        // 2. Request current UPSS (DECRQUPSS)
        vt.putChars("\u001B[&u")
        assertEquals("\u001BP!uA\u001B\\", readResponse(vt))

        // 3. Assign DEC Supplemental (0)
        vt.putChars("\u001B[0&u")
        assertEquals(0, VT320Settings.userPreferredCharacterSet)
        assertTrue(TerminalManager.userPreferredCharacterSetIsDECSupplementalGraphic)

        // 4. Request again
        vt.putChars("\u001B[&u")
        assertEquals("\u001BP!u<\u001B\\", readResponse(vt))
    }

    @Test
    fun testESC_SaveRestoreCursor() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC") // (3,0)
        vt.putChars("\u001B7") // DECSC
        vt.putChars("DEF") // (6,0)
        vt.putChars("\u001B8") // DECRC
        assertEquals(3, vt.cursorX)
        assertEquals(0, vt.cursorY)
    }

    @Test
    fun testCSI_DCH_DeleteChar() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABCDEF")
        vt.cursorX = 2 // at 'C'

        // Delete 2 characters (C and D)
        vt.putChars("\u001B[2P")

        assertEquals('A', vt.screen[0][0].char)
        assertEquals('B', vt.screen[0][1].char)
        assertEquals('E', vt.screen[0][2].char)
        assertEquals('F', vt.screen[0][3].char)
        assertEquals(' ', vt.screen[0][4].char)
    }

    @Test
    fun testCSI_DECSED_SelectiveEraseInDisplay() = synchronized(lock) {
        val vt = VT320()
        // 1. Setup screen with some protected and some unprotected rows
        vt.putChars("\u001B[1\"q") // Protected
        vt.putChars("PROT1\r\n")
        vt.putChars("\u001B[0\"q") // Unprotected
        vt.putChars("ERAS2\r\n")
        vt.putChars("\u001B[1\"q") // Protected
        vt.putChars("PROT3")

        // 2. Selective Erase Display CSI ? 2 J
        vt.putChars("\u001B[?2J")

        assertEquals('P', vt.screen[0][0].char)
        assertEquals('1', vt.screen[0][4].char)

        assertEquals(' ', vt.screen[1][0].char) // ERAS2 should be gone

        assertEquals('P', vt.screen[2][0].char)
        assertEquals('3', vt.screen[2][4].char)
    }

    @Test
    fun testCSI_DECSTBM_ScrollingRegion() = synchronized(lock) {
        val vt = VT320()

        // Default: full screen
        assertEquals(0, vt.topMargin)
        assertEquals(Terminal.DEFAULT_HEIGHT - 1, vt.bottomMargin)

        // CSI 5 ; 15 r -> Rows 5 to 15 (1-based)
        vt.putChars("\u001B[5;15r")
        assertEquals(4, vt.topMargin)
        assertEquals(14, vt.bottomMargin)
        assertEquals(0, vt.cursorX)
        assertEquals(0, vt.cursorY)

        // CSI r -> Reset to full screen
        vt.putChars("\u001B[r")
        assertEquals(0, vt.topMargin)
        assertEquals(Terminal.DEFAULT_HEIGHT - 1, vt.bottomMargin)
    }

    @Test
    fun testCSI_DECTST_ConfidenceTest() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC")
        // DECTST 1 - Power-up reset
        vt.putChars("\u001B[1y")
        assertEquals(0, vt.cursorX)
        assertEquals(' ', vt.screen[0][0].char)
    }

    @Test
    fun testESC_DECALN_AlignmentTest() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("\u001B#8")
        assertEquals('E', vt.screen[0][0].char)
        assertEquals('E', vt.screen[vt.rows - 1][vt.columns - 1].char)
        assertEquals(0, vt.cursorX)
        assertEquals(0, vt.cursorY)
    }

    @Test
    fun testCSI_DECOM_OriginMode() = synchronized(lock) {
        val vt = VT320()

        // Setup margins: row 5 to 15
        vt.putChars("\u001B[5;15r")
        assertEquals(4, vt.topMargin)
        assertEquals(14, vt.bottomMargin)

        // Default: DECOM Reset (absolute to screen)
        vt.putChars("\u001B[1;1H")
        assertEquals(0, vt.cursorY)

        // DECOM Set: CSI ? 6 h (relative to margins)
        vt.putChars("\u001B[?6h")
        assertTrue(vt.originMode)
        // Cursor should have moved to top margin
        assertEquals(4, vt.cursorY)

        // Position 1;1 should now be row 5 (index 4)
        vt.putChars("\u001B[1;1H")
        assertEquals(4, vt.cursorY)

        // Position 2;1 should be row 6 (index 5)
        vt.putChars("\u001B[2;1H")
        assertEquals(5, vt.cursorY)

        // Try to position outside margins while DECOM is set - should be clamped
        vt.putChars("\u001B[20;1H")
        assertEquals(14, vt.cursorY) // Clamped to bottom margin

        // DECOM Reset: CSI ? 6 l
        vt.putChars("\u001B[?6l")
        assertFalse(vt.originMode)
        // Cursor should move to 1;1 of screen
        assertEquals(0, vt.cursorY)

        // Position 1;1 should be screen top again
        vt.putChars("\u001B[1;1H")
        assertEquals(0, vt.cursorY)
    }

    @Test
    fun testCSI_IRM_InsertMode() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("AB")
        vt.cursorX = 1 // between A and B

        // Default: Replace mode. 'X' should overwrite 'B'.
        vt.putChar('X')
        assertEquals("AX", vt.screen[0].take(2).map { it.char }.joinToString(""))

        // IRM Set: CSI 4 h (Insert Mode)
        vt.putChars("\u001B[1;1H") // back to start
        vt.putChars("\u001B[4h")
        assertTrue(vt.insertMode)

        vt.putChar('Y')
        // Should be "YAX"
        assertEquals("YAX", vt.screen[0].take(3).map { it.char }.joinToString(""))

        // IRM Reset: CSI 4 l (Replacement Mode)
        vt.putChars("\u001B[1;1H")
        vt.putChars("\u001B[4l")
        assertFalse(vt.insertMode)

        vt.putChar('Z')
        // Should be "ZAX"
        assertEquals("ZAX", vt.screen[0].take(3).map { it.char }.joinToString(""))
    }

    @Test
    fun testCSI_IRM_LineEndPush() = synchronized(lock) {
        val vt = VT320()

        // 1. 80 Columns
        vt.setColumnMode(80)
        vt.putChars("\u001B[4h") // IRM On

        // Put 'A' at index 78 and 'B' at index 79 (columns 79 and 80)
        vt.putChars("\u001B[1;79H")
        vt.putChar('A')
        vt.putChar('B')
        assertEquals('A', vt.screen[0][78].char)
        assertEquals('B', vt.screen[0][79].char)

        // Move back to index 78 (column 79) and insert 'X'
        vt.putChars("\u001B[1;79H")
        vt.putChar('X')

        // Result: index 78='X', index 79='A'. 'B' was pushed off the screen.
        assertEquals('X', vt.screen[0][78].char)
        assertEquals('A', vt.screen[0][79].char)

        // 2. 132 Columns
        vt.allow80or132Mode = true
        vt.putChars("\u001B[?3h")
        vt.putChars("\u001B[4h")

        // Put 'C' at 130, 'D' at 131 (columns 131 and 132)
        vt.putChars("\u001B[1;131H")
        vt.putChar('C')
        vt.putChar('D')

        // Insert 'Y' at 130 (column 131)
        vt.putChars("\u001B[1;131H")
        vt.putChar('Y')

        // Result: 130='Y', 131='C'. 'D' was pushed off.
        assertEquals('Y', vt.screen[0][130].char)
        assertEquals('C', vt.screen[0][131].char)
    }

    @Test
    fun testCSI_IL_DL_LineInsertDelete() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("LINE1\r\nLINE2\r\nLINE3")
        vt.putChars("\u001B[2;1H") // Move to LINE2

        // IL - Insert 1 Line (CSI 1L)
        vt.putChars("\u001B[1L")
        assertEquals(' ', vt.screen[1][0].char)
        assertEquals('L', vt.screen[2][0].char) // LINE2 moved down

        // DL - Delete 1 Line (CSI 1M)
        vt.putChars("\u001B[1M")
        assertEquals('L', vt.screen[1][0].char) // LINE2 moved back up
        assertEquals('L', vt.screen[2][0].char) // LINE3 moved back up
    }

    @Test
    fun testCSI_ICH_InsertChar() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABCDEF")
        vt.cursorX = 2 // at 'C'

        // ICH - Insert 2 Characters (CSI 2@)
        vt.putChars("\u001B[2@")

        assertEquals('A', vt.screen[0][0].char)
        assertEquals('B', vt.screen[0][1].char)
        assertEquals(' ', vt.screen[0][2].char)
        assertEquals(' ', vt.screen[0][3].char)
        assertEquals('C', vt.screen[0][4].char)
        assertEquals('D', vt.screen[0][5].char)
        // E and F should be pushed right
    }

    @Test
    fun testCSI_ECH_EraseChar() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABCDEF")
        vt.cursorX = 2 // at 'C'

        // ECH - Erase 2 Characters (CSI 2X)
        vt.putChars("\u001B[2X")

        assertEquals('A', vt.screen[0][0].char)
        assertEquals('B', vt.screen[0][1].char)
        assertEquals(' ', vt.screen[0][2].char)
        assertEquals(' ', vt.screen[0][3].char)
        assertEquals('E', vt.screen[0][4].char) // E and F stay in place
        assertEquals('F', vt.screen[0][5].char)
        assertEquals(2, vt.cursorX) // Cursor does not move
    }

    @Test
    fun testCSI_EL_EraseInLine() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABCDEF")
        vt.cursorX = 2 // at 'C'

        // EL 0 - Erase to end of line
        vt.putChars("\u001B[0K")
        assertEquals("AB    ", vt.screen[0].take(6).map { it.char }.joinToString(""))

        // Reset and test EL 1
        vt.putChars("\rABCDEF")
        vt.cursorX = 2
        vt.putChars("\u001B[1K")
        assertEquals("   DEF", vt.screen[0].take(6).map { it.char }.joinToString(""))

        // Reset and test EL 2
        vt.putChars("\rABCDEF")
        vt.putChars("\u001B[2K")
        assertEquals("      ", vt.screen[0].take(6).map { it.char }.joinToString(""))
    }

    @Test
    fun testCSI_DECSEL_SelectiveEraseInLine() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("\u001B[1\"q") // Protected
        vt.putChars("AB")
        vt.putChars("\u001B[0\"q") // Unprotected
        vt.putChars("CD")

        // DECSEL 2 - Erase entire line selectively
        vt.putChars("\u001B[?2K")
        assertEquals('A', vt.screen[0][0].char)
        assertEquals('B', vt.screen[0][1].char)
        assertEquals(' ', vt.screen[0][2].char)
        assertEquals(' ', vt.screen[0][3].char)
    }

    @Test
    fun testCSI_ED_EraseInDisplay() = synchronized(lock) {
        val vt = VT320()

        fun fill() {
            vt.putChars("\u001B[H") // Home
            vt.putChars("123\r\n456\r\n789")
        }

        // ED 0 - Erase from cursor to end
        fill()
        vt.putChars("\u001B[2;2H") // Move to '5'
        vt.putChars("\u001B[0J")
        assertEquals("123", vt.screen[0].take(3).map { it.char }.joinToString(""))
        assertEquals("4  ", vt.screen[1].take(3).map { it.char }.joinToString(""))
        assertEquals("   ", vt.screen[2].take(3).map { it.char }.joinToString(""))

        // ED 1 - Erase from start to cursor
        fill()
        vt.putChars("\u001B[2;2H") // Move to '5'
        vt.putChars("\u001B[1J")
        assertEquals("   ", vt.screen[0].take(3).map { it.char }.joinToString(""))
        assertEquals("  6", vt.screen[1].take(3).map { it.char }.joinToString(""))
        assertEquals("789", vt.screen[2].take(3).map { it.char }.joinToString(""))

        // ED 2 - Erase all
        fill()
        vt.putChars("\u001B[2J")
        assertEquals("   ", vt.screen[0].take(3).map { it.char }.joinToString(""))
        assertEquals("   ", vt.screen[1].take(3).map { it.char }.joinToString(""))
        assertEquals("   ", vt.screen[2].take(3).map { it.char }.joinToString(""))
    }

    @Test
    fun testCSI_CursorPositioning() = synchronized(lock) {
        val vt = VT320()

        // CUP - CSI 10 ; 20 H
        vt.putChars("\u001B[10;20H")
        assertEquals(19, vt.cursorX)
        assertEquals(9, vt.cursorY)

        // HVP - CSI 5 ; 5 f
        vt.putChars("\u001B[5;5f")
        assertEquals(4, vt.cursorX)
        assertEquals(4, vt.cursorY)

        // CUU - CSI 2 A (Up)
        vt.putChars("\u001B[2A")
        assertEquals(2, vt.cursorY)

        // CUD - CSI 3 B (Down)
        vt.putChars("\u001B[3B")
        assertEquals(5, vt.cursorY)

        // CUF - CSI 10 C (Forward)
        vt.putChars("\u001B[10C")
        assertEquals(14, vt.cursorX)

        // CUB - CSI 5 D (Backward)
        vt.putChars("\u001B[5D")
        assertEquals(9, vt.cursorX)
    }

    @Test
    fun testCSI_DECTCEM_TextCursorEnable() = synchronized(lock) {
        val vt = VT320()

        // CSI ? 25 l (Reset - Disable)
        vt.putChars("\u001B[?25l")
        assertFalse(vt.cursorVisible)

        // CSI ? 25 h (Set - Enable)
        vt.putChars("\u001B[?25h")
        assertTrue(vt.cursorVisible)
    }

    @Test
    fun testCSI_MC_MediaCopy() = synchronized(lock) {
        val vt = VT320()

        // 1. ANSI MC
        vt.putChars("\u001B[5i")
        assertEquals(PrinterStatus.CONTROLLER, vt.printerStatus)
        vt.putChars("\u001B[4i")
        assertEquals(PrinterStatus.READY, vt.printerStatus)

        // 2. Private MC
        vt.putChars("\u001B[?5i")
        assertEquals(PrinterStatus.AUTO, vt.printerStatus)
        vt.putChars("\u001B[?4i")
        assertEquals(PrinterStatus.READY, vt.printerStatus)
    }

    @Test
    fun testCSI_DECPFF_DECPEX() = synchronized(lock) {
        val vt = VT320()

        // DECPFF - Print Form Feed (Mode 18)
        vt.putChars("\u001B[?18h")
        assertTrue(VT320Settings.printTerminator)
        vt.putChars("\u001B[?18l")
        assertFalse(VT320Settings.printTerminator)

        // DECPEX - Printer Extent (Mode 19)
        vt.putChars("\u001B[?19h") // Set -> Full Screen (false in settings)
        assertFalse(VT320Settings.printerScreenSize)
        vt.putChars("\u001B[?19l") // Reset -> Scrolling Region (true in settings)
        assertTrue(VT320Settings.printerScreenSize)
    }

    @Test
    fun testCSI_KAM_KeyboardAction() = synchronized(lock) {
        val vt = VT320()

        // Locked: CSI 2 h
        vt.putChars("\u001B[2h")
        assertTrue(vt.keyboardLocked)

        // Unlocked: CSI 2 l
        vt.putChars("\u001B[2l")
        assertFalse(vt.keyboardLocked)
    }

    @Test
    fun testCSI_LNM_NewLineMode() = synchronized(lock) {
        val vt = VT320()

        // Set: CSI 20 h
        vt.putChars("\u001B[20h")
        assertTrue(vt.newLineMode)

        // Reset: CSI 20 l
        vt.putChars("\u001B[20l")
        assertFalse(vt.newLineMode)
    }

    @Test
    fun testCSI_DECKBUM_KeyboardUsageMode() = synchronized(lock) {
        val vt = VT320()

        // Data Processing: CSI ? 68 h
        vt.putChars("\u001B[?68h")
        assertTrue(vt.keyboardUsageMode)

        // Typewriter: CSI ? 68 l
        vt.putChars("\u001B[?68l")
        assertFalse(vt.keyboardUsageMode)
    }

    @Test
    fun testESC_KeypadModes() = synchronized(lock) {
        val vt = VT320()

        // DECKPAM - Application Keypad (ESC =)
        vt.putChars("\u001B=")
        assertTrue(vt.keypadMode)

        // DECKPNM - Numeric Keypad (ESC >)
        vt.putChars("\u001B>")
        assertFalse(vt.keypadMode)
    }

    @Test
    fun testDCS_DECUDK() = synchronized(lock) {
        val vt = VT320()

        // 1. Define F6 (Key 17) as "ABC" (414243)
        // DCS 0 ; 1 | 17 / 41 42 43 ST (Pc=0: clear all, Pw=1: no lock)
        vt.putChars("\u001BP0;1|17/414243\u001B\\")

        val f6Udk = (vt.keyboard as VT320Keyboard).getUdk(17)
        assertEquals("ABC", f6Udk)
        assertFalse(vt.udkLocked)

        // 2. Lock UDKs
        // DCS 1 ; 0 | ST (Pc=1: keep current, Pw=0: lock)
        vt.putChars("\u001BP1;0|\u001B\\")
        assertTrue(vt.udkLocked)

        // 3. DSR 25 - UDK Status Report (Private mode prefix '?' is required)
        vt.putChars("\u001B[?25n")
        val resp = readResponse(vt)
        assertEquals("\u001B[?21n", resp) // ?21n = Locked
    }

    @Test
    fun testCSI_DSR_OperatingStatus() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("\u001B[5n")
        assertEquals("\u001B[0n", readResponse(vt))
    }

    @Test
    fun testCSI_DSR_PrinterStatus() = synchronized(lock) {
        val vt = VT320()
        VT320Settings.printerToHost = 1
        vt.printerStatus = PrinterStatus.READY
        vt.putChars("\u001B[?15n")
        assertEquals("\u001B[?10n", readResponse(vt))
    }

    @Test
    fun testCSI_DSR_KeyboardLanguage() = synchronized(lock) {
        val vt = VT320()
        VT320Settings.keyboardLanguage = 11 // Swedish
        vt.putChars("\u001B[?26n")
        assertEquals("\u001B[?27;12n", readResponse(vt))
    }

    @Test
    fun testCSI_DSR_Special() = synchronized(lock) {
        val vt = VT320()

        // Macro space
        vt.putChars("\u001B[62n")
        assertEquals("\u001B[0*{", readResponse(vt))

        // Memory checksum
        vt.putChars("\u001B[63n")
        assertEquals("\u001BP63!~0000\u001B\\", readResponse(vt))
    }

    @Test
    fun testCSI_DECRQTSR_DECRSTS() = synchronized(lock) {
        val vt = VT320()

        // DECRQTSR 1 (Request Terminal State)
        vt.putChars($$"\u001B[1$u")
        val respState = readResponse(vt)
        assertTrue(respState.startsWith($$"\u001BP1$s"))

        // DECRSTS 1 (Request Status - Attributes)
        vt.putChars($$"\u001B[1$w")
        val respAttr = readResponse(vt)
        assertEquals($$"\u001BP1$r0m\u001B\\", respAttr)

        // DECRSTS invalid
        vt.putChars($$"\u001B[5$w")
        val respInvalid = readResponse(vt)
        assertEquals($$"\u001BP0$r\u001B\\", respInvalid)
    }

    @Test
    fun testCSI_DECRQPSR_DECRSPS() = synchronized(lock) {
        val vt = VT320()

        // 1. Request Tab Stops (DECRQPSR 2)
        vt.tabulator.clearAll()
        vt.tabulator.set(10, true)
        vt.tabulator.set(20, true)

        vt.putChars($$"\u001B[2$t")
        val resp = readResponse(vt)
        assertEquals($$"\u001BP2$u11/21\u001B\\", resp) // 1-based columns

        // 2. Restore Tab Stops (DECRSPS 2)
        vt.tabulator.clearAll()
        // Restore from previous response data: DCS 2 $ t 5 / 15 ST
        vt.putChars($$"\u001BP2$t5/15\u001B\\")
        assertTrue(vt.tabulator.stops[4])
        assertTrue(vt.tabulator.stops[14])
        assertFalse(vt.tabulator.stops[10])
    }

    @Test
    fun testCSI_DECRQM_ReportMode() = synchronized(lock) {
        val vt = VT320()

        // 1. ANSI Mode (SRM 12)
        vt.localEcho = true // SRM is Reset (2)
        vt.putChars($$"\u001B[12$p")
        assertEquals($$"\u001B[12;2$y", readResponse(vt))

        vt.localEcho = false // SRM is Set (1)
        vt.putChars($$"\u001B[12$p")
        assertEquals($$"\u001B[12;1$y", readResponse(vt))

        // 2. DEC Private Mode (DECSCNM 5)
        vt.screenReverse = false // Reset (2)
        vt.putChars($$"\u001B[?5$p")
        assertEquals($$"\u001B[?5;2$y", readResponse(vt))

        vt.screenReverse = true // Set (1)
        vt.putChars($$"\u001B[?5$p")
        assertEquals($$"\u001B[?5;1$y", readResponse(vt))

        vt.putChars($$"\u001B[?99$p")
        assertEquals($$"\u001B[?99;0$y", readResponse(vt))
    }

    @Test
    fun testDCS_DECRQSS_ReportSetting() = synchronized(lock) {
        val vt = VT320()

        // 1. Request SGR
        vt.putChars("\u001B[1;4m") // Bold, Underline
        vt.putChars($$"\u001BP$qm\u001B\\")
        assertEquals($$"\u001BP1$r0;1;4m\u001B\\", readResponse(vt))

        // 2. Request DECSTBM
        vt.putChars("\u001B[5;15r")
        vt.putChars($$"\u001BP$qr\u001B\\")
        assertEquals($$"\u001BP1$r5;15r\u001B\\", readResponse(vt))

        // 3. Request DECSCA
        vt.putChars("\u001B[1\"q")
        vt.putChars($$"\u001BP$q\"q\u001B\\")
        assertEquals($$"\u001BP1$r1\"q\u001B\\", readResponse(vt))

        // 4. Request DECSASD
        vt.putChars($$"\u001B[1$}")
        vt.putChars($$"\u001BP$q$}\u001B\\")
        assertEquals($$"\u001BP1$r1$}\u001B\\", readResponse(vt))

        // 5. Request DECSSDT
        vt.putChars($$"\u001B[2$~")
        vt.putChars($$"\u001BP$q$~\u001B\\")
        assertEquals($$"\u001BP1$r2$~\u001B\\", readResponse(vt))

        // 6. Request DECAUPSS
        vt.putChars("\u001B[1&u")
        vt.putChars($$"\u001BP$q&u\u001B\\")
        assertEquals($$"\u001BP1$r1&u\u001B\\", readResponse(vt))

        // 7. Invalid request
        vt.putChars($$"\u001BP$qXYZ\u001B\\")
        assertEquals($$"\u001BP0$rXYZ\u001B\\", readResponse(vt))
    }

    @Test
    fun testVT52_ModeTransitions() = synchronized(lock) {
        val vt = VT320()

        // 1. ANSI -> VT52 (CSI ? 2 l)
        vt.putChars("\u001B[?2l")
        assertEquals(52, vt.conformanceLevel)
        assertEquals(TerminalManager.OperatingMode.VT52, TerminalManager.operatingMode)

        // 2. VT52 -> ANSI (ESC <)
        vt.putChars("\u001B<")
        assertEquals(63, vt.conformanceLevel)
        assertEquals(TerminalManager.OperatingMode.ANSI, TerminalManager.operatingMode)
    }

    @Test
    fun testVT52_BasicSequences() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("\u001B[?2l") // Enter VT52

        // A - Up
        vt.cursorY = 5
        vt.putChars("\u001BA")
        assertEquals(4, vt.cursorY)

        // B - Down
        vt.putChars("\u001BB")
        assertEquals(5, vt.cursorY)

        // C - Right
        vt.cursorX = 10
        vt.putChars("\u001BC")
        assertEquals(11, vt.cursorX)

        // D - Left
        vt.putChars("\u001BD")
        assertEquals(10, vt.cursorX)

        // H - Home
        vt.putChars("\u001BH")
        assertEquals(0, vt.cursorX)
        assertEquals(0, vt.cursorY)

        // K - Erase to EOL
        vt.putChars("HELLO")
        vt.cursorX = 2
        vt.putChars("\u001BK")
        assertEquals('H', vt.screen[0][0].char)
        assertEquals('E', vt.screen[0][1].char)
        assertEquals(' ', vt.screen[0][2].char)

        // Y - Direct Address (Row 10, Col 20) -> 10+31=41(') , 20+31=51(3)
        vt.putChars("\u001BY)3")
        assertEquals(9, vt.cursorY)
        assertEquals(19, vt.cursorX)

        // Z - Identify
        vt.putChars("\u001BZ")
        assertEquals("\u001B/Z", readResponse(vt))

        // F/G - Graphics Mode
        vt.putChars("\u001BF")
        assertTrue(vt.vt52GraphicsMode)
        vt.putChars("\u001BG")
        assertFalse(vt.vt52GraphicsMode)
    }
}
