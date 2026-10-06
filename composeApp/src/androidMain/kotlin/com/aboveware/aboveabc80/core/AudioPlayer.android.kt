package com.aboveware.aboveabc80.core

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

actual class AudioPlayer actual constructor(actual val sampleRate: Int) {
    private var audioTrack: AudioTrack? = null

    actual fun start() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT
        )
        check(minBufferSize > 0) { "AudioTrack buffer query failed: $minBufferSize" }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            error("AudioTrack initialization failed")
        }
        audioTrack = track
        track.play()
    }

    actual fun play(samples: FloatArray) {
        val track = checkNotNull(audioTrack) { "Audio playback has not started" }
        var offset = 0
        while (offset < samples.size) {
            val written = track.write(samples, offset, samples.size - offset, AudioTrack.WRITE_BLOCKING)
            check(written > 0) { "AudioTrack write failed: $written" }
            offset += written
        }
    }

    actual fun stop() {
        val track = audioTrack ?: return
        try {
            if (track.playState != AudioTrack.PLAYSTATE_STOPPED) track.stop()
        } finally {
            track.release()
            audioTrack = null
        }
    }
}
