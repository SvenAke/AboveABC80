package com.aboveware.abovecpm.core

import com.aboveware.abovecpm.NativeLib
import com.aboveware.abovecpm.toHex
import java.nio.ByteBuffer
import java.nio.ByteOrder

class CPU(bytes: ByteArray? = null) {
    var af: Int = 0
    var bc: Int = 0
    var de: Int = 0
    var hl: Int = 0
    var af_: Int = 0
    var bc_: Int = 0
    var de_: Int = 0
    var hl_: Int = 0
    var ix: Int = 0
    var iy: Int = 0
    var i: Short = 0
    var r: Int = 0
    var r7: Short = 0
    var sp: Int = 0
    var pc: Int = 0
    var memptr: Int = 0
    var iff2_read: Int = 0
    var iff1: Short = 0
    var iff2: Short = 0
    var im: Short = 0
    var halted: Int = 0
    var totalCycles: Long = 0
    var q: Short = 0
    var interrupts_enabled_at: Long = 0

    val iReg: Int get() = i.toInt() and 0xFF
    val rReg: Int get() = (r and 0x7F) or (r7.toInt() and 0x80)

    val a: Int get() = (af shr 8) and 0xFF
    val h: Int get() = (hl shr 8) and 0xFF
    val l: Int get() = hl and 0xFF
    val f: Int get() = af and 0xFF

    val sign: Boolean get() = (f and 0x80) != 0
    val zero: Boolean get() = (f and 0x40) != 0
    val x: Boolean get() = (f and 0x20) != 0
    val halfCarry: Boolean get() = (f and 0x10) != 0
    val y: Boolean get() = (f and 0x08) != 0
    val parity: Boolean get() = (f and 0x04) != 0
    val negative: Boolean get() = (f and 0x02) != 0
    val carry: Boolean get() = (f and 0x01) != 0
    val cycle: ULong get() = totalCycles.toULong()
    
    fun tStatesToNs(tStates: ULong) = (tStates * 1000000000UL).floorDiv(frequency)
    val frequency = 3500000UL

    init {
        if (bytes != null) {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            // regpairs: af, bc, de, hl, af_, bc_, de_, hl_, ix, iy (20 bytes)
            af = buffer.short.toInt() and 0xFFFF
            bc = buffer.short.toInt() and 0xFFFF
            de = buffer.short.toInt() and 0xFFFF
            hl = buffer.short.toInt() and 0xFFFF
            af_ = buffer.short.toInt() and 0xFFFF
            bc_ = buffer.short.toInt() and 0xFFFF
            de_ = buffer.short.toInt() and 0xFFFF
            hl_ = buffer.short.toInt() and 0xFFFF
            ix = buffer.short.toInt() and 0xFFFF
            iy = buffer.short.toInt() and 0xFFFF
            
            i = (buffer.get().toInt() and 0xFF).toShort()
            r = buffer.short.toInt() and 0xFFFF
            r7 = (buffer.get().toInt() and 0xFF).toShort()
            sp = buffer.short.toInt() and 0xFFFF
            pc = buffer.short.toInt() and 0xFFFF
            memptr = buffer.short.toInt() and 0xFFFF
            iff2_read = buffer.int
            iff1 = (buffer.get().toInt() and 0xFF).toShort()
            iff2 = (buffer.get().toInt() and 0xFF).toShort()
            im = (buffer.get().toInt() and 0xFF).toShort()
            halted = buffer.int
            totalCycles = buffer.long
            q = (buffer.get().toInt() and 0xFF).toShort()
            interrupts_enabled_at = buffer.int.toLong() and 0xFFFFFFFFL
        } else {
            NativeLib.getObject().updateCPU(this)
        }
    }

    override fun toString() =
        "AF=${af.toHex(4)} BC=${bc.toHex(4)} DE=${de.toHex(4)} HL=${hl.toHex(4)}\n" +
                "AF'=${af_.toHex(4)} BC'=${bc_.toHex(4)} DE'=${de_.toHex(4)} HL'=${hl_.toHex(4)}\n" +
                "IX=${ix.toHex(4)} IY=${iy.toHex(4)} SP=${sp.toHex(4)} PC=${pc.toHex(4)}\n" +
                "I=${iReg.toHex(2)} R=${rReg.toHex(2)} IFF1=$iff1 IFF2=$iff2 IM=$im HALTED=$halted\n" +
                "T-States=$cycle MEMPTR=${memptr.toHex(4)} " +
                "(S=${if (sign) '1' else '0'} Z=${if (zero) '1' else '0'} H=${if (halfCarry) '1' else '0'} " +
                "P=${if (parity) '1' else '0'} N=${if (negative) '1' else '0'} C=${if (carry) '1' else '0'})"
}

@OptIn(ExperimentalUnsignedTypes::class)
fun cpu() = CPU()
