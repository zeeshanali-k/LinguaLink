package com.devscion.lingualink.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sound.sampled.*

class JvmAudioPlayer : AudioPlayer {

    private var sourceLine: SourceDataLine? = null

    override suspend fun playAudioStream(audioBytes: ByteArray, mimeType: String) =
        withContext(Dispatchers.IO) {
            println("[Player] play request: ${audioBytes.size}B")
            try {
                val inputStream = audioBytes.inputStream()
                val audioStream = AudioSystem.getAudioInputStream(inputStream)
                val format = audioStream.format
                println("[Player] decoded format: $format")
                val info = DataLine.Info(SourceDataLine::class.java, format)
                val line = AudioSystem.getLine(info) as SourceDataLine
                line.open(format)
                line.start()
                val buffer = ByteArray(PLAYBACK_BUFFER_SIZE)
                var bytesRead: Int
                var totalPlayed = 0
                while (audioStream.read(buffer, 0, buffer.size).also { bytesRead = it } != -1) {
                    line.write(buffer, 0, bytesRead)
                    totalPlayed += bytesRead
                }
                line.drain()
                line.close()
                println("[Player] playback done: $totalPlayed bytes")
            } catch (e: Exception) {
                println("[Player] error: ${e::class.simpleName}: ${e.message}")
            }
        }

    override fun stop() {
        sourceLine?.stop()
        sourceLine?.flush()
        sourceLine?.close()
        sourceLine = null
    }
}
