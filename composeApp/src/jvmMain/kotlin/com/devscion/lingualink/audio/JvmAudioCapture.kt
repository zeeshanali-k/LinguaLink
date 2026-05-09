package com.devscion.lingualink.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.sound.sampled.*
import kotlin.math.sqrt

class JvmAudioCapture : AudioCapture {

    private var targetLine: TargetDataLine? = null
    private var captureJob: Job? = null

    private val _audioChunks = Channel<ByteArray>(capacity = Channel.BUFFERED)
    override val audioChunks: Flow<ByteArray> = _audioChunks.receiveAsFlow()

    private val _audioLevel = MutableStateFlow(0f)
    override val audioLevel: StateFlow<Float> = _audioLevel

    override val isCapturing: Boolean get() = targetLine?.isOpen == true

    override fun isMicAvailable(): Boolean {
        val info = DataLine.Info(TargetDataLine::class.java, AUDIO_FORMAT)
        return AudioSystem.isLineSupported(info)
    }

    override fun startCapture(scope: CoroutineScope) {
        println("[Mic] startCapture called")
        val info = DataLine.Info(TargetDataLine::class.java, AUDIO_FORMAT)
        if (!AudioSystem.isLineSupported(info)) {
            throw IllegalStateException("Microphone not supported on this system")
        }

        try {
            targetLine = (AudioSystem.getLine(info) as TargetDataLine).also { line ->
                line.open(AUDIO_FORMAT)
                line.start()
                println("[Mic] line opened: format=${line.format}, isActive=${line.isActive}, isOpen=${line.isOpen}")
            }
        } catch (e: LineUnavailableException) {
            throw IllegalStateException("Microphone unavailable: ${e.message}")
        }

        captureJob = scope.launch(Dispatchers.IO) {
            println("[Mic] capture loop started (coroutineActive=$isActive, lineOpen=${targetLine?.isOpen})")
            val buffer = ByteArray(CHUNK_SIZE_BYTES)
            var totalChunks = 0
            while (isActive && targetLine?.isOpen == true) {
                val bytesRead = targetLine!!.read(buffer, 0, buffer.size)
                if (bytesRead < 0) break
                if (bytesRead > 0) {
                    val chunk = buffer.copyOf(bytesRead)
                    val sendResult = _audioChunks.trySend(chunk)
                    totalChunks++
                    if (totalChunks <= 3 || totalChunks % 50 == 0) {
                        println("[Mic] chunk #$totalChunks: ${bytesRead}B, trySend.isSuccess=${sendResult.isSuccess}")
                    }
                    _audioLevel.value = calculateLevel(chunk)
                }
            }
            println("[Mic] capture loop exited (coroutineActive=$isActive, lineOpen=${targetLine?.isOpen}, totalChunks=$totalChunks)")
        }
    }

    override fun stopCapture() {
        captureJob?.cancel()
        targetLine?.stop()
        targetLine?.close()
        targetLine = null
        _audioLevel.value = 0f
    }

    private fun calculateLevel(bytes: ByteArray): Float {
        var sum = 0.0
        for (i in bytes.indices step 2) {
            if (i + 1 < bytes.size) {
                val sample = (bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF)
                sum += sample * sample
            }
        }
        val rms = sqrt(sum / (bytes.size / 2))
        return (rms / 32768.0).toFloat().coerceIn(0f, 1f)
    }
}
