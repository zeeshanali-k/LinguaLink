package com.devscion.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devscion.lingualink.data.model.Session
import com.devscion.lingualink.data.model.SessionType
import com.devscion.lingualink.data.repository.MessageRepository
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.ui.theme.LinguaLinkColors
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    sessionRepo: SessionRepository,
    messageRepo: MessageRepository,
    onReopen: (Long, SessionType, String, String) -> Unit,
    onBack: () -> Unit
) {
    var sessions by remember { mutableStateOf<List<Session>>(emptyList()) }
    var deleteTarget by remember { mutableStateOf<Session?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        sessions = sessionRepo.getAllSessions()
    }

    Column(modifier = Modifier.fillMaxSize().background(LinguaLinkColors.Background)) {
        Surface(color = LinguaLinkColors.Surface, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("←", color = LinguaLinkColors.TextSecondary) }
                Text(
                    "Session History",
                    style = MaterialTheme.typography.titleMedium,
                    color = LinguaLinkColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (sessions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No sessions yet. Start a call or chat.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = LinguaLinkColors.TextSecondary
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                items(sessions) { session ->
                    SessionRow(
                        session = session,
                        onReopen = { onReopen(session.id, session.sessionType, session.sourceLanguage, session.targetLanguage) },
                        onDelete = { deleteTarget = session }
                    )
                    HorizontalDivider(color = LinguaLinkColors.Divider)
                }
            }
        }
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Session?", color = LinguaLinkColors.TextPrimary) },
            text = { Text("This will permanently delete the session and all its messages.", color = LinguaLinkColors.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        sessionRepo.deleteSession(target.id)
                        sessions = sessionRepo.getAllSessions()
                    }
                    deleteTarget = null
                }) {
                    Text("Delete", color = LinguaLinkColors.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
            containerColor = LinguaLinkColors.Surface
        )
    }
}

@Composable
private fun SessionRow(session: Session, onReopen: () -> Unit, onDelete: () -> Unit) {
    val icon = if (session.sessionType == SessionType.CALL) "🎙" else "💬"
    val typeLabel = if (session.sessionType == SessionType.CALL) "Voice Call" else "Chat"
    val langPair = "${session.sourceLanguage.uppercase()} → ${session.targetLanguage.uppercase()}"
    val duration = session.durationSeconds?.let { formatDuration(it) } ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "$typeLabel  $langPair",
                style = MaterialTheme.typography.bodyMedium,
                color = LinguaLinkColors.TextPrimary
            )
            Text(
                "$duration  •  ${formatTimestamp(session.startedAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = LinguaLinkColors.TextSecondary
            )
        }
        TextButton(onClick = onReopen) { Text("↗", color = LinguaLinkColors.Primary) }
        TextButton(onClick = onDelete) { Text("🗑", color = LinguaLinkColors.Error) }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m}m ${s}s"
}

private fun formatTimestamp(epochMs: Long): String {
    // Simple readable date; full formatting is platform-specific
    val seconds = epochMs / 1000
    val day = (seconds / 86400) % 31 + 1
    val month = (seconds / (86400 * 30)) % 12 + 1
    val year = 1970 + (seconds / (86400 * 365)).toInt()
    return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
}
