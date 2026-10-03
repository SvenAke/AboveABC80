package com.aboveware.abovecpm.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

actual class AudioPlayer actual constructor(actual val sampleRate: Int) {
    private var line: SourceDataLine? = null

    actual fun start() {
        val format = AudioFormat(sampleRate.toFloat(), 16, 1, true, false)
        line = AudioSystem.getSourceDataLine(format)
        line?.open(format, 4096)
        line?.start()
    }

    actual fun play(samples: FloatArray) {
        val byteBuffer = ByteBuffer.allocate(samples.size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        for (sample in samples) {
            val s = (sample * 32767).toInt().toShort()
            byteBuffer.putShort(s)
        }
        line?.write(byteBuffer.array(), 0, byteBuffer.capacity())
    }

    actual fun stop() {
        line?.drain()
        line?.stop()
        line?.close()
        line = null
    }
}
