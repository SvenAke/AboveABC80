package com.aboveware.abovecpm.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aboveware.abovecpm.ZXLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

const val LINE1 = 18
const val LINE2 = 20
const val LINE3 = 22
const val LINE4 = 24
const val HEADING = 17

const val answerBack = "Answerback="
const val answerBackConcealed = "<Concealed>"
const val answerBackEnterPrompt = "Enter Answerback "
const val answerBackPrompt = "Press ENTER to change this field - Press Cursor Keys to move"
const val auto = "Auto"
const val autoPrint = "Auto Print"
const val autoAnswerBackKey = "autoAnswerBack"
const val autoAnswerBackFormat = "%sAuto Answerback"
const val autoRepeat = "autoRepeat"
const val autoRepeatFormat = "%sAuto Repeat"
const val allRightReserved = "All Rights Preserved"
const val bitsFormatKey = "bitsFormat"
const val bitsFormatDefault = 0
const val bitsFormatFormat = "%s"
const val block = "Block"
const val breakKey = "breakKey"
const val breakKeyFormat = "%sBreak"
const val caps = "Caps"
const val clearAllTabs = "Clear All Tabs"
const val clearComm = "Clear Comm"
const val clearDisplay = "Clear Display"
const val columnMode = "columnMode"
const val columnModeFormat = "%s Columns"
const val comm = "Comm"
const val communicationsSetup = "Communications Set-Up"
const val characterSetModeKey = "characterSetMode"
const val characterSetModeFormat = "%s Characters"
const val compose = "compose"
const val answerbackConcealedKey = "concealed"
const val concealedFormat = "%sConcealed"
const val controlMode = "controlsMode"
const val controlModeFormat = "%s Controls"
const val controller = "Controller"
const val cursor = "Cursor"
const val cursorKeysModeKey = "cursorKeysMode"
const val cursorKeysModeFormat = "%s Cursor Keys"
const val cursorStyle = "cursorStyle"
const val cursorStyleFormat = "%s Cursor Style"
const val darkLight = "Dark Text, Light Screen"
const val defaultText = "Default"
const val disconnectDelayKey = "disconnectDelay"
const val disconnectDelayFormat = "Disconnect, %s Delay"
const val display = "Display"
const val displaySetUp = "Display Set-Up"
const val dsrConnected = "DSR Connected"
const val eighty = "80"
const val exitText = "Exit"
const val formFeedTerminator = "Terminator = FF"
const val fullPage = "Full Page"
const val general = "General"
const val generalSetup = "General Set-Up"
const val insert = "Insert"
const val interpret = "Interpret"
const val jump = "Jump"
const val keyClick = "keyClick"
const val keyClickFormat = "%sKeyclick"
const val keyboard = "Keyboard"
const val keyboardLanguageKey = "keyboardLanguage"
const val keyboardFormat = "%s Keyboard"
const val keyboardSetup = "Keyboard Set-Up"
const val keypadModeKey = "keypadMode"
const val lightDark = "Light Text, Dark Screen"
const val limited = "Limited"
const val local = "Local"
const val localEcho = "localEcho"
const val localEchoFormat = "%sLocal Echo"
const val lockKey = "lockKey"
const val lockKeyFormat = "%s Lock"
const val locked = "Locked"
const val marginBell = "marginBell"
const val marginBellFormat = "%sMargin Bell"
const val mode = "Mode"
const val modem = "Modem"
const val printAll = "Print All Characters"
const val nationalLineDrawing = "National and Line Drawing"
const val nationalOnly = "National Only"
const val newLineMode = "newLineMode"
const val newLineModeFormat = "%sNew Line"
const val userPreferredCharacterSet = "userPreferredCharacterSet"
const val userPreferredCharacterSetFormat = "UPSS %s"
const val userPreferredCharacterSetDEC = "DEC Supplemental"
const val userPreferredCharacterSetISO = "ISO Latin-1"
const val no = "No "
const val normal = "Normal"
const val noAuto = "No Auto"
const val noCursor = "No Cursor"
const val noTerminator = "No Terminator"
const val none = "None"
const val not = "Not "
const val numeric = "Numeric"
const val onLine = "On-Line"
const val onLineLocalKey = "OnLineLocal"
const val oneHundredThirtyTwo = "132"
const val oneStopBit = "1 Stop Bit"
const val onlineLocalFormat = "%s"
const val operatingMode = "textOperatingMode"
const val operatingModeFormat = "%s"
const val hostPortKey = "port"
const val portRS232DataLeads = "RS232, Data Leads Only"
const val portDEC423DataLeads = "DEC-423, Data Leads Only"
const val portFormat = "%s"
const val portRS232Modem = "RS232, Modem Control"
const val portDEC423Modem = "DEC-423, Modem Control"
const val printTerminatorKey = "printTerminator"
const val printTerminatorFormat = "%s"
const val printedDataTypeKey = "printedDataType"
const val printedDataTypeFormat = "Print %s"
const val printerPrompt = "Printer"
const val printerBitsFormatKey = "printerBitsFormat"
const val printerBitsFormatDefault = 0
const val printerBitsFormatFormat = "%s"
const val printerModeKey = "printerMode"
const val printerModeFormat = "%s"
const val printerModeNormal = "Printer Mode Normal"
const val printerModeAuto = "Printer Mode Auto Print"
const val printerModeController = "Printer Mode Controller"
const val printerScreenSizeKey = "printerScreenSize"
const val printerScreenSizeFormat = "Print %s"
const val printerSetup = "Printer Set-Up"
const val printerSpeedKey = "printerSpeed"
const val printerSpeedDefault = 7
const val printerSpeedFormat = "Speed=%s"
const val printerStopBitsKey = "printerStopBits"
const val printerStopBitsFormat = "%s"
const val printerToHostKey = "printerToHost"
const val printerToHostFormat = "%sPrinter To Host"
const val recallText = "Recall"
const val receive = "receive"
const val receiveFormat = "Receive=%s"
const val replace = "Replace"
const val resetTerminalText = "Reset Terminal"
const val scrollRegion = "Scroll Region"
const val saveText = "Save"
const val screenDisplayType = "screenDisplayType"
const val screenDisplayTypeFormat = "%s"
const val scrollMode = "scrollMode"
const val scrollModeFormat = "%s Scroll"
const val set8ColumnsTabs = "Set 8 Column Tabs"
const val setUpDirectory = "Set-Up Directory"
const val setUpLanguageKey = "SetUpLanguage"
const val setUpLanguageFormat = "Set-Up=%s"
const val shift = "Shift"
const val sixtyMilliSeconds = "60ms"
const val smooth = "Smooth"
const val stopBitsKey = "stopBits"
const val stopBitsFormat = "%s"
const val tab = "Tab"
const val tabSetup = "Tab Set-Up"
const val terminalKeyMapKey = "terminalKeyMap"
const val terminalKeyMapFormat = "%s Keys"
const val terminalTransmitSpeedKey = "terminalTransmitSpeed"
const val terminalTransmitSpeedFormat = "%s Transmit"
const val textCursor = "textCursor"
const val textCursorFormat = "%s"
const val toDirectory = "To Directory"
const val toNextSetup = "To Next Set-Up"
const val transmit = "transmit"
const val transmitDefault = 8
const val transmitFormat = "Transmit=%s"
const val twoSeconds = "2s"
const val twoStopBits = "2 Stop Bits"
const val underline = "Underline"
const val unlimited = "Unlimited"
const val unlocked = "Unlocked"
const val userDefinedKeys = "userDefinedKeys"
const val userDefinedKeysFormat = "User Defined Keys %s"
const val userFeatures = "userFeaturesSetting"
const val userFeaturesFormat = "User Features %s"
const val vt100Id = "VT100"
const val vt300_7bit = "VT300 Mode, 7-Bit Controls"
const val vt300_8bit = "VT300 Mode, 8-Bit Controls"
const val vt100Mode = "VT100 Mode"
const val terminalModeId = "terminalModeId"
const val terminalModeIdFormat = "%s ID"
const val vt101Id = "VT101"
const val vt102Id = "VT102"
const val vt220Id = "VT220"
const val vt320Id = "VT320"
const val vt52Mode = "VT52 Mode"
const val warningBell = "marginBell"
const val warningBellFormat = "%sWarning Bell"
const val wrapMode = "wrapMode"
const val wrapModeFormat = "%s Wrap"
const val printerXOffKey = "printerXOff"
const val xOff = "xOff"
const val xOffFormat = "%s"
const val printerXOffFormat = "%s"
const val statusDisplay = "statusDisplay"
const val statusDisplayFormat = "%s"
const val noStatusDisplay = "No Status Display"
const val statusDisplayIndicator = "Indicator"
const val statusDisplayHostWritable = "Host Writable"
const val composeKey = "composeKey"
const val composeKeyFormat = "%sCompose"
const val backArrowKey = "backArrow"
const val backArrowKeyFormat = "<x] %s"
const val backArrowKeyDelete = "Delete"
const val backArrowKeyBackspace = "Backspace"
const val commaPointKey = "commaPoint"
const val commaPointKeyFormat = "%s"
const val commaPointKeyLabel = ",, and .. Keys"
const val commaPointKeySend = ",, and .. Keys Send ,< and .>"
const val angleBracketKey = "angleBracket"
const val angleBracketKeyFormat = "%s"
const val angleBracketKeyLabel = "<> Key"
const val angleBracketKeySend = "<> Key Sends `~"
const val tildeKey = "tildeKey"
const val tildeKeyFormat = "%s"
const val tildeKeyLabel = "`~ Key"
const val tildeKeySend = "`~ Key Sends ESC"

/**
 * VTSetup: Implements the terminal-rendered setup screen for VT320.
 * Prepends all settings with "vt220_".
 */
class VTSetup(private val vt: VT320) {

    companion object {
        const val ESC = "\u001B"
        const val CSI = "\u001B["
        const val UP = "\u001B[A"
        const val DOWN = "\u001B[B"
        const val RIGHT = "\u001B[C"
        const val LEFT = "\u001B[D"
        const val ENTER = "\r"
        const val SETUP_KEY = "\u001B[28~" // F13/Help used as Setup
        const val DEL_KEY = "\u001B[3~"   // PC Delete key
    }

    var isVisible by mutableStateOf(value = false)
        private set

    private var currentScreen by mutableStateOf<SetupScreen?>(null)
    private val statusLine = StatusLine()

    inner class StatusLine {
        fun clear() {
            vt.statusLine = Array(vt.columns) { TerminalCell(' ') }
        }

        fun set(pos: Int, text: String, attr: TerminalAttributes = TerminalAttributes()) {
            val newStatus = vt.statusLine.copyOf()
            text.forEachIndexed { i, c ->
                if (pos + i < vt.columns) newStatus[pos + i] = TerminalCell(c, attr)
            }
            vt.statusLine = newStatus
        }

        fun indicator() {
            vt.updateStatusLine(setupIndicator = "")
        }
    }

    private fun send(s: String) {
        s.forEach { vt.putChar(it) }
    }

    private fun bold() = send("${CSI}1m")
    private fun off() = send("${CSI}0m")
    private fun reverse() = send("${CSI}7m")
    private fun underscore() = send("${CSI}4m")
    private fun cup(row: Int, col: Int) = send("${CSI}${row};${col}H")
    private fun ed(p: Int) = send("$CSI${p}J")
    private fun el(p: Int) = send("$CSI${p}K")
    private fun decdwl() = send("${ESC}#6")

    private fun translate(key: String): String {
        val langIndex = VT320Settings.setUpLanguage
        if (langIndex == 0) return VTSetupTranslations.translate(key, 0)

        val is132 = vt.columns == 132
        val finalIndex = if (is132) langIndex + 2 else langIndex
        return VTSetupTranslations.translate(key, finalIndex)
    }

    open class Setting(val name: String)

    open class ParameterSetting(
        name: String, val format: String, private var defaultIndex: Int,
        val parameters: List<String>
    ) : Setting(name) {
        protected var currentIndex = 0

        fun index(): Int {
            if (parameters.isEmpty()) return 0
            return currentIndex.coerceIn(0, parameters.size - 1)
        }

        open fun next(): Int {
            if (parameters.isEmpty()) return 0
            currentIndex = (index() + 1) % parameters.size
            return index()
        }

        fun recall() {
            // Using a generic getInt for internal ParameterSetting which uses string names dynamically
            // But since I made getInt private, I need a way to access it or update Setting classes.
            // Actually, I can just use the new methods if I map names to them, but that's tedious.
            // I'll make getInt internal instead of private to support the Setting class for now,
            // or I can update the Setting classes to use the new methods.
            // Given the volume, I'll make getInt/setInt internal.
            val saved = VT320Settings.getInt(name, defaultIndex)
            currentIndex = if (parameters.isEmpty()) 0 else saved.coerceIn(0, parameters.size - 1)
        }

        fun save() {
            VT320Settings.setInt(name, index())
        }

        fun resetToFactory() {
            currentIndex =
                if (parameters.isEmpty()) 0 else defaultIndex.coerceIn(0, parameters.size - 1)
            save()
        }
    }

    abstract inner class Field(val selectable: Boolean = true, val action: () -> Unit = {}) {
        var line = 0
        var startCol = 0
        var endCol = 0
        open fun draw(row: Int, col: Int): Int {
            val text = " ${text().padEnd(fieldWidth() + 1, ' ')}"
            line = row
            startCol = col
            endCol = col + text.length
            cup(row, col)
            if (selectable) {
                reverse()
                if (active) bold()
            } else {
                off()
            }
            send(text)
            off()
            positionCursor()
            return endCol + 1
        }

        open fun fieldWidth() = text().length

        open fun positionCursor() {
            if (active && selectable) {
                vt.cursorX = startCol
                vt.cursorY = line
            }
        }

        var activeField = false
        open var active: Boolean
            get() = activeField
            set(value) {
                activeField = value
            }

        open fun recall() {}
        open fun save() {}
        open fun text() = ""
        open fun onEvent(key: String) = false
        fun match(nextLine: Int, range: IntRange) = if (line == nextLine)
            (startCol..endCol).intersect(range).size else 0
    }

    open inner class TextField(val text: String) : Field(false) {
        override fun text(): String = translate(text)
    }

    inner class ActionField(val text: String, action: () -> Unit) : Field(action = action) {
        override fun text() = translate(text)
    }

    inner class ParameterField(
        val setting: ParameterSetting,
        val onParameterChanged: (ParameterSetting) -> Unit = {}
    ) :
        Field(action = {
            setting.next()
            setting.save() // Active immediately at runtime
            onParameterChanged(setting)
        }) {
        override fun text(): String {
            setting.apply {
                val idx = index()
                val paramText = if (idx in parameters.indices) parameters[idx] else "???"
                return translate(format).replace("%s", translate(paramText))
            }
        }

        override fun fieldWidth() = setting.parameters.map { parameter ->
            translate(setting.format).replace("%s", translate(parameter))
        }.maxOf { it.length }

        override fun save() = setting.save()
        override fun recall() = setting.recall()
    }

    open inner class TextParameterField(var text: String) : Field() {
        override fun text() = translate(text)

        override fun onEvent(key: String) = when (key) {
            UP -> false
            DOWN -> false
            RIGHT -> right()
            LEFT -> left()
            ENTER -> enter()
            else -> handle(key)
        }

        open fun handle(key: String) = false
        open fun right() = false
        open fun left() = false
        open fun enter() = false
    }

    inner class AnswerBackField(private val concealedSetting: ConcealedSetting) :
        TextParameterField("") {
        private var edit = false
        private var answerBackPosition = 0
        private var answerBackPositionMax = 0
        private var answerBackMessage = initiate()

        fun initiate() = MutableList(30) { ' ' }.apply {
            text.forEachIndexed { index, char ->
                if (index < 30) this[index] = char
            }
            answerBackPosition = text.length.coerceAtMost(29)
            answerBackPositionMax = text.length.coerceAtMost(29)
        }

        override fun text(): String {
            val message = translate(
                if (concealedSetting.index() == 0)
                    text
                else answerBackConcealed
            )
            return "%s%s".format(translate(answerBack), message.padEnd(30, ' '))
        }

        override fun recall() {
            text = VT320Settings.answerBack
        }

        override fun save() {
            VT320Settings.answerBack = text
        }

        override var active: Boolean
            get() = activeField
            set(value) {
                if (value && !activeField) {
                    answerBackMessage = initiate()
                }
                if (activeField && !value) {
                    edit = false
                    vt.activeStatusLine = false
                }
                activeField = value
            }

        override fun positionCursor() {
            if (active) {
                if (edit) {
                    val promptText = translate(answerBackEnterPrompt)
                    vt.activeStatusLine = true
                    vt.cursorX = 1 + promptText.length + answerBackPosition
                } else {
                    vt.activeStatusLine = false
                    super.positionCursor()
                }
            }
        }

        override fun draw(row: Int, col: Int): Int {
            if (active) {
                statusLine.clear()
                val promptText = translate(answerBackEnterPrompt)
                val prompt = if (edit)
                    "$promptText${answerBackMessage.joinToString(separator = "")}"
                else
                    translate(answerBackPrompt)
                statusLine.set(1, prompt)
            }
            return super.draw(row, col)
        }

        override fun onEvent(key: String) = when (key) {
            DEL_KEY -> {
                if (edit) {
                    if (answerBackPosition <= answerBackPositionMax) {
                        answerBackMessage.removeAt(answerBackPosition)
                        answerBackMessage.add(' ')
                        answerBackPositionMax = (answerBackPositionMax - 1).coerceAtLeast(0)
                        true
                    } else false
                } else false
            }

            else -> super.onEvent(key)
        }

        override fun handle(key: String): Boolean {
            key.forEach {
                when (it) {
                    '\u0008', '\u007F' -> { // Backspace or VT220 Rubout
                        if (edit && answerBackPosition > 0) {
                            left()
                            answerBackMessage.removeAt(answerBackPosition)
                            answerBackMessage.add(' ')
                            answerBackPositionMax = (answerBackPositionMax - 1).coerceAtLeast(0)
                        }
                    }

                    else -> {
                        if (it.code >= 32) {
                            answerBackMessage[answerBackPosition] = it
                            answerBackPositionMax =
                                answerBackPositionMax.coerceAtLeast(answerBackPosition)
                            answerBackPosition = (answerBackPosition + 1).coerceAtMost(29)
                        }
                    }
                }
            }
            return true
        }

        override fun enter(): Boolean {
            if (edit) {
                text = if (answerBackPositionMax == 0 && answerBackMessage[0] == ' ') ""
                else answerBackMessage.slice(0..answerBackPositionMax).joinToString(separator = "")
                save() // Active immediately at runtime
                // Entering a new message resets concealment
                concealedSetting.resetToNotConcealed()
            } else
                answerBackMessage = initiate()
            edit = !edit
            return true
        }

        override fun left(): Boolean {
            if (edit) answerBackPosition = (answerBackPosition - 1).coerceAtLeast(0)
            return edit
        }

        override fun right(): Boolean {
            if (edit) answerBackPosition =
                (1 + answerBackPosition).coerceAtMost(answerBackPositionMax).coerceAtMost(29)
            return edit
        }
    }

    inner class TabField : TextParameterField("") {
        private var position = 0

        override fun draw(row: Int, col: Int): Int {
            val width = vt.columns
            cup(row, col)
            el(0)
            reverse()
            for (i in 0 until width) {
                send(if (vt.tabulator.stops[i]) "T" else " ")
            }
            off()

            // Ruler row below T indicators
            cup(row + 1, col)
            el(0)
            for (i in 0 until width) {
                if (((i + 1) / 10) % 2 == 1) reverse() else off()
                send(((i + 1) % 10).toString())
            }
            off()

            if (active) {
                cup(row, position + 1)
                bold()
                send(if (vt.tabulator.stops[position]) "T" else "_")

                cup(row + 1, position + 1)
                if (((position + 1) / 10) % 2 == 1) reverse() else off()
                bold()
                send(((position + 1) % 10).toString())
                off()
            }
            return width
        }

        fun clearAll() = vt.tabulator.clearAll()
        fun reset() = vt.tabulator.reset()

        override fun enter(): Boolean {
            vt.tabulator.toggle(position)
            return true
        }

        override fun right(): Boolean {
            if (active) position = (position + 1).coerceAtMost(vt.columns - 1)
            return active
        }

        override fun left(): Boolean {
            if (active) position = (position - 1).coerceAtLeast(0)
            return active
        }

        override fun handle(key: String): Boolean {
            if (key == "\t") {
                position = vt.tabulator.nextTab(position).coerceAtMost(vt.columns - 1)
            }
            return active
        }

        override fun fieldWidth(): Int = vt.columns
    }

    inner class SetupScreenLine(val row: Int, fields: List<Field>) : ArrayList<Field>(fields) {
        fun recall() {
            forEach { it.recall() }
        }

        fun save() {
            forEach { it.save() }
        }

        fun draw() {
            var col = 1
            forEach { col = it.draw(row, col) }
        }
    }

    open inner class SetupScreen(
        private val title: String, lines: List<SetupScreenLine>
    ) : ArrayList<SetupScreenLine>(lines) {

        fun recall() {
            forEach { it.recall() }
        }

        fun save() {
            forEach { it.save() }
        }

        private fun heading() {
            off()
            cup(HEADING, 1)
            el(2)
            ed(0)
            decdwl()
            val headingTitle = title.trim()
            send(translate(headingTitle))
            val version = "Above VT320 V1.0"
            underscore()
            cup(HEADING, vt.columns / 2 - version.length + 1)
            send(version)
            off()
        }

        fun up() = upOrDown { if (it == 0) size - 1 else it - 1 }
        fun down() = upOrDown { if (it == (size - 1)) 0 else it + 1 }

        private fun upOrDown(direction: (Int) -> Int) {
            val flat = flatten()
            val current = flat.firstOrNull { it.active } ?: return
            current.active = false

            var found = false
            var index = indexOfFirst { it.contains(current) }
            val range = current.startCol..current.endCol

            while (!found) {
                index = direction(index)
                val line = this[index]
                val best = line.filter { it.selectable }.maxByOrNull { it.match(line.row, range) }
                if (best != null) {
                    best.active = true
                    found = true
                }
            }
        }

        fun left() {
            val flat = flatten().filter { it.selectable }
            val idx = flat.indexOfFirst { it.active }
            if (idx == -1) return
            flat[idx].active = false
            flat[(idx - 1 + flat.size) % flat.size].active = true
        }

        fun right() {
            val flat = flatten().filter { it.selectable }
            val idx = flat.indexOfFirst { it.active }
            if (idx == -1) return
            flat[idx].active = false
            flat[(idx + 1) % flat.size].active = true
        }

        fun enter() {
            flatten().firstOrNull { it.active }?.action?.invoke()
        }

        fun onEvent(code: String): Boolean {
            val current = flatten().firstOrNull { it.active }
            if (current?.onEvent(code) == true) return true

            when (code) {
                UP -> up()
                DOWN -> down()
                LEFT -> left()
                RIGHT -> right()
                ENTER -> enter()
                SETUP_KEY -> toggle()
                else -> return false
            }
            return true
        }

        open fun draw() {
            ZXLog.terminal("VTSetup: draw() called")
            vt.activeStatusLine = false // Ensure drawing commands target the main screen area
            heading()
            bottom()
            forEach { it.draw() }

            // Re-apply cursor position for active field
            flatten().firstOrNull { it.active }?.positionCursor()
        }

        open fun bottom() {
            statusLine.clear()
            statusLine.indicator()
        }
    }

    // Concrete Screens
    inner class SetupDirectory : SetupScreen(
        setUpDirectory, listOf(
            SetupScreenLine(
                LINE1, listOf(
                    ActionField(display) { currentScreen = displaySetupScreen }.apply {
                        active = true
                    },
                    ActionField(general) { currentScreen = generalSetupScreen },
                    ActionField(comm) { currentScreen = communicationSetupScreen },
                    ActionField(printerPrompt) { currentScreen = printerSetupScreen },
                    ActionField(keyboard) { currentScreen = keyboardSetupScreen },
                    ActionField(tab) { currentScreen = tabSetupScreen }
                )),
            SetupScreenLine(
                LINE2, listOf(
                    ParameterField(onLineLocalSetting),
                    ActionField(clearDisplay) {
                        vt.clearScreen()
                        savedScreen = Array(vt.rows) { Array(vt.columns) { TerminalCell() } }
                        savedCursorX = 0
                        savedCursorY = 0
                        currentScreen?.draw()
                    },
                    ActionField(clearComm) { vt.clearComm() },
                    ActionField(resetTerminalText) {
                        vt.ris()
                        savedScreen = null
                        exit()
                    },
                    ActionField(recallText) { recall() },
                    ActionField(saveText) {
                        save()
                        showMessage("Done")
                    }
                )),
            SetupScreenLine(
                LINE3, listOf(
                    ParameterField(setUpLanguageSetting),
                    ParameterField(keyboardSetting) { setting ->
                        vt.applyKeyboardLanguage(setting.index())
                        vt.updateStatusLine()
                    },
                    ActionField(defaultText) { factoryDefault() },
                    ActionField(exitText) { toggle() }
                )),
            SetupScreenLine(
                LINE4, listOf(
                    TextField("Copyright \u00A9 2026 aboveWare - All Rights Reserved")
                )
            )
        ))

    inner class DisplaySetup : SetupScreen(
        displaySetUp, listOf(
            SetupScreenLine(
                LINE1, listOf(
                    ActionField(toNextSetup) { currentScreen = generalSetupScreen }.apply {
                        active = true
                    },
                    ActionField(toDirectory) { currentScreen = setUpDirectoryScreen },
                    ParameterField(columnModeSetting) { setting ->
                        val cols = if (setting.index() == 0) 80 else 132
                        vt.setColumnMode(cols)
                        // Discard saved screen and reset cursor as it's no longer valid for the new width
                        // VT320 hardware clears the screen when switching column modes.
                        savedScreen = null
                        savedCursorX = 0
                        savedCursorY = 0
                    },
                    ParameterField(controlsModeSetting) { setting ->
                        vt.interpretControls = setting.index() == 0
                    }
                )
            ),
            SetupScreenLine(
                LINE2, listOf(
                    ParameterField(wrapModeSetting) { setting ->
                        vt.autoWrap = setting.index() == 1
                    },
                    ParameterField(scrollModeSetting) { setting ->
                        vt.smoothScroll = setting.index() == 0
                    },
                    ParameterField(screenDisplayTypeSetting) { setting ->
                        vt.screenReverse = setting.index() == 1
                    }
                )
            ),
            SetupScreenLine(
                LINE3, listOf(
                    ParameterField(textCursorSetting) { setting ->
                        vt._cursorVisible = setting.index() == 0
                    },
                    ParameterField(cursorStyleSetting) { setting ->
                        vt.cursorStyle =
                            if (setting.index() == 0) CursorStyle.BLOCK else CursorStyle.UNDERLINE
                    },
                    ParameterField(statusDisplaySetting) { setting ->
                        val modes = StatusLineManager.Mode.entries
                        vt.statusLineManager.mode =
                            modes[setting.index().coerceIn(0, modes.size - 1)]
                        vt.updateStatusLine()
                    }
                )
            )
        )
    )

    inner class GeneralSetup : SetupScreen(
        generalSetup, listOf(
            SetupScreenLine(
                LINE1, listOf(
                    ActionField(toNextSetup) {
                        currentScreen = communicationSetupScreen
                    }.apply { active = true },
                    ActionField(toDirectory) { currentScreen = setUpDirectoryScreen },
                    ParameterField(operationModeSetting) { setting ->
                        val (level, controls) = when (setting.index()) {
                            0 -> 63 to 1 // VT300, 7-bit
                            1 -> 63 to 0 // VT300, 8-bit
                            2 -> 61 to 1 // VT100
                            3 -> 52 to 1 // VT52
                            else -> 63 to 1
                        }

                        if (level == 52) {
                            ZXLog.terminal("VT320: Switching to VT52 mode")
                        } else {
                            val seq = "\u001B[${level};${controls}\"p"
                            seq.forEach { vt.putChar(it) }
                        }
                    },
                    ParameterField(vt100ModeTerminalIdSetting)
                )
            ),
            SetupScreenLine(
                LINE2, listOf(
                    ParameterField(userDefinedKeysSetting) { setting ->
                        vt.udkLocked = setting.index() == 1
                    },
                    ParameterField(userFeaturesSettingInstance) { setting ->
                        vt.allow80or132Mode = setting.index() == 0
                    },
                    ParameterField(characterSetModeSetting)
                )
            ),
            SetupScreenLine(
                LINE3, listOf(
                    ParameterField(keypadModeSetting),
                    ParameterField(cursorKeysModeSetting),
                    ParameterField(newLineModeSetting)
                )
            ),
            SetupScreenLine(
                LINE4, listOf(
                    ParameterField(userPreferredCharacterSet) { setting ->
                        TerminalManager.userPreferredCharacterSetIsDECSupplementalGraphic =
                            setting.index() == 0
                        vt.updateStatusLine()
                    }
                )
            )
        )
    )

    inner class CommunicationsSetup : SetupScreen(
        communicationsSetup, listOf(
            SetupScreenLine(
                LINE1, listOf(
                    ActionField(toNextSetup) { currentScreen = printerSetupScreen }.apply {
                        active = true
                    },
                    ActionField(toDirectory) { currentScreen = setUpDirectoryScreen },
                    ParameterField(transmitSetting),
                    ParameterField(receiveSetting)
                )
            ),
            SetupScreenLine(
                LINE2, listOf(
                    ParameterField(xOffSetting),
                    ParameterField(bitsFormatSetting),
                    ParameterField(stopBitsSetting),
                    ParameterField(localEchoSetting)
                )
            ),
            SetupScreenLine(
                LINE3, listOf(
                    ParameterField(hostPortSetting),
                    ParameterField(disconnectDelaySetting),
                    ParameterField(terminalTransmitSpeedSetting)
                )
            ),
            SetupScreenLine(
                LINE4, listOf(
                    ParameterField(autoAnswerBackSetting),
                    AnswerBackField(concealedSetting),
                    ParameterField(concealedSetting)
                )
            )
        )
    )

    inner class PrinterSetup : SetupScreen(
        printerSetup, listOf(
            SetupScreenLine(
                LINE1, listOf(
                    ActionField(toNextSetup) {
                        currentScreen = keyboardSetupScreen
                    }.apply { active = true },
                    ActionField(toDirectory) { currentScreen = setUpDirectoryScreen },
                    ParameterField(printerSpeedSetting),
                    ParameterField(printerToHostSetting)
                )
            ),
            SetupScreenLine(
                LINE2, listOf(
                    ParameterField(printerModeSetting) { setting ->
                        vt.setupPrinterStatus = when (setting.index()) {
                            1 -> PrinterStatus.AUTO
                            2 -> PrinterStatus.CONTROLLER
                            else -> PrinterStatus.READY
                        }
                        currentScreen?.bottom()
                    },
                    ParameterField(printerXOffSetting),
                    ParameterField(printerBitsFormatSetting),
                    ParameterField(printerStopBitsSetting)
                )
            ),
            SetupScreenLine(
                LINE3, listOf(
                    ParameterField(printerScreenSizeSetting),
                    ParameterField(printedDataTypeSetting),
                    ParameterField(printTerminatorSetting)
                )
            )
        )
    )

    inner class KeyboardSetup : SetupScreen(
        keyboardSetup, listOf(
            SetupScreenLine(
                LINE1, listOf(
                    ActionField(toNextSetup) { currentScreen = tabSetupScreen }.apply {
                        active = true
                    },
                    ActionField(toDirectory) { currentScreen = setUpDirectoryScreen },
                    ParameterField(terminalKeyMapSetting),
                    ParameterField(lockKeySetting)
                )
            ),
            SetupScreenLine(
                LINE2, listOf(
                    ParameterField(autoRepeatSetting) { setting ->
                        vt.autoRepeatMode = setting.index() == 1
                    },
                    ParameterField(keyClickSetting),
                    ParameterField(marginBellSetting),
                    ParameterField(warningBellSetting),
                    ParameterField(breakKeySetting)
                )
            ),
            SetupScreenLine(
                LINE3, listOf(
                    ParameterField(keyboardSetting) { setting ->
                        vt.applyKeyboardLanguage(setting.index())
                        vt.updateStatusLine()
                    },
                    ParameterField(composeKeySetting),
                    ParameterField(backArrowKeySetting)
                )
            ),
            SetupScreenLine(
                LINE4, listOf(
                    ParameterField(commaPointKeySetting),
                    ParameterField(angleBracketKeySetting),
                    ParameterField(tildeKeySetting)
                )
            )
        )
    )

    inner class TabSetup : SetupScreen(
        tabSetup, listOf(
            SetupScreenLine(
                LINE1, listOf(
                    ActionField(toNextSetup) {
                        currentScreen = displaySetupScreen
                    }.apply {
                        active = true
                    },
                    ActionField(toDirectory) {
                        currentScreen = setUpDirectoryScreen
                    },
                    ActionField(clearAllTabs) { tabField.clearAll(); currentScreen?.draw() },
                    ActionField(set8ColumnsTabs) { tabField.reset(); currentScreen?.draw() }
                )),
            SetupScreenLine(LINE2, listOf(tabField))
        ))

    // Settings
    class KeyboardSetting :
        ParameterSetting(
            keyboardLanguageKey, keyboardFormat, 0, listOf(
                "North American", "British", "Flemish", "Canadian (French)", "Danish", "Finnish",
                "German/Austrian", "Dutch", "Italian", "Swiss (French)", "Swiss (German)",
                "Swedish", "Norwegian", "French/Belgian", "Spanish", "Portuguese"
            )
        )

    class StatusDisplaySetting : ParameterSetting(
        statusDisplay,
        statusDisplayFormat,
        0,
        listOf(noStatusDisplay, statusDisplayIndicator, statusDisplayHostWritable)
    )

    class OnLineLocalSetting :
        ParameterSetting(onLineLocalKey, onlineLocalFormat, 0, listOf(onLine, local))

    class SetUpLanguageSetting :
        ParameterSetting(
            setUpLanguageKey,
            setUpLanguageFormat,
            0,
            listOf("English", "Francais", "Deutsch")
        )

    class ColumnModeSetting :
        ParameterSetting(columnMode, columnModeFormat, 0, listOf(eighty, oneHundredThirtyTwo))

    class ScrollModeSetting :
        ParameterSetting(scrollMode, scrollModeFormat, 0, listOf(smooth, jump))

    class ControlsModeSetting :
        ParameterSetting(controlMode, controlModeFormat, 0, listOf(interpret, display))

    class WrapModeSetting : ParameterSetting(wrapMode, wrapModeFormat, 0, listOf(noAuto, auto))
    class ScreenDisplayTypeSetting : ParameterSetting(
        screenDisplayType,
        screenDisplayTypeFormat,
        0,
        listOf(lightDark, darkLight)
    )

    class TextCursorSetting :
        ParameterSetting(textCursor, textCursorFormat, 0, listOf(cursor, noCursor))

    class CursorStyleSetting :
        ParameterSetting(cursorStyle, cursorStyleFormat, 0, listOf(block, underline))

    class OperationModeSetting : ParameterSetting(
        nationalOnly,
        operatingModeFormat,
        0,
        listOf(vt300_7bit, vt300_8bit, vt100Mode, vt52Mode)
    )

    class VT100ModeTerminalIdSetting : ParameterSetting(
        terminalModeId,
        terminalModeIdFormat,
        0,
        listOf(vt320Id, vt100Id, vt101Id, vt102Id, vt220Id)
    )

    class UserFeaturesSetting :
        ParameterSetting(userFeatures, userFeaturesFormat, 0, listOf(unlocked, locked))

    class UserDefinedKeysSetting :
        ParameterSetting(userDefinedKeys, userDefinedKeysFormat, 0, listOf(unlocked, locked))


    class KeypadModeSetting :
        ParameterSetting(keypadModeKey, keyboardFormat, 0, listOf(numeric, "Application"))

    class CursorKeysModeSetting :
        ParameterSetting(cursorKeysModeKey, cursorKeysModeFormat, 0, listOf(normal, "Application"))

    class NewLineModeSetting :
        ParameterSetting(newLineMode, newLineModeFormat, 0, listOf(no, ""))

    class CharacterSetModeSetting :
        ParameterSetting(characterSetModeKey, characterSetModeFormat, 0, listOf("7-bit", "8-bit"))

    class UserPreferredCharacterSet : ParameterSetting(
        userPreferredCharacterSet,
        userPreferredCharacterSetFormat,
        0,
        listOf(userPreferredCharacterSetDEC, userPreferredCharacterSetISO)
    )

    class TransmitSetting : ParameterSetting(
        transmit,
        transmitFormat,
        transmitDefault,
        listOf("75", "110", "150", "300", "600", "1200", "2400", "4800", "9600", "19200", "38400")
    )

    class ReceiveSetting :
        ParameterSetting(
            receive,
            receiveFormat,
            0,
            listOf(
                "Transmit",
                "75",
                "110",
                "150",
                "300",
                "600",
                "1200",
                "2400",
                "4800",
                "9600",
                "19200"
            )
        )

    class XOffSetting :
        ParameterSetting(xOff, xOffFormat, 0, listOf("XOFF at 64", "XOFF at 128", "No XOFF"))

    class BitsFormatSetting : ParameterSetting(
        bitsFormatKey,
        bitsFormatFormat,
        bitsFormatDefault,
        listOf(
            "8 bits, no parity",
            "8 bits, even parity",
            "8 bits, odd parity",
            "7 bits, even parity",
            "7 bits, odd parity",
            "7 bits, mark parity",
            "7 bits, space parity"
        )
    )

    class StopBitsSetting :
        ParameterSetting(stopBitsKey, stopBitsFormat, 0, listOf(oneStopBit, twoStopBits))

    class LocalEchoSetting : ParameterSetting(localEcho, localEchoFormat, 0, listOf(no, ""))
    class HostPortSetting : ParameterSetting(
        hostPortKey,
        portFormat,
        0,
        listOf(portRS232DataLeads, portRS232Modem, portDEC423DataLeads, portDEC423Modem)
    )

    class DisconnectDelaySetting : ParameterSetting(
        disconnectDelayKey,
        disconnectDelayFormat,
        0,
        listOf(twoSeconds, sixtyMilliSeconds)
    )

    class TerminalTransmitSpeedSetting : ParameterSetting(
        terminalTransmitSpeedKey,
        terminalTransmitSpeedFormat,
        0,
        listOf(limited, unlimited)
    )

    class AutoAnswerBackSetting :
        ParameterSetting(autoAnswerBackKey, autoAnswerBackFormat, 0, listOf(no, ""))

    class ConcealedSetting :
        ParameterSetting(answerbackConcealedKey, concealedFormat, 0, listOf(not, "Concealed")) {

        override fun next(): Int {
            // Cannot reset to "Not Concealed" (0) once "Concealed" (1)
            if (index() == 1) return 1
            return super.next()
        }

        fun resetToNotConcealed() {
            currentIndex = 0
            save()
        }
    }

    class PrinterSpeedSetting : ParameterSetting(
        printerSpeedKey,
        printerSpeedFormat,
        printerSpeedDefault,
        listOf("75", "110", "150", "300", "600", "1200", "2400", "4800", "9600", "19200")
    )

    class PrinterToHostSetting :
        ParameterSetting(printerToHostKey, printerToHostFormat, 0, listOf(no, ""))

    class PrinterModeSetting :
        ParameterSetting(
            printerModeKey,
            printerModeFormat,
            0,
            listOf(printerModeNormal, printerModeAuto, printerModeController)
        )

    class PrinterXOffSetting :
        ParameterSetting(printerXOffKey, printerXOffFormat, 0, listOf("XOFF", "No XOFF"))

    class PrinterBitsFormatSetting : ParameterSetting(
        printerBitsFormatKey,
        printerBitsFormatFormat,
        1,
        listOf(
            "8 Bits, No Parity",
            "8 Bits, Even Parity",
            "8 Bits, Odd Parity",
            "7 Bits, No Parity",
            "7 Bits, Mark Parity",
            "7 Bits, Space Parity",
            "7 Bits, Even Parity",
            "7 Bits, Odd Parity"
        )
    )

    class PrinterStopBitsSetting : ParameterSetting(
        printerStopBitsKey,
        printerStopBitsFormat,
        0,
        listOf(oneStopBit, twoStopBits)
    )

    class PrinterScreenSizeSetting : ParameterSetting(
        printerScreenSizeKey,
        printerScreenSizeFormat,
        0,
        listOf(fullPage, scrollRegion)
    )

    class PrintedDataTypeSetting :
        ParameterSetting(
            printedDataTypeKey,
            printedDataTypeFormat,
            0,
            listOf(nationalOnly, nationalLineDrawing, printAll)
        )

    class PrintTerminatorSetting : ParameterSetting(
        printTerminatorKey,
        printTerminatorFormat,
        0,
        listOf(noTerminator, formFeedTerminator)
    )

    class TerminalKeyMapSetting : ParameterSetting(
        terminalKeyMapKey,
        terminalKeyMapFormat,
        0,
        listOf("Typewriter", "Data Processing")
    )

    class LockKeySetting : ParameterSetting(lockKey, lockKeyFormat, 0, listOf(caps, shift))
    class AutoRepeatSetting : ParameterSetting(autoRepeat, autoRepeatFormat, 1, listOf(no, ""))
    class KeyClickSetting : ParameterSetting(keyClick, keyClickFormat, 1, listOf(no, ""))
    class MarginBellSetting : ParameterSetting(marginBell, marginBellFormat, 1, listOf(no, ""))
    class WarningBellSetting :
        ParameterSetting(warningBell, warningBellFormat, 1, listOf(no, ""))

    class BreakKeySetting : ParameterSetting(breakKey, breakKeyFormat, 1, listOf(no, ""))
    class ComposeKeySetting : ParameterSetting(composeKey, composeKeyFormat, 1, listOf(no, ""))
    class BackArrowKeySetting : ParameterSetting(
        backArrowKey,
        backArrowKeyFormat,
        0,
        listOf(backArrowKeyDelete, backArrowKeyBackspace)
    )

    class CommaPointKeySetting :
        ParameterSetting(
            commaPointKey,
            commaPointKeyFormat,
            0,
            listOf(commaPointKeyLabel, commaPointKeySend)
        )

    class AngleBracketKeySetting :
        ParameterSetting(
            angleBracketKey,
            angleBracketKeyFormat,
            0,
            listOf(angleBracketKeyLabel, angleBracketKeySend)
        )

    class TildeKeySetting :
        ParameterSetting(tildeKey, tildeKeyFormat, 0, listOf(tildeKeyLabel, tildeKeySend))

    private val keyboardSetting = KeyboardSetting()
    private val onLineLocalSetting = OnLineLocalSetting()
    private val setUpLanguageSetting = SetUpLanguageSetting()
    private val columnModeSetting = ColumnModeSetting()
    private val controlsModeSetting = ControlsModeSetting()
    private val wrapModeSetting = WrapModeSetting()
    private val scrollModeSetting = ScrollModeSetting()
    private val screenDisplayTypeSetting = ScreenDisplayTypeSetting()
    private val textCursorSetting = TextCursorSetting()
    private val cursorStyleSetting = CursorStyleSetting()
    private val statusDisplaySetting = StatusDisplaySetting()
    private val operationModeSetting = OperationModeSetting()
    private val vt100ModeTerminalIdSetting = VT100ModeTerminalIdSetting()
    private val userDefinedKeysSetting = UserDefinedKeysSetting()
    private val userFeaturesSettingInstance = UserFeaturesSetting()
    private val characterSetModeSetting = CharacterSetModeSetting()
    private val keypadModeSetting = KeypadModeSetting()
    private val cursorKeysModeSetting = CursorKeysModeSetting()
    private val newLineModeSetting = NewLineModeSetting()
    private val userPreferredCharacterSet = UserPreferredCharacterSet()
    private val transmitSetting = TransmitSetting()
    private val receiveSetting = ReceiveSetting()
    private val xOffSetting = XOffSetting()
    private val bitsFormatSetting = BitsFormatSetting()
    private val stopBitsSetting = StopBitsSetting()
    private val localEchoSetting = LocalEchoSetting()
    private val hostPortSetting = HostPortSetting()
    private val disconnectDelaySetting = DisconnectDelaySetting()
    private val terminalTransmitSpeedSetting = TerminalTransmitSpeedSetting()
    private val autoAnswerBackSetting = AutoAnswerBackSetting()
    private val printerSpeedSetting = PrinterSpeedSetting()
    private val printerToHostSetting = PrinterToHostSetting()
    private val printerModeSetting = PrinterModeSetting()
    private val printerXOffSetting = PrinterXOffSetting()
    private val printerBitsFormatSetting = PrinterBitsFormatSetting()
    private val printerStopBitsSetting = PrinterStopBitsSetting()
    private val printerScreenSizeSetting = PrinterScreenSizeSetting()
    private val printedDataTypeSetting = PrintedDataTypeSetting()
    private val printTerminatorSetting = PrintTerminatorSetting()
    private val terminalKeyMapSetting = TerminalKeyMapSetting()
    private val lockKeySetting = LockKeySetting()
    private val autoRepeatSetting = AutoRepeatSetting()
    private val keyClickSetting = KeyClickSetting()
    private val marginBellSetting = MarginBellSetting()
    private val warningBellSetting = WarningBellSetting()
    private val breakKeySetting = BreakKeySetting()
    private val composeKeySetting = ComposeKeySetting()
    private val backArrowKeySetting = BackArrowKeySetting()
    private val commaPointKeySetting = CommaPointKeySetting()
    private val angleBracketKeySetting = AngleBracketKeySetting()
    private val tildeKeySetting = TildeKeySetting()

    private val concealedSetting = ConcealedSetting()
    private val tabField = TabField()

    private val setUpDirectoryScreen = SetupDirectory()
    private val displaySetupScreen = DisplaySetup()
    private val generalSetupScreen = GeneralSetup()
    private val communicationSetupScreen = CommunicationsSetup()
    private val printerSetupScreen = PrinterSetup()
    private val keyboardSetupScreen = KeyboardSetup()
    private val tabSetupScreen = TabSetup()

    private val setUpScreens = listOf(
        setUpDirectoryScreen, displaySetupScreen, generalSetupScreen,
        communicationSetupScreen, printerSetupScreen, keyboardSetupScreen, tabSetupScreen
    )

    private var savedScreen: Array<Array<TerminalCell>>? = null
    private var savedCursorX = 0
    private var savedCursorY = 0
    private var savedLineAttributes: Array<VT320.LineAttribute>? = null
    private var savedTopMargin = 0
    private var savedBottomMargin = 0
    private var savedCurrentAttr = TerminalAttributes()
    private var savedActiveStatusLine = false
    private var savedConformanceLevel = 63

    fun toggle() {
        ZXLog.terminal("VTSetup: toggle() called, current isVisible=$isVisible")
        if (isVisible) exit() else enter()
    }

    private fun enter() {
        ZXLog.terminal("VTSetup: enter() - saving screen and starting draw")
        isVisible = true

        // Save current terminal state
        savedScreen = vt.screen.map { it.copyOf() }.toTypedArray()
        savedCursorX = vt.cursorX
        savedCursorY = vt.cursorY
        savedLineAttributes = vt.lineAttributes.copyOf()
        savedTopMargin = vt.topMargin
        savedBottomMargin = vt.bottomMargin
        savedCurrentAttr = vt.currentAttr
        savedActiveStatusLine = vt.activeStatusLine
        savedConformanceLevel = vt.conformanceLevel

        // Prepare terminal for menu drawing without a full RIS
        vt.clearScreen()
        vt.softReset() // Clears state/margins/attrs but keeps interpretControls if we are careful
        // The setup UI always speaks VT320/ANSI, regardless of the host mode.
        vt.conformanceLevel = 63

        // Ensure menu reflects current (even unsaved) settings
        setUpScreens.forEach { it.recall() }
        vt.setupPrinterStatus = vt.printerStatus

        currentScreen = setUpDirectoryScreen
        currentScreen?.draw()
        ZXLog.terminal("VTSetup: enter() - draw complete")
    }

    private fun exit() {
        ZXLog.terminal("VTSetup: exit() - restoring screen. savedScreen present: ${savedScreen != null}")
        isVisible = false

        // Restore terminal state
        if (savedScreen != null) {
            vt.screen = savedScreen!!
        } else {
            vt.clearScreen()
        }

        vt.cursorX = savedCursorX
        vt.cursorY = savedCursorY
        savedLineAttributes?.let { vt.lineAttributes = it }
        vt.topMargin = savedTopMargin
        vt.bottomMargin = savedBottomMargin
        vt.currentAttr = savedCurrentAttr
        vt.activeStatusLine = savedActiveStatusLine
        vt.conformanceLevel = savedConformanceLevel
        vt.printerStatus = vt.setupPrinterStatus

        // Apply pending mode changes from Setup
        vt.keypadMode = VT320Settings.keypadMode == 1
        vt.cursorKeysMode = VT320Settings.cursorKeysMode == 1
        vt.newLineMode = VT320Settings.newLineMode == 1
        vt.characterSetMode8Bit = VT320Settings.characterSetMode == 1

        val modeIndex = VT320Settings.statusDisplay
        vt.statusLineManager.mode =
            StatusLineManager.Mode.entries.getOrElse(modeIndex) { StatusLineManager.Mode.INDICATOR }

        currentScreen = null
        vt.updateStatusLine()
    }

    fun handleInput(code: String): Boolean {
        if (!isVisible) return false
        ZXLog.terminal("VTSetup: handleInput code='${code.replace("\u001B", "ESC")}'")
        if (isPartialSequence(code)) return false

        val handled = when (code) {
            UP, DOWN, LEFT, RIGHT, ENTER, SETUP_KEY, DEL_KEY -> {
                val result = currentScreen?.onEvent(code) == true
                if (result) currentScreen?.draw()
                true
            }

            else -> {
                if (!code.startsWith(ESC)) {
                    val result = currentScreen?.onEvent(code) == true
                    if (result) currentScreen?.draw()
                    result
                } else false
            }
        }

        return handled
    }

    fun isPartialSequence(code: String): Boolean {
        if (code == ESC || code == CSI) return true
        if (code.startsWith(CSI) && code.length < 10 && !code.last()
                .isLetter() && code.last() != '~'
        ) return true
        return false
    }

    fun recall() {
        isVisible = false
        VT320Settings.recall()
        setUpScreens.forEach { it.recall() }

        val modeIndex = VT320Settings.printerMode
        vt.setupPrinterStatus = when (modeIndex) {
            1 -> PrinterStatus.AUTO
            2 -> PrinterStatus.CONTROLLER
            else -> PrinterStatus.READY
        }
        vt.applyKeyboardLanguage()
    }

    fun save() {
        setUpScreens.forEach { it.save() }
        VT320Settings.save()
    }

    fun factoryDefault() {
        setUpScreens.forEach { screen ->
            screen.forEach { line ->
                line.forEach { field ->
                    if (field is ParameterField) {
                        field.setting.resetToFactory()
                    }
                }
            }
        }
        VT320Settings.answerBack = ""
        tabField.reset()
        vt.applyKeyboardLanguage()
        vt.clearScreen()
        currentScreen?.draw()
        vt.updateStatusLine()
        showMessage("Done")
    }

    private fun showMessage(msg: String) {
        val translatedMsg = " ${translate(msg)} "
        CoroutineScope(Dispatchers.Main).launch {
            statusLine.clear()
            statusLine.set(
                (vt.columns - translatedMsg.length) / 2,
                translatedMsg,
                TerminalAttributes(inverse = true)
            )
            delay(1000.milliseconds)
            currentScreen?.bottom()
        }
    }
}
