package com.aboveware.aboveabc80

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgramCompatibilityTest {
    @Test
    fun usesModelSpecificExtensionsAsHints() {
        val absExecutable = byteArrayOf(0, 0, 0, 0xFF.toByte(), 0, 0xFF.toByte(), 0)
        assertEquals(
            ProgramCompatibilityHint(ProgramModelHint.ABC806, ProgramCompatibilityEvidence.EXTENSION_806),
            programCompatibilityHint("demo.806", absExecutable)
        )
        assertEquals(
            ProgramCompatibilityHint(ProgramModelHint.ABC800, ProgramCompatibilityEvidence.EXTENSION_800),
            programCompatibilityHint("demo.800", absExecutable)
        )
        assertEquals(
            ProgramCompatibilityHint(ProgramModelHint.UNKNOWN, ProgramCompatibilityEvidence.INSUFFICIENT),
            programCompatibilityHint("demopic.806", byteArrayOf(0, 0, 0, 1, 1, 2, 3))
        )
    }

    @Test
    fun detectsBasicIiAndHighResolutionCommandHints() {
        assertEquals(
            ProgramCompatibilityHint(ProgramModelHint.ABC800_806, ProgramCompatibilityEvidence.BASIC_II_RECORD),
            programCompatibilityHint("demo.bac", byteArrayOf(0x8F.toByte(), 0x01))
        )
        assertEquals(
            ProgramCompatibilityHint(
                ProgramModelHint.ABC806,
                ProgramCompatibilityEvidence.HIGH_RESOLUTION_COMMANDS
            ),
            programCompatibilityHint("demo.bas", "10 ATTRIBUTE 1".encodeToByteArray())
        )
    }

    @Test
    fun reportsUnknownWhenEvidenceIsInsufficient() {
        assertEquals(
            ProgramCompatibilityHint(ProgramModelHint.UNKNOWN, ProgramCompatibilityEvidence.INSUFFICIENT),
            programCompatibilityHint("demo.bas", "10 PRINT 1".encodeToByteArray())
        )
        assertTrue(isProgramCompatibilityCandidate("demo.BAS"))
        assertFalse(isProgramCompatibilityCandidate("picture.bmp"))
    }
}
