package com.aboveware.aboveabc80.terminal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Manages the VT320 status line (25th row).
 */
class StatusLineManager(private val vt: VT320) {

    enum class Mode {
        NONE,           // Visible only in setup or when explicitly selected by host
        INDICATOR,      // Always visible, shows system info
        HOST_WRITABLE   // Always visible, content controlled by host
    }

    var mode by mutableStateOf(Mode.NONE)
    var printerStatus by mutableStateOf(PrinterStatus.READY)

    // The actual content of the status line
    var cells by mutableStateOf(Array(132) { TerminalCell() })

    // Content written by the host (saved separately to allow switching modes)
    private var hostCells = Array(132) { TerminalCell() }

    /**
     * Determines if the status line should be rendered on screen.
     */
    fun isVisible(): Boolean {
        if (vt.setup.isVisible) return true
        return mode != Mode.NONE
    }

    /**
     * Updates the status line content based on the current mode and terminal state.
     */
    fun update(setupIndicator: String? = null) {
        if (vt.setup.isVisible) {
            renderSetupContent(setupIndicator)
            return
        }

        when (mode) {
            Mode.NONE -> {
                // Not visible, but we might want to keep it updated for when it becomes visible
                renderIndicatorContent()
            }

            Mode.INDICATOR -> {
                renderIndicatorContent()
            }

            Mode.HOST_WRITABLE -> {
                cells = hostCells
            }
        }
    }

    private fun renderSetupContent(setupIndicator: String?) {
        val attr = TerminalAttributes(inverse = false)
        val newCells = Array(vt.columns) { TerminalCell(' ', attr) }

        // Setup always shows (01,001)
        val posText = "(01,001)"
        posText.forEachIndexed { i, c ->
            if (i < vt.columns) newCells[i] = TerminalCell(c, attr)
        }

        setupIndicator?.forEachIndexed { i, c ->
            val pos = posText.length + 2 + i
            if (pos < vt.columns) newCells[pos] = TerminalCell(c, attr)
        }

        val printerText = "Printer: ${vt.setupPrinterStatus.text}"
        val start = (vt.columns - printerText.length) / 2
        printerText.forEachIndexed { i, c ->
            if (start + i < vt.columns) newCells[start + i] = TerminalCell(c, attr)
        }

        cells = newCells
    }

    private fun renderIndicatorContent() {
        val attr = TerminalAttributes(inverse = true)
        val newCells = Array(vt.columns) { TerminalCell(' ', attr) }

        // Format: (01,001)
        val posText = "(${(vt.cursorY + 1).toString().padStart(2, '0')},${
            (vt.cursorX + 1).toString().padStart(3, '0')
        })"
        posText.forEachIndexed { i, c ->
            if (i < vt.columns) newCells[i] = TerminalCell(c, attr)
        }

        val printerText = "Printer: ${printerStatus.text}"
        val start = (vt.columns - printerText.length) / 2
        printerText.forEachIndexed { i, c ->
            if (start + i < vt.columns) newCells[start + i] = TerminalCell(c, attr)
        }

        cells = newCells
    }

    /**
     * Called by the terminal when host writes to the status line.
     */
    fun writeFromHost(x: Int, c: Char, attr: TerminalAttributes) {
        if (x in 0 until 132) {
            hostCells[x] = TerminalCell(c, attr)
            if (mode == Mode.HOST_WRITABLE) {
                val newCells = cells.copyOf()
                newCells[x] = TerminalCell(c, attr)
                cells = newCells
            }
        }
    }

    fun erase(p: Int, x: Int) {
        when (p) {
            0 -> for (i in x until 132) hostCells[i] = TerminalCell()
            1 -> for (i in 0..x) hostCells[i] = TerminalCell()
            2 -> for (i in 0 until 132) hostCells[i] = TerminalCell()
        }
        if (mode == Mode.HOST_WRITABLE) {
            cells = hostCells.copyOf()
        }
    }

    fun clearHostContent() {
        erase(2, 0)
    }
}
