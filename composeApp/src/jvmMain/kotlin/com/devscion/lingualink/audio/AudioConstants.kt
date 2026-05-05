package com.devscion.lingualink.audio

import javax.sound.sampled.AudioFormat

val AUDIO_FORMAT = AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED,
    16000f,
    16,
    1,
    2,
    16000f,
    false
)

const val CHUNK_SIZE_BYTES = 3200   // 16000 Hz * 2 bytes * 0.1s
const val PLAYBACK_BUFFER_SIZE = 8192
