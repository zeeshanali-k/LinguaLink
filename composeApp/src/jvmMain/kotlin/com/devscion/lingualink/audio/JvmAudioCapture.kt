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

    override val isCapturing: Boolean get() = targetLine?.isActive == true

    override fun isMicAvailable(): Boolean {
        val info = DataLine.Info(TargetDataLine::class.java, AUDIO_FORMAT)
        return AudioSystem.isLineSupported(info)
    }

    override fun startCapture(scope: CoroutineScope) {
        val info = DataLine.Info(TargetDataLine::class.java, AUDIO_FORMAT)
        if (!AudioSystem.isLineSupported(info)) {
            throw IllegalStateException("Microphone not supported on this system")
        }

        try {
            targetLine = (AudioSystem.getLine(info) as TargetDataLine).also { line ->
                line.open(AUDIO_FORMAT)
                line.start()
            }
        } catch (e: LineUnavailableException) {
            throw IllegalStateException("Microphone unavailable: ${e.message}")
        }

        captureJob = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(CHUNK_SIZE_BYTES)
            while (isActive && targetLine?.isActive == true) {
                val bytesRead = targetLine!!.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    val chunk = buffer.copyOf(bytesRead)
                    _audioChunks.trySend(chunk)
                    _audioLevel.value = calculateLevel(chunk)
                }
            }
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
