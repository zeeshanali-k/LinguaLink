package com.devscion.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.audio.AudioCapture
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.model.ConversationMessage
import com.devscion.lingualink.data.repository.MessageRepository
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.pipeline.PipelineState
import com.devscion.lingualink.pipeline.TranslationPipeline
import com.devscion.lingualink.ui.components.MessageBubble
import com.devscion.lingualink.ui.components.StatusBadge
import com.devscion.lingualink.ui.components.WaveformIndicator
import com.devscion.lingualink.ui.theme.LinguaLinkColors
import kotlinx.coroutines.launch

class CallViewModel(
    private val pipeline: TranslationPipeline,
    private val audioCapture: AudioCapture,
    private val messageRepo: MessageRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    val pipelineState = pipeline.state
    val audioLevel = pipeline.audioLevel
    val messages = mutableStateListOf<ConversationMessage>()

    var isListening by mutableStateOf(false)
        private set

    private var sessionId = -1L
    private var sessionStartTime = 0L

    fun initialize(
        sessionId: Long,
        sourceLang: String,
        targetLang: String,
        config: ConfigManager.AppConfig
    ) {
        this.sessionId = sessionId
        sessionStartTime = System.currentTimeMillis()

        pipeline.configure(
            sessionId = sessionId,
            sourceLang = sourceLang,
            targetLang = targetLang,
            deepgramApiKey = config.deepgramApiKey
        )

        viewModelScope.launch {
            messages.addAll(messageRepo.getMessagesBySession(sessionId))
        }

        viewModelScope.launch {
            pipeline.messages.collect { msg ->
                messageRepo.insertMessage(msg)
                messages.add(msg)
            }
        }
    }

    fun toggleListening() {
        if (isListening) {
            pipeline.stopVoiceTranslation(audioCapture)
            isListening = false
        } else {
            if (!audioCapture.isMicAvailable()) return
            pipeline.startVoiceTranslation(audioCapture)
            isListening = true
        }
    }

    fun swapLanguages() = pipeline.swapLanguages()

    fun endSession(onEnded: () -> Unit) {
        if (isListening) {
            pipeline.stopVoiceTranslation(audioCapture)
            isListening = false
        }
        val durationSeconds = (System.currentTimeMillis() - sessionStartTime) / 1000
        viewModelScope.launch {
            sessionRepo.closeSession(sessionId, durationSeconds)
            onEnded()
        }
    }
}

@Composable
fun CallScreen(
    sessionId: Long,
    sourceLang: String,
    targetLang: String,
    vm: CallViewModel,
    onBack: () -> Unit,
    configManager: ConfigManager
) {
    val state by vm.pipelineState.collectAsState()
    val audioLevel by vm.audioLevel.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) {
        val config = configManager.load() ?: ConfigManager.AppConfig()
        vm.initialize(sessionId, sourceLang, targetLang, config)
    }

    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) {
            listState.animateScrollToItem(vm.messages.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(LinguaLinkColors.Background)
    ) {
        // Top bar
        Surface(color = LinguaLinkColors.Surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("←", color = LinguaLinkColors.TextSecondary) }
                Text(
                    "Voice Translation",
                    style = MaterialTheme.typography.titleMedium,
                    color = LinguaLinkColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${sourceLang.uppercase()} → ${targetLang.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LinguaLinkColors.TextSecondary
                )
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = { vm.swapLanguages() }) {
                    Text("⇄ Swap", color = LinguaLinkColors.Primary)
                }
            }
        }

        // Live panels
        Row(modifier = Modifier.fillMaxWidth().height(200.dp).padding(16.dp)) {
            // Mic/transcript panel
            Surface(
                color = LinguaLinkColors.Surface,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("YOU (${sourceLang.uppercase()})", style = MaterialTheme.typography.labelSmall,
                         color = LinguaLinkColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    WaveformIndicator(level = audioLevel, isActive = vm.isListening)
                    Spacer(Modifier.height(8.dp))
                    val partial = (state as? PipelineState.Transcribing)?.partial ?: ""
                    Text(
                        partial.ifBlank { if (vm.isListening) "Listening..." else "Press mic to start" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (partial.isNotBlank()) LinguaLinkColors.TextPrimary else LinguaLinkColors.TextSecondary
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // Translation panel
            Surface(
                color = LinguaLinkColors.Surface,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TRANSLATION (${targetLang.uppercase()})", style = MaterialTheme.typography.labelSmall,
                         color = LinguaLinkColors.TextSecondary)
                    Spacer(Modifier.height(8.dp))
                    StatusBadge(state)
                    Spacer(Modifier.height(8.dp))
                    val translating = (state as? PipelineState.Translating)?.original ?: ""
                    val speaking = (state as? PipelineState.Speaking)?.translated ?: ""
                    val display = when {
                        speaking.isNotBlank() -> speaking
                        translating.isNotBlank() -> "Translating: $translating"
                        vm.messages.isNotEmpty() -> vm.messages.last().translatedText
                        else -> ""
                    }
                    Text(display, style = MaterialTheme.typography.bodyMedium, color = LinguaLinkColors.Primary)
                }
            }
        }

        // Conversation history
        Text(
            "CONVERSATION HISTORY",
            style = MaterialTheme.typography.labelSmall,
            color = LinguaLinkColors.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(Modifier.height(4.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
        ) {
            items(vm.messages) { msg -> MessageBubble(msg) }
        }

        // Controls
        Surface(color = LinguaLinkColors.Surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { vm.toggleListening() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vm.isListening) LinguaLinkColors.Accent else LinguaLinkColors.Primary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (vm.isListening) "⏸ Stop Listening" else "🎙 Start Listening")
                }
                Spacer(Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { vm.endSession(onBack) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("⏹ End Session", color = LinguaLinkColors.Error)
                }
            }
        }
    }
}
