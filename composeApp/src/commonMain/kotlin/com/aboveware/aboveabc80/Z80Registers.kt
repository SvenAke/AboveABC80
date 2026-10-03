package com.aboveware.aboveabc80

/**
 * Z80 Registers state for state saving/restoring and BIOS initialization.
 */
data class Z80Registers(
    val af: Short, val bc: Short, val de: Short, val hl: Short,
    val af2: Short, val bc2: Short, val de2: Short, val hl2: Short,
    val ix: Short, val iy: Short, val sp: Short, val pc: Short,
    val i: Byte, val r: Byte, val iff1: Byte, val iff2: Byte, val im: Byte,
    val tStates: Int,
    val holdIntReqCycles: Byte,
    val flags: Byte,
    val memPtr: Short,
    val totalCycles: Long = 0L
)
