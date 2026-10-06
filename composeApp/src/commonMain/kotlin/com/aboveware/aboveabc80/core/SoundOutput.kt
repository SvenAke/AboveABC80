package com.aboveware.aboveabc80.core

import kotlin.math.PI
import kotlin.math.exp

class SoundOutput(sampleRate: Int = 44100, volumePercent: Int = 100) {
    init {
        require(sampleRate > 0)
        require(volumePercent in 0..400)
    }

    @Volatile
    var volumePercent: Int = volumePercent
        set(value) {
            require(value in 0..400)
            field = value
        }

    private val dcDecay = exp(-2.0 * PI * 20.0 / sampleRate)
    private val gainStep = 1.0 - exp(-1.0 / (sampleRate * 0.01))
    private var previousInput = 0.0
    private var previousOutput = 0.0
    private var gain = 0.0

    fun process(samples: FloatArray): FloatArray {
        val targetGain = volumePercent / 100.0
        for (index in samples.indices) {
            val input = samples[index].toDouble()
            // Remove the chip's DC bias before boosting its short sound pulses.
            val output = dcDecay * (previousOutput + input - previousInput)
            previousInput = input
            previousOutput = output
            gain += gainStep * (targetGain - gain)
            samples[index] = (output * gain).coerceIn(-1.0, 1.0).toFloat()
        }
        return samples
    }
}
