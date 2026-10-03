package com.aboveware.aboveabc80.core

import com.aboveware.aboveabc80.Constants

class Beeper(
    val sampleRate: Int = 44100,
    cpuFrequency: Int = Constants.CPU_FREQUENCY
) {
    private var tStatesPerSecond = cpuFrequency.toLong()
    private var lastTState = 0L
    private var level = 0.0f

    // For DC offset removal (High-pass filter)
    private var lastIn = 0.0f
    private var lastOut = 0.0f
    private val filterCoefficient = 0.999f

    private val lock = Any()
    private val changes = mutableListOf<Pair<Long, Float>>()

    fun setCpuFrequency(frequency: Int) {
        require(frequency > 0) { "CPU frequency must be positive" }
        synchronized(lock) {
            tStatesPerSecond = frequency.toLong()
        }
    }

    fun onEarChanged(level: Float, tStates: Long) {
        synchronized(lock) {
            changes.add(tStates to level)
        }
    }

    fun getSamples(count: Int, currentTStates: Long): FloatArray {
        val samples = FloatArray(count)
        val tStatesPerSample = tStatesPerSecond.toDouble() / sampleRate

        synchronized(lock) {
            var changeIdx = 0
            for (i in 0 until count) {
                val sampleTStart = lastTState + (i * tStatesPerSample)
                val sampleTEnd = lastTState + ((i + 1) * tStatesPerSample)

                var totalLevel = 0.0
                var currentT = sampleTStart

                // Integrate the level over the duration of one sample
                while (changeIdx < changes.size && changes[changeIdx].first.toDouble() < sampleTEnd) {
                    val changeT = changes[changeIdx].first.toDouble()
                    if (changeT > currentT) {
                        totalLevel += level * (changeT - currentT)
                        currentT = changeT
                    }
                    level = changes[changeIdx].second
                    changeIdx++
                }

                if (sampleTEnd > currentT) {
                    totalLevel += level * (sampleTEnd - currentT)
                }

                val rawSample = (totalLevel / tStatesPerSample).toFloat()

                // DC offset removal (High-pass filter) similar to Fuse
                val filteredSample = rawSample - lastIn + filterCoefficient * lastOut
                lastIn = rawSample
                lastOut = filteredSample

                samples[i] = filteredSample
            }

            // Clean up processed changes
            if (changeIdx > 0) {
                repeat(changeIdx) { changes.removeAt(0) }
            }

            lastTState += (count * tStatesPerSample).toLong()

            // If we are falling too far behind or jumping ahead, resync
            if (kotlin.math.abs(lastTState - currentTStates) > tStatesPerSecond / 10) {
                lastTState = currentTStates
            }
        }

        return samples
    }

    fun getPendingSamples(currentTStates: Long): FloatArray {
        var tStatesDelta = currentTStates - lastTState

        if (tStatesDelta < -0x40000000L) {
            tStatesDelta += 0x100000000L
        } else if (tStatesDelta < 0) {
            return FloatArray(0)
        }

        val maxSamples = sampleRate / 10
        val count = (tStatesDelta.toDouble() / tStatesPerSecond * sampleRate).toInt()
            .coerceIn(0, maxSamples)
        return if (count > 0) getSamples(count, currentTStates) else FloatArray(0)
    }

    fun reset(tStates: Long) {
        synchronized(lock) {
            changes.clear()
            lastTState = tStates
            level = 0.0f
            lastIn = 0.0f
            lastOut = 0.0f
        }
    }
}
