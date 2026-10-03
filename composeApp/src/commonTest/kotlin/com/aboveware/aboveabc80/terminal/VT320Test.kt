package com.aboveware.aboveabc80.terminal

import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VT320Test {

    companion object {
        private val lock = Any()
    }

    @BeforeTest
    fun setup() = synchronized(lock) {
        if (!CharacterSet.isLoaded()) {
            // Try different paths to find resources in test environment
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
                    println("VT320Test: Loaded ROMs from $path")
                    break
                }
            }

            // Try recursive search if standard paths fail
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
        // Override for test consistency. New VT320() instances in tests will read these.
        VT320Settings.nationalOnly = 0
        VT320Settings.characterSetMode = 1 // 8-bit
        VT320Settings.nationalReplacement = false
        VT320Settings.operatingMode = "ANSI"
        TerminalManager.operatingMode = TerminalManager.OperatingMode.ANSI
        TerminalManager.autoUppercase = false
        VT320Settings.terminalType = "VT320"
        VT320Settings.answerBack = ""
    }

    private fun assertSent(vt: VT320, expected: String, msg: String) {
        vt.flush()
        val actual = StringBuilder()
        while (vt.hasChar()) {
            actual.append(vt.getChar().toChar())
        }
        assertEquals(expected, actual.toString(), msg)
    }

    private fun VT320.putChars(s: String) {
        s.forEach { putChar(it) }
    }

    @Test
    fun testPrintCharacter() = synchronized(lock) {
        val vt = VT320()
        vt.putChar('A')
        assertEquals(1, vt.cursorX)
        assertEquals(0, vt.cursorY)
        assertEquals('A', vt.screen[0][0].char)
    }

    @Test
    fun testC0_ENQ() = synchronized(lock) {
        val vt = VT320()
        VT320Settings.answerBack = "VT320-OK"
        vt.putChar('\u0005')
        assertTrue(vt.hasChar())
        assertEquals('V'.code, vt.getChar())
        assertEquals('T'.code, vt.getChar())
    }

    @Test
    fun testC0_BEL() = synchronized(lock) {
        val vt = VT320()
        var bellCalled = false
        vt.onBell = { bellCalled = true }
        vt.putChar('\u0007')
        assertTrue(bellCalled)
    }

    @Test
    fun testC0_BS() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("AB")
        assertEquals(2, vt.cursorX)
        vt.putChar('\u0008')
        assertEquals(1, vt.cursorX)
        // Check that it doesn't go below 0
        vt.putChar('\u0008')
        assertEquals(0, vt.cursorX)
        vt.putChar('\u0008')
        assertEquals(0, vt.cursorX)
    }

    @Test
    fun testC0_TAB() = synchronized(lock) {
        val vt = VT320()
        vt.putChar('\u0009')
        assertEquals(8, vt.cursorX)
        vt.putChar('\u0009')
        assertEquals(16, vt.cursorX)

        // Test tab with content
        vt.putChars("A\t")
        assertEquals(24, vt.cursorX)
    }

    @Test
    fun testC0_LF() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC")
        vt.putChar('\n')
        assertEquals(3, vt.cursorX) // Should NOT move to col 0 by default
        assertEquals(1, vt.cursorY)
    }

    @Test
    fun testC0_VT() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC")
        vt.putChar('\u000B')
        assertEquals(3, vt.cursorX)
        assertEquals(1, vt.cursorY)
    }

    @Test
    fun testC0_FF() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC")
        vt.putChar('\u000C')
        assertEquals(3, vt.cursorX)
        assertEquals(1, vt.cursorY)
    }

    @Test
    fun testC0_CR() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC")
        vt.putChar('\r')
        assertEquals(0, vt.cursorX)
        assertEquals(0, vt.cursorY)
    }

    @Test
    fun testC0_CRLF() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("A\r\nB")
        assertEquals(1, vt.cursorX)
        assertEquals(1, vt.cursorY)
        assertEquals('A', vt.screen[0][0].char)
        assertEquals('B', vt.screen[1][0].char)
    }

    @Test
    fun testC0_NewLineMode() = synchronized(lock) {
        val vt = VT320()
        vt.newLineMode = true
        vt.putChars("ABC")
        vt.putChar('\n')
        assertEquals(0, vt.cursorX)
        assertEquals(1, vt.cursorY)

        vt.putChars("DEF")
        vt.putChar('\u000B')
        assertEquals(0, vt.cursorX)
        assertEquals(2, vt.cursorY)

        vt.putChars("GHI")
        vt.putChar('\u000C')
        assertEquals(0, vt.cursorX)
        assertEquals(3, vt.cursorY)
    }

    @Test
    fun testC0_SO_SI() = synchronized(lock) {
        val vt = VT320()
        // verify they don't crash and leave state as NORMAL
        vt.putChar('\u000E') // SO
        vt.putChar('A')
        assertEquals('A', vt.screen[0][0].char)

        vt.putChar('\u000F') // SI
        vt.putChar('B')
        assertEquals('B', vt.screen[0][1].char)
    }

    @Test
    fun testC0_Unknown() = synchronized(lock) {
        val vt = VT320()
        val initialX = vt.cursorX
        val initialY = vt.cursorY

        // 0x00 NUL, 0x01 SOH, 0x02 STX, 0x03 ETX, 0x04 EOT, 0x06 ACK
        val unknown = charArrayOf('\u0000', '\u0001', '\u0002', '\u0003', '\u0004', '\u0006')
        for (c in unknown) {
            vt.putChar(c)
        }

        assertEquals(initialX, vt.cursorX)
        assertEquals(initialY, vt.cursorY)
        assertEquals(' ', vt.screen[0][0].char)
    }


    @Test
    fun testESC_IndexAndReverseIndex() = synchronized(lock) {
        val vt = VT320()
        // Index (ESC D) - moves cursor down
        vt.putChars("\u001BD")
        assertEquals(0, vt.cursorX)
        assertEquals(1, vt.cursorY)

        // Reverse Index (ESC M) - moves cursor up
        vt.putChars("\u001BM")
        assertEquals(0, vt.cursorX)
        assertEquals(0, vt.cursorY)
    }

    @Test
    fun testESC_NextLine() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC\u001BE")
        assertEquals(0, vt.cursorX)
        assertEquals(1, vt.cursorY)
    }

    @Test
    fun testESC_SaveAndRestoreCursor() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC") // cursor at (3, 0)
        vt.putChars("\u001B7") // Save
        vt.putChars("DEF") // cursor at (6, 0)
        vt.putChars("\u001B8") // Restore
        assertEquals(3, vt.cursorX)
        assertEquals(0, vt.cursorY)
    }

    @Test
    fun testCSI_CursorMovement() = synchronized(lock) {
        val vt = VT320()
        // CUP (Cursor Position) CSI 5;10H -> row 5, col 10 (1-based)
        vt.putChars("\u001B[5;10H")
        assertEquals(9, vt.cursorX)
        assertEquals(4, vt.cursorY)

        // CUU (Up) CSI 2A
        vt.putChars("\u001B[2A")
        assertEquals(2, vt.cursorY)

        // CUD (Down) CSI 1B
        vt.putChars("\u001B[1B")
        assertEquals(3, vt.cursorY)

        // CUF (Forward) CSI 5C
        vt.putChars("\u001B[5C")
        assertEquals(14, vt.cursorX)

        // CUB (Backward) CSI 4D
        vt.putChars("\u001B[4D")
        assertEquals(10, vt.cursorX)
    }

    @Test
    fun testCSI_EraseInDisplay() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABC\nDEF\nGHI")

        // CSI 2J - Clear all
        vt.putChars("\u001B[2J")
        for (y in 0 until 3) {
            for (x in 0 until 3) {
                assertEquals(' ', vt.screen[y][x].char, "Mismatch at $x,$y")
            }
        }
    }

    @Test
    fun testCSI_EraseInLine() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("ABCDEF")
        vt.putChars("\u001B[1;3H") // Move to row 1, col 3
        assertEquals(2, vt.cursorX)

        // CSI 0K - Clear to end of line
        vt.putChars("\u001B[0K")
        assertEquals('A', vt.screen[0][0].char)
        assertEquals('B', vt.screen[0][1].char)
        assertEquals(' ', vt.screen[0][2].char)
        assertEquals(' ', vt.screen[0][3].char)
    }

    @Test
    fun testCSI_SGR() = synchronized(lock) {
        val vt = VT320()
        // CSI 1;4m - Bold and Underline
        vt.putChars("\u001B[1;4mX")
        val cell = vt.screen[0][0]
        assertTrue(cell.attr.bold)
        assertTrue(cell.attr.underline)
        assertFalse(cell.attr.inverse)

        // CSI 0m - Reset
        vt.putChars("\u001B[0mY")
        val cell2 = vt.screen[0][1]
        assertFalse(cell2.attr.bold)
        assertFalse(cell2.attr.underline)
    }

    @Test
    fun testCSI_ScrollingRegion() = synchronized(lock) {
        val vt = VT320()
        // Fill rows with identifiable content
        for (y in 0 until 5) {
            vt.putChars("ROW$y")
            vt.putChars("\r\n")
        }

        // DECSTBM - Set Scrolling Region to rows 2-4 (1-based, indices 1-3)
        vt.putChars("\u001B[2;4r")
        assertEquals(0, vt.cursorX)
        assertEquals(0, vt.cursorY)

        // Move to bottom of region (index 3)
        vt.putChars("\u001B[4;1H")
        assertEquals(3, vt.cursorY)

        // Line feed at bottom of region should scroll region up
        vt.putChars("NEW\r\n")
        assertEquals(0, vt.cursorX)
        assertEquals(3, vt.cursorY)

        // Verify content:
        // ROW0 (index 0) - outside region, unchanged
        // ROW2 (index 1) - scrolled up from index 2
        // NEW3 (index 2) - scrolled up from index 3 (where NEW overwrote ROW3)
        // Empty (index 3) - cleared after scroll
        // ROW4 (index 4) - outside region, unchanged
        assertEquals("ROW0", vt.screen[0].take(4).map { it.char }.joinToString(""))
        assertEquals("ROW2", vt.screen[1].take(4).map { it.char }.joinToString(""))
        assertEquals("NEW3", vt.screen[2].take(4).map { it.char }.joinToString(""))
        assertTrue(vt.screen[3].all { it.char == ' ' }, "Index 3 should be empty")
        assertEquals("ROW4", vt.screen[4].take(4).map { it.char }.joinToString(""))
    }

    @Test
    fun testStatusLine() = synchronized(lock) {
        val vt = VT320()

        // 1. Host writable status line
        // DECSSDT 2 (Host writable)
        vt.putChars("\u001B[2$~")
        assertEquals(StatusLineManager.Mode.HOST_WRITABLE, vt.statusLineManager.mode)

        // DECSASD 1 (Select Active Status Display)
        vt.putChars("\u001B[1$}")
        assertTrue(vt.activeStatusLine)

        // Write to status line
        vt.putChars("STATUS OK")
        assertEquals('S', vt.statusLineManager.cells[0].char)
        assertEquals('T', vt.statusLineManager.cells[1].char)

        // DECSASD 0 (Back to main display)
        vt.putChars("\u001B[0$}")
        assertFalse(vt.activeStatusLine)

        // Write to main display
        vt.putChars("MAIN")
        assertEquals('M', vt.screen[0][0].char)
        assertEquals('A', vt.screen[0][1].char)

        // Verify status line still has its content
        assertEquals('S', vt.statusLineManager.cells[0].char)
    }

    @Test
    fun testPrinterStatusMC() = synchronized(lock) {
        val vt = VT320()

        // MC 5 (Printer Controller On)
        vt.putChars("\u001B[5i")
        assertEquals(PrinterStatus.CONTROLLER, vt.printerStatus)

        // MC 4 (Printer Controller Off)
        vt.putChars("\u001B[4i")
        assertEquals(PrinterStatus.READY, vt.printerStatus)

        // Private MC ?5 (Auto Print On)
        vt.putChars("\u001B[?5i")
        assertEquals(PrinterStatus.AUTO, vt.printerStatus)

        // Private MC ?4 (Auto Print Off)
        vt.putChars("\u001B[?4i")
        assertEquals(PrinterStatus.READY, vt.printerStatus)
    }

    @Test
    fun testSelectiveErase() = synchronized(lock) {
        val vt = VT320()
        // 1. Write protected text
        vt.putChars("\u001B[1\"q") // DECSCA 1 (Protected)
        vt.putChars("PROT")

        // 2. Write unprotected text
        vt.putChars("\u001B[0\"q") // DECSCA 0 (Unprotected)
        vt.putChars("ERAS")

        // 3. Normal Erase Line (CSI 2K) - should erase EVERYTHING
        vt.putChars("\u001B[2K")
        assertEquals(' ', vt.screen[0][0].char)
        assertEquals(' ', vt.screen[0][4].char)

        // 4. Write again
        vt.cursorX = 0
        vt.putChars("\u001B[1\"qPROT\u001B[0\"qERAS")

        // 5. Selective Erase Line (CSI ? 2 K) - should erase only unprotected
        vt.putChars("\u001B[?2K")
        assertEquals('P', vt.screen[0][0].char)
        assertEquals('R', vt.screen[0][1].char)
        assertEquals('O', vt.screen[0][2].char)
        assertEquals('T', vt.screen[0][3].char)
        assertEquals(' ', vt.screen[0][4].char)
    }

    @Test
    fun testSoftReset() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("\u001B[1m") // Bold
        assertTrue(vt.currentAttr.bold)

        vt.putChars("\u001B[!p") // DECSTR
        assertFalse(vt.currentAttr.bold)
    }

    @Test
    fun testUDK() = synchronized(lock) {
        val vt = VT320()
        // Pc=0 (clear), Pw=1 (don't lock)
        // Key 17 (F6) = "012" (0x30, 0x31, 0x32)
        vt.putChars("\u001BP0;1|17/303132\u001B\\")

        // Trigger F6 (scan code "64")
        vt.keyboard.handleKeyEvent("64", null, null, null, null, null, null)
        assertSent(vt, "012", "UDK F6 should send '012'")

        // Clear UDKs
        vt.putChars("\u001BP0;1|\u001B\\")
        vt.keyboard.handleKeyEvent("64", null, null, null, null, null, null)
        assertSent(vt, "\u001B[17~", "F6 should be back to default after clearing UDKs")
    }

    @Test
    fun testLineInsertionDeletion() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("LINE1\r\nLINE2\r\nLINE3")

        // Move to line 2
        vt.putChars("\u001B[2;1H")

        // Delete Line CSI 1M
        vt.putChars("\u001B[1M")
        // "LINE2" should be gone, "LINE3" moved up to row 2
        assertEquals('L', vt.screen[1][0].char)
        assertEquals('I', vt.screen[1][1].char)
        assertEquals('N', vt.screen[1][2].char)
        assertEquals('E', vt.screen[1][3].char)
        assertEquals('3', vt.screen[1][4].char)

        // Insert Line CSI 1L
        vt.putChars("\u001B[1L")
        // Row 2 should now be empty, "LINE3" moved down to row 3
        assertEquals(' ', vt.screen[1][0].char)
        assertEquals('L', vt.screen[2][0].char)
    }

    @Test
    fun testReset() = synchronized(lock) {
        val vt = VT320()
        vt.putChars("TEST\u001B[1m")
        vt.putChars("\u001Bc") // RIS
        assertEquals(0, vt.cursorX)
        assertEquals(0, vt.cursorY)
        assertFalse(vt.screen[0][0].attr.bold)
        assertEquals(' ', vt.screen[0][0].char)
    }

    @Test
    fun testGSetDesignation() = synchronized(lock) {
        val vt = VT320()
        // ESC ( B - Designate G0 as ASCII (B)
        vt.putChars("\u001B(B")
        // This should hit handleDesignate and set the state back to NORMAL
        // Since we can't easily check the private 'graphics' state, we verify it doesn't get stuck
        vt.putChar('X')
        assertEquals('X', vt.screen[0][0].char)
    }

    @Test
    fun testTabOverflow() = synchronized(lock) {
        val vt = VT320()
        // Move near end of line
        vt.putChars("\u001B[1;75H")
        assertEquals(74, vt.cursorX)
        // Tab should wrap to next line
        vt.putChar('\t')
        assertEquals(0, vt.cursorX)
        assertEquals(1, vt.cursorY)
    }

    @Test
    fun testC1_IND() = synchronized(lock) {
        VT320Settings.nationalOnly = 1
        VT320Settings.characterSetMode = 1
        val vt = VT320()
        vt.putChar(0x84.toChar())
        assertEquals(0, vt.cursorX)
        assertEquals(1, vt.cursorY)
        VT320Settings.nationalOnly = 0
    }

    @Test
    fun testC1_NEL() = synchronized(lock) {
        VT320Settings.nationalOnly = 1
        VT320Settings.characterSetMode = 1
        val vt = VT320()
        vt.putChars("ABC")
        vt.putChar(0x85.toChar())
        assertEquals(0, vt.cursorX)
        assertEquals(1, vt.cursorY)
        VT320Settings.nationalOnly = 0
    }

    @Test
    fun testC1_RI() = synchronized(lock) {
        VT320Settings.nationalOnly = 1
        VT320Settings.characterSetMode = 1
        val vt = VT320()
        vt.cursorY = 5
        vt.putChar(0x8D.toChar())
        assertEquals(0, vt.cursorX)
        assertEquals(4, vt.cursorY)
        VT320Settings.nationalOnly = 0
    }

    @Test
    fun testC1_SS2_SS3() = synchronized(lock) {
        VT320Settings.nationalOnly = 1
        VT320Settings.characterSetMode = 1
        val vt = VT320()
        // Just verify they don't crash and leave state as NORMAL
        vt.putChar(0x8E.toChar()) // SS2
        vt.putChar('A')
        assertEquals('Á', vt.screen[0][0].char)

        vt.putChar(0x8F.toChar()) // SS3
        vt.putChar('B')
        assertEquals('Â', vt.screen[0][1].char)
        VT320Settings.nationalOnly = 0
    }

    @Test
    fun testC1_CSI() = synchronized(lock) {
        VT320Settings.nationalOnly = 1
        VT320Settings.characterSetMode = 1
        val vt = VT320()
        // 0x9B is CSI. 0x9B + '5' + 'C' should move cursor forward by 5.
        vt.putChar(0x9B.toChar())
        vt.putChar('5')
        vt.putChar('C')
        assertEquals(5, vt.cursorX)
        assertEquals(0, vt.cursorY)
        VT320Settings.nationalOnly = 0
    }

    @Test
    fun testC1_ST() = synchronized(lock) {
        VT320Settings.nationalOnly = 1
        VT320Settings.characterSetMode = 1
        val vt = VT320()
        // 0x9B enters CSI state, 0x9C should exit it (back to NORMAL)
        vt.putChar(0x9B.toChar())
        vt.putChar(0x9C.toChar())
        vt.putChar('X')
        assertEquals('X', vt.screen[0][0].char)
        VT320Settings.nationalOnly = 0
    }

    @Test
    fun testC1_Disabled() = synchronized(lock) {
        VT320Settings.nationalOnly = 0 // Disabled (7-bit controls)
        val vt = VT320()
        vt.putChar(0x84.toChar()) // IND if enabled
        assertEquals(0, vt.cursorY) // Should NOT have moved
    }

    @Test
    fun testC1_7BitEquivalents() = synchronized(lock) {
        VT320Settings.nationalOnly = 1
        VT320Settings.characterSetMode = 1

        val pairs = listOf(
            "\u0084" to "\u001BD",      // IND
            "\u0085" to "\u001BE",      // NEL
            "\u0088" to "\u001BH",      // HTS
            "\u008D" to "\u001BM",      // RI
            "\u008E" to "\u001BN",      // SS2
            "\u008F" to "\u001BO",      // SS3
            "\u009B5C" to "\u001B[5C",  // CSI 5C
            "\u009C" to "\u001B\\"      // ST
        )

        try {
            for ((eightBit, sevenBit) in pairs) {
                // Perform the 8-bit sequence and record the state
                val vt8 = VT320()
                vt8.putChars(eightBit)

                // Perform the 7-bit equivalent sequence
                val vt7 = VT320()
                vt7.putChars(sevenBit)

                // Assert that the resulting state is the same
                assertEquals(
                    vt8.cursorX,
                    vt7.cursorX,
                    "cursorX mismatch for $eightBit vs $sevenBit"
                )
                assertEquals(
                    vt8.cursorY,
                    vt7.cursorY,
                    "cursorY mismatch for $eightBit vs $sevenBit"
                )
                assertTrue(
                    vt8.tabulator.stops.contentEquals(vt7.tabulator.stops),
                    "Tab stops mismatch for $eightBit vs $sevenBit"
                )

                // For sequences that might affect internal state, verify they behave identically for the next char
                vt8.putChar('X')
                vt7.putChar('X')
                assertEquals(
                    vt8.cursorX,
                    vt7.cursorX,
                    "cursorX mismatch after 'X' for $eightBit vs $sevenBit"
                )
                assertEquals(
                    vt8.cursorY,
                    vt7.cursorY,
                    "cursorY mismatch after 'X' for $eightBit vs $sevenBit"
                )

                val lastX8 = if (vt8.cursorX > 0) vt8.cursorX - 1 else 0
                val lastX7 = if (vt7.cursorX > 0) vt7.cursorX - 1 else 0
                assertEquals(
                    vt8.screen[vt8.cursorY][lastX8].char, vt7.screen[vt7.cursorY][lastX7].char,
                    "Screen content mismatch after 'X' for $eightBit vs $sevenBit"
                )
            }
        } finally {
            VT320Settings.nationalOnly = 0
        }
    }

    @Test
    fun testC1ControlSelection() = synchronized(lock) {
        val vt = VT320()

        // Default should be 7-bit controls (for VT300 mode)
        assertFalse(vt.eightBitControls)

        // Select 8-bit C1 controls (ESC sp G)
        vt.putChars("\u001B G")
        assertTrue(vt.eightBitControls)

        // Trigger a response (e.g. DSR - Device Status Report CSI 5n)
        vt.putChars("\u001B[5n")
        var response = ""
        while (vt.hasChar()) response += vt.getChar().toChar()
        // Response should be 8-bit CSI (0x9B) "0n"
        assertEquals("\u009B0n", response)

        // Select 7-bit C1 controls (ESC sp F)
        vt.putChars("\u001B F")
        assertFalse(vt.eightBitControls)

        // Trigger response again
        vt.putChars("\u001B[5n")
        response = ""
        while (vt.hasChar()) response += vt.getChar().toChar()
        // Response should be 7-bit CSI (ESC [) "0n"
        assertEquals("\u001B[0n", response)
    }

    @Test
    fun testDECSCL() = synchronized(lock) {
        val vt = VT320()

        // CSI 6 1 \" p -> VT100 mode (Level 1, implies 7-bit controls)
        vt.putChars("\u001B[61\"p")
        assertEquals(61, vt.conformanceLevel)
        assertFalse(vt.eightBitControls)

        // CSI 6 3 ; 0 \" p -> VT300 mode, 8-bit controls
        vt.putChars("\u001B[63;0\"p")
        assertEquals(63, vt.conformanceLevel)
        assertTrue(vt.eightBitControls)

        // CSI 6 3 ; 1 \" p -> VT300 mode, 7-bit controls
        vt.putChars("\u001B[63;1\"p")
        assertEquals(63, vt.conformanceLevel)
        assertFalse(vt.eightBitControls)
    }

    @Test
    fun testAnsiConformanceLevel() = synchronized(lock) {
        val vt = VT320()

        // ESC sp L -> Level 1
        vt.putChars("\u001B L")
        assertEquals(61, vt.conformanceLevel)
        assertFalse(vt.eightBitControls)

        // ESC sp N -> Level 3
        vt.putChars("\u001B N")
        assertEquals(63, vt.conformanceLevel)
        assertTrue(vt.eightBitControls)
    }

    @Test
    fun testDECNRCM() = synchronized(lock) {
        val vt = VT320()

        // 1. Reset (Multinational) - Default
        vt.putChars("\u001B[?42l")
        assertEquals(false, VT320Settings.nationalReplacement, "DECNRCM should be reset")

        // Designate G0 as Swedish (H)
        vt.putChars("\u001B(H")
        // Swedish 'H' should be ignored and designated as ASCII because nationalReplacement is false
        vt.putChar('#')
        assertEquals('#', vt.screen[0][0].char)

        // 2. Set (National)
        vt.putChars("\u001B[?42h")
        assertEquals(true, VT320Settings.nationalReplacement, "DECNRCM should be set")
        assertEquals(false, vt.characterSetMode8Bit, "Should be in 7-bit mode")

        // Designate G0 as German (K)
        vt.putChars("\u001B(K")
        vt.putChar('@') // 0x40 is § in German set
        assertEquals('§', vt.screen[0][1].char)
    }

    @Test
    fun testSCS() = synchronized(lock) {
        val vt = VT320()

        // 1. Designate Special Graphics (0) to G0 (94-set)
        vt.putChars("\u001B(0")
        // 'q' (0x71) in Special Graphics is '─'
        vt.putChar('q')
        assertEquals(
            '─',
            vt.screen[vt.cursorY][vt.cursorX - 1].char,
            "Special Graphics q should be ─"
        )

        // 2. Designate British (A) to G1 (94-set)
        vt.putChars("\u001B)A")
        // Enable national replacement to use NRC sets
        vt.putChars("\u001B[?42h")
        // Invoke G1 into GL
        vt.putChar('\u000E') // SO
        // In British set, '#' (0x23) is '£' (0xA3)
        vt.putChar('#')
        assertEquals('£', vt.screen[vt.cursorY][vt.cursorX - 1].char, "British # should be £")

        // 3. Designate ISO Latin-1 (A) to G2 (96-set)
        vt.putChars("\u001B.A")
        // Invoke G2 into GL
        vt.putChars("\u001Bn") // LS2
        // In ISO Latin-1 (96-set), 0x21 is '¡'
        vt.putChar('!') // 0x21
        assertEquals(
            '¡',
            vt.screen[vt.cursorY][vt.cursorX - 1].char,
            "ISO Latin-1 ! in GL should be ¡"
        )

        // 4. Designate ISO Latin-1 (A) to G3 (96-set)
        vt.putChars("\u001B/A")
        // Invoke G3 into GR
        vt.putChars("\u001B|") // LS3R
        // In ISO Latin-1 (96-set in GR), 0xA1 should be '¡'
        vt.putChar(0xA1.toChar())
        assertEquals(
            '¡',
            vt.screen[vt.cursorY][vt.cursorX - 1].char,
            "ISO Latin-1 0xA1 in GR should be ¡"
        )
    }

    @Test
    fun testKeyboardSetupLink() = synchronized(lock) {
        val vt = VT320()

        // 1. Switch to Swedish keyboard (Index 11)
        VT320Settings.keyboardLanguage = 11
        vt.applyKeyboardLanguage()

        assertTrue(
            VT320Settings.nationalReplacement,
            "National replacement should be enabled for Swedish"
        )
        assertFalse(vt.characterSetMode8Bit, "Should be in 7-bit mode for Swedish")

        // Swedish '7' (0x37) designator maps 0x40 to 'É' (Wait, let's check mapping)
        // Graphics.kt says Swedish (7 or H):
        // chars[0x40] = glyph('É', ...), chars[0x5B] = glyph('Ä', ...) etc.
        // Actually, my testDECNRCM earlier used German (K). 
        // Let's use German (Index 6) to be consistent with my knowledge.

        VT320Settings.keyboardLanguage = 6 // German
        vt.applyKeyboardLanguage()

        // German 'K' maps 0x40 to '§'
        vt.putChar('@') // 0x40
        assertEquals(
            '§',
            vt.screen[vt.cursorY][vt.cursorX - 1].char,
            "German keyboard should designate German NRC"
        )

        // 2. Switch back to North American (Index 0)
        VT320Settings.keyboardLanguage = 0
        vt.applyKeyboardLanguage()

        assertFalse(
            VT320Settings.nationalReplacement,
            "National replacement should be disabled for US"
        )
        assertTrue(vt.characterSetMode8Bit, "Should be in 8-bit mode for US")

        vt.putChar('@')
        assertEquals(
            '@',
            vt.screen[vt.cursorY][vt.cursorX - 1].char,
            "US keyboard should designate ASCII"
        )
    }

    @Test
    fun testStartupKeyboardLanguage() = synchronized(lock) {
        // 1. Pre-set language to German
        VT320Settings.keyboardLanguage = 6

        // 2. Instantiate VT320 - should call applyKeyboardLanguage() in init
        val vt = VT320()

        assertTrue(
            VT320Settings.nationalReplacement,
            "National mode should be enabled at startup for German"
        )
        assertFalse(
            vt.characterSetMode8Bit,
            "Terminal should be in 7-bit mode at startup for German"
        )

        // German 'K' maps 0x40 ('@') to '§'
        vt.putChar('@')
        assertEquals(
            '§',
            vt.screen[0][0].char,
            "Startup with German keyboard should use German NRC"
        )

        // Removed RIS test due to singleton race conditions in parallel test execution
    }

    @Test
    fun testKeyboard_EditingKeys() = synchronized(lock) {
        val keys = listOf(
            "8A" to "\u001B[1~", // Find
            "8B" to "\u001B[2~", // Insert Here
            "8C" to "\u001B[3~", // Remove
            "8D" to "\u001B[4~", // Select
            "8E" to "\u001B[5~", // Prev Screen
            "8F" to "\u001B[6~"  // Next Screen
        )

        // 1. VT300 Mode (Conformance Level 63)
        VT320Settings.nationalOnly = 0
        val vt300 = VT320()
        for ((code, expected) in keys) {
            vt300.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
            assertSent(vt300, expected, "VT300 Mode: Key $code failed")
        }

        // 2. VT100 Mode (Conformance Level 61)
        VT320Settings.nationalOnly = 2
        val vt100 = VT320()
        for ((code, _) in keys) {
            vt100.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
            assertFalse(vt100.hasChar(), "VT100 Mode: Key $code should not send any code")
        }

        // 3. VT52 Mode (Conformance Level 52)
        VT320Settings.nationalOnly = 3
        val vt52 = VT320()
        for ((code, _) in keys) {
            vt52.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
            assertFalse(vt52.hasChar(), "VT52 Mode: Key $code should not send any code")
        }

        // Reset settings
        VT320Settings.nationalOnly = 0
    }

    @Test
    fun testKeyboard_ArrowKeys() = synchronized(lock) {
        val keys = listOf(
            "AA" to arrayOf("\u001B[A", "\u001BOA", "\u001BA"), // Up
            "A9" to arrayOf("\u001B[B", "\u001BOB", "\u001BB"), // Down
            "A8" to arrayOf("\u001B[C", "\u001BOC", "\u001BC"), // Right
            "A7" to arrayOf("\u001B[D", "\u001BOD", "\u001BD")  // Left
        )

        val vt = VT320()

        // 1. ANSI Mode, Cursor (Normal) Key Setting - CSI ? 1 l
        vt.conformanceLevel = 63 // VT300
        vt.putChars("\u001B[?1l") // Reset DECCKM (Normal)
        assertFalse(vt.cursorKeysMode)

        for ((code, expected) in keys) {
            vt.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
            assertSent(vt, expected[0], "Normal ANSI Mode: Key $code failed")
        }

        // 2. ANSI Mode, Application Key Setting - CSI ? 1 h
        vt.putChars("\u001B[?1h") // Set DECCKM (Application)
        assertTrue(vt.cursorKeysMode)

        for ((code, expected) in keys) {
            vt.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
            assertSent(vt, expected[1], "Application ANSI Mode: Key $code failed")
        }

        // 3. VT52 Mode (Regardless of key setting)
        vt.conformanceLevel = 52
        for ((code, expected) in keys) {
            vt.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
            assertSent(vt, expected[2], "VT52 Mode: Key $code failed")
        }
    }

    @Test
    fun testKeyboard_NumericKeypad() = synchronized(lock) {
        val vt = VT320()
        val keyboard = vt.keyboard

        val keys = listOf(
            "92" to "0", "96" to "1", "97" to "2", "98" to "3", "99" to "4",
            "9A" to "5", "9B" to "6", "9D" to "7", "9E" to "8", "9F" to "9",
            "A0" to "-", "9C" to ",", "94" to ".", "13" to "Enter",
            "A1" to "PF1", "A2" to "PF2", "A3" to "PF3", "A4" to "PF4"
        )

        fun verifyKey(code: String, label: String, expected: String, msg: String) {
            keyboard.handleKeyEvent(code, label, null, null, null, null, null)
            assertSent(vt, expected, "$msg: Key $label (code $code) failed")
        }

        // 1. ANSI Numeric Mode
        vt.conformanceLevel = 63 // ANSI
        vt.putChars("\u001B>") // DECPNM (Numeric)
        assertFalse(vt.keypadMode)
        val expectedAnsiNumeric = mapOf(
            "0" to "0", "1" to "1", "2" to "2", "3" to "3", "4" to "4",
            "5" to "5", "6" to "6", "7" to "7", "8" to "8", "9" to "9",
            "-" to "-", "," to ",", "." to ".", "Enter" to "\r",
            "PF1" to "\u001BOP", "PF2" to "\u001BOQ", "PF3" to "\u001BOR", "PF4" to "\u001BOS"
        )
        keys.forEach { (code, label) ->
            verifyKey(
                code,
                label,
                expectedAnsiNumeric[label]!!,
                "ANSI Numeric"
            )
        }

        // 2. ANSI Application Mode
        vt.putChars("\u001B=") // DECKPAM (Application)
        assertTrue(vt.keypadMode)
        val expectedAnsiApp = mapOf(
            "0" to "\u001BOp",
            "1" to "\u001BOq",
            "2" to "\u001BOr",
            "3" to "\u001BOs",
            "4" to "\u001BOt",
            "5" to "\u001BOu",
            "6" to "\u001BOv",
            "7" to "\u001BOw",
            "8" to "\u001BOx",
            "9" to "\u001BOy",
            "-" to "\u001BOm",
            "," to "\u001BOl",
            "." to "\u001BOn",
            "Enter" to "\u001BOM",
            "PF1" to "\u001BOP",
            "PF2" to "\u001BOQ",
            "PF3" to "\u001BOR",
            "PF4" to "\u001BOS"
        )
        keys.forEach { (code, label) ->
            verifyKey(
                code,
                label,
                expectedAnsiApp[label]!!,
                "ANSI Application"
            )
        }

        // 3. VT52 Numeric Mode
        vt.conformanceLevel = 52
        vt.putChars("\u001B>")
        assertFalse(vt.keypadMode)
        val expectedVT52Numeric = mapOf(
            "0" to "0", "1" to "1", "2" to "2", "3" to "3", "4" to "4",
            "5" to "5", "6" to "6", "7" to "7", "8" to "8", "9" to "9",
            "-" to "-", "," to ",", "." to ".", "Enter" to "\r",
            "PF1" to "\u001BP", "PF2" to "\u001BQ", "PF3" to "\u001BR", "PF4" to ""
        )
        keys.forEach { (code, label) ->
            verifyKey(
                code,
                label,
                expectedVT52Numeric[label]!!,
                "VT52 Numeric"
            )
        }

        // 4. VT52 Application Mode
        vt.putChars("\u001B=")
        assertTrue(vt.keypadMode)
        val expectedVT52App = mapOf(
            "0" to "\u001B?p",
            "1" to "\u001B?q",
            "2" to "\u001B?r",
            "3" to "\u001B?s",
            "4" to "\u001B?t",
            "5" to "\u001B?u",
            "6" to "\u001B?v",
            "7" to "\u001B?w",
            "8" to "\u001B?x",
            "9" to "\u001B?y",
            "-" to "\u001B?m",
            "," to ",",
            "." to "\u001B?n",
            "Enter" to "\u001B?M",
            "PF1" to "\u001BP",
            "PF2" to "\u001BQ",
            "PF3" to "\u001BR",
            "PF4" to ""
        )
        keys.forEach { (code, label) ->
            verifyKey(
                code,
                label,
                expectedVT52App[label]!!,
                "VT52 Application"
            )
        }
    }

    @Test
    fun testKeyboard_FunctionKeys() = synchronized(lock) {
        val fKeys = listOf(
            "64" to "\u001B[17~", // F6
            "65" to "\u001B[18~", // F7
            "66" to "\u001B[19~", // F8
            "67" to "\u001B[20~", // F9
            "68" to "\u001B[21~", // F10
            "71" to "\u001B[23~", // F11
            "72" to "\u001B[24~", // F12
            "73" to "\u001B[25~", // F13
            "74" to "\u001B[26~", // F14
            "7C" to "\u001B[28~", // Help
            "7D" to "\u001B[29~", // Do
            "80" to "\u001B[31~", // F17
            "81" to "\u001B[32~", // F18
            "82" to "\u001B[33~", // F19
            "83" to "\u001B[34~"  // F20
        )

        val localKeys = listOf("56", "57", "58", "59", "5A")

        // 1. VT300 Mode (Conformance Level 63)
        VT320Settings.nationalOnly = 0
        val vt300 = VT320()
        for ((code, expected) in fKeys) {
            if (code == "7C") continue // Skip Help key as it toggles Setup and clears buffer
            vt300.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
            val actual = StringBuilder()
            while (vt300.hasChar()) {
                actual.append(vt300.getChar().toChar())
            }
            assertEquals(expected, actual.toString(), "VT300 Mode: Key $code failed")
        }
        for (code in localKeys) {
            vt300.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
            assertFalse(vt300.hasChar(), "VT300 Mode: Local Key $code should not send code")
        }

        // 2. VT100 Mode (Conformance Level 61) and VT52 Mode (Conformance Level 52)
        val modes = listOf(
            2 to "VT100", // National Only = 2 -> conformanceLevel = 61
            3 to "VT52"   // National Only = 3 -> conformanceLevel = 52
        )

        for ((modeVal, modeName) in modes) {
            VT320Settings.nationalOnly = (modeVal)
            val vt = VT320()

            // F6-F10, F14-F20: Should send nothing
            val nothingKeys = fKeys.filterNot { it.first in listOf("71", "72", "73") }
            for ((code, _) in nothingKeys) {
                vt.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
                assertFalse(vt.hasChar(), "$modeName Mode: Key $code should not send code")
            }

            // F11: ESC
            vt.keyboard.handleKeyEvent("71", null, null, null, null, null, null)
            assertSent(vt, "\u001B", "$modeName Mode: F11 failed")

            // F12: BS
            vt.keyboard.handleKeyEvent("72", null, null, null, null, null, null)
            assertSent(vt, "\u0008", "$modeName Mode: F12 failed")

            // F13: LF
            vt.keyboard.handleKeyEvent("73", null, null, null, null, null, null)
            assertSent(vt, "\u000A", "$modeName Mode: F13 failed")

            for (code in localKeys) {
                vt.keyboard.handleKeyEvent(code, null, null, null, null, null, null)
                assertFalse(vt.hasChar(), "$modeName Mode: Local Key $code should not send code")
            }
        }

        // Reset settings
        VT320Settings.nationalOnly = 0
    }

    @Test
    fun testKeyboard_CtrlChars() = synchronized(lock) {
        val vt = VT320()
        val kb = vt.keyboard

        fun sendKey(
            code: String,
            label: String? = null,
            normal: String? = null,
            shifted: String? = null
        ) {
            kb.handleKeyEvent(code, label, normal, shifted, null, null, null)
        }

        // Toggle CTRL on
        kb.handleKeyEvent("CTRL")

        // Ctrl + Space -> 0x00 (NUL)
        sendKey("32", " ", " ", " ")
        assertSent(vt, "\u0000", "Ctrl+Space")

        // Ctrl + 2 -> 0x00 (NUL)
        sendKey("50", "2", "2", "@")
        assertSent(vt, "\u0000", "Ctrl+2")

        // Ctrl + A -> 0x01
        sendKey("100", "A", "a", "A")
        assertSent(vt, "\u0001", "Ctrl+A")

        // Ctrl + Z -> 0x1A
        sendKey("125", "Z", "z", "Z")
        assertSent(vt, "\u001A", "Ctrl+Z")

        // Ctrl + 3 or [ -> 0x1B (ESC)
        sendKey("51", "3", "3", "#")
        assertSent(vt, "\u001B", "Ctrl+3")
        sendKey("91", "[", "[", "{")
        assertSent(vt, "\u001B", "Ctrl+[")

        // Ctrl + 4 or / -> 0x1C (FS)
        sendKey("52", "4", "4", "$")
        assertSent(vt, "\u001C", "Ctrl+4")
        sendKey("47", "/", "/", "?")
        assertSent(vt, "\u001C", "Ctrl+/")

        // Ctrl + 5 or ] -> 0x1D (GS)
        sendKey("53", "5", "5", "%")
        assertSent(vt, "\u001D", "Ctrl+5")
        sendKey("93", "]", "]", "}")
        assertSent(vt, "\u001D", "Ctrl+]")

        // Ctrl + 6 or ~ -> 0x1E (RS)
        sendKey("54", "6", "6", "^")
        assertSent(vt, "\u001E", "Ctrl+6")
        sendKey("126", "~", "~", "`")
        assertSent(vt, "\u001E", "Ctrl+~")

        // Ctrl + 7 or ? -> 0x1F (US)
        sendKey("55", "7", "7", "&")
        assertSent(vt, "\u001F", "Ctrl+7")
        sendKey("63", "?", "?", "/")
        assertSent(vt, "\u001F", "Ctrl+?")

        // Ctrl + 8 -> 0x7F (DEL)
        sendKey("56", "8", "8", "*")
        assertSent(vt, "\u007F", "Ctrl+8")

        // Toggle CTRL off
        kb.handleKeyEvent("CTRL")

        // Verify A is now 'a' (default normal char)
        sendKey("100", "A", "a", "A")
        assertSent(vt, "a", "A (normal)")

        // Dedicated keys
        // Return (13) -> \r (when newLineMode is off)
        vt.newLineMode = false
        kb.handleKeyEvent("13")
        assertSent(vt, "\r", "Return (newline off)")

        // Tab (9) -> \t
        kb.handleKeyEvent("9")
        assertSent(vt, "\t", "Tab")

        // Backspace (8) -> \u0008 (when backArrow is 1) or \u007F (when backArrow is 0)
        VT320Settings.backArrow = 1
        kb.handleKeyEvent("8")
        assertSent(vt, "\u0008", "Backspace (backArrow=1)")

        VT320Settings.backArrow = 0
        kb.handleKeyEvent("8")
        assertSent(vt, "\u007F", "Backspace (backArrow=0)")
    }

    @Test
    fun testDECDLD() = synchronized(lock) {
        val vt = VT320()

        // DECDLD sequence for a soft set designated as ' @' (space @)
        // Sixel data starting after ' @'
        val dcs = "0;1;1;0;0;2;0;0;{ @_o_o_o_/o_o_o_o"
        vt.putChars("\u001BP$dcs\u001B\\")

        // Designate ' @' to G1
        vt.putChars("\u001B) @")
        // Invoke G1 into GL
        vt.putChar('\u000E') // SO

        // Draw character remapped in DRCS
        vt.putChar('!')
        assertEquals(" @", vt.graphics.characterSets.drcsFontBuffer.designator())
        assertEquals(vt.cursorX, 1, "Soft character should be drawn")
    }

    @Test
    fun testDECDLD_B() = synchronized(lock) {
        val vt = VT320()
        // Use 'B' as designator (conflicts with ASCII but our logic should handle it)
        val dcs = "0;1;1;0;0;2;0;0;{B_o_o_o_/o_o_o_o"
        vt.putChars("\u001BP$dcs\u001B\\")
        assertEquals("B", vt.graphics.characterSets.drcsFontBuffer.designator())

        // Designate 'B' to G1
        vt.putChars("\u001B)B")
        // Invoke G1 into GL
        vt.putChar('\u000E') // SO

        // Check character
        vt.putChar('!')
        assertEquals(vt.cursorX, 1, "Soft character 'B' should be drawn")
    }

    private fun loadSoftFont(vt: VT320, resourceName: String) {
        val stream = VT320Test::class.java.classLoader.getResourceAsStream(resourceName)
            ?: throw RuntimeException("Resource not found: $resourceName")
        val bytes = stream.readBytes()
        println("Loading $resourceName (${bytes.size} bytes)")
        bytes.forEach { vt.putChar((it.toInt() and 0xFF).toChar()) }
    }

    @Test
    fun testJetpacFont() = synchronized(lock) {
        val vt = VT320()
        val stream = VT320Test::class.java.classLoader.getResourceAsStream("jetpac.fnt")
            ?: throw RuntimeException("Resource not found: jetpac.fnt")
        val bytes = stream.readBytes()

        // Convert too 7-bit to be safe
        var first = true
        bytes.forEach {
            val b = it.toInt() and 0xFF
            if (first && b == 0x90) {
                vt.putChars("\u001BP")
            } else if (b == 0x9C) {
                vt.putChars("\u001B\\")
            } else {
                vt.putChar(b.toChar())
            }
            first = false
        }

        println("DRCS Designator after load: '${vt.graphics.characterSets.drcsFontBuffer.designator()}'")

        // Jetpac font designator is 'B'
        vt.putChars("\u001B)B")
        vt.putChar('\u000E') // SO

        vt.putChars("ABC")
        assertEquals(vt.cursorX, 3)
    }

    @Test
    fun testRIS_ResetsModes() = synchronized(lock) {
        val vt = VT320()

        // Set some modes
        vt.putChars("\u001B=") // DECKPAM
        assertTrue(vt.keypadMode)
        vt.putChars("\u001B[?1h") // DECCKM
        assertTrue(vt.cursorKeysMode)

        // Full Reset
        vt.reset() // ris()

        assertFalse(vt.keypadMode, "Keypad should be Numeric after RIS")
        assertFalse(vt.cursorKeysMode, "Cursor keys should be Normal after RIS")
    }
}
