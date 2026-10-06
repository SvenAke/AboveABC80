package com.aboveware.aboveabc80.core

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SoundOutputTest {
    private fun tone(count: Int = 4410) = FloatArray(count) {
        (0.1 * sin(2.0 * PI * 440 * it / 44100)).toFloat()
    }

    private fun energy(samples: FloatArray) = samples.sumOf { (it * it).toDouble() }

    @Test
    fun volumeBoostQuadruplesAmplitudeWithoutRestart() {
        val output = SoundOutput()
        output.process(tone())
        val normal = energy(output.process(tone()))
        output.volumePercent = 400
        output.process(tone())
        val boosted = energy(output.process(tone()))
        assertTrue(boosted / normal in 15.8..16.2, "400 % must quadruple amplitude")
    }

    @Test
    fun muteAndUnmuteApplyToTheExistingOutput() {
        val output = SoundOutput(volumePercent = 400)
        output.process(tone())
        output.volumePercent = 0
        output.process(tone(44100))
        assertTrue(output.process(tone()).all { abs(it) < 0.00001f })
        output.volumePercent = 100
        output.process(tone())
        assertTrue(energy(output.process(tone())) > 10)
    }

    @Test
    fun removesDcBeforeBoostingMusicPulses() {
        val output = SoundOutput(volumePercent = 400)
        output.process(FloatArray(44100) { -0.54f })
        assertTrue(output.process(FloatArray(4410) { -0.54f }).all { abs(it) < 0.00001f })
        val pulses = output.process(FloatArray(44100) { if (it % 400 == 0) -0.4f else -0.54f })
        assertTrue(pulses.max() > 0.5f, "Quiet pulses must survive boosting a large DC bias")
        assertTrue(pulses.min() < 0f, "Output must be AC, not a boosted DC level")
        assertTrue(pulses.all { it.isFinite() && it in -1f..1f })
    }

    @Test
    fun limitsBoostedPcmAndRejectsInvalidSettings() {
        val output = SoundOutput(volumePercent = 400)
        val loud = output.process(FloatArray(4410) { if (it % 2 == 0) 1f else -1f })
        assertTrue(loud.all { it.isFinite() && it in -1f..1f })
        assertFailsWith<IllegalArgumentException> { output.volumePercent = 401 }
        assertFailsWith<IllegalArgumentException> { output.volumePercent = -1 }
        assertFailsWith<IllegalArgumentException> { SoundOutput(sampleRate = 0) }
    }

    @Test
    fun gainChangesAreSmoothedAndProcessingIsIndependentOfChunkSize() {
        val whole = SoundOutput()
        val chunked = SoundOutput()
        val samples = tone()
        val expected = whole.process(samples.copyOf())
        val actual = chunked.process(samples.copyOfRange(0, 1000)) +
            chunked.process(samples.copyOfRange(1000, samples.size))
        assertTrue(expected.indices.all { expected[it] == actual[it] })
        val output = SoundOutput(volumePercent = 0)
        output.process(tone())
        output.volumePercent = 400
        assertTrue(abs(output.process(floatArrayOf(0.1f))[0]) < 0.001f,
            "A volume change must not instantly jump to full gain")
    }
}
