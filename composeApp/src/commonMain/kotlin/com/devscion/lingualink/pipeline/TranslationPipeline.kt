package com.devscion.lingualink.pipeline

import com.devscion.lingualink.audio.AudioCapture
import com.devscion.lingualink.audio.AudioPlayer
import com.devscion.lingualink.data.model.ConversationMessage
import com.devscion.lingualink.data.model.Speaker
import com.devscion.lingualink.data.model.deepgramCodeFor
import com.devscion.lingualink.network.AsrClient
import com.devscion.lingualink.network.ChatMessage
import com.devscion.lingualink.network.LlmClient
import com.devscion.lingualink.network.TtsClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class TranslationPipeline(
    private val asrClient: AsrClient,
    private val llmClient: LlmClient,
    private val ttsClient: TtsClient,
    private val audioPlayer: AudioPlayer
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _state = MutableStateFlow<PipelineState>(PipelineState.Idle)
    val state: StateFlow<PipelineState> = _state

    private val _messages = MutableSharedFlow<ConversationMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<ConversationMessage> = _messages

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel

    private val conversationHistory = mutableListOf<ChatMessage>()

    private var activeJob: Job? = null
    private var sessionId: Long = -1
    private var sourceLang: String = "en"
    private var targetLang: String = "ur"
    private var deepgramApiKey: String = ""
    private var currentSpeaker: Speaker = Speaker.USER_A

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

    fun startVoiceTranslation(audioCapture: AudioCapture) {
        activeJob?.cancel()
        activeJob = scope.launch {
            _state.value = PipelineState.Listening

            launch {
                audioCapture.audioLevel.collect { level -> _audioLevel.value = level }
            }

            audioCapture.startCapture(this)

            asrClient.streamTranscription(
                audioChunks = audioCapture.audioChunks,
                apiKey = deepgramApiKey,
                languageCode = deepgramCodeFor(sourceLang)
            ).collect { result ->
                if (!result.isFinal) {
                    _state.value = PipelineState.Transcribing(result.text)
                } else {
                    processTranslation(result.text, result.confidence, isVoice = true)
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

    fun translateText(inputText: String) {
        if (inputText.isBlank()) return
        activeJob?.cancel()
        activeJob = scope.launch {
            processTranslation(inputText, confidence = null, isVoice = false)
        }
    }

    private suspend fun processTranslation(originalText: String, confidence: Float?, isVoice: Boolean) {
        try {
            _state.value = PipelineState.Translating(originalText)

            val translatedText = llmClient.translate(
                text = originalText,
                sourceLang = sourceLang,
                targetLang = targetLang,
                conversationHistory = conversationHistory.toList()
            )

            conversationHistory.add(ChatMessage("user", originalText))
            conversationHistory.add(ChatMessage("assistant", translatedText))

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

            _state.value = PipelineState.Speaking(translatedText)
            val voiceId = if (currentSpeaker == Speaker.USER_A) TtsClient.VOICE_USER_A else TtsClient.VOICE_USER_B
            val audioBytes = ttsClient.synthesize(translatedText, voiceId)
            audioBytes?.let { audioPlayer.playAudioStream(it) }

            _state.value = if (isVoice) PipelineState.Listening else PipelineState.Idle

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.value = PipelineState.Error("Translation failed: ${e.message}")
        }
    }

    fun swapLanguages() {
        val temp = sourceLang
        sourceLang = targetLang
        targetLang = temp
        conversationHistory.clear()
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    fun dispose() {
        scope.cancel()
        audioPlayer.stop()
    }
}
