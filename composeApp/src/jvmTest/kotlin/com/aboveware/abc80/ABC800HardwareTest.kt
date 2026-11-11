package com.aboveware.abc80

import com.aboveware.abc80.terminal.ComputerModel
import com.aboveware.abc80.terminal.TerminalManager
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

    @Test
    fun hrBoardProvidesBankedRamAndMirroredRegistersOnBothModels() {
        for (model in listOf(ComputerModel.ABC800M, ComputerModel.ABC800C)) {
            for (enabled in listOf(true, false)) {
                withMachine(model, hrEnabled = enabled) { lib ->
                    lib.copyToMemory(0x1234, bytes(0xa6))
                    lib.copyToMemory(0x8000, bytes(
                        0xf3,
                        0x3e, 5, 0xd3, 0x23, 0x3e, 0xe8, 0xd3, 0x23,
                        0x3e, 0x5a, 0x32, 0x34, 0x12,
                        0x3e, 0x69, 0x32, 0xff, 0x3f,
                        0x3a, 0x34, 0x12, 0x32, 0x10, 0xa0,
                        0x3a, 0xff, 0x3f, 0x32, 0x11, 0xa0,
                        0x3e, 5, 0xd3, 0x23, 0x3e, 0x68, 0xd3, 0x23,
                        0x3a, 0x34, 0x12, 0x32, 0x12, 0xa0,
                        0x3e, 0x41, 0x32, 0x00, 0x78,
                        0x3a, 0x00, 0x78, 0x32, 0x13, 0xa0,
                        0x3e, 7, 0xd3, 0x1e, 0x3e, 0x83, 0xd3, 0x0f,
                        0x3e, 0x55, 0x32, 0x01, 0xa0, 0x18, 0xfe
                    ))
                    startAndWait(lib)
                    lib.freeze(true)
                    assertEquals(if (enabled) 0x5a else 0xff, lib.peek(0xa010), "$model HR=$enabled")
                    assertEquals(if (enabled) 0x69 else 0xff, lib.peek(0xa011))
                    assertEquals(0xa6, lib.peek(0xa012), "BASIC ROM must remain readable")
                    assertEquals(0x41, lib.peek(0xa013), "Text RAM must work without the HR board")
                    assertEquals(if (enabled) 0x8307 else 0, lib.getAbc800MHighResolutionControl())
                    assertEquals(if (enabled) 0x5a else 0, lib.getAbc800MHighResolutionScreen()[0x1234].toInt())
                    Assembler.instance.loadRomFiles()
                    assertEquals(0, lib.getAbc800MHighResolutionControl())
                    assertTrue(lib.getAbc800MHighResolutionScreen().all { it == 0.toByte() })
                }
            }
        }
    }

    @Test
    fun graphicsRomExecutionBanksVideoRamWithoutChangingDtr() {
        for (model in listOf(ComputerModel.ABC800M, ComputerModel.ABC800C)) {
            withMachine(model) { lib ->
                lib.copyToMemory(0x1234, bytes(0xa6))
                lib.copyToMemory(0x7800, bytes(
                    0x3a, 0x34, 0x12, 0x32, 0x10, 0xa0, 0xc9
                ))
                lib.copyToMemory(0x8000, bytes(
                    0xf3, 0x31, 0x00, 0xf0,
                    0x3e, 5, 0xd3, 0x23, 0x3e, 0xe8, 0xd3, 0x23,
                    0x3e, 0x5a, 0x32, 0x34, 0x12,
                    0x3e, 5, 0xd3, 0x23, 0x3e, 0x68, 0xd3, 0x23,
                    0xcd, 0x00, 0x78,
                    0x3a, 0x34, 0x12, 0x32, 0x11, 0xa0,
                    0x3e, 0x55, 0x32, 0x01, 0xa0, 0x18, 0xfe
                ))
                startAndWait(lib)
                assertEquals(0x5a, lib.peek(0xa010), "$model graphics-ROM banking")
                assertEquals(0xa6, lib.peek(0xa011), "$model must restore BASIC ROM banking")
            }
        }
    }

    @Test
    fun bothModelsBootAndRunBasicWithOrWithoutHrBoard() {
        for (model in listOf(ComputerModel.ABC800M, ComputerModel.ABC800C)) {
            for (enabled in listOf(true, false)) {
                withMachine(model, hrEnabled = enabled) { lib ->
                    lib.resetCpu()
                    lib.startEmulator(3_580_000)
                    lib.freeze(false)
                    delay(2_000)
                    "PRINT 12345\r".forEach {
                        lib.sendKey(it.code)
                        delay(30)
                    }
                    var completed = false
                    repeat(200) {
                        if (!completed) {
                            val text = if (model == ComputerModel.ABC800M) {
                                decodeABC800MScreen(lib.getAbc800MTextScreen())
                            } else {
                                decodeABC800CScreen(lib.getAbc800CTextScreen())
                            }
                            completed = text.any { it.trim() == "12345" }
                            if (!completed) delay(10)
                        }
                    }
                    assertTrue(completed, "$model HR=$enabled did not evaluate PRINT")
                }
            }
        }
    }

    @Test
    fun basicFgpointReadsBackAllFourHrPixelValues() {
        for (model in listOf(ComputerModel.ABC800M, ComputerModel.ABC800C)) {
            withMachine(model) { lib ->
                lib.resetCpu()
                lib.startEmulator(3_580_000)
                lib.freeze(false)
                delay(2_000)
                val command = "10 FOR P%=0% TO 3% : FGPOINT 100%,100%,P% : PRINT FGPOINT(100%,100%); : NEXT P%\rRUN\r"
                command.forEach {
                    lib.sendKey(it.code)
                    delay(if (it == '\r') 300 else 30)
                }
                delay(1_000)
                val rows = if (model == ComputerModel.ABC800M) {
                    decodeABC800MScreen(lib.getAbc800MTextScreen())
                } else {
                    decodeABC800CScreen(lib.getAbc800CTextScreen())
                }
                assertTrue(
                    rows.any { it.trim().split(Regex("\\s+")) == listOf("0", "1", "2", "3") },
                    "$model FGPOINT readback failed:\n${rows.joinToString("\n")}"
                )
            }
        }
    }

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

    private fun withMachine(
        model: ComputerModel,
        hrEnabled: Boolean = true,
        block: suspend (NativeLib) -> Unit
    ) = runBlocking {
        val previousModel = TerminalManager.computerModel
        val previousHr = getPersistedString("abc800_hr_enabled", "true")
        val lib = NativeLib.getObject()
        lib.freeze(true)
        try {
            TerminalManager.computerModel = model
            setPersistedString("abc800_hr_enabled", hrEnabled.toString())
            Assembler.instance.loadRomFiles()
            block(lib)
        } finally {
            lib.freeze(true)
            TerminalManager.computerModel = previousModel
            setPersistedString("abc800_hr_enabled", previousHr)
            Assembler.instance.loadRomFiles()
            lib.resetCpu()
            lib.freeze(false)
        }
    }
}
