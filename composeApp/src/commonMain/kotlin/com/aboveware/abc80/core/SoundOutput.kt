package com.aboveware.abc80.core

import com.aboveware.abc80.Abc80CassetteStatus
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class CassetteSoundGenerator(private val sampleRate: Float = 44100f) {
    private var motorPhase = 0f
    private var dataPhase = 0f
    private var flutterPhase = 0f
    private var noiseState = 0x12345678
    private var bitPhase = 0f
    private var bitIndex = 0
    private var lfsr = 0xACE1
    private var currentBit = false
    private var prevTone = 0f
    private var lastActivity = Abc80CassetteStatus.Activity.Idle

    // ABC 80 cassette protocol @ 750 baud:
    // Each block = 296 bytes (2368 bits):
    //   - 32 bytes (256 bits) of 0x00 leader/pilot tone (steady 750 Hz tone)
    //   - 3x SYN (0x16), STX (0x02), block no, 256 data bytes, ETX (0x03), 2x CRC (2112 bits of FSK/biphasal data)
    //   - Brief inter-block gap (~90 bits / ~120 ms of tape hiss & motor hum between blocks)
    private val leaderBits = 256
    private val dataEndBit = 2368
    private val totalCycleBits = 2458

    fun generateSample(activity: Abc80CassetteStatus.Activity, cassetteBit: Boolean): Float {
        if (activity == Abc80CassetteStatus.Activity.Idle) {
            lastActivity = activity
            return 0f
        }
        if (lastActivity == Abc80CassetteStatus.Activity.Idle) {
            bitPhase = 0f
            bitIndex = 0
            dataPhase = 0f
        }
        lastActivity = activity

        // Wow & flutter (gentle tape transport variation)
        flutterPhase += 2f * PI.toFloat() * 4.2f / sampleRate
        if (flutterPhase > 2f * PI.toFloat()) flutterPhase -= 2f * PI.toFloat()
        val flutter = 1.0f + 0.008f * sin(flutterPhase)

        // 1. Mechanical motor hum (100 Hz + 200 Hz harmonic)
        val motorFreq = 100f * flutter
        motorPhase += 2f * PI.toFloat() * motorFreq / sampleRate
        if (motorPhase > 2f * PI.toFloat()) motorPhase -= 2f * PI.toFloat()
        val motorHum = 0.015f * sin(motorPhase) + 0.007f * sin(2f * motorPhase)

        // 2. Tape hiss (analog cassette noise floor)
        noiseState = (noiseState * 1664525 + 1013904223)
        val rawNoise = ((noiseState ushr 16) and 0xFFFF) / 65535f - 0.5f
        val tapeHiss = rawNoise * 0.014f

        // 3. Advance 750 baud bit clock
        val baudRate = 750f * flutter
        bitPhase += baudRate / sampleRate
        if (bitPhase >= 1f) {
            bitPhase -= 1f
            bitIndex = (bitIndex + 1) % totalCycleBits
            when {
                bitIndex < leaderBits -> {
                    // Pilot/leader: steady 0-bits (750 Hz fundamental)
                    currentBit = false
                }
                bitIndex < dataEndBit -> {
                    // Data payload: pseudo-random 750 baud bit stream mixed with live cassetteBit
                    val lsb = lfsr and 1
                    lfsr = lfsr ushr 1
                    if (lsb != 0) lfsr = lfsr xor 0xB400
                    currentBit = ((lfsr and 1) != 0) xor cassetteBit
                }
                else -> {
                    // Inter-block gap (no data carrier)
                    currentBit = false
                }
            }
        }

        // 4. Data carrier signal (ABC 80 biphase / FSK: 750 Hz for '0', 1500 Hz for '1')
        val rawDataTone = if (bitIndex < dataEndBit) {
            val dataFreq = (if (currentBit) 1500f else 750f) * flutter
            dataPhase += 2f * PI.toFloat() * dataFreq / sampleRate
            if (dataPhase > 2f * PI.toFloat()) dataPhase -= 2f * PI.toFloat()
            val sq = if (sin(dataPhase) >= 0f) 1f else -1f
            // Soften square wave with fundamental sine to mimic magnetic tape head rolloff
            0.055f * (0.65f * sq + 0.35f * sin(dataPhase))
        } else {
            0f
        }

        // One-pole low-pass filter (~3.2 kHz cutoff) for authentic warm cassette deck acoustics
        prevTone += 0.35f * (rawDataTone - prevTone)

        return motorHum + tapeHiss + prevTone
    }
}

class SoundOutput(private val sampleRate: Int = 44100, volumePercent: Int = 100) {
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

    private val cassetteSound = CassetteSoundGenerator(sampleRate.toFloat())
    private val dcDecay = exp(-2.0 * PI * 20.0 / sampleRate)
    private val gainStep = 1.0 - exp(-1.0 / (sampleRate * 0.01))
    private var previousInput = 0.0
    private var previousOutput = 0.0
    private var gain = 0.0

    fun process(
        samples: FloatArray,
        activity: Abc80CassetteStatus.Activity = Abc80CassetteStatus.activity,
        cassetteBit: Boolean = false
    ): FloatArray {
        val targetGain = volumePercent / 100.0
        for (index in samples.indices) {
            val input = samples[index].toDouble() + cassetteSound.generateSample(activity, cassetteBit)
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
