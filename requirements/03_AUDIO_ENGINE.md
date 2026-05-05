# VoxFlow — Spec 03: Audio Engine
> Read this for TASK 5 only.

---

## TASK 5 — Audio Capture + Playback (JVM Desktop)

All audio work uses `javax.sound.sampled`. No third-party audio library needed.

### Audio Format (use consistently everywhere)

```kotlin
val AUDIO_FORMAT = AudioFormat(
    AudioFormat.Encoding.PCM_SIGNED,
    16000f,       // 16kHz sample rate — required by Deepgram
    16,           // 16-bit depth
    1,            // Mono (1 channel)
    2,            // Frame size bytes (16-bit mono = 2 bytes)
    16000f,       // Frame rate
    false         // Little-endian
)

const val CHUNK_DURATION_MS = 100          // Send 100ms chunks to Deepgram
const val CHUNK_SIZE_BYTES = 3200          // 16000 Hz * 2 bytes * 0.1s = 3200 bytes
const val PLAYBACK_BUFFER_SIZE = 8192
```

---

### audio/AudioCapture.kt

```kotlin
package com.voxflow.audio

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.sound.sampled.*

class AudioCapture {

    private var targetLine: TargetDataLine? = null
    private var captureJob: Job? = null

    // Emits raw PCM byte chunks of CHUNK_SIZE_BYTES each
    private val _audioChunks = Channel<ByteArray>(capacity = Channel.BUFFERED)
    val audioChunks: Flow<ByteArray> = _audioChunks.receiveAsFlow()

    // Emits audio level 0.0–1.0 for waveform indicator
    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    val isCapturing: Boolean get() = targetLine?.isActive == true

    fun startCapture(scope: CoroutineScope) {
        val info = DataLine.Info(TargetDataLine::class.java, AUDIO_FORMAT)
        if (!AudioSystem.isLineSupported(info)) {
            throw IllegalStateException("Microphone not supported on this system")
        }

        targetLine = (AudioSystem.getLine(info) as TargetDataLine).also { line ->
            line.open(AUDIO_FORMAT)
            line.start()
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

    fun stopCapture() {
        captureJob?.cancel()
        targetLine?.stop()
        targetLine?.close()
        targetLine = null
        _audioLevel.value = 0f
    }

    private fun calculateLevel(bytes: ByteArray): Float {
        // RMS amplitude normalized to 0.0–1.0
        var sum = 0.0
        for (i in bytes.indices step 2) {
            if (i + 1 < bytes.size) {
                val sample = (bytes[i + 1].toInt() shl 8) or (bytes[i].toInt() and 0xFF)
                sum += sample * sample
            }
        }
        val rms = Math.sqrt(sum / (bytes.size / 2))
        return (rms / 32768.0).toFloat().coerceIn(0f, 1f)
    }
}
```

---

### audio/AudioPlayer.kt

```kotlin
package com.voxflow.audio

import kotlinx.coroutines.*
import javax.sound.sampled.*

class AudioPlayer {

    private var sourceLine: SourceDataLine? = null

    // Play raw PCM bytes (same AUDIO_FORMAT as capture)
    suspend fun playPcm(pcmBytes: ByteArray) = withContext(Dispatchers.IO) {
        val line = getOrOpenLine()
        line.write(pcmBytes, 0, pcmBytes.size)
    }

    // Play MP3/WAV bytes received from ElevenLabs (as AudioInputStream)
    suspend fun playAudioStream(audioBytes: ByteArray, mimeType: String = "audio/mpeg") =
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
                // Log but don't crash — TTS failure should not break the session
                println("AudioPlayer error: ${e.message}")
            }
        }

    fun stop() {
        sourceLine?.stop()
        sourceLine?.flush()
        sourceLine?.close()
        sourceLine = null
    }

    private fun getOrOpenLine(): SourceDataLine {
        return sourceLine ?: run {
            val info = DataLine.Info(SourceDataLine::class.java, AUDIO_FORMAT)
            (AudioSystem.getLine(info) as SourceDataLine).also { line ->
                line.open(AUDIO_FORMAT, PLAYBACK_BUFFER_SIZE)
                line.start()
                sourceLine = line
            }
        }
    }
}
```

**Acceptance:**
- Write a manual test: start capture for 3 seconds, print chunk count and average audio level, stop.
- Confirm chunk count is ~30 (100ms × 30 = 3 seconds).
- AudioPlayer plays a simple sine wave byte array without errors.
- No crashes on systems without a microphone (catch the `LineUnavailableException` gracefully and expose an `isMicAvailable(): Boolean` function on AudioCapture).
