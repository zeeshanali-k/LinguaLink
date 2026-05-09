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
import com.devscion.lingualink.network.deepgramVoiceFor
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

    @OptIn(ExperimentalCoroutinesApi::class)
    fun configure(
        sessionId: Long,
        sourceLang: String,
        targetLang: String,
        deepgramApiKey: String,
        speaker: Speaker = Speaker.USER_A
    ) {
        // Cancel any in-flight work from a previous session and reset transient state
        // so a fresh session doesn't show stale subtitles/audio level from the last one.
        activeJob?.cancel()
        activeJob = null
        this.sessionId = sessionId
        this.sourceLang = sourceLang
        this.targetLang = targetLang
        this.deepgramApiKey = deepgramApiKey
        this.currentSpeaker = speaker
        conversationHistory.clear()
        _messages.resetReplayCache()
        _state.value = PipelineState.Idle
        _audioLevel.value = 0f
    }

    fun startVoiceTranslation(audioCapture: AudioCapture) {
        println("[ASR] startVoiceTranslation")
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
                println("[Pipeline] transcript (final=${result.isFinal}): \"${result.text}\"")
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
        println("[ASR] stopVoiceTranslation")
        audioCapture.stopCapture()
        activeJob?.cancel()
        _state.value = PipelineState.Idle
        _audioLevel.value = 0f
    }

    fun translateText(inputText: String) {
        println("[ASR] translateText")
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
            val voiceModel = deepgramVoiceFor(currentSpeaker, targetLang)
            println("[Pipeline] TTS voice for $currentSpeaker→$targetLang = $voiceModel")
            val audioBytes = voiceModel?.let { ttsClient.synthesize(translatedText, it) }
            println("[Pipeline] TTS bytes received: ${audioBytes?.size ?: "null"}")
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
