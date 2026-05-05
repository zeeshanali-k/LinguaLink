package com.devscion.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.network.LlmClient
import com.devscion.lingualink.network.TtsClient
import com.devscion.lingualink.ui.theme.LinguaLinkColors
import kotlinx.coroutines.launch

class SetupViewModel(
    private val llmClient: LlmClient,
    private val ttsClient: TtsClient,
    private val configManager: ConfigManager
) : ViewModel() {

    var dropletUrl by mutableStateOf("")
    var deepgramKey by mutableStateOf("")
    var elevenLabsKey by mutableStateOf("")
    var testStatus by mutableStateOf<String?>(null)
    var isTesting by mutableStateOf(false)
    var isSaving by mutableStateOf(false)

    val canSave get() = dropletUrl.isNotBlank() && deepgramKey.isNotBlank()

    init {
        val cfg = configManager.load()
        if (cfg != null) {
            dropletUrl = cfg.amdDropletBaseUrl
            deepgramKey = cfg.deepgramApiKey
            elevenLabsKey = cfg.elevenlabsApiKey
        }
    }

    fun testConnection() {
        isTesting = true
        testStatus = null
        viewModelScope.launch {
            try {
                llmClient.configure(dropletUrl)
                val response = llmClient.chat("ping", "Reply with the single word: pong")
                testStatus = if (response.isNotBlank()) "ok" else "error: empty response"
            } catch (e: Exception) {
                testStatus = "error: ${e.message}"
            } finally {
                isTesting = false
            }
        }
    }

    fun save(onSuccess: () -> Unit) {
        isSaving = true
        viewModelScope.launch {
            val config = ConfigManager.AppConfig(
                amdDropletBaseUrl = dropletUrl,
                deepgramApiKey = deepgramKey,
                elevenlabsApiKey = elevenLabsKey
            )
            configManager.save(config)
            llmClient.configure(dropletUrl)
            ttsClient.configure(elevenLabsKey)
            isSaving = false
            onSuccess()
        }
    }
}

@Composable
fun SetupScreen(vm: SetupViewModel, onSaved: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(LinguaLinkColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "LinguaLink",
                style = MaterialTheme.typography.headlineLarge,
                color = LinguaLinkColors.Primary
            )
            Text(
                "Real-time AI Voice Translation",
                style = MaterialTheme.typography.bodyLarge,
                color = LinguaLinkColors.TextSecondary
            )

            Spacer(Modifier.height(8.dp))

            ConfigField(
                label = "AMD Droplet Base URL *",
                value = vm.dropletUrl,
                onValueChange = { vm.dropletUrl = it },
                placeholder = "http://192.168.x.x:8000",
                isPassword = false
            )

            ConfigField(
                label = "Deepgram API Key *",
                value = vm.deepgramKey,
                onValueChange = { vm.deepgramKey = it },
                placeholder = "Your Deepgram key",
                isPassword = true
            )

            ConfigField(
                label = "ElevenLabs API Key (optional — TTS)",
                value = vm.elevenLabsKey,
                onValueChange = { vm.elevenLabsKey = it },
                placeholder = "Leave blank to disable voice output",
                isPassword = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = { vm.testConnection() },
                    enabled = vm.canSave && !vm.isTesting,
                    modifier = Modifier.weight(1f)
                ) {
                    if (vm.isTesting) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Test Connection")
                }

                Button(
                    onClick = { vm.save(onSaved) },
                    enabled = vm.canSave && !vm.isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    if (vm.isSaving) {
                        CircularProgressIndicator(
                            Modifier.size(16.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Save & Continue")
                }
            }

            vm.testStatus?.let { status ->
                val isOk = status == "ok"
                val color = if (isOk) LinguaLinkColors.Success else LinguaLinkColors.Error
                val msg = if (isOk) "Connected to droplet" else status
                Text(msg, color = color, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = LinguaLinkColors.TextSecondary)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder, color = LinguaLinkColors.TextSecondary) },
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else KeyboardType.Uri),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LinguaLinkColors.Primary,
                unfocusedBorderColor = LinguaLinkColors.Divider,
                focusedTextColor = LinguaLinkColors.TextPrimary,
                unfocusedTextColor = LinguaLinkColors.TextPrimary,
                cursorColor = LinguaLinkColors.Primary
            )
        )
    }
}
