package com.aboveware.aboveabc80.terminal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ComputerModelTest {
    @Test
    fun modelProfilesProvideTheirTextWidthsAndClocks() {
        assertFalse(ComputerModel.ABC80.isWideScreen(false))
        assertTrue(ComputerModel.ABC800M.isWideScreen(false))
        assertFalse(ComputerModel.ABC800C.isWideScreen(true))
        assertFalse(ComputerModel.ABC80.isAbc800)
        assertTrue(ComputerModel.ABC800M.isAbc800)
        assertTrue(ComputerModel.ABC800C.isAbc800)

        assertEquals(3_000_000, ComputerModel.ABC80.cpuFrequencyHz(3))
        assertEquals(3_580_000, ComputerModel.ABC800M.cpuFrequencyHz(4))
        assertEquals(3_580_000, ComputerModel.ABC800C.cpuFrequencyHz(4))
        assertEquals(5_000_000, ComputerModel.ABC800C.cpuFrequencyHz(5))
    }

    @Test
    fun storedModelFallsBackToAbc80ForUnknownValues() {
        assertEquals(ComputerModel.ABC800M, ComputerModel.fromStoredValue("ABC800M"))
        assertEquals(ComputerModel.ABC80, ComputerModel.fromStoredValue("unknown"))
    }

    @Test
    fun cpuSpeedLabelsShowNominalModelClock() {
        assertEquals("3.58 MHz", ComputerModel.ABC800C.cpuSpeedLabel(4))
        assertEquals("5.0 MHz", ComputerModel.ABC800C.cpuSpeedLabel(5))
        assertEquals("MAX", ComputerModel.ABC800C.cpuSpeedLabel(9))
    }
}
