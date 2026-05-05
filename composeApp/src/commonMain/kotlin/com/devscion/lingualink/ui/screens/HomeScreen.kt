package com.devscion.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.model.SessionType
import com.devscion.lingualink.data.model.SupportedLanguages
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.pipeline.TranslationPipeline
import com.devscion.lingualink.ui.theme.LinguaLinkColors
import kotlinx.coroutines.launch

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = LinguaLinkColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LinguaLink",
                    style = MaterialTheme.typography.titleLarge,
                    color = LinguaLinkColors.TextPrimary
                )
                TextButton(onClick = onSettings) {
                    Text("Settings", color = LinguaLinkColors.TextSecondary)
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "TRANSLATION DIRECTION",
                style = MaterialTheme.typography.labelSmall,
                color = LinguaLinkColors.TextSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                LanguageDropdown(
                    selected = vm.sourceLang,
                    onSelected = { vm.sourceLang = it },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { vm.swapLanguages() }) {
                    Text("⇄", color = LinguaLinkColors.Primary, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(8.dp))
                LanguageDropdown(
                    selected = vm.targetLang,
                    onSelected = { vm.targetLang = it },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(32.dp))

            ModeCard(
                icon = "🎙",
                title = "Voice Call Translation",
                subtitle = "Real-time mic → translated speech",
                buttonText = "Start Call",
                onClick = {
                    if (vm.sourceLang == vm.targetLang) {
                        scope.launch { snackbarHostState.showSnackbar("Source and target language must differ") }
                    } else {
                        vm.createCallSession { id -> onStartCall(id, vm.sourceLang, vm.targetLang) }
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            ModeCard(
                icon = "💬",
                title = "Chat Translation",
                subtitle = "Type messages, get instant translation",
                buttonText = "Open Chat",
                onClick = {
                    if (vm.sourceLang == vm.targetLang) {
                        scope.launch { snackbarHostState.showSnackbar("Source and target language must differ") }
                    } else {
                        vm.createChatSession { id -> onOpenChat(id, vm.sourceLang, vm.targetLang) }
                    }
                }
            )

            Spacer(Modifier.height(24.dp))

            TextButton(
                onClick = onHistory,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("📋 View History", color = LinguaLinkColors.TextSecondary)
            }
        }
    }
}

@Composable
private fun LanguageDropdown(selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val lang = SupportedLanguages.find { it.code == selected } ?: SupportedLanguages.first()

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = LinguaLinkColors.TextPrimary)
        ) {
            Text("${lang.name} ▼")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(LinguaLinkColors.Surface)
        ) {
            SupportedLanguages.forEach { l ->
                DropdownMenuItem(
                    text = { Text(l.name, color = LinguaLinkColors.TextPrimary) },
                    onClick = { onSelected(l.code); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun ModeCard(
    icon: String,
    title: String,
    subtitle: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Surface(
        color = LinguaLinkColors.Surface,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = LinguaLinkColors.TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = LinguaLinkColors.TextSecondary)
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = onClick) { Text(buttonText) }
        }
    }
}
