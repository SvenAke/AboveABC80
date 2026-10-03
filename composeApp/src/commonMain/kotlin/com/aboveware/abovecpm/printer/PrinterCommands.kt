package com.aboveware.abovecpm.printer

object PrinterCommands {
    const val ESC = "\u001B"
    const val CR = "\r"
    const val LF = "\n"
    const val FF = "\u000C"

    // ESC/P Commands
    const val RESET = "$ESC@"
    const val BOLD_ON = "${ESC}E"
    const val BOLD_OFF = "${ESC}F"
    const val ITALIC_ON = "${ESC}4"
    const val ITALIC_OFF = "${ESC}5"
    const val UNDERLINE_ON = "${ESC}-1"
    const val UNDERLINE_OFF = "${ESC}-0"
    const val DOUBLE_WIDE_ON = "${ESC}W1"
    const val DOUBLE_WIDE_OFF = "${ESC}W0"
    const val PROPORTIONAL_ON = "${ESC}p1"
    const val PROPORTIONAL_OFF = "${ESC}p0"
    const val MASTER_SELECT = "${ESC}!"

    const val COMPRESSED_ON = "\u000F"
    const val COMPRESSED_OFF = "\u0012"

    const val SET_LINE_SPACING_N_72 = "${ESC}A"
    const val LINE_SPACING_1_6 = "${ESC}2"
    const val SET_HORIZONTAL_TABS = "${ESC}D"

    const val GRAPHICS_60_DPI = "${ESC}K"
    const val GRAPHICS_120_DPI = "${ESC}L"
    const val GRAPHICS_FX_BIT_IMAGE = "${ESC}*"
}
