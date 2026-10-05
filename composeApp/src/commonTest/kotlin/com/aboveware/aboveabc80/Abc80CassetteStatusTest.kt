package com.aboveware.aboveabc80

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Abc80CassetteStatusTest {
    @BeforeTest
    fun reset() {
        Abc80CassetteStatus.hide()
    }

    @AfterTest
    fun cleanup() {
        Abc80CassetteStatus.hide()
    }

    @Test
    fun indexingDoesNotStartPlaybackAnimation() {
        Abc80CassetteStatus.show("Indexing cassette")
        assertTrue(Abc80CassetteStatus.visible)
        assertEquals(Abc80CassetteStatus.Activity.Idle, Abc80CassetteStatus.activity)
    }

    @Test
    fun readingPreservesProgressWhenMotorKeepsRunning() {
        Abc80CassetteStatus.startReading("MUSIK.BAS")
        assertEquals(Abc80CassetteStatus.Activity.Reading, Abc80CassetteStatus.activity)
        assertTrue(Abc80CassetteStatus.visible)
        assertEquals("Loading MUSIK.BAS...", Abc80CassetteStatus.text)
        Abc80CassetteStatus.show("Loading MUSIK.BAS...", false, 256, 1024)
        Abc80CassetteStatus.startReading("MUSIK.BAS")
        assertFalse(Abc80CassetteStatus.indeterminate)
        assertEquals(256, Abc80CassetteStatus.progress)
    }

    @Test
    fun writingSelectsRecordingAndPreservesStatus() {
        Abc80CassetteStatus.startWriting("MUSIK.BAS")
        assertEquals(Abc80CassetteStatus.Activity.Writing, Abc80CassetteStatus.activity)
        assertEquals("Saving MUSIK.BAS...", Abc80CassetteStatus.text)
        assertTrue(Abc80CassetteStatus.visible)
        Abc80CassetteStatus.show("Saving block")
        Abc80CassetteStatus.startWriting("MUSIK.BAS")
        assertEquals("Saving block", Abc80CassetteStatus.text)
    }

    @Test
    fun endOfTapeOffersRestartAndCancel() {
        Abc80CassetteStatus.startReading("MUSIK.BAS")
        Abc80CassetteStatus.programNotFound("MUSIK.BAS")
        assertEquals("MUSIK.BAS", Abc80CassetteStatus.missingProgram)
        assertEquals(Abc80CassetteStatus.EndOfTapeAction.Waiting, Abc80CassetteStatus.endOfTapeAction)
        Abc80CassetteStatus.restartSearch()
        assertEquals(null, Abc80CassetteStatus.missingProgram)
        assertEquals(Abc80CassetteStatus.EndOfTapeAction.Restart, Abc80CassetteStatus.endOfTapeAction)
        assertFalse(Abc80CassetteStatus.cancelled)
        Abc80CassetteStatus.programNotFound("MUSIK.BAS")
        Abc80CassetteStatus.cancel()
        assertEquals(null, Abc80CassetteStatus.missingProgram)
        assertEquals(Abc80CassetteStatus.EndOfTapeAction.Cancel, Abc80CassetteStatus.endOfTapeAction)
        assertTrue(Abc80CassetteStatus.cancelled)
        Abc80CassetteStatus.hide()
        assertEquals(null, Abc80CassetteStatus.missingProgram)
    }

    @Test
    fun stoppingClearsAnimationAndCancellation() {
        Abc80CassetteStatus.startReading("MUSIK.BAS")
        Abc80CassetteStatus.cancel()
        assertTrue(Abc80CassetteStatus.cancelled)
        Abc80CassetteStatus.hide()
        assertEquals(Abc80CassetteStatus.Activity.Idle, Abc80CassetteStatus.activity)
        assertFalse(Abc80CassetteStatus.visible)
        assertFalse(Abc80CassetteStatus.cancelled)
        Abc80CassetteStatus.startWriting("MUSIK.BAS")
        Abc80CassetteStatus.hide()
        assertEquals(Abc80CassetteStatus.Activity.Idle, Abc80CassetteStatus.activity)
    }
}
