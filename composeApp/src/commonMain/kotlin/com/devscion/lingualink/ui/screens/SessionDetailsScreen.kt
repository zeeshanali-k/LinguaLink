package com.devscion.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.audio.AudioPlayer
import com.devscion.lingualink.data.model.ConversationMessage
import com.devscion.lingualink.data.model.Session
import com.devscion.lingualink.data.model.SessionType
import com.devscion.lingualink.data.model.Speaker
import com.devscion.lingualink.data.model.languageByCode
import com.devscion.lingualink.data.repository.MessageRepository
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.network.TtsClient
import com.devscion.lingualink.network.deepgramVoiceFor
import com.devscion.lingualink.ui.theme.AmbientMeshBackground
import com.devscion.lingualink.ui.theme.Chip
import com.devscion.lingualink.ui.theme.ChipKind
import com.devscion.lingualink.ui.theme.GlassCard
import com.devscion.lingualink.ui.theme.GlowDivider
import com.devscion.lingualink.ui.theme.IconBtn
import com.devscion.lingualink.ui.theme.LL
import com.devscion.lingualink.ui.theme.MonoFamily
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SessionDetailsViewModel(
    private val sessionRepo: SessionRepository,
    private val messageRepo: MessageRepository,
    private val ttsClient: TtsClient,
    private val audioPlayer: AudioPlayer,
) : ViewModel() {

    var session by mutableStateOf<Session?>(null)
        private set
    val messages = mutableStateListOf<ConversationMessage>()

    var playingMessageId by mutableStateOf<Long?>(null)
        private set
    var loading by mutableStateOf(true)
        private set

    private var currentPlaybackJob: Job? = null

    fun load(sessionId: Long) {
        loading = true
        viewModelScope.launch {
            session = sessionRepo.getSessionById(sessionId)
            messages.clear()
            messages.addAll(messageRepo.getMessagesBySession(sessionId))
            loading = false
        }
    }

    fun toggleReplay(message: ConversationMessage) {
        // Tapping the currently-playing message stops it.
        if (playingMessageId == message.id) {
            currentPlaybackJob?.cancel()
            audioPlayer.stop()
            playingMessageId = null
            return
        }

        // Otherwise, stop any in-flight playback and start fresh.
        currentPlaybackJob?.cancel()
        audioPlayer.stop()

        currentPlaybackJob = viewModelScope.launch {
            playingMessageId = message.id
            try {
                val voice = deepgramVoiceFor(message.speaker, message.targetLanguage)
                val bytes = ttsClient.synthesize(message.translatedText, voice) ?: return@launch
                audioPlayer.playAudioStream(bytes)
            } finally {
                if (playingMessageId == message.id) playingMessageId = null
            }
        }
    }

    override fun onCleared() {
        currentPlaybackJob?.cancel()
        audioPlayer.stop()
    }
}

@Composable
fun SessionDetailsScreen(
    sessionId: Long,
    vm: SessionDetailsViewModel,
    onBack: () -> Unit,
    onContinue: (Session) -> Unit,
) {
    LaunchedEffect(sessionId) { vm.load(sessionId) }

    val t = LL.tokens
    val session = vm.session

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp
            val veryCompact = maxWidth < 460.dp

            Column(modifier = Modifier.fillMaxSize()) {
                DetailsTopbar(
                    onBack = onBack,
                    onContinue = if (session != null) ({ onContinue(session) }) else null,
                    compact = compact,
                )

                if (vm.loading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = t.cyan)
                    }
                } else if (session == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Session not found", color = t.text2, fontSize = 14.sp)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = if (veryCompact) 14.dp else if (compact) 18.dp else 28.dp,
                                vertical = 16.dp,
                            ),
                    ) {
                        DetailsHeader(session = session, messageCount = vm.messages.size, compact = compact)
                        Spacer(Modifier.height(16.dp))

                        if (vm.messages.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No transcript captured for this session.",
                                    color = t.text3,
                                    fontFamily = MonoFamily,
                                    fontSize = 11.sp,
                                    letterSpacing = 0.6.sp,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                items(vm.messages, key = { it.id }) { msg ->
                                    DetailMessageCard(
                                        message = msg,
                                        playing = vm.playingMessageId == msg.id,
                                        onTogglePlay = { vm.toggleReplay(msg) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Topbar ─────────────────────────────

@Composable
private fun DetailsTopbar(
    onBack: () -> Unit,
    onContinue: (() -> Unit)?,
    compact: Boolean,
) {
    val t = LL.tokens
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(t.cyan.copy(alpha = 0.025f), Color.Transparent))
                )
                .padding(horizontal = if (compact) 14.dp else 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconBtn(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack, contentDescription = "Back")
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "TRANSCRIPT",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                )
                Text("Session details", color = t.text0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            if (onContinue != null) {
                Spacer(Modifier.width(8.dp))
                ContinueButton(onClick = onContinue)
            }
        }
        GlowDivider(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ContinueButton(onClick: () -> Unit) {
    val t = LL.tokens
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(t.brandGradientHorizontal)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = t.bg0, modifier = Modifier.size(14.dp))
        Text("Continue", color = t.bg0, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ───────────────────────────── Header ─────────────────────────────

@Composable
private fun DetailsHeader(session: Session, messageCount: Int, compact: Boolean) {
    val t = LL.tokens
    val src = languageByCode(session.sourceLanguage)
    val tgt = languageByCode(session.targetLanguage)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (session.sessionType == SessionType.CALL) "Voice call" else "Text chat",
                color = t.text0,
                fontSize = if (compact) 20.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
            )
            Spacer(Modifier.weight(1f))
            Chip(text = src.code.uppercase(), kind = ChipKind.Cyan)
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = t.text3, modifier = Modifier.size(12.dp))
            Chip(text = tgt.code.uppercase(), kind = ChipKind.Violet)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MetaText(label = "FROM", value = src.name)
            MetaText(label = "TO", value = tgt.name)
            MetaText(
                label = "DURATION",
                value = session.durationSeconds?.let { formatDurationLong(it) } ?: "—",
            )
            MetaText(label = "MESSAGES", value = messageCount.toString())
        }
    }
}

@Composable
private fun MetaText(label: String, value: String) {
    val t = LL.tokens
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            color = t.text3,
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
        )
        Text(value, color = t.text1, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ───────────────────────────── Message card ─────────────────────────────

@Composable
private fun DetailMessageCard(
    message: ConversationMessage,
    playing: Boolean,
    onTogglePlay: () -> Unit,
) {
    val t = LL.tokens
    val accent = if (message.speaker == Speaker.USER_A) t.cyan else t.violet
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Original (transcript)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LangBadge(code = message.sourceLanguage.uppercase(), accent = t.cyan)
                    Text(
                        "TRANSCRIPT",
                        color = t.text3,
                        fontFamily = MonoFamily,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                    )
                }
                Text(
                    message.originalText,
                    color = t.text0,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                // Translation
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LangBadge(code = message.targetLanguage.uppercase(), accent = t.violet)
                    Text(
                        "TRANSLATION",
                        color = t.text3,
                        fontFamily = MonoFamily,
                        fontSize = 9.sp,
                        letterSpacing = 1.2.sp,
                    )
                }
                Text(
                    message.translatedText,
                    color = t.text1,
                    fontSize = 12.5.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 17.sp,
                )
            }
            ReplayButton(playing = playing, accent = accent, onClick = onTogglePlay)
        }
    }
}

@Composable
private fun ReplayButton(playing: Boolean, accent: Color, onClick: () -> Unit) {
    val t = LL.tokens
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (playing) accent.copy(alpha = 0.20f) else Color.White.copy(alpha = if (t.isDark) 0.05f else 0.0f))
            .border(1.dp, if (playing) accent.copy(alpha = 0.50f) else t.border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (playing) "Stop" else "Replay",
            tint = if (playing) accent else t.text1,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun LangBadge(code: String, accent: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.14f))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            code,
            color = accent,
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatDurationLong(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return when {
        m == 0L -> "${s}s"
        s == 0L -> "${m}m"
        else -> "${m}m ${s}s"
    }
}
