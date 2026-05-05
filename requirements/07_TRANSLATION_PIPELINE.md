# VoxFlow — Spec 07: Translation Pipeline
> Read this for TASK 9 only.

---

## TASK 9 — TranslationPipeline (Orchestrator)

This is the core of VoxFlow. It wires AudioCapture → Deepgram → LLM → TTS → AudioPlayer into a single reactive pipeline driven by Kotlin coroutines and StateFlow.

The pipeline runs the same logic for both Call mode and Chat mode — the only difference is the input source (microphone vs keyboard).

---

### Pipeline States

```kotlin
sealed class PipelineState {
    object Idle : PipelineState()
    object Listening : PipelineState()          // Mic active, waiting for speech
    data class Transcribing(val partial: String) : PipelineState()  // Live partial transcript
    data class Translating(val original: String) : PipelineState()  // Calling LLM
    data class Speaking(val translated: String) : PipelineState()   // TTS playing
    data class Error(val message: String) : PipelineState()
}
```

---

### pipeline/TranslationPipeline.kt

```kotlin
package com.voxflow.pipeline

import com.voxflow.audio.AudioCapture
import com.voxflow.audio.AudioPlayer
import com.voxflow.data.model.ConversationMessage
import com.voxflow.data.model.Speaker
import com.voxflow.network.ChatMessage
import com.voxflow.network.DeepgramClient
import com.voxflow.network.LlmClient
import com.voxflow.network.TtsClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TranslationPipeline(
    private val deepgramClient: DeepgramClient,
    private val llmClient: LlmClient,
    private val ttsClient: TtsClient,
    private val audioPlayer: AudioPlayer
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // UI observes these
    private val _state = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val state: StateFlow<PipelineState> = _state

    private val _messages = MutableSharedFlow<ConversationMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ConversationMessage> = _messages

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    // LLM conversation history for context-aware translation
    private val conversationHistory = mutableListOf<ChatMessage>()

    private var activeJob: Job? = null
    private var sessionId: Long = -1
    private var sourceLang: String = "en"
    private var targetLang: String = "ur"
    private var deepgramApiKey: String = ""
    private var currentSpeaker: Speaker = Speaker.USER_A

    // --- Configuration ---

    fun configure(
        sessionId: Long,
        sourceLang: String,
        targetLang: String,
        deepgramApiKey: String,
        speaker: Speaker = Speaker.USER_A
    ) {
        this.sessionId = sessionId
        this.sourceLang = sourceLang
        this.targetLang = targetLang
        this.deepgramApiKey = deepgramApiKey
        this.currentSpeaker = speaker
        conversationHistory.clear()
    }

    // --- CALL MODE: Microphone → Deepgram → LLM → TTS ---

    fun startVoiceTranslation(audioCapture: AudioCapture) {
        activeJob?.cancel()
        activeJob = scope.launch {
            _state.value = PipelineState.Listening

            // Observe audio levels for waveform UI
            launch {
                audioCapture.audioLevel.collect { level ->
                    _audioLevel.value = level
                }
            }

            // Start mic capture
            audioCapture.startCapture(this)

            // Get the Deepgram language code for source language
            val deepgramLang = getDeepgramLanguageCode(sourceLang)

            // Stream transcription results
            deepgramClient.streamTranscription(
                audioChunks = audioCapture.audioChunks,
                apiKey = deepgramApiKey,
                languageCode = deepgramLang
            ).collect { result ->
                if (!result.isFinal) {
                    // Show live partial transcript
                    _state.value = PipelineState.Transcribing(result.text)
                } else {
                    // Final transcript — translate and speak
                    processTranslation(
                        originalText = result.text,
                        confidence = result.confidence,
                        isVoice = true
                    )
                }
            }

            audioCapture.stopCapture()
            _state.value = PipelineState.Idle
        }
    }

    fun stopVoiceTranslation(audioCapture: AudioCapture) {
        audioCapture.stopCapture()
        activeJob?.cancel()
        _state.value = PipelineState.Idle
        _audioLevel.value = 0f
    }

    // --- CHAT MODE: Typed text → LLM → TTS (optional) ---

    fun translateText(inputText: String) {
        if (inputText.isBlank()) return
        activeJob?.cancel()
        activeJob = scope.launch {
            processTranslation(originalText = inputText, confidence = null, isVoice = false)
        }
    }

    // --- Core translation logic (shared by both modes) ---

    private suspend fun processTranslation(
        originalText: String,
        confidence: Float?,
        isVoice: Boolean
    ) {
        try {
            // Step 1: Translate
            _state.value = PipelineState.Translating(originalText)

            val translatedText = llmClient.translate(
                text = originalText,
                sourceLang = sourceLang,
                targetLang = targetLang,
                conversationHistory = conversationHistory.toList()
            )

            // Step 2: Update conversation history for context
            conversationHistory.add(ChatMessage("user", originalText))
            conversationHistory.add(ChatMessage("assistant", translatedText))

            // Step 3: Emit message to UI and DB
            val message = ConversationMessage(
                sessionId = sessionId,
                speaker = currentSpeaker,
                originalText = originalText,
                translatedText = translatedText,
                sourceLanguage = sourceLang,
                targetLanguage = targetLang,
                confidence = confidence
            )
            _messages.emit(message)

            // Step 4: TTS (non-blocking — don't let TTS failure block the pipeline)
            _state.value = PipelineState.Speaking(translatedText)
            val voiceId = if (currentSpeaker == Speaker.USER_A)
                TtsClient.VOICE_USER_A else TtsClient.VOICE_USER_B

            val audioBytes = ttsClient.synthesize(translatedText, voiceId)
            audioBytes?.let { audioPlayer.playAudioStream(it) }

            _state.value = if (isVoice) PipelineState.Listening else PipelineState.Idle

        } catch (e: CancellationException) {
            throw e  // Never swallow cancellation
        } catch (e: Exception) {
            _state.value = PipelineState.Error("Translation failed: ${e.message}")
        }
    }

    // --- Swap languages (reverses translation direction) ---

    fun swapLanguages() {
        val temp = sourceLang
        sourceLang = targetLang
        targetLang = temp
        conversationHistory.clear()  // Clear context — direction changed
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    fun dispose() {
        scope.cancel()
        audioPlayer.stop()
    }

    private fun getDeepgramLanguageCode(bcp47: String): String = when (bcp47) {
        "en" -> "en-US"
        "zh" -> "zh-CN"
        "pt" -> "pt-BR"
        else -> bcp47  // Most codes match directly
    }
}
```

**Acceptance:**
- Unit test `swapLanguages()` — verify source and target are swapped.
- Unit test `processTranslation()` with mocked LlmClient (returns "مرحبا") and mocked TtsClient (returns null) — verify message is emitted with correct fields.
- `PipelineState` transitions in correct order: Idle → Listening → Transcribing → Translating → Speaking → Listening.
- TTS failure (null return) does NOT change state to Error.
