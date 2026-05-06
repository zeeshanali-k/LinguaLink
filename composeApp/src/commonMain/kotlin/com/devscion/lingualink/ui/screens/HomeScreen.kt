package com.devscion.lingualink.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.model.SessionType
import com.devscion.lingualink.data.model.SupportedLanguages
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.ui.theme.AmbientMeshBackground
import com.devscion.lingualink.ui.theme.AvatarBrushes
import com.devscion.lingualink.ui.theme.BrandMark
import com.devscion.lingualink.ui.theme.BrandWordmark
import com.devscion.lingualink.ui.theme.Chip
import com.devscion.lingualink.ui.theme.ChipKind
import com.devscion.lingualink.ui.theme.GhostButton
import com.devscion.lingualink.ui.theme.GlassCard
import com.devscion.lingualink.ui.theme.GlowDivider
import com.devscion.lingualink.ui.theme.GradientButton
import com.devscion.lingualink.ui.theme.IconBtn
import com.devscion.lingualink.ui.theme.LL
import com.devscion.lingualink.ui.theme.MonoFamily
import com.devscion.lingualink.ui.theme.OrbVisualizer
import com.devscion.lingualink.ui.theme.SharedKeys
import com.devscion.lingualink.ui.theme.glass
import com.devscion.lingualink.ui.theme.sharedAcrossScreens
import com.devscion.lingualink.ui.theme.sharedBoundsAcrossScreens
import kotlinx.coroutines.launch
import kotlin.random.Random

class HomeViewModel(
    private val sessionRepo: SessionRepository,
    private val configManager: ConfigManager
) : ViewModel() {

    var sourceLang by mutableStateOf("en")
    var targetLang by mutableStateOf("ur")

    init {
        val cfg = configManager.load()
        if (cfg != null) {
            sourceLang = cfg.sourceLanguage
            targetLang = cfg.targetLanguage
        }
    }

    fun swapLanguages() {
        val tmp = sourceLang; sourceLang = targetLang; targetLang = tmp
        persistLanguages()
    }

    fun createCallSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = sessionRepo.createSession(SessionType.CALL, sourceLang, targetLang)
            onCreated(id)
        }
    }

    fun createChatSession(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = sessionRepo.createSession(SessionType.CHAT, sourceLang, targetLang)
            onCreated(id)
        }
    }

    private fun persistLanguages() {
        val cfg = configManager.load() ?: return
        configManager.save(cfg.copy(sourceLanguage = sourceLang, targetLanguage = targetLang))
    }
}

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onStartCall: (Long, String, String) -> Unit,
    onOpenChat: (Long, String, String) -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())
        TickerWords(modifier = Modifier.fillMaxSize())

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent,
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                val compact = maxWidth < 720.dp
                val veryCompact = maxWidth < 460.dp
                val src = SupportedLanguages.find { it.code == vm.sourceLang } ?: SupportedLanguages.first()
                val tgt = SupportedLanguages.find { it.code == vm.targetLang } ?: SupportedLanguages.first()

                Column(modifier = Modifier.fillMaxSize()) {
                    HomeTopbar(
                        sourceCode = src.code.uppercase(),
                        targetCode = tgt.code.uppercase(),
                        onSettings = onSettings,
                        onHistory = onHistory,
                        compact = compact,
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = if (veryCompact) 16.dp else 28.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 28.dp),
                    ) {
                        if (compact) Spacer(Modifier.height(4.dp))

                        // Hero orb
                        OrbVisualizer(
                            coreSize = if (compact) 64.dp else 84.dp,
                            modifier = Modifier.sharedAcrossScreens(SharedKeys.HERO_ORB),
                        )

                        // Headline
                        HeroHeadline(compact = compact)

                        // Language pair card
                        LangPairCard(
                            youSpeak = tgt.name,
                            youSpeakNative = "${tgt.nativeName} · ${tgt.code.uppercase()}",
                            theySpeak = src.name,
                            theySpeakNative = "${src.nativeName} · ${src.code.uppercase()}",
                            onPickYou = { code -> vm.targetLang = code },
                            onPickThem = { code -> vm.sourceLang = code },
                            currentYou = tgt.code,
                            currentThem = src.code,
                            onSwap = { vm.swapLanguages() },
                            compact = compact,
                        )

                        // Mode cards
                        ModeCards(
                            onStartCall = {
                                if (vm.sourceLang == vm.targetLang) {
                                    scope.launch { snackbarHostState.showSnackbar("Source and target language must differ") }
                                } else {
                                    vm.createCallSession { id -> onStartCall(id, vm.sourceLang, vm.targetLang) }
                                }
                            },
                            onOpenChat = {
                                if (vm.sourceLang == vm.targetLang) {
                                    scope.launch { snackbarHostState.showSnackbar("Source and target language must differ") }
                                } else {
                                    vm.createChatSession { id -> onOpenChat(id, vm.sourceLang, vm.targetLang) }
                                }
                            },
                            compact = compact,
                        )

                        TrustFooter(compact = compact)
                    }
                }
            }
        }
    }
}

// ───────────────────────────── Topbar ─────────────────────────────

@Composable
private fun HomeTopbar(
    sourceCode: String,
    targetCode: String,
    onSettings: () -> Unit,
    onHistory: () -> Unit,
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
                .padding(horizontal = if (compact) 16.dp else 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier.sharedBoundsAcrossScreens(SharedKeys.BRAND_MARK),
            ) {
                BrandMark(size = 28.dp)
            }
            Spacer(Modifier.width(2.dp))
            BrandWordmark(size = 16.dp)
            Spacer(Modifier.weight(1f))

            if (!compact) {
                Chip(
                    text = "$sourceCode → $targetCode",
                    kind = ChipKind.Violet,
                    leading = Icons.Default.Language,
                    modifier = Modifier.sharedBoundsAcrossScreens(SharedKeys.LANG_PILL),
                )
                Spacer(Modifier.width(4.dp))
            }
            IconBtn(icon = Icons.Default.History, onClick = onHistory, contentDescription = "History")
            IconBtn(icon = Icons.Default.Settings, onClick = onSettings, contentDescription = "Settings")
        }
        GlowDivider(modifier = Modifier.fillMaxWidth())
    }
}

// ───────────────────────────── Hero ─────────────────────────────

@Composable
private fun HeroHeadline(compact: Boolean) {
    val t = LL.tokens
    val title = buildAnnotatedString {
        withStyle(SpanStyle(color = t.text0)) { append("Speak any language.") }
        append(" ")
        if (!compact) append("\n")
        withStyle(
            SpanStyle(
                brush = t.brandGradientHorizontal,
                fontWeight = FontWeight.SemiBold,
            )
        ) { append("Hear yours.") }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            fontSize = if (compact) 28.sp else 38.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = if (compact) 32.sp else 42.sp,
            letterSpacing = (-0.6).sp,
            modifier = Modifier.fillMaxWidth(),
            color = t.text0,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "Real-time AI translation for calls and chats. Pick the languages you'll be moving between today — you can change anytime.",
            color = t.text2,
            fontSize = if (compact) 13.sp else 15.sp,
            lineHeight = if (compact) 19.sp else 22.sp,
            modifier = Modifier.widthIn(max = 520.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ───────────────────────────── Language pair ─────────────────────────────

@Composable
private fun LangPairCard(
    youSpeak: String,
    youSpeakNative: String,
    theySpeak: String,
    theySpeakNative: String,
    onPickYou: (String) -> Unit,
    onPickThem: (String) -> Unit,
    currentYou: String,
    currentThem: String,
    onSwap: () -> Unit,
    compact: Boolean,
) {
    val t = LL.tokens
    GlassCard(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth(),
        strong = true,
        contentPadding = PaddingValues(if (compact) 16.dp else 24.dp),
    ) {
        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LangPicker(label = "You speak", name = youSpeak, native = youSpeakNative, current = currentYou, onPick = onPickYou)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    SwapButton(onClick = onSwap)
                }
                LangPicker(label = "They speak", name = theySpeak, native = theySpeakNative, current = currentThem, onPick = onPickThem)
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                LangPicker(
                    modifier = Modifier.weight(1f),
                    label = "You speak",
                    name = youSpeak,
                    native = youSpeakNative,
                    current = currentYou,
                    onPick = onPickYou,
                )
                SwapButton(onClick = onSwap)
                LangPicker(
                    modifier = Modifier.weight(1f),
                    label = "They speak",
                    name = theySpeak,
                    native = theySpeakNative,
                    current = currentThem,
                    onPick = onPickThem,
                )
            }
        }
    }
}

@Composable
private fun LangPicker(
    label: String,
    name: String,
    native: String,
    current: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = LL.tokens
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (t.isDark) 0.03f else 0.0f))
            .border(1.dp, t.border, RoundedCornerShape(12.dp))
            .clickable { expanded = true }
            .padding(18.dp),
    ) {
        Column {
            Text(
                label.uppercase(),
                color = t.text3,
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(name, color = t.text0, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
            Spacer(Modifier.height(4.dp))
            Text(native, color = t.text2, fontSize = 13.sp)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(t.bg2),
        ) {
            SupportedLanguages.forEach { l ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(l.code.uppercase(), color = t.cyan, fontFamily = MonoFamily, fontSize = 11.sp, letterSpacing = 1.0.sp, modifier = Modifier.width(28.dp))
                            Text(l.name, color = t.text0)
                            Spacer(Modifier.weight(1f))
                            Text(l.nativeName, color = t.text3, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    },
                    onClick = { onPick(l.code); expanded = false },
                    trailingIcon = {
                        if (l.code == current) {
                            Box(
                                modifier = Modifier.size(8.dp).clip(CircleShape).background(t.cyan)
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun SwapButton(onClick: () -> Unit) {
    val t = LL.tokens
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(t.cyan.copy(alpha = 0.15f), t.violet.copy(alpha = 0.15f))
                )
            )
            .border(1.dp, t.borderStrong, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.SwapHoriz, contentDescription = "Swap languages", tint = t.text0, modifier = Modifier.size(20.dp))
    }
}

// ───────────────────────────── Mode cards ─────────────────────────────

@Composable
private fun ModeCards(onStartCall: () -> Unit, onOpenChat: () -> Unit, compact: Boolean) {
    if (compact) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeCard(
                icon = Icons.Default.Mic,
                tag = "VOICE",
                title = "Voice call translation",
                subtitle = "Real-time mic → translated speech with live subtitles.",
                cta = "Start translating",
                onClick = onStartCall,
                accent = LL.tokens.cyan,
            )
            ModeCard(
                icon = Icons.AutoMirrored.Filled.Send,
                tag = "TEXT",
                title = "Chat translation",
                subtitle = "Type messages, get instant tap-to-toggle translation.",
                cta = "Open chat",
                onClick = onOpenChat,
                accent = LL.tokens.violet,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModeCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Mic,
                tag = "VOICE",
                title = "Voice call translation",
                subtitle = "Real-time mic → translated speech with live subtitles.",
                cta = "Start translating",
                onClick = onStartCall,
                accent = LL.tokens.cyan,
            )
            ModeCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.Send,
                tag = "TEXT",
                title = "Chat translation",
                subtitle = "Type messages, get instant tap-to-toggle translation.",
                cta = "Open chat",
                onClick = onOpenChat,
                accent = LL.tokens.violet,
            )
        }
    }
}

@Composable
private fun ModeCard(
    icon: ImageVector,
    tag: String,
    title: String,
    subtitle: String,
    cta: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val t = LL.tokens
    Column(
        modifier = modifier
            .glass(t, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.18f), t.violet.copy(alpha = 0.10f))
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
            }
            Text(
                tag,
                color = accent,
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.6.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Text(title, color = t.text0, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
        Text(subtitle, color = t.text2, fontSize = 13.sp, lineHeight = 19.sp)
        Spacer(Modifier.height(2.dp))
        GradientButton(text = cta, onClick = onClick, leading = Icons.Default.AutoAwesome)
    }
}

// ───────────────────────────── Trust footer ─────────────────────────────

@Composable
private fun TrustFooter(compact: Boolean) {
    val t = LL.tokens
    val items = listOf(
        "End-to-end encrypted",
        "On-device fallback",
        "<200ms latency",
    )
    val arrangement = if (compact) Arrangement.Center else Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            if (compact && i > 0) Spacer(Modifier.width(10.dp))
            Text(
                "◆ ${item}",
                color = t.text3,
                fontFamily = MonoFamily,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
            )
        }
    }
}

// ───────────────────────────── Floating tickers ─────────────────────────────

private val TickerWords = listOf(
    "Bonjour", "你好", "مرحبا", "Hola", "Привет", "こんにちは", "안녕", "Hallo",
    "Olá", "नमस्ते", "Ciao", "Merhaba", "Hej", "Salam", "Xin chào", "γειά",
)

@Composable
private fun TickerWords(modifier: Modifier = Modifier) {
    val t = LL.tokens
    val transition = rememberInfiniteTransition(label = "tickers")
    BoxWithConstraints(modifier = modifier) {
        val w = maxWidth
        val h = maxHeight
        val seeds = remember(w, h) {
            TickerWords.mapIndexed { i, word ->
                TickerSpec(
                    word = word,
                    xFrac = ((i.toFloat() / TickerWords.size) + Random.nextFloat() * 0.04f).coerceIn(0.02f, 0.96f),
                    yFrac = 0.20f + Random.nextFloat() * 0.60f,
                    delay = i * 0.8f,
                    durationMs = 10000 + Random.nextInt(6000),
                )
            }
        }
        seeds.forEach { spec ->
            val anim by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(spec.durationMs, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                    initialStartOffset = androidx.compose.animation.core.StartOffset((spec.delay * 1000f).toInt())
                ),
                label = "tick-${spec.word}"
            )
            // Float upward over the cycle, fade in/out.
            val yOffsetDp = (h.value * spec.yFrac) - (anim * 80f)  // drift up by 80dp
            val alpha = when {
                anim < 0.15f -> anim / 0.15f * 0.5f
                anim < 0.85f -> 0.5f
                else -> (1f - anim) / 0.15f * 0.5f
            }.coerceIn(0f, 0.5f)
            Box(
                modifier = Modifier
                    .offset(x = (w.value * spec.xFrac).dp, y = yOffsetDp.dp)
            ) {
                Text(
                    spec.word,
                    color = t.text3.copy(alpha = alpha),
                    fontFamily = MonoFamily,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

private data class TickerSpec(
    val word: String,
    val xFrac: Float,
    val yFrac: Float,
    val delay: Float,
    val durationMs: Int,
)
