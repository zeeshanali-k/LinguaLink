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
import androidx.compose.ui.input.key.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.model.ConversationMessage
import com.devscion.lingualink.data.repository.MessageRepository
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.pipeline.TranslationPipeline
import com.devscion.lingualink.ui.components.MessageBubble
import com.devscion.lingualink.ui.components.StatusBadge
import com.devscion.lingualink.ui.theme.LinguaLinkColors
import kotlinx.coroutines.launch

class ChatViewModel(
    private val pipeline: TranslationPipeline,
    private val messageRepo: MessageRepository,
    private val sessionRepo: SessionRepository
) : ViewModel() {

    val pipelineState = pipeline.state
    val messages = mutableStateListOf<ConversationMessage>()
    var inputText by mutableStateOf("")
    var isTranslating by mutableStateOf(false)

    private var sessionId = -1L
    private var sessionStartTime = 0L

    fun initialize(sessionId: Long, sourceLang: String, targetLang: String, config: ConfigManager.AppConfig) {
        this.sessionId = sessionId
        sessionStartTime = System.currentTimeMillis()
        pipeline.configure(sessionId, sourceLang, targetLang, config.deepgramApiKey)

        viewModelScope.launch { messages.addAll(messageRepo.getMessagesBySession(sessionId)) }
        viewModelScope.launch {
            pipeline.messages.collect { msg ->
                messageRepo.insertMessage(msg)
                messages.add(msg)
                isTranslating = false
            }
        }
    }

    fun sendMessage() {
        if (inputText.isBlank()) return
        isTranslating = true
        pipeline.translateText(inputText)
        inputText = ""
    }

    fun swapLanguages() = pipeline.swapLanguages()

    fun endSession(onEnded: () -> Unit) {
        val duration = (System.currentTimeMillis() - sessionStartTime) / 1000
        viewModelScope.launch { sessionRepo.closeSession(sessionId, duration); onEnded() }
    }
}

@Composable
fun ChatScreen(
    sessionId: Long,
    sourceLang: String,
    targetLang: String,
    vm: ChatViewModel,
    onBack: () -> Unit,
    configManager: ConfigManager
) {
    val state by vm.pipelineState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(sessionId) {
        val config = configManager.load() ?: ConfigManager.AppConfig()
        vm.initialize(sessionId, sourceLang, targetLang, config)
    }

    LaunchedEffect(vm.messages.size) {
        if (vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.size - 1)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(LinguaLinkColors.Background)
    ) {
        Surface(color = LinguaLinkColors.Surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("←", color = LinguaLinkColors.TextSecondary) }
                Text(
                    "Chat Translation",
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

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            items(vm.messages) { msg -> MessageBubble(msg) }
        }

        // Status bar
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Translating: ${sourceLang.uppercase()} → ${targetLang.uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = LinguaLinkColors.TextSecondary
            )
            Spacer(Modifier.width(16.dp))
            StatusBadge(state)
        }

        Spacer(Modifier.height(8.dp))

        // Input row
        Surface(color = LinguaLinkColors.Surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = vm.inputText,
                        onValueChange = { vm.inputText = it },
                        modifier = Modifier.weight(1f).onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                                vm.sendMessage(); true
                            } else false
                        },
                        placeholder = { Text("Type your message here...", color = LinguaLinkColors.TextSecondary) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LinguaLinkColors.Primary,
                            unfocusedBorderColor = LinguaLinkColors.Divider,
                            focusedTextColor = LinguaLinkColors.TextPrimary,
                            unfocusedTextColor = LinguaLinkColors.TextPrimary,
                            cursorColor = LinguaLinkColors.Primary
                        )
                    )

                    Button(
                        onClick = { vm.sendMessage() },
                        enabled = vm.inputText.isNotBlank() && !vm.isTranslating
                    ) {
                        if (vm.isTranslating) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Send")
                        }
                    }
                }

                OutlinedButton(
                    onClick = { vm.endSession(onBack) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("⏹ End Session", color = LinguaLinkColors.Error)
                }
            }
        }
    }
}
