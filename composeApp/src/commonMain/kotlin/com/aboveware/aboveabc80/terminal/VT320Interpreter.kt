package com.aboveware.aboveabc80.terminal

import com.aboveware.aboveabc80.Abc80Log

/**
 * VT320 Escape Sequence Interpreter
 * 
 * This file handles the parsing and execution of terminal control sequences.
 * 
 * Summary of implemented sequences:
 * 
 * C0 Controls:
 * - ENQ (05) : Enquiry - Trigger Answerback
 * - BEL (07) : Bell - Trigger Warning Bell
 * - BS  (08) : Backspace
 * - HT  (09) : Horizontal Tab
 * - LF/VT/FF (0A-0C) : Line Feed / Vertical Tab / Form Feed
 * - CR  (0D) : Carriage Return
 * - SO  (0E) : Shift Out (LS1 - Locking Shift 1)
 * - SI  (0F) : Shift In (LS0 - Locking Shift 0)
 * - ESC (1B) : Escape - Sequence Introducer
 * 
 * C1 Controls (8-bit):
 * - IND (84) : Index
 * - NEL (85) : Next Line
 * - HTS (88) : Horizontal Tab Set
 * - RI  (8D) : Reverse Index
 * - SS2 (8E) : Single Shift 2
 * - SS3 (8F) : Single Shift 3
 * - DCS (90) : Device Control String
 * - CSI (9B) : Control Sequence Introducer
 * - ST  (9C) : String Terminator
 * 
 * ESC Sequences (Escape + Character):
 * - ESC [ : CSI - Control Sequence Introducer
 * - ESC ( / ) / * / + : SCS - Designate G0-G3 Character Sets (94-char)
 * - ESC - / . / / : SCS - Designate G1-G3 Character Sets (96-char)
 * - ESC N / O : SS2 / SS3 - Single Shift 2 / 3
 * - ESC n / o : LS2 / LS3 - Locking Shift 2 / 3
 * - ESC | / } / ~ : LS3R / LS2R / LS1R - Locking Shift 3/2/1 Right
 * - ESC = / > : DECKPAM / DECKPNM - Keypad Application / Numeric Mode
 * - ESC D / M / E : IND / RI / NEL - Index / Reverse Index / Next Line
 * - ESC 7 / 8 : DECSC / DECRC - Save / Restore Cursor
 * - ESC H : HTS - Horizontal Tab Set
 * - ESC \ : ST - String Terminator
 * - ESC c : RIS - Reset to Initial State
 * - ESC # 3 : DECDHL - Double-Height Line (Top Half)
 * - ESC # 4 : DECDHL - Double-Height Line (Bottom Half)
 * - ESC # 5 : DECSWL - Single-Width Line
 * - ESC # 6 : DECDWL - Double-Width Line
 * - ESC # 8 : DECALN - Screen Alignment Test
 * - ESC P : DCS - Device Control String
 * 
 * CSI Sequences (ESC [ ...):
 * - CUU / CUD / CUF / CUB : Cursor Up / Down / Forward / Backward
 * - CUP / HVP : Cursor Position / Horizontal Vertical Position
 * - TBC : Tabulation Clear
 * - ED  / DECSED : Erase In Display / Selective Erase In Display
 * - EL  / DECSEL : Erase In Line / Selective Erase In Line
 * - IL  / DL : Insert Line / Delete Line
 * - ICH / DCH : Insert Character / Delete Character
 * - ECH : Erase Character
 * - DA  : Device Attributes (Primary, Secondary & Third-level)
 * - SGR : Set Graphics Rendition (Bold, Underline, Blink, Inverse, etc.)
 * - DECLL / DECSCUSR : Load LEDs / Set Cursor Style
 * - DECSTBM : Set Scrolling Region
 * - MC  : Media Copy (Print operations)
 *   - ANSI MC: 0 (Print Screen), 4 (Printer Controller Off), 5 (Printer Controller On)
 *   - DEC Private MC: ?1 (Print Line), ?4 (Auto Print Off), ?5 (Auto Print On)
 * - DSR : Device Status Report
 *   - ANSI DSR: 5 (Operating Status), 6 (Cursor Position)
 *   - DEC Private DSR: ?15 (Printer Status), ?25 (UDK Status), ?26 (Keyboard Language)
 *   - Special DSR: 62 (Macro Space), 63 (Memory Checksum)
 * - DECTST : Confidence Test
 * - ANSI Modes (SM / RM):
 *   - KAM (2)  : Keyboard Action Mode (Locked/Unlocked)
 *   - IRM (4)  : Insertion-Replacement Mode (Insert/Replace)
 *   - SRM (12) : Send/Receive Mode (Local Echo Off/On)
 *   - LNM (20) : Line Feed/New Line Mode (New Line/Line Feed)
 * - DEC Private Modes (DECSET / DECRST):
 *   - DECCKM  (1)  : Cursor Keys Mode (Application/Normal)
 *   - DECANM  (2)  : ANSI Mode (ANSI/VT52)
 *   - DECCOLM (3)  : Column Mode (132/80)
 *   - DECSCLM (4)  : Scrolling Mode (Smooth/Jump)
 *   - DECSCNM (5)  : Screen Mode (Reverse/Normal)
 *   - DECOM   (6)  : Origin Mode (Relative/Absolute)
 *   - DECAWN  (7)  : Autowrap Mode (On/Off)
 *   - DECARM  (8)  : Auto Repeat Mode (On/Off)
 *   - DECPFF  (18) : Print Form Feed Mode (On/Off)
 *   - DECPEX  (19) : Printer Extent Mode (Full Screen/Scrolling Region)
 *   - DECTCEM (25) : Text Cursor Enable (Visible/Hidden)
 *   - DECNRCM (42) : National Replacement Character Set Mode (National/Multinational)
 *   - DECKBUM (68) : Keyboard Usage Mode (Data Processing/Typewriter)
 * - DECAUPSS (CSI Ps & u) : Assign User Preferred Supplemental Sets
 * - DECRQUPSS (CSI & u) : Request User Preferred Supplemental Set
 * - DECULOCK (CSI ! w) : User Features Lock
 * - DECSCL   (CSI " p) : Select Conformance Level
 * - DECSCA   (CSI " q) : Select Character Protection Attribute
 * - DECSASD  (CSI $ }) : Select Active Status Display
 * - DECSSDT  (CSI $ ~) : Select Status Display Type
 * - DECRQTSR (CSI $ u) : Request Terminal State Report
 * - DECRQPSR (CSI $ t) : Request Presentation State Report
 * - DECRQM   (CSI $ p) : Request Mode (ANSI or DEC Private)
 * - DECRSTS  (CSI $ w) : Request Status
 * 
 * DCS Strings (ESC P ... ST):
 * - DECDLD ( { ) : Dynamically Loadable Descriptor (Soft Character Set)
 * - DECUDK ( | ) : Define User Defined Keys (UDK)
 * - DECTSR ( $ s ) : Terminal State Report (Response to DECRQTSR)
 * - DECCIR ( 1 $ u ) : Cursor Information Report (Response to DECRQPSR)
 * - DECTABSR ( 2 $ u ) : Tab Stop Report (Response to DECRQPSR)
 * - DECRQSS ( $ q ) : Request Selection or Setting
 * - DECRPSS ( $ r ) : Report Selection or Setting (Response to DECRQSS)
 * - DECRSPS ( $ t ) : Restore Presentation State
 * - DECRPM ( $ y ) : Report Mode (Response to DECRQM)
 * - Status Report ( $ r ) : Response to DECRSTS
 * 
 * VT52 Mode Sequences (ESC + ...):
 * - ESC A / B / C / D : Cursor Up / Down / Right / Left
 * - ESC F / G : Enter / Exit Graphics Mode
 * - ESC H : Cursor Home
 * - ESC I : Reverse Index
 * - ESC J / K : Erase to End of Screen / Line
 * - ESC Y row col : Direct Cursor Address
 * - ESC Z : Identify (Response: ESC / Z)
 * - ESC = / > : Alternate / Numeric Keypad Mode
 * - ESC < : Enter ANSI Mode
 */

/**
 * Handles C1 control characters (0x80 - 0x9F).
 */
internal fun VT320.handleC1(c: Char) {
    when (c.code) {
        0x84 -> lineFeed()                   // IND - Index
        0x85 -> {                            // NEL - Next Line
            cursorX = 0
            lineFeed()
        }

        0x88 -> tabulator.set(cursorX, true) // HTS - Horizontal Tab Set
        0x8D -> reverseIndex()               // RI  - Reverse Index
        0x8E -> graphics.ss2()               // SS2 - Single Shift 2
        0x8F -> graphics.ss3()               // SS3 - Single Shift 3
        0x90 -> {                            // DCS - Device Control String
            state = VT320.State.DCS
            params.clear()
            currentParam = 0
            hasParam = false
        }

        0x9B -> {                            // CSI - Control Sequence Introducer
            state = VT320.State.CSI
            params.clear()
            currentParam = 0
            hasParam = false
            isPrivateMode = false
            csiPrefix = '\u0000'
        }

        0x9C -> state = VT320.State.NORMAL   // ST  - String Terminator
    }
}

/**
 * Handles character set designation sequences (SCS).
 */
internal fun VT320.handleDesignate(
    c: Char,
    index: Graphics.CharacterSetIndex,
    extState: VT320.State,
    is96: Boolean
) {
    if (c == '%') {
        state = extState
        return
    }
    if (c in '\u0020'..'\u002F') {
        designatorBuffer += c
        return
    }
    graphics.designateGraphicSet(index, designatorBuffer + c, is96)
    state = VT320.State.NORMAL
    designatorBuffer = ""
}

/**
 * Handles C0 control characters (0x00 - 0x1F) and printable characters.
 */
internal fun VT320.handleNormal(c: Char) {
    when (c.code) {
        0x05 -> triggerAnswerback() // ENQ - Enquiry
        0x07 -> triggerWarningBell() // BEL - Bell
        0x08 -> if (cursorX > 0) cursorX-- // BS  - Backspace
        0x09 -> { // HT - Horizontal Tab
            val nextX = tabulator.nextTab(cursorX)
            if (activeStatusLine) {
                cursorX = nextX.coerceAtMost(columns - 1)
            } else {
                cursorX = nextX
                if (cursorX >= columns) {
                    cursorX = 0
                    lineFeed()
                }
            }
        }

        0x0A, 0x0B, 0x0C -> { // LF, VT, FF - Line Feed, Vertical Tab, Form Feed
            if (newLineMode) cursorX = 0
            lineFeed()
        }

        0x0D -> cursorX = 0 // CR - Carriage Return
        0x0E -> graphics.gL = Graphics.CharacterSetIndex.G1 // SO - Shift Out (LS1)
        0x0F -> graphics.gL = Graphics.CharacterSetIndex.G0 // SI - Shift In (LS0)
        0x1B -> state = VT320.State.ESC // ESC - Escape
        else -> {
            if (c.code >= 32) {
                drawChar(c)
            }
        }
    }
}

/**
 * Handles two-character escape sequences starting with ESC.
 */
internal fun VT320.handleEsc(c: Char) {
    if (conformanceLevel == 52) {
        handleVt52(c)
        return
    }
    val prevState = lastStateBeforeEsc
    state = VT320.State.NORMAL
    when (c) {
        '[' -> { // CSI - Control Sequence Introducer
            state = VT320.State.CSI
            params.clear()
            currentParam = 0
            hasParam = false
            isPrivateMode = false
            csiPrefix = '\u0000'
        }

        '(' -> {
            state = VT320.State.DESIGNATE_G0; designatorBuffer = ""
        }   // SCS - G0
        ')' -> {
            state = VT320.State.DESIGNATE_G1; designatorBuffer = ""
        }   // SCS - G1
        '*' -> {
            state = VT320.State.DESIGNATE_G2; designatorBuffer = ""
        }   // SCS - G2
        '+' -> {
            state = VT320.State.DESIGNATE_G3; designatorBuffer = ""
        }   // SCS - G3
        '-' -> {
            state = VT320.State.DESIGNATE96_G1; designatorBuffer = ""
        } // SCS - G1 (96-char)
        '.' -> {
            state = VT320.State.DESIGNATE96_G2; designatorBuffer = ""
        } // SCS - G2 (96-char)
        '/' -> {
            state = VT320.State.DESIGNATE96_G3; designatorBuffer = ""
        } // SCS - G3 (96-char)
        'N' -> graphics.ss2() // SS2 - Single Shift 2
        'O' -> graphics.ss3() // SS3 - Single Shift 3
        'n' -> graphics.gL = Graphics.CharacterSetIndex.G2 // LS2 - Locking Shift 2
        'o' -> graphics.gL = Graphics.CharacterSetIndex.G3 // LS3 - Locking Shift 3
        '|' -> graphics.gR = Graphics.CharacterSetIndex.G3 // LS3R - Locking Shift 3 Right
        '}' -> graphics.gR = Graphics.CharacterSetIndex.G2 // LS2R - Locking Shift 2 Right
        '~' -> graphics.gR = Graphics.CharacterSetIndex.G1 // LS1R - Locking Shift 1 Right
        '=' -> { // DECKPAM - Keypad Application Mode
            VT320Settings.keypadMode = 1
            keypadMode = true
        }

        '>' -> { // DECKPNM - Keypad Numeric Mode
            VT320Settings.keypadMode = 0
            keypadMode = false
        }

        'D' -> lineFeed()    // IND - Index
        'M' -> reverseIndex() // RI - Reverse Index
        'E' -> { // NEL - Next Line
            cursorX = 0
            lineFeed()
        }

        '7' -> { // DECSC - Save Cursor
            savedCursorX = cursorX
            savedCursorY = cursorY
            savedAttr = currentAttr
        }

        '8' -> { // DECRC - Restore Cursor
            cursorX = savedCursorX
            cursorY = savedCursorY
            currentAttr = savedAttr
        }

        'H' -> tabulator.set(cursorX, true) // HTS - Horizontal Tab Set
        '\\' -> { // ST - String Terminator
            if (prevState == VT320.State.DCS_DATA) {
                finishDecdld()
            }
            if (prevState == VT320.State.DCS_UDK) {
                finishUdkEntry()
            }
            if (prevState == VT320.State.DCS_RSPS) {
                finishRsps()
            }
            if (prevState == VT320.State.DCS_RQSS) {
                finishRqss()
            }
            state = VT320.State.NORMAL
        }

        'c' -> { // RIS - Reset to Initial State
            Abc80Log.terminal("VT320: Full Reset (RIS) received")
            ris()
        }

        '#' -> state = VT320.State.HASH
        'P' -> { // DCS - Device Control String
            state = VT320.State.DCS
            params.clear()
            currentParam = 0
            hasParam = false
        }

        ' ' -> { // ESC sp - Special sequence introducer
            state = VT320.State.SPACE
            params.clear()
            currentParam = 0
            hasParam = false
        }
    }
}

/**
 * Handles sequences starting with ESC #.
 */
internal fun VT320.handleHash(c: Char) {
    state = VT320.State.NORMAL
    when (c) {
        '3' -> { // DECDHL - Double-Height Line (Top Half)
            val newAttrs = lineAttributes.copyOf()
            newAttrs[cursorY] = VT320.LineAttribute.DOUBLE_HEIGHT_TOP
            lineAttributes = newAttrs
        }

        '4' -> { // DECDHL - Double-Height Line (Bottom Half)
            val newAttrs = lineAttributes.copyOf()
            newAttrs[cursorY] = VT320.LineAttribute.DOUBLE_HEIGHT_BOTTOM
            lineAttributes = newAttrs
        }

        '5' -> { // DECSWL - Single-Width Line
            val newAttrs = lineAttributes.copyOf()
            newAttrs[cursorY] = VT320.LineAttribute.NORMAL
            lineAttributes = newAttrs
        }

        '6' -> { // DECDWL - Double-Width Line
            val newAttrs = lineAttributes.copyOf()
            newAttrs[cursorY] = VT320.LineAttribute.DOUBLE_WIDTH
            lineAttributes = newAttrs
        }

        '8' -> { // DECALN - Screen Alignment Test
            decaln()
        }
    }
}

/**
 * Handles VT52 specific escape sequences.
 */
internal fun VT320.handleVt52(c: Char) {
    state = VT320.State.NORMAL
    when (c) {
        'A' -> if (cursorY > 0) cursorY-- // Up
        'B' -> if (cursorY < rows - 1) cursorY++ // Down
        'C' -> if (cursorX < columns - 1) cursorX++ // Right
        'D' -> if (cursorX > 0) cursorX-- // Left
        'F' -> vt52GraphicsMode = true // Enter Graphics Mode
        'G' -> vt52GraphicsMode = false // Exit Graphics Mode
        'H' -> {
            cursorX = 0; cursorY = 0
        } // Home
        'I' -> reverseIndex() // Reverse Index
        'J' -> eraseInDisplayInternal(0, false) // Erase to end of screen
        'K' -> if (cursorX == columns - 1) {
            eraseInLineInternal(2, false)
        } else {
            eraseInLineInternal(0, false)
        }
        'Y' -> state = VT320.State.VT52_Y1 // Direct Cursor Address
        'Z' -> sendResponse("\u001B/Z") // Identify (Response: ESC / Z)
        '=' -> keypadMode = true // Alternate Keypad
        '>' -> keypadMode = false // Numeric Keypad
        '<' -> { // Enter ANSI mode
            conformanceLevel = 63
            TerminalManager.operatingMode = TerminalManager.OperatingMode.ANSI
        }
    }
}

internal fun VT320.handleVt52Y1(c: Char) {
    currentParam = (c.code - 0x20).coerceIn(0, rows - 1)
    state = VT320.State.VT52_Y2
}

internal fun VT320.handleVt52Y2(c: Char) {
    cursorY = currentParam
    cursorX = (c.code - 0x20).coerceIn(0, columns - 1)
    state = VT320.State.NORMAL
}

/**
 * Initial entry point for Control Sequence Introducer (CSI) parsing.
 */
internal fun VT320.handleCsi(c: Char) {
    when (c) {
        '?' -> {
            isPrivateMode = true
            csiPrefix = c
        }

        '>', '=' -> {
            isPrivateMode = false
            csiPrefix = c
        }

        in '0'..'9' -> {
            state = VT320.State.PARAM
            currentParam = c - '0'
            hasParam = true
        }

        ';' -> {
            params.add(if (hasParam) currentParam else 0)
            currentParam = 0
            hasParam = false
        }

        '$' -> {
            params.add(if (hasParam) currentParam else 0)
            state = VT320.State.DOLLAR
        }

        ' ' -> {
            params.add(if (hasParam) currentParam else 0)
            state = VT320.State.SPACE
        }

        '\"' -> {
            params.add(if (hasParam) currentParam else 0)
            currentParam = 0
            hasParam = false
            state = VT320.State.QUOTE
        }

        '!' -> {
            params.add(if (hasParam) currentParam else 0)
            state = VT320.State.EXCLAMATION
        }

        '&' -> {
            params.add(if (hasParam) currentParam else -1)
            state = VT320.State.AMPERSAND
        }

        else -> {
            params.add(if (hasParam) currentParam else 0)
            executeCsi(c)
            state = VT320.State.NORMAL
        }
    }
}

/**
 * Handles parameter parsing for CSI and DCS sequences.
 */
internal fun VT320.handleParam(c: Char) {
    when (c) {
        in '0'..'9' -> {
            currentParam = currentParam * 10 + (c - '0')
        }

        ';' -> {
            params.add(currentParam)
            currentParam = 0
            hasParam = false
            state = VT320.State.CSI
        }

        '$' -> {
            params.add(currentParam)
            currentParam = 0
            hasParam = false
            state = VT320.State.DOLLAR
        }

        ' ' -> {
            params.add(currentParam)
            currentParam = 0
            hasParam = false
            state = VT320.State.SPACE
        }

        '\"' -> {
            params.add(currentParam)
            currentParam = 0
            hasParam = false
            state = VT320.State.QUOTE
        }

        '!' -> {
            params.add(currentParam)
            currentParam = 0
            hasParam = false
            state = VT320.State.EXCLAMATION
        }

        '&' -> {
            params.add(currentParam)
            currentParam = 0
            hasParam = true // Explicitly marked as having a param
            state = VT320.State.AMPERSAND
        }

        else -> {
            params.add(currentParam)
            executeCsi(c)
            state = VT320.State.NORMAL
        }
    }
}

/**
 * Handles sequences starting with CSI/ESC and a space.
 */
internal fun VT320.handleSpace(c: Char) {
    state = VT320.State.NORMAL
    when (c) {
        'q' -> { // DECSCUSR - Set Cursor Style
            val p = getParam(0, 0)
            cursorStyle = when (p) {
                0, 1, 2 -> CursorStyle.BLOCK
                3, 4 -> CursorStyle.UNDERLINE
                else -> cursorStyle
            }
        }

        'F' -> { // S7C1T - Select 7-bit C1 controls
            eightBitControls = false
            Abc80Log.terminal("VT320: 7-bit C1 controls selected")
        }

        'G' -> { // S8C1T - Select 8-bit C1 controls
            eightBitControls = true
            Abc80Log.terminal("VT320: 8-bit C1 controls selected")
        }

        'L' -> { // Conformance Level 1 (VT100)
            conformanceLevel = 61
            eightBitControls = false
            Abc80Log.terminal("VT320: Conformance Level 1 (VT100) selected")
        }

        'M' -> { // Conformance Level 2 (VT200)
            conformanceLevel = 62
            eightBitControls = true
            Abc80Log.terminal("VT320: Conformance Level 2 (VT200) selected")
        }

        'N' -> { // Conformance Level 3 (VT300)
            conformanceLevel = 63
            eightBitControls = true
            Abc80Log.terminal("VT320: Conformance Level 3 (VT300) selected")
        }
    }
}

/**
 * Handles sequences starting with CSI &.
 */
internal fun VT320.handleAmpersand(c: Char) {
    state = VT320.State.NORMAL
    when (c) {
        'u' -> { // DECAUPSS / DECRQUPSS
            val p = params.getOrNull(0) ?: -1
            if (p == -1) {
                // DECRQUPSS - Request current UPSS. Response: DCS ! u designator ST
                val designator = if (VT320Settings.userPreferredCharacterSet == 0) "<" else "A"
                sendResponse("\u001BP!u$designator\u001B\\")
            } else {
                // DECAUPSS - Assign User Preferred Supplemental Sets
                // p can be 0 or 1. Default (if it were 0) is DEC.
                val isDec = p <= 0
                TerminalManager.userPreferredCharacterSetIsDECSupplementalGraphic = isDec
                VT320Settings.userPreferredCharacterSet = if (isDec) 0 else 1
                updateStatusLine()
            }
        }
    }
}

/**
 * Handles sequences starting with CSI !.
 */
internal fun VT320.handleExclamation(c: Char) {
    state = VT320.State.NORMAL
    when (c) {
        'w' -> { // DECULOCK - User Features Lock
            val p = getParam(0, 0)
            when (p) {
                0, 1 -> { // Lock
                    allow80or132Mode = false
                    VT320Settings.userFeaturesSetting = 1
                }

                2 -> { // Unlock
                    allow80or132Mode = true
                    VT320Settings.userFeaturesSetting = 0
                }
            }
            updateStatusLine()
        }

        'p' -> softReset() // DECSTR - Soft Terminal Reset
    }
}

/**
 * Handles sequences starting with CSI ".
 */
internal fun VT320.handleQuote(c: Char) {
    state = VT320.State.NORMAL
    when (c) {
        'p' -> { // DECSCL - Select Conformance Level
            val level = getParam(0, 63)
            val controls = getParam(1, 0)

            val levelStr = when (level) {
                61 -> "VT100"
                62 -> "VT200"
                63 -> "VT300"
                else -> "VT300 (Level $level)"
            }
            conformanceLevel = level
            val controlStr =
                if (controls == 1 || level == 61) "7-bit controls" else "8-bit controls"
            eightBitControls = if (level == 61) false else (controls == 0 || controls == 2)

            Abc80Log.terminal("VT320: Conformance Level set to $levelStr ($controlStr)")
        }

        'q' -> { // DECSCA - Select Character Protection Attribute
            val p = getParam(0, 0)
            // 1 = protected (not erasable by DECSED/DECSEL)
            // 0 or 2 = not protected (erasable)
            currentAttr = currentAttr.copy(selectiveErase = (p == 1))
        }
    }
}

/**
 * Entry point for Device Control String (DCS) parsing.
 */
internal fun VT320.handleDcs(c: Char) {
    when (c) {
        in '0'..'9' -> {
            currentParam = currentParam * 10 + (c - '0')
            hasParam = true
        }

        ';' -> {
            params.add(currentParam)
            currentParam = 0
            hasParam = false
        }

        '{' -> { // DECDLD - Dynamically Loadable Descriptor (Soft Character Set)
            params.add(if (hasParam) currentParam else 0)
            startDecdld()
            state = VT320.State.DCS_DATA
        }

        '|' -> { // DECUDK - Define User Defined Keys (UDK)
            params.add(if (hasParam) currentParam else 0)
            startUdk()
            state = VT320.State.DCS_UDK
        }

        '$' -> {
            params.add(if (hasParam) currentParam else 0)
            state = VT320.State.DCS_DOLLAR
        }

        else -> {
            state = VT320.State.NORMAL
        }
    }
}

/**
 * Handles data within a DCS string (e.g., DECDLD data).
 */
internal fun VT320.handleDcsData(c: Char) {
    val code = c.code
    if (code == 0x9C) { // ST - String Terminator
        finishDecdld()
        state = VT320.State.NORMAL
        return
    }

    if (code == 0x0A || code == 0x0D) return // Ignore newlines

    // Inside DCS data string
    softCharacterSet?.let {
        if (c == ';') {
            it.done()
        } else {
            val added = it.add(c)
            if (!added) {
                // Abc80Log.terminal("VT320: SoftCharacterSet rejected byte 0x${code.toString(16).uppercase()}")
            }
        }
    }
}

/**
 * Initializes Dynamic Character Set loading (DECDLD).
 */
internal fun VT320.startDecdld() {
    softCharacterSet = CharacterSet.SoftCharacterSet().apply {
        // Pfn ; Pcn ; Pe ; Pcmw ; Pw ; Pt ; Pcmh ; Pcss
        fontNumber = getParam(0, 0)
        startingCharacter = getParam(1, 0)
        eraseControl = getParam(2, 0)
        characterMatrixWidth = getParam(3, 0)
        if (characterMatrixWidth == 0) characterMatrixWidth = 15 // Default for 80 cols
        fontWidth = getParam(4, 0)
        textOrFullCell = getParam(5, 0)
        characterMatrixHeight = getParam(6, 0)
        if (characterMatrixHeight == 0) characterMatrixHeight = 12 // Default
        characterSetSize = getParam(7, 0)
    }
    Abc80Log.terminal("VT320: Started DECDLD with params $params")
}

/**
 * Finalizes Dynamic Character Set loading.
 */
internal fun VT320.finishDecdld() {
    softCharacterSet?.let {
        it.finish()
        graphics.characterSets.drcsFontBuffer.load(it)
        Abc80Log.terminal("VT320: Finished DECDLD. Designator: '${it.designator()}'")
    }
    softCharacterSet = null
}

/**
 * Handles data within a User Defined Key (UDK) string.
 */
internal fun VT320.handleDcsUdk(c: Char) {
    val code = c.code
    if (code == 0x9C) { // ST - String Terminator
        finishUdkEntry()
        state = VT320.State.NORMAL
        return
    }

    when (c) {
        ';' -> finishUdkEntry()
        '/' -> {
            udkKeyNum = udkBuffer.toIntOrNull() ?: -1
            udkBuffer = ""
        }

        else -> {
            if (c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F') {
                udkBuffer += c
            }
        }
    }
}

/**
 * Initializes User Defined Key (UDK) definition.
 */
internal fun VT320.startUdk() {
    val clearMode = getParam(0, 0)
    val lockMode = getParam(1, 0) // Pw: 0=lock, 1=don't lock

    if (clearMode == 0) {
        (keyboard as? VT320Keyboard)?.clearUdks()
    }

    // Pw = 0 means keys are locked (cannot be redefined until next reset/clear)
    // Pw = 1 means keys are not locked.
    udkLocked = (lockMode == 0)
    VT320Settings.userDefinedKeys = if (udkLocked) 1 else 0

    udkBuffer = ""
    udkKeyNum = -1
    Abc80Log.terminal("VT320: Started UDK definition with params $params")
}

/**
 * Finalizes a single UDK entry.
 */
internal fun VT320.finishUdkEntry() {
    if (udkKeyNum != -1 && udkBuffer.isNotEmpty()) {
        val definition = hexToString(udkBuffer)
        (keyboard as? VT320Keyboard)?.defineUdk(udkKeyNum, definition)
        Abc80Log.terminal("VT320: Defined UDK $udkKeyNum = '$definition'")
    }
    udkKeyNum = -1
    udkBuffer = ""
}

/**
 * Converts a hex string to a regular string.
 */
internal fun VT320.hexToString(hex: String): String {
    val sb = StringBuilder()
    var i = 0
    while (i < hex.length) {
        if (i + 1 < hex.length) {
            val byte = hex.substring(i, i + 2).toIntOrNull(16)
            if (byte != null) sb.append(byte.toChar())
            i += 2
        } else {
            i++
        }
    }
    return sb.toString()
}

internal fun VT320.handleDcsDollar(c: Char) {
    when (c) {
        't' -> {
            state = VT320.State.DCS_RSPS
            udkBuffer = "" // Reuse buffer for DECRSPS data
        }

        'q' -> {
            state = VT320.State.DCS_RQSS
            udkBuffer = "" // Reuse buffer for DECRQSS request data
        }

        else -> {
            state = VT320.State.NORMAL
        }
    }
}

internal fun VT320.handleDcsRqss(c: Char) {
    val code = c.code
    if (code == 0x9C) { // ST
        finishRqss()
        state = VT320.State.NORMAL
        return
    }
    udkBuffer += c
}

private fun VT320.finishRqss() {
    when (val request = udkBuffer) {
        "m" -> { // SGR - Set Graphics Rendition
            val params = mutableListOf<Int>()
            if (currentAttr.bold) params.add(1)
            if (currentAttr.underline) params.add(4)
            if (currentAttr.blink) params.add(5)
            if (currentAttr.inverse) params.add(7)
            val pStr = if (params.isEmpty()) "0" else "0;" + params.joinToString(";")
            sendResponse("\u001BP1\$r${pStr}m\u001B\\")
        }

        "r" -> { // DECSTBM - Set Scrolling Region
            sendResponse("\u001BP1\$r${topMargin + 1};${bottomMargin + 1}r\u001B\\")
        }

        "\"q" -> { // DECSCA - Select Character Protection Attribute
            val p = if (currentAttr.selectiveErase) 1 else 0
            sendResponse("\u001BP1\$r$p\"q\u001B\\")
        }

        "\"p" -> { // DECSCL - Select Conformance Level
            val controls = if (eightBitControls) 0 else 1
            sendResponse("\u001BP1\$r$conformanceLevel;$controls\"p\u001B\\")
        }

        "\$}" -> { // DECSASD - Select Active Status Display
            val p = if (activeStatusLine) 1 else 0
            sendResponse("\u001BP1\$r$p\$}\u001B\\")
        }

        "\$~" -> { // DECSSDT - Select Status Display Type
            val p = when (statusLineManager.mode) {
                StatusLineManager.Mode.NONE -> 0
                StatusLineManager.Mode.INDICATOR -> 1
                StatusLineManager.Mode.HOST_WRITABLE -> 2
            }
            sendResponse("\u001BP1\$r$p\$~\u001B\\")
        }

        "&u" -> { // DECAUPSS - User Preferred Supplemental Set
            val p = VT320Settings.userPreferredCharacterSet
            sendResponse("\u001BP1\$r$p&u\u001B\\")
        }

        else -> {
            // Invalid or unsupported request
            sendResponse("\u001BP0\$r$request\u001B\\")
        }
    }
    udkBuffer = ""
}

internal fun VT320.handleDcsRsps(c: Char) {
    val code = c.code
    if (code == 0x9C) { // ST
        finishRsps()
        state = VT320.State.NORMAL
        return
    }
    udkBuffer += c
}

private fun VT320.finishRsps() {
    val p = getParam(0, 0)
    when (p) {
        2 -> { // Restore Tab Stops
            tabulator.clearAll()
            val stops = udkBuffer.split('/')
            for (s in stops) {
                val col = s.toIntOrNull()
                if (col != null && col in 1..columns) {
                    tabulator.set(col - 1, true)
                }
            }
        }
    }
    udkBuffer = ""
}

/**
 * Handles sequences starting with CSI $.
 */
internal fun VT320.handleDollar(c: Char) {
    state = VT320.State.NORMAL
    when (c) {
        'p' -> { // DECRQM - Request Mode
            val p = getParam(0, 0)
            if (isPrivateMode) {
                // DEC Private Mode
                val mode = when (p) {
                    1 -> if (cursorKeysMode) 1 else 2
                    3 -> if (columns == 132) 1 else 2
                    4 -> if (smoothScroll) 1 else 2
                    5 -> if (screenReverse) 1 else 2
                    6 -> if (originMode) 1 else 2
                    7 -> if (autoWrap) 1 else 2
                    8 -> if (autoRepeatMode) 1 else 2
                    18 -> if (VT320Settings.printTerminator) 1 else 2
                    19 -> if (!VT320Settings.printerScreenSize) 1 else 2
                    25 -> if (cursorVisible) 1 else 2
                    42 -> if (VT320Settings.nationalReplacement) 1 else 2
                    68 -> if (keyboardUsageMode) 1 else 2
                    else -> 0 // Not recognized
                }
                sendResponse("\u001B[?${p};${mode}\$y")
            } else {
                // ANSI Mode
                val mode = when (p) {
                    2 -> if (keyboardLocked) 1 else 2
                    4 -> if (insertMode) 1 else 2
                    12 -> if (localEcho) 2 else 1
                    20 -> if (newLineMode) 1 else 2
                    else -> 0
                }
                sendResponse("\u001B[${p};${mode}\$y")
            }
        }

        't' -> { // DECRQPSR - Request Presentation State Report
            val p = getParam(0, 0)
            when (p) {
                1 -> { // DECCIR - Cursor Information Report
                    // dummy report for now
                    sendResponse("\u001BP1\$u${cursorY + 1};${cursorX + 1};1;0;0;0;0;2;B;0;V;A\u001B\\")
                }

                2 -> { // DECTABSR - Tab Stop Report
                    val stops = mutableListOf<Int>()
                    for (i in 0 until 132) {
                        if (tabulator.stops[i]) stops.add(i + 1)
                    }
                    val data = stops.joinToString("/")
                    sendResponse("\u001BP2\$u$data\u001B\\")
                }
            }
        }

        '}' -> { // DECSASD - Select Active Status Display
            val p = getParam(0, 0)
            val newActiveStatusLine = (p == 1)
            if (newActiveStatusLine != activeStatusLine) {
                if (newActiveStatusLine) {
                    // Switch to status line: save main cursor
                    savedMainCursorX = cursorX
                    savedMainCursorY = cursorY
                    cursorX = statusLineCursorX

                    // Host-writable mode is required for status line to be active
                    statusLineManager.mode = StatusLineManager.Mode.HOST_WRITABLE
                } else {
                    // Switch back to main display: save status cursor and restore main
                    statusLineCursorX = cursorX
                    cursorX = savedMainCursorX
                    cursorY = savedMainCursorY
                }
                activeStatusLine = newActiveStatusLine
                updateStatusLine()
            }
        }

        '~' -> { // DECSSDT - Select Status Display Type
            val p = getParam(0, 0)
            statusLineManager.mode = when (p) {
                1 -> StatusLineManager.Mode.INDICATOR
                2 -> StatusLineManager.Mode.HOST_WRITABLE
                else -> StatusLineManager.Mode.NONE
            }
            updateStatusLine()
        }

        'u' -> { // DECRQTSR - Request Terminal State Report
            val p = getParam(0, 0)
            if (p == 1) {
                // Respond with DECTSR: DCS 1 $ s D...D ST
                sendResponse("\u001BP1\$s0\u001B\\")
            }
        }

        'w' -> { // DECRSTS - Request Status
            val p = getParam(0, 0)
            if (p == 1) {
                // Request Terminal Attributes. Response: DCS 1 $ r Pt m ST
                sendResponse("\u001BP1\$r0m\u001B\\")
            } else {
                sendResponse("\u001BP0\$r\u001B\\")
            }
        }
    }
}

/**
 * Executes a parsed Control Sequence Introducer (CSI) command.
 */
internal fun VT320.executeCsi(c: Char) {
    when (c) {
        '@' -> if (!activeStatusLine) insertChar(getParam(0, 1)) // ICH - Insert Character
        'A' -> if (!activeStatusLine) {
            val min = if (originMode) topMargin else 0
            cursorY = (cursorY - getParam(0, 1)).coerceAtLeast(min)
        } // CUU - Cursor Up
        'B' -> if (!activeStatusLine) {
            val max = if (originMode) bottomMargin else rows - 1
            cursorY = (cursorY + getParam(0, 1)).coerceAtMost(max)
        } // CUD - Cursor Down
        'C' -> cursorX =
            (cursorX + getParam(0, 1)).coerceAtMost(columns - 1) // CUF - Cursor Forward
        'D' -> cursorX =
            (cursorX - getParam(0, 1)).coerceAtLeast(0)           // CUB - Cursor Backward
        'H', 'f' -> { // CUP / HVP - Cursor Position / Horizontal Vertical Position
            if (activeStatusLine) {
                cursorX = (getParam(1, 1) - 1).coerceIn(0, columns - 1)
                // cursorY is fixed to status line
            } else {
                val row = getParam(0, 1)
                val col = getParam(1, 1)
                if (originMode) {
                    cursorY = (topMargin + row - 1).coerceIn(topMargin, bottomMargin)
                    cursorX = (col - 1).coerceIn(0, columns - 1)
                } else {
                    cursorY = (row - 1).coerceIn(0, rows - 1)
                    cursorX = (col - 1).coerceIn(0, columns - 1)
                }
            }
        }

        'g' -> { // TBC - Tabulation Clear
            when (getParam(0, 0)) {
                0 -> tabulator.set(cursorX, false)
                3 -> tabulator.clearAll()
            }
        }

        'J' -> { // ED - Erase In Display / DECSED - Selective Erase In Display
            if (activeStatusLine) statusLineManager.erase(getParam(0, 0), cursorX)
            else eraseInDisplayInternal(getParam(0, 0), isPrivateMode)
        }

        'K' -> { // EL - Erase In Line / DECSEL - Selective Erase In Line
            if (activeStatusLine) statusLineManager.erase(getParam(0, 0), cursorX)
            else eraseInLineInternal(getParam(0, 0), isPrivateMode)
        }

        'L' -> if (!activeStatusLine) insertLine(getParam(0, 1)) // IL - Insert Line
        'M' -> if (!activeStatusLine) deleteLine(getParam(0, 1)) // DL - Delete Line
        'P' -> if (!activeStatusLine) deleteChar(getParam(0, 1)) // DCH - Delete Character
        'X' -> if (!activeStatusLine) eraseChar(getParam(0, 1))  // ECH - Erase Character
        'c' -> { // DA - Device Attributes
            when (csiPrefix) {
                '>' -> {
                    // CSI > c - Secondary Device Attributes
                    sendResponse("\u001B[>1;10;0c") // VT320 version 1.0
                }

                '=' -> {
                    // CSI = c - Third-level Device Attributes
                    // Response: DCS ! | 00000000 ST
                    sendResponse("\u001BP!|00000000\u001B\\")
                }

                else -> {
                    // CSI c - Primary Device Attributes
                    val idIndex = VT320Settings.terminalModeId

                    val id = when (conformanceLevel) {
                        61 -> when (idIndex) {
                            2 -> "1;0" // VT101
                            3 -> "6"   // VT102
                            else -> "1;2" // VT100
                        }

                        62 -> "62;1;2;6;7;8" + if (eightBitControls) ";9" else ""
                        63 -> when (idIndex) {
                            4 -> "62;1;2;6;7;8" + if (eightBitControls) ";9" else ""
                            else -> "63;1;2;6;7;8" + if (eightBitControls) ";9" else ""
                        }

                        else -> "63;1;2;6;7;8" + if (eightBitControls) ";9" else ""
                    }
                    sendResponse("\u001B[?${id}c")
                }
            }
        }

        'm' -> setGraphicsRendition() // SGR - Set Graphics Rendition
        'q' -> { // DECLL - Load LEDs
            if (isPrivateMode) {
                // DECSCUSR - Set Cursor Style handled in handleSpace
            }
            if (params.isEmpty()) {
                keyboard.setLed(0, true)
            } else {
                for (p in params) {
                    keyboard.setLed(p, true)
                }
            }
        }

        'r' -> { // DECSTBM - Set Scrolling Region
            topMargin = (getParam(0, 1) - 1).coerceIn(0, rows - 1)
            bottomMargin = (getParam(1, rows) - 1).coerceIn(topMargin, rows - 1)
            cursorX = 0
            cursorY = 0
        }

        'i' -> { // MC - Media Copy
            val p = getParam(0, 0)
            if (isPrivateMode) {
                when (p) {
                    1 -> printCurrentLine()   // DEC Private MC ? 1 i - Print Line
                    4 -> printerStatus =
                        PrinterStatus.READY // DEC Private MC ? 4 i - Auto Print Off
                    5 -> printerStatus = PrinterStatus.AUTO  // DEC Private MC ? 5 i - Auto Print On
                }
            } else {
                when (p) {
                    0 -> printScreen()        // ANSI MC 0 i - Print Screen
                    4 -> printerStatus = PrinterStatus.READY // ANSI MC 4 i - Printer Controller Off
                    5 -> printerStatus =
                        PrinterStatus.CONTROLLER // ANSI MC 5 i - Printer Controller On
                }
            }
        }

        'n' -> { // DSR - Device Status Report
            val p = getParam(0, 0)
            if (isPrivateMode) {
                when (p) {
                    15 -> { // Printer Status
                        if (VT320Settings.printerToHost == 1) {
                            val resp = when (printerStatus) {
                                PrinterStatus.READY, PrinterStatus.AUTO, PrinterStatus.CONTROLLER -> 10
                                PrinterStatus.NOT_READY -> 11
                                PrinterStatus.NONE -> 13
                            }
                            sendResponse("\u001B[?${resp}n")
                        }
                    }

                    25 -> { // UDK Status
                        sendResponse(if (udkLocked) "\u001B[?21n" else "\u001B[?20n")
                    }

                    26 -> { // Keyboard Language Report
                        val lang = VT320Settings.keyboardLanguage + 1
                        sendResponse("\u001B[?27;${lang}n")
                    }
                }
            } else {
                when (p) {
                    5 -> sendResponse("\u001B[0n") // Terminal OK
                    6 -> sendResponse("\u001B[${cursorY + 1};${cursorX + 1}R") // Cursor position (CPR)
                    62 -> { // DECMSR - Macro Space Report
                        sendResponse("\u001B[0*{")
                    }

                    63 -> { // DECCKSR - Memory Checksum Report
                        // Response: DCS Ps ! ~ d d d d ST
                        // Using Ps from request, checksum 0000
                        val ps = getParam(0, 0)
                        sendResponse("\u001BP${ps}!~0000\u001B\\")
                    }
                }
            }
        }

        'h' -> { // SM - Set Mode / DECSET - DEC Private Mode Set
            Abc80Log.terminal("VT320: SM/DECSET params=$params isPrivate=$isPrivateMode")
            params.forEach { p ->
                if (isPrivateMode) {
                    when (p) {
                        1 -> { // DECCKM - Cursor Keys Mode
                            VT320Settings.cursorKeysMode = 1
                            cursorKeysMode = true
                        }

                        2 -> { // DECANM - ANSI Mode
                            conformanceLevel = 63
                            TerminalManager.operatingMode = TerminalManager.OperatingMode.ANSI
                        }

                        3 -> if (allow80or132Mode) setColumnMode(132) // DECCOLM - Column Mode (132)
                        4 -> { // DECSCLM - Scrolling Mode (Smooth)
                            VT320Settings.scrollMode = true
                            smoothScroll = true
                        }

                        5 -> { // DECSCNM - Screen Mode (Reverse)
                            VT320Settings.screenDisplayType = 1
                            screenReverse = true
                        }

                        6 -> { // DECOM - Origin Mode
                            originMode = true
                            cursorX = 0
                            cursorY = topMargin
                        }

                        7 -> { // DECAWN - Autowrap Mode
                            autoWrap = true
                            VT320Settings.wrapMode = true
                        }

                        8 -> { // DECARM - Auto Repeat Mode
                            autoRepeatMode = true
                            VT320Settings.autoRepeat = true
                        }

                        18 -> VT320Settings.printTerminator = true // DECPFF - Print Form Feed On
                        19 -> VT320Settings.printerScreenSize = false // DECPEX - Print Full Screen
                        25 -> _cursorVisible = true // DECTCEM - Text Cursor Enable
                        42 -> { // DECNRCM - National Replacement Character Set Mode
                            VT320Settings.characterSetMode = 0 // 7-bit
                            VT320Settings.nationalReplacement = true
                            if (!setup.isVisible) {
                                characterSetMode8Bit = false
                            }
                            graphics.refreshDesignations()
                        }

                        68 -> keyboardUsageMode = true // DECKBUM - Data Processing
                    }
                } else {
                    when (p) {
                        2 -> keyboardLocked = true // KAM - Locked
                        4 -> insertMode = true // IRM - Insertion Mode
                        12 -> { // SRM - Send/Receive Mode (Local Echo Off)
                            VT320Settings.localEcho = false
                            localEcho = false
                        }

                        20 -> { // LNM - Line Feed/New Line Mode
                            VT320Settings.newLineMode = 1
                            newLineMode = true
                        }
                    }
                }
            }
        }

        'l' -> { // RM - Reset Mode / DECRST - DEC Private Mode Reset
            Abc80Log.terminal("VT320: RM/DECRST params=$params isPrivate=$isPrivateMode")
            params.forEach { p ->
                if (isPrivateMode) {
                    when (p) {
                        1 -> { // DECCKM - Cursor Keys Mode
                            VT320Settings.cursorKeysMode = 0
                            cursorKeysMode = false
                        }

                        2 -> { // DECANM - Reset switches to VT52
                            conformanceLevel = 52
                            TerminalManager.operatingMode = TerminalManager.OperatingMode.VT52
                        }

                        3 -> if (allow80or132Mode) setColumnMode(80) // DECCOLM - Column Mode (80)
                        4 -> { // DECSCLM - Scrolling Mode (Jump)
                            VT320Settings.scrollMode = false
                            smoothScroll = false
                        }

                        5 -> { // DECSCNM - Screen Mode (Normal)
                            VT320Settings.screenDisplayType = 0
                            screenReverse = false
                        }

                        6 -> { // DECOM - Origin Mode
                            originMode = false
                            cursorX = 0
                            cursorY = 0
                        }

                        7 -> { // DECAWN - Autowrap Mode
                            autoWrap = false
                            VT320Settings.wrapMode = false
                        }

                        8 -> { // DECARM - Auto Repeat Mode
                            autoRepeatMode = false
                            VT320Settings.autoRepeat = false
                        }

                        18 -> VT320Settings.printTerminator = false // DECPFF - Print Form Feed Off
                        19 -> VT320Settings.printerScreenSize =
                            true // DECPEX - Print Scrolling Region
                        25 -> _cursorVisible = false // DECTCEM - Text Cursor Enable
                        42 -> { // DECNRCM - Multinational Mode (8-bit)
                            VT320Settings.characterSetMode = 1
                            VT320Settings.nationalReplacement = false
                            if (!setup.isVisible) {
                                characterSetMode8Bit = true
                            }
                            graphics.refreshDesignations()
                        }

                        68 -> keyboardUsageMode = false // DECKBUM - Typewriter
                    }
                } else {
                    when (p) {
                        2 -> keyboardLocked = false // KAM - Unlocked
                        4 -> insertMode = false // IRM - Replacement Mode
                        12 -> { // SRM - Send/Receive Mode (Local Echo On)
                            VT320Settings.localEcho = true
                            localEcho = true
                        }

                        20 -> { // LNM - Line Feed/New Line Mode
                            VT320Settings.newLineMode = 0
                            newLineMode = false
                        }
                    }
                }
            }
        }

        'y' -> { // DECTST - Confidence Test
            val p = getParam(0, 0)
            if (p == 1) {
                ris() // Power-up reset
            }
        }
    }
}

/**
 * Returns a parameter value by index, with a default fallback.
 */
internal fun VT320.getParam(index: Int, default: Int): Int {
    if (index >= params.size) return default
    val p = params[index]
    return if (p == 0) default else p
}

/**
 * Handles SGR (Set Graphics Rendition) parameters.
 */
internal fun VT320.setGraphicsRendition() {
    if (params.isEmpty()) {
        currentAttr = TerminalAttributes()
        return
    }
    for (p in params) {
        when (p) {
            0 -> currentAttr = TerminalAttributes()           // Reset
            1 -> currentAttr = currentAttr.copy(bold = true)   // Bold
            4 -> currentAttr = currentAttr.copy(underline = true) // Underline
            5 -> currentAttr = currentAttr.copy(blink = true)  // Blink
            7 -> currentAttr = currentAttr.copy(inverse = true) // Inverse
            22 -> currentAttr = currentAttr.copy(bold = false)  // Normal intensity
            24 -> currentAttr = currentAttr.copy(underline = false) // Not underlined
            25 -> currentAttr = currentAttr.copy(blink = false) // Not blinking
            27 -> currentAttr = currentAttr.copy(inverse = false) // Positive image
        }
    }
}
