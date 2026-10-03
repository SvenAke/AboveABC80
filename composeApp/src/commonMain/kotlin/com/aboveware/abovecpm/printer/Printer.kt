package com.aboveware.abovecpm.printer

abstract class Printer(val name: String) {
    // Basic settings in Inches
    var horizontalDpi: Int = 120
    var verticalDpi: Int = 72
    var paperWidthInches: Float = 8.0f
    var autoLineFeed: Boolean = false // Controlled by drivers/settings

    // Virtual DPI for the shared buffer
    protected val virtualDpi = VirtualPrinter.VIRTUAL_DPI

    // State in inches for model-independent positioning
    protected var cursorXInch: Float = 0f

    // Style State
    protected var isBold = false
    protected var isItalic = false
    protected var isUnderline = false
    protected var isDoubleWide = false
    protected var isCompressed = false
    protected var isElite = false
    protected var isProportional = false
    protected var isUserDefinedEnabled = false
    protected var activeColor: Int = 1 // 1=Black, 2=Red

    protected val horizontalTabs = mutableListOf<Float>()
    protected val userDefinedChars = mutableMapOf<Int, ByteArray>()

    protected var currentLineStartIndex = -1

    // Results go here
    protected val lines = VirtualPrinter.instance.lines

    init {
        resetTabs()
    }

    abstract val testData: List<String>

    abstract fun printChar(c: Char)

    protected abstract fun drawGlyph(c: Char, xInch: Float)

    protected open fun calculateCharWidthInch(c: Char): Float {
        return 1.0f / 10.0f // Default 10 CPI
    }

    open fun reset() {
        cursorXInch = 0f
        isBold = false
        isItalic = false
        isUnderline = false
        isDoubleWide = false
        isCompressed = false
        isElite = false
        isProportional = false
        isUserDefinedEnabled = false
        activeColor = 1
        currentLineStartIndex = -1
        userDefinedChars.clear()
        resetTabs()
    }

    protected open fun resetTabs() {
        horizontalTabs.clear()
        // Default tabs every 8 characters @ 10 CPI = 0.8 inches
        for (i in 1..32) horizontalTabs.add(i * 0.8f)
    }

    protected fun handleTab() {
        val nextTab = horizontalTabs.firstOrNull { it > cursorXInch + 0.001f }
        if (nextTab != null) {
            cursorXInch = nextTab
        } else {
            // Fallback: Default tabs every 0.8 inches (8 chars @ 10 CPI)
            val tabSize = 0.8f
            val currentTabCount = (cursorXInch / tabSize).toInt()
            cursorXInch = (currentTabCount + 1) * tabSize
        }
    }

    protected fun setPixel(y: Int, x: Int, color: Int = activeColor) {
        if (currentLineStartIndex == -1) {
            // Allocate a new 8-row block if we don't have one
            currentLineStartIndex = lines.size
            repeat(8) { lines.add(IntArray(VirtualPrinter.PRINTER_WIDTH_TOTAL)) }
        }

        val targetRow = currentLineStartIndex + y
        if (targetRow < lines.size && x >= 0 && x < VirtualPrinter.PRINTER_WIDTH_TOTAL) {
            val row = lines[targetRow]
            if (row[x] == 0) {
                val newRow = row.copyOf()
                newRow[x] = color
                lines[targetRow] = newRow
            }
        }
    }

    protected fun getFont8x8(c: Char): ByteArray {
        val mappedChar = mapCharacter(c)
        val code = mappedChar.code
        // Explicitly handle space, null, and non-breaking space
        if (code == 32 || code == 0 || code == 160) return ByteArray(8) { 0x00.toByte() }

        // Try exact character first (important for lowercase vs uppercase distinction)
        return VirtualPrinter.fontData[code]
            ?: VirtualPrinter.fontData[mappedChar.uppercaseChar().code]
            ?: ByteArray(8) { 0x00.toByte() } // Fallback to empty space for unknown characters
    }

    protected open fun mapCharacter(c: Char): Char = c

    open fun syncWithDipSwitches() {}
}
