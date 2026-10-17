package com.aboveware.aboveabc80

import com.aboveware.aboveabc80.terminal.ComputerModel
import com.aboveware.aboveabc80.terminal.TerminalManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ABC800HardwareTest {
    @Test
    fun monochromeDtrSelectsVideoRamWithActiveLowPolarity() =
        checkDtrBanking(ComputerModel.ABC800M)

    @Test
    fun colorDtrSelectsVideoRamWithActiveLowPolarity() =
        checkDtrBanking(ComputerModel.ABC800C)

    private fun checkDtrBanking(model: ComputerModel) = withMachine(model) { lib ->
        lib.copyToMemory(0x1234, bytes(0xa6))
        lib.copyToMemory(0x8000, bytes(
            0xf3,
            0x3e, 5, 0xd3, 0x23, 0x3e, 0xe8, 0xd3, 0x23,
            0x3e, 0x5a, 0x32, 0x34, 0x12,
            0x3a, 0x34, 0x12, 0x32, 0x10, 0xa0,
            0x3e, 5, 0xd3, 0x23, 0x3e, 0x68, 0xd3, 0x23,
            0x3a, 0x34, 0x12, 0x32, 0x11, 0xa0,
            0x3e, 5, 0xd3, 0x23, 0x3e, 0xe8, 0xd3, 0x23,
            0x3a, 0x34, 0x12, 0x32, 0x12, 0xa0,
            0x3e, 0x55, 0x32, 0x01, 0xa0, 0x18, 0xfe
        ))
        startAndWait(lib)
        assertEquals(0x5a, lib.peek(0xa010))
        assertEquals(0xa6, lib.peek(0xa011))
        assertEquals(0x5a, lib.peek(0xa012))
    }

    @Test
    fun timerInterruptsResumeHaltAndRetiAllowsTheNextInterrupt() =
        withMachine(ComputerModel.ABC800M) { lib ->
            lib.copyToMemory(0xffd6, bytes(0x00, 0x90))
            lib.copyToMemory(0x9000, bytes(
                0xf5, 0x3a, 0x00, 0xa0, 0x3c, 0x32, 0x00, 0xa0,
                0xf1, 0xfb, 0xed, 0x4d
            ))
            lib.copyToMemory(0x8000, bytes(
                0xf3, 0x31, 0x00, 0xf0,
                0x3e, 0xff, 0xed, 0x47, 0xed, 0x5e,
                0x3e, 0xd0, 0xd3, 0x7c,
                0x3e, 0xa5, 0xd3, 0x7f,
                0x3e, 125, 0xd3, 0x7f,
                0xfb, 0x76, 0x76, 0xf3,
                0x3e, 3, 0xd3, 0x63,
                0x3e, 0x55, 0x32, 0x01, 0xa0, 0x18, 0xfe
            ))
            startAndWait(lib)
            assertEquals(2, lib.peek(0xa000), "Both HALTs must receive a CTC channel 3 interrupt")
            delay(50)
            assertEquals(2, lib.peek(0xa000), "Software reset must stop the timer")
        }

    @Test
    fun timerCounterReadbackUsesTheSelectedPrescaler() =
        withMachine(ComputerModel.ABC800C) { lib ->
            lib.copyToMemory(0x8000, bytes(
                0xf3,
                0x3e, 0x05, 0xd3, 0x63, 0x3e, 125, 0xd3, 0x63,
                0x06, 20, 0x10, 0xfe,
                0xdb, 0x7f, 0x32, 0x10, 0xa0,
                0x3e, 0x27, 0xd3, 0x63, 0x3e, 125, 0xd3, 0x63,
                0x06, 20, 0x10, 0xfe,
                0xdb, 0x63, 0x32, 0x11, 0xa0,
                0x3e, 0x55, 0x32, 0x01, 0xa0, 0x18, 0xfe
            ))
            startAndWait(lib)
            assertTrue(lib.peek(0xa010) in 107..109, "16-cycle prescaler: ${lib.peek(0xa010)}")
            assertEquals(124, lib.peek(0xa011), "256-cycle prescaler")
        }

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    private suspend fun startAndWait(lib: NativeLib) {
        lib.copyToMemory(0, bytes(0xc3, 0x00, 0x80))
        lib.resetCpu()
        lib.startEmulator(3_580_000)
        lib.freeze(false)
        repeat(100) {
            if (lib.peek(0xa001) == 0x55) return
            delay(10)
        }
        error("Synthetic ABC800 program did not finish: ${lib.getCPUString()}")
    }

    private fun withMachine(model: ComputerModel, block: suspend (NativeLib) -> Unit) = runBlocking {
        val previousModel = TerminalManager.computerModel
        val lib = NativeLib.getObject()
        lib.freeze(true)
        try {
            TerminalManager.computerModel = model
            Assembler.instance.loadRomFiles()
            block(lib)
        } finally {
            lib.freeze(true)
            TerminalManager.computerModel = previousModel
            Assembler.instance.loadRomFiles()
            lib.resetCpu()
            lib.freeze(false)
        }
    }
}
