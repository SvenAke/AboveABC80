package com.aboveware.abovecpm.core

expect class AudioPlayer(sampleRate: Int = 44100) {
    val sampleRate: Int
    fun play(samples: FloatArray)
    fun start()
    fun stop()
}
