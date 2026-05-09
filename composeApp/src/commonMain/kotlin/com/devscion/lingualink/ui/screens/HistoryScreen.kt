package com.devscion.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devscion.lingualink.data.model.Session
import com.devscion.lingualink.data.model.SessionType
import com.devscion.lingualink.data.model.languageByCode
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.ui.theme.AmbientMeshBackground
import com.devscion.lingualink.ui.theme.AvatarBrushes
import com.devscion.lingualink.ui.theme.BrandMark
import com.devscion.lingualink.ui.theme.BrandWordmark
import com.devscion.lingualink.ui.theme.Chip
import com.devscion.lingualink.ui.theme.ChipKind
import com.devscion.lingualink.ui.theme.GlassCard
import com.devscion.lingualink.ui.theme.GlowDivider
import com.devscion.lingualink.ui.theme.GradientAvatar
import com.devscion.lingualink.ui.theme.IconBtn
import com.devscion.lingualink.ui.theme.LL
import com.devscion.lingualink.ui.theme.MonoFamily
import com.devscion.lingualink.ui.theme.SharedKeys
import com.devscion.lingualink.ui.theme.sharedBoundsAcrossScreens
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    sessionRepo: SessionRepository,
    onOpenDetails: (Long) -> Unit,
    onBack: () -> Unit
) {
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var deleteTarget by remember { mutableStateOf<Session?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sessions = sessionRepo.getAllSessions()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp
            val veryCompact = maxWidth < 460.dp

            Column(modifier = Modifier.fillMaxSize()) {
                HistoryTopbar(onBack = onBack, compact = compact)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = if (veryCompact) 14.dp else if (compact) 18.dp else 28.dp,
                            vertical = 16.dp,
                        ),
                ) {
                    HistoryHeader(sessions = sessions, compact = compact)
                    Spacer(Modifier.height(16.dp))

                    if (sessions.isEmpty()) {
                        EmptyHistoryState()
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(sessions) { session ->
                                CallCard(
                                    session = session,
                                    onOpen = { onOpenDetails(session.id) },
                                    onDelete = { deleteTarget = session },
                                    compact = compact,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        DeleteDialog(
            session = target,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                scope.launch {
                    sessionRepo.deleteSession(target.id)
                    sessions = sessionRepo.getAllSessions()
                }
                deleteTarget = null
            },
        )
    }
}

// ───────────────────────────── Topbar ─────────────────────────────

@Composable
private fun HistoryTopbar(onBack: () -> Unit, compact: Boolean) {
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

            Box(modifier = Modifier.sharedBoundsAcrossScreens(SharedKeys.BRAND_MARK)) {
                BrandMark(size = 28.dp)
            }
            Spacer(Modifier.width(2.dp))
            BrandWordmark(size = 16.dp)
            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "ARCHIVE",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                )
                Text("Call history", color = t.text0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        GlowDivider(modifier = Modifier.fillMaxWidth())
    }
}

// ───────────────────────────── Header / stats ─────────────────────────────

@Composable
private fun HistoryHeader(sessions: List<Session>, compact: Boolean) {
    val t = LL.tokens
    val total = sessions.size
    val totalSeconds = sessions.sumOf { it.durationSeconds ?: 0L }
    val languages = sessions
        .flatMap { listOf(it.sourceLanguage, it.targetLanguage) }
        .distinct()
        .size

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Conversations",
                color = t.text0,
                fontSize = if (compact) 20.sp else 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.4).sp,
            )
            Text(
                "Every call & chat, transcribed, translated, summarized.",
                color = t.text2,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
            )
        }

        if (!compact) {
            StatChip(value = total.toString(), label = "Sessions", accent = t.cyan)
            StatChip(value = formatTotalTalkTime(totalSeconds), label = "Total time", accent = t.text0)
            StatChip(value = languages.toString(), label = "Languages", accent = t.violet)
        }
    }

    if (compact) {
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatChip(value = total.toString(), label = "Sessions", accent = t.cyan, modifier = Modifier.weight(1f))
            StatChip(value = formatTotalTalkTime(totalSeconds), label = "Time", accent = t.text0, modifier = Modifier.weight(1f))
            StatChip(value = languages.toString(), label = "Languages", accent = t.violet, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatChip(
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val t = LL.tokens
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (t.isDark) 0.03f else 0.0f))
            .border(1.dp, t.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(value, color = accent, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
        Text(
            label.uppercase(),
            color = t.text3,
            fontFamily = MonoFamily,
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
        )
    }
}

// ───────────────────────────── Empty state ─────────────────────────────

@Composable
private fun EmptyHistoryState() {
    val t = LL.tokens
    Box(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                "Nothing here yet",
                color = t.text0,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "Start a call or chat from Home — they'll show up here once you're done.",
                color = t.text2,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

// ───────────────────────────── Call card ─────────────────────────────

@Composable
private fun CallCard(
    session: Session,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    compact: Boolean,
) {
    val t = LL.tokens
    val isCall = session.sessionType == SessionType.CALL
    val src = languageByCode(session.sourceLanguage)
    val tgt = languageByCode(session.targetLanguage)
    val avatarBrush = avatarFor(session.id)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Avatar + type badge
            Box(contentAlignment = Alignment.Center) {
                GradientAvatar(initials = src.code.uppercase(), size = 38.dp, brush = avatarBrush)
            }

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (isCall) Icons.Default.Phone else Icons.Default.Chat,
                        null,
                        tint = if (isCall) t.cyan else t.violet,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        if (isCall) "Voice call" else "Text chat",
                        color = t.text0,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Chip(text = src.code.uppercase(), kind = ChipKind.Neutral)
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = t.text3, modifier = Modifier.size(10.dp))
                    Chip(text = tgt.code.uppercase(), kind = ChipKind.Cyan)
                }
                Spacer(Modifier.height(6.dp))
                if (!compact) {
                    Text(
                        buildSummary(session, src.name, tgt.name),
                        color = t.text2,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                    )
                } else {
                    Text(
                        formatRelativeTime(session.startedAt),
                        color = t.text2,
                        fontSize = 12.sp,
                        fontFamily = MonoFamily,
                        letterSpacing = 0.4.sp,
                    )
                }
            }

            // Right-side meta
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 4.dp),
            ) {
                if (!compact) {
                    Text(
                        formatRelativeTime(session.startedAt),
                        color = t.text3,
                        fontSize = 10.5.sp,
                        fontFamily = MonoFamily,
                        letterSpacing = 0.4.sp,
                    )
                }
                Text(
                    session.durationSeconds?.let { formatDuration(it) } ?: "—",
                    color = t.text1,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    IconBtn(icon = Icons.Default.PlayArrow, onClick = onOpen, contentDescription = "Reopen", size = 26.dp)
                    IconBtn(icon = Icons.Default.Delete, onClick = onDelete, contentDescription = "Delete", size = 26.dp)
                }
            }
        }
    }
}

@Composable
private fun DeleteDialog(
    session: Session,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val t = LL.tokens
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete this session?",
                color = t.text0,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Text(
                "This will permanently delete the ${if (session.sessionType == SessionType.CALL) "call" else "chat"} and all its messages. This cannot be undone.",
                color = t.text2,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = t.red, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = t.text2)
            }
        },
        containerColor = t.bg2,
        titleContentColor = t.text0,
        textContentColor = t.text2,
    )
}

// ───────────────────────────── Helpers ─────────────────────────────

private fun avatarFor(id: Long): Brush = when ((id % 7).toInt()) {
    0 -> AvatarBrushes.Coral
    1 -> AvatarBrushes.Indigo
    2 -> AvatarBrushes.Aqua
    3 -> AvatarBrushes.Violet
    4 -> AvatarBrushes.Amber
    5 -> AvatarBrushes.Mint
    else -> AvatarBrushes.Rose
}

private fun buildSummary(session: Session, srcName: String, tgtName: String): String {
    val type = if (session.sessionType == SessionType.CALL) "Live voice translation" else "Text chat translation"
    val dur = session.durationSeconds?.let { formatDurationLong(it) } ?: "in progress"
    return "AI SUMMARY · $type between $srcName and $tgtName, $dur."
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
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

private fun formatTotalTalkTime(seconds: Long): String {
    if (seconds <= 0) return "0m"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return when {
        h > 0 -> "${h}h ${m}m"
        else -> "${m}m"
    }
}

private fun formatRelativeTime(epochMs: Long): String = formatDate(epochMs)

// Simple year-month-day formatter — full locale-aware DateTime formatting is platform-specific.
private fun formatDate(epochMs: Long): String {
    val seconds = epochMs / 1000
    val day = (seconds / 86400) % 31 + 1
    val month = (seconds / (86400 * 30)) % 12 + 1
    val year = 1970 + (seconds / (86400 * 365)).toInt()
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
