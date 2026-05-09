package com.devscion.lingualink.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.model.ConversationMessage
import com.devscion.lingualink.data.model.Speaker
import com.devscion.lingualink.data.model.languageByCode
import com.devscion.lingualink.data.repository.MessageRepository
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.pipeline.PipelineState
import com.devscion.lingualink.pipeline.TranslationPipeline
import com.devscion.lingualink.ui.theme.AmbientMeshBackground
import com.devscion.lingualink.ui.theme.Chip
import com.devscion.lingualink.ui.theme.ChipKind
import com.devscion.lingualink.ui.theme.GlassCard
import com.devscion.lingualink.ui.theme.GlowDivider
import com.devscion.lingualink.ui.theme.GradientButton
import com.devscion.lingualink.ui.theme.IconBtn
import com.devscion.lingualink.ui.theme.LL
import com.devscion.lingualink.ui.theme.MonoFamily
import com.devscion.lingualink.ui.theme.OrbVisualizer
import com.devscion.lingualink.ui.theme.PulseDot
import com.devscion.lingualink.ui.theme.SharedKeys
import com.devscion.lingualink.ui.theme.glass
import com.devscion.lingualink.ui.theme.sharedAcrossScreens
import com.devscion.lingualink.ui.theme.sharedBoundsAcrossScreens
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

    var sourceLang by mutableStateOf("en")
        private set
    var targetLang by mutableStateOf("ur")
        private set

    private var sessionId = -1L
    private var sessionStartTime = 0L
    private var deepgramApiKey: String = ""
    private var initialized = false

    fun initialize(sessionId: Long, sourceLang: String, targetLang: String, config: ConfigManager.AppConfig) {
        if (initialized && this.sessionId == sessionId) return
        initialized = true
        this.sessionId = sessionId
        this.sourceLang = sourceLang
        this.targetLang = targetLang
        this.deepgramApiKey = config.deepgramApiKey
        sessionStartTime = System.currentTimeMillis()
        messages.clear()
        pipeline.configure(sessionId, sourceLang, targetLang, config.deepgramApiKey)

        viewModelScope.launch { messages.addAll(messageRepo.getMessagesBySession(sessionId)) }
        viewModelScope.launch {
            pipeline.messages.collect { msg ->
                if (msg.sessionId != sessionId) return@collect
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

    fun swapLanguages() {
        val tmp = sourceLang
        sourceLang = targetLang
        targetLang = tmp
        pipeline.configure(sessionId, sourceLang, targetLang, deepgramApiKey)
    }

    fun endSession(onEnded: () -> Unit) {
        val duration = (System.currentTimeMillis() - sessionStartTime) / 1000
        viewModelScope.launch { sessionRepo.closeSession(sessionId, duration); onEnded() }
    }
}

// ───────────────────────────── Screen ─────────────────────────────

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

    // Read languages from VM so swaps update the UI immediately.
    val srcLang = languageByCode(vm.sourceLang)
    val tgtLang = languageByCode(vm.targetLang)

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp
            val veryCompact = maxWidth < 460.dp

            Column(modifier = Modifier.fillMaxSize()) {
                ChatTopbar(
                    sourceCode = srcLang.code.uppercase(),
                    targetCode = tgtLang.code.uppercase(),
                    onBack = onBack,
                    onSwap = { vm.swapLanguages() },
                    onEnd = { vm.endSession(onBack) },
                    compact = compact,
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (vm.messages.isEmpty()) {
                        ChatEmptyState(
                            sourceLang = srcLang.name,
                            targetLang = tgtLang.name,
                            compact = compact,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    horizontal = if (veryCompact) 12.dp else if (compact) 16.dp else 28.dp,
                                    vertical = 16.dp,
                                ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(vm.messages) { msg ->
                                MessageBubble(
                                    message = msg,
                                    sourceCode = msg.sourceLanguage.uppercase(),
                                    targetCode = msg.targetLanguage.uppercase(),
                                    compact = compact,
                                )
                            }
                            if (vm.isTranslating) {
                                item { TranslatingIndicator() }
                            }
                        }
                    }
                }

                StatusStrip(state = state, isTranslating = vm.isTranslating)

                ChatInput(
                    text = vm.inputText,
                    onTextChange = { vm.inputText = it },
                    onSend = { vm.sendMessage() },
                    canSend = vm.inputText.isNotBlank() && !vm.isTranslating,
                    sourceCode = srcLang.code.uppercase(),
                    sourceName = srcLang.name,
                    compact = compact,
                )
            }
        }
    }
}

// ───────────────────────────── Topbar ─────────────────────────────

@Composable
private fun ChatTopbar(
    sourceCode: String,
    targetCode: String,
    onBack: () -> Unit,
    onSwap: () -> Unit,
    onEnd: () -> Unit,
    compact: Boolean,
) {
    val t = LL.tokens
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(t.violet.copy(alpha = 0.025f), Color.Transparent)
                    )
                )
                .padding(horizontal = if (compact) 14.dp else 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconBtn(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack, contentDescription = "Back")

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "MESSAGES",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                )
                Text(
                    "Conversation",
                    color = t.text0,
                    fontSize = if (compact) 16.sp else 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Chip(
                text = "$sourceCode → $targetCode",
                kind = ChipKind.Violet,
                leading = Icons.Default.Language,
                modifier = Modifier.sharedBoundsAcrossScreens(SharedKeys.LANG_PILL),
            )
            if (!compact) {
                IconBtn(icon = Icons.Default.SwapHoriz, onClick = onSwap, contentDescription = "Swap")
            }
            IconBtn(icon = Icons.Default.CallEnd, onClick = onEnd, contentDescription = "End session")
        }
        GlowDivider(modifier = Modifier.fillMaxWidth())
    }
}

// ───────────────────────────── Empty state ─────────────────────────────

@Composable
private fun ChatEmptyState(
    sourceLang: String,
    targetLang: String,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val t = LL.tokens
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        OrbVisualizer(
            coreSize = if (compact) 70.dp else 90.dp,
            speaking = false,
            modifier = Modifier.sharedAcrossScreens(SharedKeys.HERO_ORB),
        )

        Text(
            "No active conversation",
            color = t.text0,
            fontSize = if (compact) 18.sp else 20.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.2).sp,
        )

        Text(
            "Type a message in $sourceLang and LinguaLink will translate every line into $targetLang in real time.",
            color = t.text2,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            modifier = Modifier.widthIn(max = 380.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Chip(text = "TAP TO TOGGLE", leading = Icons.Default.AutoAwesome)
            Chip(text = "INSTANT TRANSLATION", kind = ChipKind.Cyan, leading = Icons.Default.Memory)
        }
    }
}

// ───────────────────────────── Message bubble ─────────────────────────────

@Composable
private fun MessageBubble(
    message: ConversationMessage,
    sourceCode: String,
    targetCode: String,
    compact: Boolean,
) {
    val t = LL.tokens
    val outgoing = message.speaker == Speaker.USER_A
    val accent = if (outgoing) t.violet else t.cyan
    var showOriginalAsPrimary by remember(message.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        val bubbleMaxWidth = if (compact) 320.dp else 520.dp
        Column(
            modifier = Modifier
                .widthIn(max = bubbleMaxWidth)
                .clip(RoundedCornerShape(if (outgoing) 16.dp else 16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            accent.copy(alpha = if (t.isDark) 0.10f else 0.10f),
                            accent.copy(alpha = if (t.isDark) 0.04f else 0.04f),
                        )
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
                .clickable { showOriginalAsPrimary = !showOriginalAsPrimary }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Header: language chip + tap hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LanguagePill(
                    code = if (showOriginalAsPrimary) sourceCode else targetCode,
                    accent = accent,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (showOriginalAsPrimary) "ORIGINAL" else "TRANSLATED",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 9.5.sp,
                    letterSpacing = 1.2.sp,
                )
            }
            // Primary text (large)
            val primary = if (showOriginalAsPrimary) message.originalText else message.translatedText
            Text(
                primary,
                color = t.text0,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Medium,
            )
            // Secondary line
            val secondary = if (showOriginalAsPrimary) message.translatedText else message.originalText
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "→",
                    color = accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    secondary,
                    color = t.text2,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 17.sp,
                    modifier = Modifier.weight(1f),
                )
            }
            // Footer: confidence + sub language indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (showOriginalAsPrimary) "tap to see translation" else "tap to see original",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.0.sp,
                )
                Spacer(Modifier.weight(1f))
                if (message.confidence != null) {
                    Text(
                        "conf ${(message.confidence * 100).toInt()}%",
                        color = t.text3,
                        fontFamily = MonoFamily,
                        fontSize = 9.sp,
                        letterSpacing = 1.0.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguagePill(code: String, accent: Color) {
    val t = LL.tokens
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            code,
            color = accent,
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 1.4.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TranslatingIndicator() {
    val t = LL.tokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(t.violet.copy(alpha = 0.10f))
                .border(1.dp, t.violet.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            PulseDot(color = t.violet, size = 6.dp)
            Text(
                "AI translating…",
                color = t.violet,
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

// ───────────────────────────── Status strip ─────────────────────────────

@Composable
private fun StatusStrip(state: PipelineState, isTranslating: Boolean) {
    val t = LL.tokens
    val (label, color) = when {
        isTranslating -> "Translating" to t.violet
        state is PipelineState.Error -> "Error: ${state.message}" to t.red
        else -> "Ready" to t.green
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PulseDot(color = color, size = 6.dp)
        Text(
            label,
            color = color,
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
        )
    }
}

// ───────────────────────────── Input ─────────────────────────────

@Composable
private fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,
    sourceCode: String,
    sourceName: String,
    compact: Boolean,
) {
    val t = LL.tokens
    var focused by remember { mutableStateOf(false) }
    val borderColor = if (focused) t.cyan.copy(alpha = 0.45f) else t.border
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 12.dp else 22.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(t.surface)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Source language pill (left)
            Column(
                modifier = Modifier.padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(t.cyan.copy(alpha = 0.12f))
                        .border(1.dp, t.cyan.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        sourceCode,
                        color = t.cyan,
                        fontFamily = MonoFamily,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
                if (!compact) {
                    Text(
                        sourceName,
                        color = t.text3,
                        fontFamily = MonoFamily,
                        fontSize = 9.sp,
                        letterSpacing = 0.8.sp,
                    )
                }
            }

            // Text field — borderless, transparent so we can use the surrounding glass surface
            BasicChatTextField(
                value = text,
                onValueChange = onTextChange,
                onSubmit = onSend,
                focused = focused,
                onFocusChanged = { focused = it },
                modifier = Modifier.weight(1f),
            )

            // Send
            SendButton(enabled = canSend, onClick = onSend)
        }
        AnimatedVisibility(visible = text.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
            Row(
                modifier = Modifier.padding(top = 6.dp, start = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Press Enter to send · Shift+Enter for newline",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 9.sp,
                    letterSpacing = 1.0.sp,
                )
            }
        }
    }
}

@Composable
private fun BasicChatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    focused: Boolean,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LL.tokens
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .onKeyEvent { event ->
                if (event.key == Key.Enter && event.type == KeyEventType.KeyDown && !event.isShiftPressed) {
                    onSubmit(); true
                } else false
            },
        placeholder = {
            Text(
                "Type your message…",
                color = t.text3,
                fontSize = 14.sp,
            )
        },
        textStyle = LocalTextStyle.current.copy(color = t.text0, fontSize = 14.sp),
        maxLines = 4,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            cursorColor = t.cyan,
            focusedTextColor = t.text0,
            unfocusedTextColor = t.text0,
        ),
    )
}

@Composable
private fun SendButton(enabled: Boolean, onClick: () -> Unit) {
    val t = LL.tokens
    val bg = if (enabled) t.brandGradient else Brush.linearGradient(listOf(t.bg3, t.bg3))
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send",
            tint = if (enabled) t.bg0 else t.text3,
            modifier = Modifier.size(16.dp),
        )
    }
}
