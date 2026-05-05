package com.devscion.lingualink.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sound.sampled.*

class JvmAudioPlayer : AudioPlayer {

    private var sourceLine: SourceDataLine? = null

    override suspend fun playAudioStream(audioBytes: ByteArray, mimeType: String) =
        withContext(Dispatchers.IO) {
            try {
                val inputStream = audioBytes.inputStream()
                val audioStream = AudioSystem.getAudioInputStream(inputStream)
                val format = audioStream.format
                val info = DataLine.Info(SourceDataLine::class.java, format)
                val line = AudioSystem.getLine(info) as SourceDataLine
                line.open(format)
                line.start()
                val buffer = ByteArray(PLAYBACK_BUFFER_SIZE)
                var bytesRead: Int
                while (audioStream.read(buffer, 0, buffer.size).also { bytesRead = it } != -1) {
                    line.write(buffer, 0, bytesRead)
                }
                line.drain()
                line.close()
            } catch (e: Exception) {
                println("AudioPlayer error (non-fatal): ${e.message}")
            }
        }

    override fun stop() {
        sourceLine?.stop()
        sourceLine?.flush()
        sourceLine?.close()
        sourceLine = null
    }
}
