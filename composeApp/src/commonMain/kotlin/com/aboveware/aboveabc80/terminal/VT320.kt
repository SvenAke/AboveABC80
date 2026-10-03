package com.aboveware.aboveabc80.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.aboveware.aboveabc80.Abc80Log
import com.aboveware.aboveabc80.playBell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * VT320 Terminal Emulator implementation.
 */
class VT320 : Terminal {
    companion object

    var columns by mutableIntStateOf(Terminal.DEFAULT_WIDTH)
        private set
    val rows = Terminal.DEFAULT_HEIGHT

    override var screen by mutableStateOf(Array(rows) { Array(columns) { TerminalCell() } })
    override var cursorX by mutableIntStateOf(0)
    override var cursorY by mutableIntStateOf(0)
    override val backgroundColor: Color = Color(0xFFD1D1C9)

    var currentKeyboardLanguage by mutableIntStateOf(VT320Settings.keyboardLanguage)
        private set

    override val keyboardXmlResId: Int
        get() = getVT320LayoutId(currentKeyboardLanguage)
    override val keyboard: TerminalKeyboard = VT320Keyboard(this)
    override val tabulator: Tabulator = Tabulator()

    override val nominalWidth: Int get() = if (columns == 132) 9 else 15
    override val nominalHeight: Int get() = 12
    override val dotStretch: Float = 2.0f

    override var onBell: (() -> Unit)? = { playBell() }
    override var onKeyClick: (() -> Unit)? = null

    override fun triggerClick() {
        if (VT320Settings.keyClick == 1) {
            onKeyClick?.invoke()
        }
    }

    override val isSetupVisible: Boolean get() = setup.isVisible

    val statusLineManager = StatusLineManager(this)
    var printerStatus: PrinterStatus
        get() = statusLineManager.printerStatus
        set(value) {
            statusLineManager.printerStatus = value
            updateStatusLine()
        }
    var setupPrinterStatus by mutableStateOf(PrinterStatus.READY)
    var statusLine: Array<TerminalCell>
        get() = statusLineManager.cells
        set(value) {
            statusLineManager.cells = value
        }

    fun updateStatusLine(setupIndicator: String? = null) {
        if (setup.isVisible && setupIndicator == null) return
        statusLineManager.update(setupIndicator)
    }

    internal val inputBuffer = mutableListOf<Int>()
    internal val lock = Any()
    internal val graphics = Graphics()

    enum class LineAttribute { NORMAL, DOUBLE_WIDTH, DOUBLE_HEIGHT_TOP, DOUBLE_HEIGHT_BOTTOM }

    var lineAttributes by mutableStateOf(Array(rows) { LineAttribute.NORMAL })

    internal enum class State {
        NORMAL, ESC, CSI, PARAM, HASH, DOLLAR, SPACE, QUOTE, EXCLAMATION, AMPERSAND,
        DESIGNATE_G0, DESIGNATE_G1, DESIGNATE_G2, DESIGNATE_G3,
        DESIGNATE96_G1, DESIGNATE96_G2, DESIGNATE96_G3,
        EXT_G0, EXT_G1, EXT_G2, EXT_G3,
        EXT96_G1, EXT96_G2, EXT96_G3,
        DCS, DCS_DATA, DCS_UDK, DCS_DOLLAR, DCS_RSPS, DCS_RQSS,
        VT52_Y1, VT52_Y2
    }

    internal var state = State.NORMAL
    internal val params = mutableListOf<Int>()
    internal var currentParam = 0
    internal var hasParam = false
    internal var isPrivateMode = false
    internal var csiPrefix = '\u0000'
    internal var _cursorVisible by mutableStateOf(true)
    override val cursorVisible: Boolean get() = _cursorVisible && state == State.NORMAL && !isBooting
    override var cursorStyle by mutableStateOf(CursorStyle.BLOCK)

    internal var designatorBuffer = ""
    internal var softCharacterSet: CharacterSet.SoftCharacterSet? = null
    internal var lastStateBeforeEsc = State.NORMAL

    var activeStatusLine = false // Selected via DECSASD
        internal set
    internal var statusLineCursorX = 0
    internal var savedMainCursorX = 0
    internal var savedMainCursorY = 0

    override var screenReverse by mutableStateOf(false)
    override var autoRepeatMode by mutableStateOf(true)

    var currentAttr = TerminalAttributes()
        internal set
    internal var savedCursorX = 0
    internal var savedCursorY = 0
    internal var savedAttr = TerminalAttributes()

    var topMargin = 0
        internal set
    var bottomMargin = rows - 1
        internal set

    var originMode by mutableStateOf(false) // DECOM
    var insertMode by mutableStateOf(false) // IRM
    override var holdScreen by mutableStateOf(false) // Hold Screen
    var keyboardLocked by mutableStateOf(false) // KAM
    var keyboardUsageMode by mutableStateOf(false) // DECKBUM (false = Typewriter, true = Data Processing)
    var vt52GraphicsMode by mutableStateOf(false) // VT52 Graphics Mode

    var scrollOffset by mutableFloatStateOf(0f)
    var scrolledOutLine by mutableStateOf<Array<TerminalCell>?>(null)
    internal var scrollJob: Job? = null
    internal val terminalScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private var isXoffReceivedFromHost = false
    private var isXoffReceivedFromPrinter = false

    private var _interpretControls by mutableStateOf(true)
    var interpretControls: Boolean
        get() = _interpretControls
        set(value) {
            _interpretControls = value
            if (!value) state = State.NORMAL // Stop any ongoing sequence when entering display mode
        }

    var autoWrap by mutableStateOf(false)
    var characterSetMode8Bit by mutableStateOf(true)
    override var conformanceLevel by mutableIntStateOf(63) // 61=VT100, 62=VT200, 63=VT300
    var eightBitControls by mutableStateOf(false)
    var udkLocked by mutableStateOf(false)
    var smoothScroll by mutableStateOf(false)
    var allow80or132Mode by mutableStateOf(true)
    override var cursorKeysMode by mutableStateOf(false) // false = Normal, true = Application
    override var keypadMode by mutableStateOf(false)     // false = Numeric, true = Application
    override var newLineMode by mutableStateOf(false)    // false = LF, true = CRLF
    var localEcho by mutableStateOf(false)
    internal var inputSeq = ""
    internal var escFlushJob: Job? = null
    internal var controllerModeSeq = ""
    internal var udkBuffer = ""
    internal var udkKeyNum = -1

    val setup = VTSetup(this)

    init {
        // Initialize column mode
        val colIndex = VT320Settings.columnMode
        columns = if (colIndex == 0) 80 else 132

        // Initialize controls mode
        interpretControls = VT320Settings.controlsMode == 0

        // Initialize auto wrap
        autoWrap = VT320Settings.wrapMode

        // Initialize auto-repeat (Default to ON)
        autoRepeatMode = VT320Settings.autoRepeat

        // Initialize smooth scroll
        smoothScroll = VT320Settings.scrollMode

        // Initialize status line mode from settings
        val modeIndex = VT320Settings.statusDisplay
        statusLineManager.mode =
            StatusLineManager.Mode.entries.getOrElse(modeIndex) { StatusLineManager.Mode.INDICATOR }
        updateStatusLine()

        // Initialize screen reverse mode
        screenReverse = VT320Settings.screenDisplayType == 1

        // Initialize cursor visibility
        _cursorVisible = VT320Settings.textCursor == 0

        // Initialize cursor style
        cursorStyle =
            if (VT320Settings.cursorStyle == 0) CursorStyle.BLOCK else CursorStyle.UNDERLINE

        // Initialize user features (Allow 80/132 Mode)
        allow80or132Mode = VT320Settings.userFeaturesSetting == 0

        // Initialize other modes
        cursorKeysMode = VT320Settings.cursorKeysMode == 1
        keypadMode = VT320Settings.keypadMode == 1
        newLineMode = VT320Settings.newLineMode == 1
        localEcho = VT320Settings.localEcho

        // Initialize operating mode (7-bit vs 8-bit controls)
        val opModeIndex = VT320Settings.nationalOnly

        // Initialize user preferred character set
        TerminalManager.userPreferredCharacterSetIsDECSupplementalGraphic =
            VT320Settings.userPreferredCharacterSet == 0

        characterSetMode8Bit = VT320Settings.characterSetMode == 1

        conformanceLevel = when (opModeIndex) {
            2 -> 61 // VT100
            3 -> 52 // VT52 (though DA isn't usually used in VT52)
            else -> 63 // VT300
        }
        eightBitControls = (opModeIndex == 1)

        // Initialize User Defined Keys setting
        udkLocked = VT320Settings.userDefinedKeys == 1

        applyKeyboardLanguage()
    }

    fun setColumnMode(cols: Int) {
        if (columns == cols) return
        columns = cols
        screen = Array(rows) { Array(columns) { TerminalCell() } }
        cursorX = 0
        cursorY = 0
        topMargin = 0
        bottomMargin = rows - 1
        lineAttributes = Array(rows) { LineAttribute.NORMAL }
        Abc80Log.terminal("VT320: Column mode changed to $cols and screen cleared")
    }

    fun applyKeyboardLanguage(index: Int = VT320Settings.keyboardLanguage) {
        currentKeyboardLanguage = index
        val designator = when (index) {
            0 -> "B" // North American (ASCII)
            1 -> "A" // British (DEC UK)
            2 -> "R" // Flemish (Belgian/French NRC - AZERTY layout)
            3 -> "Q" // Canadian (French)
            4 -> "E" // Danish (DEC Norwegian/Danish)
            5 -> "5" // Finnish
            6 -> "K" // German
            7 -> "4" // Dutch
            8 -> "Y" // Italian
            9 -> "=" // Swiss (French)
            10 -> "=" // Swiss (German)
            11 -> "7" // Swedish
            12 -> "E" // Norwegian (DEC Norwegian/Danish)
            13 -> "R" // French/Belgian (DEC French)
            14 -> "Z" // Spanish
            15 -> "%6" // Portuguese (DEC Portuguese)
            else -> "B"
        }
        graphics.designateGraphicSet(Graphics.CharacterSetIndex.G0, designator)

        // National keyboards typically use National Replacement Character Mode (7-bit)
        if (index != 0) {
            VT320Settings.nationalReplacement = true
            VT320Settings.characterSetMode = 0 // 7-bit
            if (!setup.isVisible) {
                characterSetMode8Bit = false
            }
        } else {
            VT320Settings.nationalReplacement = false
            VT320Settings.characterSetMode = 1 // 8-bit
            if (!setup.isVisible) {
                characterSetMode8Bit = true
            }
        }
        graphics.refreshDesignations()
    }

    override fun putChar(c: Char) {
        while (holdScreen && !setup.isVisible) {
            // Wait for user to release Hold Screen
            // Using a small delay to avoid pegging the CPU.
            // This is called from the emulator/CPU thread.
            try {
                // Thread.sleep is used in BIOS.kt so we use it here too
                Thread.sleep(10)
            } catch (_: Exception) {
            }
        }

        if (isBooting) {
            synchronized(lock) {
                bootBuffer.add(c)
            }
            return
        }

        synchronized(lock) {
            val code = c.code
            val maskedChar =
                if (!characterSetMode8Bit && code >= 32) (code and 0x7F).toChar() else c

            if (printerStatus == PrinterStatus.CONTROLLER && !setup.isVisible) {
                handleControllerMode(maskedChar)
                updateStatusLine()
                return
            }

            if (!interpretControls && !setup.isVisible) {
                drawChar(maskedChar)
                updateStatusLine()
                return
            }

            if (eightBitControls && state == State.NORMAL && maskedChar.code in 0x80..0x9F) {
                handleC1(maskedChar)
                updateStatusLine()
                return
            }

            // Receipt of CAN or SUB cancels DCS
            if (code == 0x18 || code == 0x1A) {
                if (state == State.DCS_DATA) {
                    softCharacterSet = null
                    state = State.NORMAL
                }
                if (state == State.DCS_UDK || state == State.DCS_RSPS) {
                    state = State.NORMAL
                }
            }

            if (maskedChar.code == 0x1B) {
                if (state == State.DCS_DATA) {
                    finishDecdld()
                }
                if (state == State.DCS_UDK) {
                    finishUdkEntry()
                }
                lastStateBeforeEsc = state
                state = State.ESC
                return
            }

            when (state) {
                State.NORMAL -> handleNormal(maskedChar)
                State.ESC -> handleEsc(maskedChar)
                State.CSI -> handleCsi(maskedChar)
                State.PARAM -> handleParam(maskedChar)
                State.DCS -> handleDcs(maskedChar)
                State.DCS_DATA -> handleDcsData(maskedChar)
                State.DCS_UDK -> handleDcsUdk(maskedChar)
                State.DCS_DOLLAR -> handleDcsDollar(maskedChar)
                State.DCS_RSPS -> handleDcsRsps(maskedChar)
                State.DCS_RQSS -> handleDcsRqss(maskedChar)
                State.HASH -> handleHash(maskedChar)
                State.DOLLAR -> handleDollar(maskedChar)
                State.SPACE -> handleSpace(maskedChar)
                State.QUOTE -> handleQuote(maskedChar)
                State.EXCLAMATION -> handleExclamation(maskedChar)
                State.AMPERSAND -> handleAmpersand(maskedChar)
                State.VT52_Y1 -> handleVt52Y1(maskedChar)
                State.VT52_Y2 -> handleVt52Y2(maskedChar)
                State.DESIGNATE_G0 -> handleDesignate(
                    maskedChar,
                    Graphics.CharacterSetIndex.G0,
                    State.EXT_G0,
                    false
                )

                State.DESIGNATE_G1 -> handleDesignate(
                    maskedChar,
                    Graphics.CharacterSetIndex.G1,
                    State.EXT_G1,
                    false
                )

                State.DESIGNATE_G2 -> handleDesignate(
                    maskedChar,
                    Graphics.CharacterSetIndex.G2,
                    State.EXT_G2,
                    false
                )

                State.DESIGNATE_G3 -> handleDesignate(
                    maskedChar,
                    Graphics.CharacterSetIndex.G3,
                    State.EXT_G3,
                    false
                )

                State.DESIGNATE96_G1 -> handleDesignate(
                    maskedChar,
                    Graphics.CharacterSetIndex.G1,
                    State.EXT96_G1,
                    true
                )

                State.DESIGNATE96_G2 -> handleDesignate(
                    maskedChar,
                    Graphics.CharacterSetIndex.G2,
                    State.EXT96_G2,
                    true
                )

                State.DESIGNATE96_G3 -> handleDesignate(
                    maskedChar,
                    Graphics.CharacterSetIndex.G3,
                    State.EXT96_G3,
                    true
                )

                State.EXT_G0 -> {
                    graphics.designateGraphicSet(
                        Graphics.CharacterSetIndex.G0,
                        "%$maskedChar",
                        false
                    ); state = State.NORMAL
                }

                State.EXT_G1 -> {
                    graphics.designateGraphicSet(
                        Graphics.CharacterSetIndex.G1,
                        "%$maskedChar",
                        false
                    ); state = State.NORMAL
                }

                State.EXT_G2 -> {
                    graphics.designateGraphicSet(
                        Graphics.CharacterSetIndex.G2,
                        "%$maskedChar",
                        false
                    ); state = State.NORMAL
                }

                State.EXT_G3 -> {
                    graphics.designateGraphicSet(
                        Graphics.CharacterSetIndex.G3,
                        "%$maskedChar",
                        false
                    ); state = State.NORMAL
                }

                State.EXT96_G1 -> {
                    graphics.designateGraphicSet(
                        Graphics.CharacterSetIndex.G1,
                        "%$maskedChar",
                        true
                    ); state = State.NORMAL
                }

                State.EXT96_G2 -> {
                    graphics.designateGraphicSet(
                        Graphics.CharacterSetIndex.G2,
                        "%$maskedChar",
                        true
                    ); state = State.NORMAL
                }

                State.EXT96_G3 -> {
                    graphics.designateGraphicSet(
                        Graphics.CharacterSetIndex.G3,
                        "%$maskedChar",
                        true
                    ); state = State.NORMAL
                }
            }
            updateStatusLine()
        }
    }

    override fun reset() {
        bootJob?.cancel()

        // Only show the startup sequence if the VT320 terminal type is selected
        if (TerminalManager.currentTerminalType != TerminalType.VT320) {
            isBooting = false
            performRis()
            return
        }

        isBooting = true
        bootBuffer.clear()

        bootJob = terminalScope.launch {
            // Step 1: Uninitialized video memory (random shapes, no letters)
            val random = Random(System.currentTimeMillis())
            val shapeChars =
                (32..47).toList() + (58..64).toList() + (91..96).toList() + (123..126).toList() + (160..255).toList()

            val noiseScreen = Array(rows) {
                Array(columns) {
                    TerminalCell(
                        char = shapeChars[random.nextInt(shapeChars.size)].toChar(),
                        attr = TerminalAttributes(
                            bold = random.nextBoolean(),
                            dim = random.nextBoolean(),
                            inverse = random.nextBoolean(),
                            blink = random.nextInt(10) == 0
                        )
                    )
                }
            }
            screen = noiseScreen
            delay(400.milliseconds)

            // Step 2: Solid rectangle over the whole screen (not bold)
            val solidScreen = Array(rows) {
                Array(columns) {
                    TerminalCell(
                        char = ' ',
                        attr = TerminalAttributes(inverse = true, bold = false)
                    )
                }
            }
            screen = solidScreen
            delay(400.milliseconds)

            // Step 3: Black vertical lines in right corner (Special Graphics '│' + Inverse)
            val verticalLinesScreen = Array(rows) {
                Array(columns) {
                    TerminalCell(
                        char = '│',
                        attr = TerminalAttributes(inverse = true, bold = false)
                    )
                }
            }
            screen = verticalLinesScreen
            delay(400.milliseconds)

            // Step 4: Bold horizontal lines + previous vertical lines (Special Graphics '┼' + Inverse + Bold)
            val gridInverseScreen = Array(rows) {
                Array(columns) {
                    TerminalCell(char = '┼', attr = TerminalAttributes(inverse = true, bold = true))
                }
            }
            screen = gridInverseScreen
            delay(400.milliseconds)

            // Step 5: No background, bold grid pattern (Both horizontal and vertical)
            val finalGridScreen = Array(rows) {
                Array(columns) {
                    TerminalCell(
                        char = '┼',
                        attr = TerminalAttributes(inverse = false, bold = true)
                    )
                }
            }
            screen = finalGridScreen
            delay(400.milliseconds)

            // Step 6: Wait in top left corner (2 seconds)
            onBell?.invoke()
            val waitScreen = Array(rows) { Array(columns) { TerminalCell() } }
            "Wait".forEachIndexed { i, c -> waitScreen[0][i] = TerminalCell(c) }
            screen = waitScreen
            delay(2000.milliseconds)

            // Step 7: Centered box with "VT320 OK" (1 second)
            val okScreen = Array(rows) { Array(columns) { TerminalCell() } }
            val text = "     VT320 OK     "
            val boxWidth = text.length + 2
            val boxHeight = 3
            val startX = (columns - boxWidth) / 2
            val startY = (rows - boxHeight) / 2

            for (y in 0 until boxHeight) {
                for (x in 0 until boxWidth) {
                    val char = when {
                        y == 0 && x == 0 -> '┌'
                        y == 0 && x == boxWidth - 1 -> '┐'
                        y == boxHeight - 1 && x == 0 -> '└'
                        y == boxHeight - 1 && x == boxWidth - 1 -> '┘'
                        y == 0 || y == boxHeight - 1 -> '─'
                        x == 0 || x == boxWidth - 1 -> '│'
                        x >= 1 && x < 1 + text.length -> text[x - 1]
                        else -> ' '
                    }
                    okScreen[startY + y][startX + x] = TerminalCell(char)
                }
            }

            // Add copyright and company text
            val copyright = "Firmware and Set-up Screens Copyright © 2026"
            val company = "aboveWare"

            val copyrightRow = startY + 1 + 3
            if (copyrightRow < rows) {
                val cpStartX = (columns - copyright.length) / 2
                copyright.forEachIndexed { i, c ->
                    if (cpStartX + i in 0 until columns) {
                        okScreen[copyrightRow][cpStartX + i] = TerminalCell(c)
                    }
                }
            }

            val companyRow = copyrightRow + 2
            if (companyRow < rows) {
                val coStartX = (columns - company.length) / 2
                company.forEachIndexed { i, c ->
                    if (coStartX + i in 0 until columns) {
                        okScreen[companyRow][coStartX + i] = TerminalCell(c)
                    }
                }
            }

            screen = okScreen
            delay(2000.milliseconds)

            // Step 8: Final actual terminal reset
            isBooting = false
            performRis()
        }
    }

    private var bootJob: Job? = null
    private var isBooting by mutableStateOf(false)
    private val bootBuffer = mutableListOf<Char>()

    internal fun ris() {
        reset()
    }

    private fun performRis() {
        val wasConnected = isConnected
        tabulator.reset()
        VT320Settings.recall()
        setup.recall()
        disconnect()
        if (wasConnected) connect()
        graphics.reset()

        // Initialize column mode from settings
        val colIndex = VT320Settings.columnMode
        columns = if (colIndex == 0) 80 else 132

        screen = Array(rows) { Array(columns) { TerminalCell() } }
        cursorX = 0
        cursorY = 0
        currentAttr = TerminalAttributes(selectiveErase = false)
        savedAttr = TerminalAttributes(selectiveErase = false)
        savedCursorX = 0
        savedCursorY = 0
        topMargin = 0
        bottomMargin = rows - 1
        screenReverse = VT320Settings.screenDisplayType == 1
        state = State.NORMAL
        params.clear()
        currentParam = 0
        hasParam = false
        lineAttributes = Array(rows) { LineAttribute.NORMAL }
        activeStatusLine = false

        // Initialize status line mode from settings
        val modeIndex = VT320Settings.statusDisplay
        statusLineManager.mode =
            StatusLineManager.Mode.entries.getOrElse(modeIndex) { StatusLineManager.Mode.INDICATOR }

        statusLineCursorX = 0
        interpretControls = VT320Settings.controlsMode == 0
        _cursorVisible = VT320Settings.textCursor == 0
        cursorStyle =
            if (VT320Settings.cursorStyle == 0) CursorStyle.BLOCK else CursorStyle.UNDERLINE

        autoWrap = VT320Settings.wrapMode
        autoRepeatMode = VT320Settings.autoRepeat

        cursorKeysMode = VT320Settings.cursorKeysMode == 1
        keypadMode = VT320Settings.keypadMode == 1
        newLineMode = VT320Settings.newLineMode == 1

        originMode = false
        insertMode = false
        keyboardLocked = false
        val opModeIndex = VT320Settings.nationalOnly
        conformanceLevel = when (opModeIndex) {
            2 -> 61
            3 -> 52
            else -> 63
        }
        eightBitControls = (opModeIndex == 1)
        characterSetMode8Bit = VT320Settings.characterSetMode == 1
        udkLocked = VT320Settings.userDefinedKeys == 1
        smoothScroll = VT320Settings.scrollMode
        allow80or132Mode = VT320Settings.userFeaturesSetting == 0
        localEcho = VT320Settings.localEcho
        clearInputBuffer()
        applyKeyboardLanguage()
        Abc80Log.terminal("VT320: RIS performed.")
        updateStatusLine()

        // Replay buffered characters
        synchronized(lock) {
            val buffered = bootBuffer.toList()
            bootBuffer.clear()
            buffered.forEach { putChar(it) }
        }
    }

    internal fun softReset() {
        topMargin = 0
        bottomMargin = rows - 1
        currentAttr = TerminalAttributes()
        state = State.NORMAL
        params.clear()
        currentParam = 0
        hasParam = false
        lineAttributes = Array(rows) { LineAttribute.NORMAL }
        activeStatusLine = false
        statusLineCursorX = 0
        _cursorVisible = true
        cursorKeysMode = false
        keypadMode = false
        originMode = false
        insertMode = false
        keyboardLocked = false
        VT320Settings.characterSetMode = 1
        VT320Settings.nationalReplacement = false
        characterSetMode8Bit = true
        graphics.softReset()
        applyKeyboardLanguage()
        Abc80Log.terminal("VT320: Soft Reset performed.")
        updateStatusLine()
    }

    fun clearComm() {
        if (printerStatus == PrinterStatus.AUTO || printerStatus == PrinterStatus.CONTROLLER) {
            printerStatus = PrinterStatus.READY
        }
        state = State.NORMAL
        params.clear()
        currentParam = 0
        hasParam = false
        inputSeq = ""
        synchronized(lock) { inputBuffer.clear() }
        onKeyEvent(0x11.toChar())
        isXoffReceivedFromHost = false
        isXoffReceivedFromPrinter = false
        Abc80Log.terminal("VT320: Clear Comm performed.")
        updateStatusLine()
    }

    private var isConnected = false
    override fun connect() {
        isConnected = true
    }

    override fun disconnect() {
        isConnected = false
        keyboard.stopRepeating()
    }

    override fun getGlyph(c: Char): CharacterSet.Glyph? = graphics.getGlyph(c, this)

    override fun getScreenState(): ByteArray {
        val buffer = ByteArray(columns * rows * 3 + 16)
        var offset = 0
        buffer[offset++] = (cursorX and 0xFF).toByte()
        buffer[offset++] = (cursorY and 0xFF).toByte()
        buffer[offset++] = (state.ordinal and 0xFF).toByte()
        buffer[offset++] = (topMargin and 0xFF).toByte()
        buffer[offset++] = (bottomMargin and 0xFF).toByte()
        buffer[offset++] = (savedCursorX and 0xFF).toByte()
        buffer[offset++] = (savedCursorY and 0xFF).toByte()
        var attrMask = 0
        if (currentAttr.bold) attrMask = attrMask or 0x01
        if (currentAttr.dim) attrMask = attrMask or 0x02
        if (currentAttr.underline) attrMask = attrMask or 0x04
        if (currentAttr.blink) attrMask = attrMask or 0x08
        if (currentAttr.inverse) attrMask = attrMask or 0x10
        buffer[offset++] = attrMask.toByte()
        var savedAttrMask = 0
        if (savedAttr.bold) savedAttrMask = savedAttrMask or 0x01
        if (savedAttr.dim) savedAttrMask = savedAttrMask or 0x02
        if (savedAttr.underline) savedAttrMask = savedAttrMask or 0x04
        if (savedAttr.blink) savedAttrMask = savedAttrMask or 0x08
        if (savedAttr.inverse) savedAttrMask = savedAttrMask or 0x10
        buffer[offset++] = savedAttrMask.toByte()
        while (offset < 16) offset++
        for (y in 0 until rows) {
            for (x in 0 until columns) {
                val cell = screen[y][x]
                buffer[offset++] = (cell.char.code and 0xFF).toByte()
                buffer[offset++] = ((cell.char.code shr 8) and 0xFF).toByte()
                var cellAttrMask = 0
                if (cell.attr.bold) cellAttrMask = cellAttrMask or 0x01
                if (cell.attr.dim) cellAttrMask = cellAttrMask or 0x02
                if (cell.attr.underline) cellAttrMask = cellAttrMask or 0x04
                if (cell.attr.blink) cellAttrMask = cellAttrMask or 0x08
                if (cell.attr.inverse) cellAttrMask = cellAttrMask or 0x10
                buffer[offset++] = cellAttrMask.toByte()
            }
        }
        return buffer
    }

    override fun setScreenState(data: ByteArray) {
        var offset = 0
        cursorX = data[offset++].toInt() and 0xFF
        cursorY = data[offset++].toInt() and 0xFF
        val stateOrdinal = data[offset++].toInt() and 0xFF
        state = State.entries.getOrElse(stateOrdinal) { State.NORMAL }
        topMargin = data[offset++].toInt() and 0xFF
        bottomMargin = data[offset++].toInt() and 0xFF
        savedCursorX = data[offset++].toInt() and 0xFF
        savedCursorY = data[offset++].toInt() and 0xFF
        val attrMask = data[offset++].toInt() and 0xFF
        currentAttr = TerminalAttributes(
            bold = (attrMask and 0x01) != 0,
            dim = (attrMask and 0x02) != 0,
            underline = (attrMask and 0x04) != 0,
            blink = (attrMask and 0x08) != 0,
            inverse = (attrMask and 0x10) != 0
        )
        val savedAttrMask = data[offset].toInt() and 0xFF
        savedAttr = TerminalAttributes(
            bold = (savedAttrMask and 0x01) != 0,
            dim = (savedAttrMask and 0x02) != 0,
            underline = (savedAttrMask and 0x04) != 0,
            blink = (savedAttrMask and 0x08) != 0,
            inverse = (savedAttrMask and 0x10) != 0
        )
        offset = 16
        val newScreen = Array(rows) { Array(columns) { TerminalCell() } }
        for (y in 0 until rows) {
            for (x in 0 until columns) {
                val charCode =
                    (data[offset++].toInt() and 0xFF) or ((data[offset++].toInt() and 0xFF) shl 8)
                val cellAttrMask = data[offset++].toInt() and 0xFF
                newScreen[y][x] = TerminalCell(
                    char = charCode.toChar(),
                    attr = TerminalAttributes(
                        bold = (cellAttrMask and 0x01) != 0,
                        dim = (cellAttrMask and 0x02) != 0,
                        underline = (cellAttrMask and 0x04) != 0,
                        blink = (cellAttrMask and 0x08) != 0,
                        inverse = (cellAttrMask and 0x10) != 0
                    )
                )
            }
        }
        screen = newScreen
        updateStatusLine()
    }

    override fun clearScreen() = clearScreenInternal()
    override fun clearInputBuffer() = clearInputBufferInternal()
    override fun hasChar(): Boolean = hasCharInput()
    override fun getChar(): Int = getCharInput()
    override fun onKeyEvent(char: Char) {
        if (keyboard.interceptKeyEvent(char)) return
        if (!setup.isVisible && !keyboardLocked &&
            !com.aboveware.aboveabc80.core.BIOS.instance.isTransientProgramRunning
        ) {
            TerminalManager.commandHistory.record(char)
        }
        handleKeyEvent(char)
    }

    override fun printScreen() = printScreenInternal()

    override fun toggleSetup() {
        setup.toggle()
    }

}
