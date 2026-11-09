package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.delay

class HighResOptTest {
    @Test
    fun highResolutionOptionPortWritesUpdateControlLatches() = runBlocking {
        val previousModel = TerminalManager.computerModel
        val native = NativeLib.getObject()
        native.freeze(true)
        try {
            TerminalManager.computerModel = ComputerModel.ABC800M
            Assembler.instance.loadRomFiles()
            native.setAbc800HrEnabled(true)
            // OUT switches the low address space to video RAM, so execute in main RAM.
            native.copyToMemory(0, byteArrayOf(0xC3.toByte(), 0x00, 0xC0.toByte()))
            native.copyToMemory(0xC000, byteArrayOf(
                0xF3.toByte(), // DI
                0x3E, 0x80.toByte(), // LD A,80h
                0xD3.toByte(), 0x06, // OUT (06h),A
                0x3E, 0xAA.toByte(), // LD A,AAh
                0xD3.toByte(), 0x07, // OUT (07h),A
                0x76 // HALT
            ))
            assertEquals(0, native.getAbc800MHighResolutionControl())
            native.startEmulator(3_580_000)
            val deadline = System.nanoTime() + 2_000_000_000L
            while (native.getAbc800MHighResolutionControl() != 0xAA80 &&
                System.nanoTime() < deadline) {
                delay(10)
            }
            native.freeze(true)
            assertEquals(0xAA80, native.getAbc800MHighResolutionControl())
        } finally {
            native.freeze(true)
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            native.resetCpu()
            native.freeze(false)
        }
    }
}
