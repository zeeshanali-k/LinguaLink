package com.devscion.lingualink.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.audio.AudioCapture
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.model.ConversationMessage
import com.devscion.lingualink.data.model.languageByCode
import com.devscion.lingualink.data.repository.MessageRepository
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.pipeline.PipelineState
import com.devscion.lingualink.pipeline.TranslationPipeline
import com.devscion.lingualink.ui.theme.AmbientMeshBackground
import com.devscion.lingualink.ui.theme.AvatarBrushes
import com.devscion.lingualink.ui.theme.BrandMark
import com.devscion.lingualink.ui.theme.Chip
import com.devscion.lingualink.ui.theme.ChipKind
import com.devscion.lingualink.ui.theme.GlassCard
import com.devscion.lingualink.ui.theme.GlowDivider
import com.devscion.lingualink.ui.theme.GradientAvatar
import com.devscion.lingualink.ui.theme.Hue
import com.devscion.lingualink.ui.theme.IconBtn
import com.devscion.lingualink.ui.theme.LL
import com.devscion.lingualink.ui.theme.LiveIndicator
import com.devscion.lingualink.ui.theme.LiveWaveform
import com.devscion.lingualink.ui.theme.MiniWave
import com.devscion.lingualink.ui.theme.ModelPill
import com.devscion.lingualink.ui.theme.MonoFamily
import com.devscion.lingualink.ui.theme.OrbVisualizer
import com.devscion.lingualink.ui.theme.PulseDot
import com.devscion.lingualink.ui.theme.SharedKeys
import com.devscion.lingualink.ui.theme.glass
import com.devscion.lingualink.ui.theme.sharedAcrossScreens
import com.devscion.lingualink.ui.theme.sharedBoundsAcrossScreens
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

// ───────────────────────────── Screen ─────────────────────────────

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

    val config = remember { configManager.load() ?: ConfigManager.AppConfig() }
    val modelDisplayName = remember(config.llmModel) { config.llmModel.substringAfterLast('/') }

    LaunchedEffect(sessionId) {
        vm.initialize(sessionId, sourceLang, targetLang, config)
    }

    // Live ticking timer (recomposes each second)
    var elapsedSeconds by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (true) {
            kotlinx.coroutines.delay(1000)
            elapsedSeconds = (System.currentTimeMillis() - start) / 1000
        }
    }

    val srcLang = languageByCode(sourceLang)
    val tgtLang = languageByCode(targetLang)

    // Activity inferred from pipeline state.
    // You = mic user, speaking sourceLang. They = listener, hearing the targetLang TTS.
    val youSpeaking = state is PipelineState.Listening || state is PipelineState.Transcribing
    val theySpeaking = state is PipelineState.Speaking

    // Subtitle text (the "headline" caption underneath the orb)
    val partial = (state as? PipelineState.Transcribing)?.partial.orEmpty()
    val translating = (state as? PipelineState.Translating)?.original.orEmpty()
    val speakingTxt = (state as? PipelineState.Speaking)?.translated.orEmpty()
    val subtitle = when {
        speakingTxt.isNotBlank() -> speakingTxt
        translating.isNotBlank() -> translating
        partial.isNotBlank() -> partial
        vm.messages.isNotEmpty() -> vm.messages.last().translatedText
        else -> "Press the mic to begin translating…"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp
            val veryCompact = maxWidth < 460.dp

            Column(modifier = Modifier.fillMaxSize()) {
                CallTopbar(
                    callerName = "Speaker · ${tgtLang.name}",
                    callerSub = "${tgtLang.code.uppercase()} · live translation",
                    callerInitials = tgtLang.code.uppercase(),
                    callerBrush = brushForLang(tgtLang.code, primary = true),
                    timerText = formatTime(elapsedSeconds),
                    sourceCode = srcLang.code.uppercase(),
                    targetCode = tgtLang.code.uppercase(),
                    modelName = modelDisplayName,
                    onBack = onBack,
                    onSwap = { vm.swapLanguages() },
                    compact = compact,
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = if (veryCompact) 14.dp else 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    PeerHeader(
                        theirName = "Speaker",
                        theirInitials = tgtLang.code.uppercase(),
                        theirBrush = brushForLang(tgtLang.code, primary = true),
                        theirLang = tgtLang.name,
                        theySpeaking = theySpeaking,
                        yourName = "You",
                        yourInitials = srcLang.code.uppercase(),
                        yourBrush = brushForLang(srcLang.code, primary = false),
                        yourLang = srcLang.name,
                        youSpeaking = youSpeaking,
                        compact = compact,
                    )

                    MetaChipStrip(
                        timer = formatTime(elapsedSeconds),
                        compact = compact,
                    )

                    OrbStage(
                        coreSize = if (veryCompact) 110.dp else if (compact) 140.dp else 180.dp,
                        subtitle = subtitle,
                        sourceCode = srcLang.code.uppercase(),
                        targetCode = tgtLang.code.uppercase(),
                        confidence = vm.messages.lastOrNull()?.confidence,
                        speaking = youSpeaking || theySpeaking,
                        showSubtitle = subtitle.isNotBlank(),
                    )

                    TranscriptArea(
                        messages = vm.messages,
                        sourceLang = srcLang.name,
                        targetLang = tgtLang.name,
                        compact = compact,
                    )

                    WaveformBar(
                        speakerInitials = if (theySpeaking) tgtLang.code.uppercase() else srcLang.code.uppercase(),
                        speaking = youSpeaking || theySpeaking,
                        hue = if (theySpeaking) Hue.Violet else Hue.Cyan,
                        timer = formatTime(elapsedSeconds),
                    )
                }

                CallControls(
                    listening = vm.isListening,
                    onToggleMic = { vm.toggleListening() },
                    onEnd = { vm.endSession(onBack) },
                    compact = compact,
                )
            }
        }
    }
}

// ───────────────────────────── Topbar ─────────────────────────────

@Composable
private fun CallTopbar(
    callerName: String,
    callerSub: String,
    callerInitials: String,
    callerBrush: Brush,
    timerText: String,
    sourceCode: String,
    targetCode: String,
    modelName: String,
    onBack: () -> Unit,
    onSwap: () -> Unit,
    compact: Boolean,
) {
    val t = LL.tokens
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(t.cyan.copy(alpha = 0.025f), Color.Transparent)
                    )
                )
                .padding(horizontal = if (compact) 14.dp else 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IconBtn(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack, contentDescription = "Back")

            // Caller card — right-aligned, hugging the center cluster (matches design)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!compact) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(callerName, color = t.text0, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(
                            callerSub.uppercase(),
                            color = t.text3,
                            fontFamily = MonoFamily,
                            fontSize = 9.5.sp,
                            letterSpacing = 1.4.sp,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                }
                GradientAvatar(initials = callerInitials, size = 28.dp, brush = callerBrush, live = true)
            }

            // Center cluster — only on wider widths
            if (!compact) {
                LiveIndicator(timer = timerText)
                MiniWave()
                ModelPill(text = modelName, leading = Icons.Default.Memory)
            } else {
                LiveIndicator(timer = timerText)
            }

            // Right
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    Chip(
                        text = "$sourceCode → $targetCode",
                        kind = ChipKind.Violet,
                        modifier = Modifier.sharedBoundsAcrossScreens(SharedKeys.LANG_PILL),
                    )
                    Spacer(Modifier.width(6.dp))
                    if (!compact) {
                        IconBtn(icon = Icons.Default.AutoAwesome, onClick = onSwap, contentDescription = "Swap")
                    }
                },
            )
        }
        GlowDivider(modifier = Modifier.fillMaxWidth())
    }
}

// ───────────────────────────── Peer header ─────────────────────────────

@Composable
private fun PeerHeader(
    theirName: String,
    theirInitials: String,
    theirBrush: Brush,
    theirLang: String,
    theySpeaking: Boolean,
    yourName: String,
    yourInitials: String,
    yourBrush: Brush,
    yourLang: String,
    youSpeaking: Boolean,
    compact: Boolean,
) {
    val t = LL.tokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp),
    ) {
        Peer(
            initials = theirInitials,
            brush = theirBrush,
            name = theirName,
            lang = theirLang,
            speaking = theySpeaking,
            mirrored = false,
            modifier = Modifier.weight(1f),
            compact = compact,
        )

        if (compact) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = t.text2, modifier = Modifier.size(18.dp))
        } else {
            TranslationArc(reverse = youSpeaking, modifier = Modifier.width(220.dp).height(56.dp))
        }

        Peer(
            initials = yourInitials,
            brush = yourBrush,
            name = yourName,
            lang = yourLang,
            speaking = youSpeaking,
            mirrored = true,
            modifier = Modifier.weight(1f),
            compact = compact,
        )
    }
}

@Composable
private fun Peer(
    initials: String,
    brush: Brush,
    name: String,
    lang: String,
    speaking: Boolean,
    mirrored: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean,
) {
    val t = LL.tokens
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (mirrored) Arrangement.End else Arrangement.Start,
    ) {
        if (mirrored) {
            PeerInfo(name = name, lang = lang, speaking = speaking, alignEnd = true, compact = compact)
            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
            PeerAvatar(initials = initials, brush = brush, speaking = speaking, accent = t.violet, size = if (compact) 36.dp else 44.dp)
        } else {
            PeerAvatar(initials = initials, brush = brush, speaking = speaking, accent = t.cyan, size = if (compact) 36.dp else 44.dp)
            Spacer(Modifier.width(if (compact) 8.dp else 12.dp))
            PeerInfo(name = name, lang = lang, speaking = speaking, alignEnd = false, compact = compact)
        }
    }
}

@Composable
private fun PeerAvatar(
    initials: String,
    brush: Brush,
    speaking: Boolean,
    accent: Color,
    size: Dp,
) {
    Box(modifier = Modifier.size(size + 12.dp), contentAlignment = Alignment.Center) {
        if (speaking) {
            // Pulsing ring
            val transition = rememberInfiniteTransition(label = "peerRing")
            val scale by transition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
                label = "scale",
            )
            val alpha by transition.animateFloat(
                initialValue = 0.9f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
                label = "alpha",
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .scale(scale)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = accent.copy(alpha = alpha),
                        shape = CircleShape,
                    )
            )
        }
        GradientAvatar(initials = initials, size = size, brush = brush)
    }
}

@Composable
private fun PeerInfo(name: String, lang: String, speaking: Boolean, alignEnd: Boolean, compact: Boolean) {
    val t = LL.tokens
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(name, color = t.text0, fontSize = if (compact) 13.sp else 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (alignEnd) Spacer(Modifier.width(0.dp))
            Icon(Icons.Default.Public, null, tint = t.text2, modifier = Modifier.size(10.dp))
            Text(
                "$lang · ${if (speaking) "speaking" else "listening"}",
                color = t.text2,
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
            )
        }
    }
}

// ───────────────────────────── Translation arc ─────────────────────────────

@Composable
private fun TranslationArc(reverse: Boolean, modifier: Modifier = Modifier) {
    val t = LL.tokens
    val transition = rememberInfiniteTransition(label = "arc")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val dashOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (reverse) 14f else -14f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Restart),
        label = "dashOffset",
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val pad = 8f
            val path = Path().apply {
                moveTo(pad, h * 0.55f)
                quadraticBezierTo(w / 2f, h * 0.0f, w - pad, h * 0.55f)
            }
            // Animated dashed stroke with cyan→violet gradient
            val brush = Brush.horizontalGradient(
                if (reverse) listOf(t.violet, t.cyan) else listOf(t.cyan, t.violet)
            )
            drawPath(
                path = path,
                brush = brush,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()), dashOffset),
                ),
                alpha = 0.7f,
            )

            // Particle traveling along the arc
            val pos = bezierPoint(
                phase,
                Offset(pad, h * 0.55f),
                Offset(w / 2f, h * 0.0f),
                Offset(w - pad, h * 0.55f),
                reverse,
            )
            val color = if (reverse) t.violet else t.cyan
            drawCircle(color = color.copy(alpha = 0.4f), radius = 6.dp.toPx(), center = pos)
            drawCircle(color = color, radius = 3.dp.toPx(), center = pos)
        }
        // Centered "translating" pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(t.bg0)
                .border(1.dp, t.border, RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = t.text2, modifier = Modifier.size(10.dp))
                Text(
                    "translating",
                    color = t.text2,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                )
            }
        }
    }
}

private fun bezierPoint(
    phase: Float,
    p0: Offset,
    p1: Offset,
    p2: Offset,
    reverse: Boolean,
): Offset {
    val tt = if (reverse) 1f - phase else phase
    val it = 1f - tt
    val x = it * it * p0.x + 2f * it * tt * p1.x + tt * tt * p2.x
    val y = it * it * p0.y + 2f * it * tt * p1.y + tt * tt * p2.y
    return Offset(x, y)
}

// ───────────────────────────── Chip strip ─────────────────────────────

@Composable
private fun MetaChipStrip(timer: String, compact: Boolean) {
    val arr = if (compact) Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally) else Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arr,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Chip(text = "LIVE", kind = ChipKind.Live)
        Chip(text = "CLAUDE-HAIKU-4-5 · 184ms", kind = ChipKind.Cyan, leading = Icons.Default.Memory)
        Chip(text = timer)
        if (!compact) {
            Chip(text = "end-to-end encrypted", kind = ChipKind.Violet, leading = Icons.Default.SecurityUpdateGood)
        }
    }
}

// ───────────────────────────── Orb stage + subtitle ─────────────────────────────

@Composable
private fun OrbStage(
    coreSize: Dp,
    subtitle: String,
    sourceCode: String,
    targetCode: String,
    confidence: Float?,
    speaking: Boolean,
    showSubtitle: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OrbVisualizer(
            coreSize = coreSize,
            speaking = speaking,
            modifier = Modifier.sharedAcrossScreens(SharedKeys.HERO_ORB),
        )
        if (showSubtitle) {
            SubtitleBubble(
                text = subtitle,
                sourceCode = sourceCode,
                targetCode = targetCode,
                confidence = confidence,
            )
        }
    }
}

@Composable
private fun SubtitleBubble(
    text: String,
    sourceCode: String,
    targetCode: String,
    confidence: Float?,
) {
    val t = LL.tokens
    Column(
        modifier = Modifier
            .widthIn(max = 620.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "$sourceCode → $targetCode",
                color = t.text3,
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
            )
            Text("·", color = t.text3, fontSize = 10.sp)
            Text(
                "live · 184ms",
                color = t.cyan,
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
            )
            if (confidence != null) {
                Text("·", color = t.text3, fontSize = 10.sp)
                Text(
                    "confidence ${(confidence * 100).toInt()}%",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(t.bg0.copy(alpha = if (t.isDark) 0.78f else 0.92f))
                .border(1.dp, t.borderStrong, RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
        ) {
            StreamingTokenText(text = text)
        }
    }
}

@Composable
private fun StreamingTokenText(text: String) {
    val t = LL.tokens
    // Words fade in with a per-word delay; cheap and feels alive without per-word animations.
    val transition = rememberInfiniteTransition(label = "stream")
    val anim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "anim",
    )
    val styled = buildAnnotatedString {
        val words = text.split(" ")
        words.forEachIndexed { i, w ->
            // Stagger reveal across words; once revealed each word stays at full opacity.
            val reveal = ((anim - i * 0.04f) * 6f).coerceIn(0f, 1f)
            withStyle(
                SpanStyle(
                    color = t.text0.copy(alpha = 0.4f + 0.6f * reveal),
                    fontWeight = FontWeight.Medium,
                )
            ) { append(w) }
            if (i < words.size - 1) append(" ")
        }
    }
    Text(
        styled,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

// ───────────────────────────── Transcript ─────────────────────────────

@Composable
private fun TranscriptArea(
    messages: List<ConversationMessage>,
    sourceLang: String,
    targetLang: String,
    compact: Boolean,
) {
    val recent = messages.takeLast(8)

    if (compact) {
        TranscriptPairColumn(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            sourceLang = sourceLang,
            targetLang = targetLang,
            messages = recent.takeLast(4),
        )
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TranscriptColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                who = sourceLang,
                tag = "TRANSCRIPT",
                accent = LL.tokens.cyan,
                lines = recent.map { it.originalText },
            )
            TranscriptColumn(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                who = targetLang,
                tag = "TRANSLATION",
                accent = LL.tokens.violet,
                lines = recent.map { it.translatedText },
            )
        }
    }
}

@Composable
private fun TranscriptColumn(
    modifier: Modifier,
    who: String,
    tag: String,
    accent: Color,
    lines: List<String>,
) {
    val t = LL.tokens
    GlassCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(who, color = t.text1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    tag,
                    color = accent,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(t.border)
            )
            if (lines.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nothing yet",
                        color = t.text3,
                        fontSize = 11.sp,
                        fontFamily = MonoFamily,
                        letterSpacing = 0.6.sp,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    lines.forEach { line ->
                        Text(
                            line,
                            color = t.text0,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TranscriptPairColumn(
    modifier: Modifier,
    sourceLang: String,
    targetLang: String,
    messages: List<ConversationMessage>,
) {
    val t = LL.tokens
    GlassCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Conversation", color = t.text1, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "$sourceLang → $targetLang",
                    color = t.text2,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.2.sp,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(t.border)
            )
            if (messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nothing yet",
                        color = t.text3,
                        fontSize = 11.sp,
                        fontFamily = MonoFamily,
                        letterSpacing = 0.6.sp,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    messages.forEach { msg ->
                        Text(
                            msg.originalText,
                            color = t.cyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 17.sp,
                        )
                        Text(
                            "→ ${msg.translatedText}",
                            color = t.violet,
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(start = 10.dp),
                        )
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Waveform bar ─────────────────────────────

@Composable
private fun WaveformBar(
    speakerInitials: String,
    speaking: Boolean,
    hue: Hue,
    timer: String,
) {
    val t = LL.tokens
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PulseDot(color = if (hue == Hue.Cyan) t.cyan else t.violet, size = 6.dp)
                Text(
                    speakerInitials,
                    color = t.text2,
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                    letterSpacing = 1.0.sp,
                )
            }
            LiveWaveform(
                modifier = Modifier.weight(1f).height(28.dp),
                bars = 64,
                speaking = speaking,
                hue = hue,
            )
            Text(
                timer,
                color = t.text2,
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                letterSpacing = 1.0.sp,
            )
        }
    }
}

// ───────────────────────────── Call controls ─────────────────────────────

@Composable
private fun CallControls(
    listening: Boolean,
    onToggleMic: () -> Unit,
    onEnd: () -> Unit,
    compact: Boolean,
) {
    val t = LL.tokens
    var captionsOn by remember { mutableStateOf(true) }
    var cameraOn by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 10.dp else 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            icon = if (listening) Icons.Default.Mic else Icons.Default.MicOff,
            label = if (listening) "Mute" else "Unmute",
            muted = !listening,
            onClick = onToggleMic,
        )
        ControlButton(
            icon = if (cameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
            label = "Camera",
            onClick = { cameraOn = !cameraOn },
        )
        ControlButton(
            icon = Icons.Default.ClosedCaption,
            label = "Subtitles",
            selected = captionsOn,
            onClick = { captionsOn = !captionsOn },
        )
        ControlButton(
            icon = Icons.Default.VolumeUp,
            label = "Speaker",
            onClick = {},
        )
        ControlButton(
            icon = Icons.Default.CallEnd,
            label = "End",
            danger = true,
            onClick = onEnd,
        )
        ControlButton(
            icon = Icons.Default.MoreHoriz,
            label = "More",
            onClick = {},
        )
    }
}

@Composable
private fun ControlButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    muted: Boolean = false,
    danger: Boolean = false,
    selected: Boolean = false,
) {
    val t = LL.tokens
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val bg = when {
            danger -> Brush.linearGradient(listOf(Color(0xFFFF5670), Color(0xFFFF2D57)))
            muted -> Brush.linearGradient(listOf(t.red.copy(alpha = 0.20f), t.red.copy(alpha = 0.20f)))
            selected -> Brush.linearGradient(listOf(t.cyan.copy(alpha = 0.14f), t.cyan.copy(alpha = 0.14f)))
            else -> Brush.linearGradient(listOf(Color.White.copy(alpha = if (t.isDark) 0.05f else 0.0f), Color.White.copy(alpha = if (t.isDark) 0.05f else 0.0f)))
        }
        val borderColor = when {
            danger -> Color.Transparent
            muted -> t.red.copy(alpha = 0.40f)
            selected -> t.cyan.copy(alpha = 0.40f)
            else -> t.border
        }
        val tint = when {
            danger -> Color.White
            muted -> t.red
            selected -> t.cyan
            else -> t.text0
        }
        Box(
            modifier = Modifier
                .size(if (danger) 56.dp else 44.dp)
                .clip(CircleShape)
                .background(bg)
                .border(1.dp, borderColor, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(17.dp))
        }
        Text(
            label,
            color = t.text3,
            fontFamily = MonoFamily,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// ───────────────────────────── Utils ─────────────────────────────

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}

@Composable
private fun brushForLang(code: String, primary: Boolean): Brush = when (code.lowercase()) {
    "en" -> AvatarBrushes.Indigo
    "fr" -> AvatarBrushes.Coral
    "es" -> AvatarBrushes.Violet
    "ja" -> AvatarBrushes.Aqua
    "de" -> AvatarBrushes.Mint
    "ar", "ur" -> AvatarBrushes.Amber
    "hi" -> AvatarBrushes.Rose
    else -> if (primary) AvatarBrushes.Coral else AvatarBrushes.Indigo
}

