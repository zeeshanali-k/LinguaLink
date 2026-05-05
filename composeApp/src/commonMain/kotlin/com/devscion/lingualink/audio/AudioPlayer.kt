package com.devscion.lingualink.audio

interface AudioPlayer {
    suspend fun playAudioStream(audioBytes: ByteArray, mimeType: String = "audio/mpeg")
    fun stop()
}
