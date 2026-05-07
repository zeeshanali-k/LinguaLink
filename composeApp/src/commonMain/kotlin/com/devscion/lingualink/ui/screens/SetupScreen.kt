package com.devscion.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.network.LlmClient
import com.devscion.lingualink.network.TtsClient
import com.devscion.lingualink.ui.theme.AmbientMeshBackground
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
import com.devscion.lingualink.ui.theme.SharedKeys
import com.devscion.lingualink.ui.theme.sharedBoundsAcrossScreens
import kotlinx.coroutines.launch

class SetupViewModel(
    private val llmClient: LlmClient,
    private val ttsClient: TtsClient,
    private val configManager: ConfigManager
) : ViewModel() {

    private val defaultConfig = ConfigManager.AppConfig()

    var llmBaseUrl by mutableStateOf(defaultConfig.llmBaseUrl)
    var llmApiKey by mutableStateOf("")
    var llmModel by mutableStateOf(defaultConfig.llmModel)
    var deepgramKey by mutableStateOf("")
    var testStatus by mutableStateOf<String?>(null)
    var isTesting by mutableStateOf(false)
    var isSaving by mutableStateOf(false)

    val canSave
        get() = llmBaseUrl.isNotBlank() &&
                llmApiKey.isNotBlank() &&
                llmModel.isNotBlank() &&
                deepgramKey.isNotBlank()

    init {
        val cfg = configManager.load()
        if (cfg != null) {
            llmBaseUrl = cfg.llmBaseUrl.ifBlank { defaultConfig.llmBaseUrl }
            llmApiKey = cfg.llmApiKey
            llmModel = cfg.llmModel.ifBlank { defaultConfig.llmModel }
            deepgramKey = cfg.deepgramApiKey
        }
    }

    fun testConnection() {
        isTesting = true
        testStatus = null
        viewModelScope.launch {
            try {
                llmClient.configure(llmBaseUrl, llmApiKey, llmModel)
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
                llmBaseUrl = llmBaseUrl,
                llmApiKey = llmApiKey,
                llmModel = llmModel,
                deepgramApiKey = deepgramKey
            )
            configManager.save(config)
            llmClient.configure(llmBaseUrl, llmApiKey, llmModel)
            ttsClient.configure(deepgramKey)
            isSaving = false
            onSuccess()
        }
    }
}

// ───────────────────────────── Screen ─────────────────────────────

@Composable
fun SetupScreen(vm: SetupViewModel, onSaved: () -> Unit, onBack: (() -> Unit)? = null) {
    val t = LL.tokens

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp
            val veryCompact = maxWidth < 460.dp

            Column(modifier = Modifier.fillMaxSize()) {
                SetupTopbar(onBack = onBack, compact = compact)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = if (veryCompact) 14.dp else if (compact) 18.dp else 28.dp,
                            vertical = 24.dp,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    SetupHero(compact = compact)

                    GlassCard(
                        modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth(),
                        contentPadding = PaddingValues(if (compact) 18.dp else 24.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            FieldGroup(
                                icon = Icons.Default.Router,
                                accent = LL.tokens.cyan,
                                label = "Fireworks AI base URL",
                                hint = "Default works for the AMD-hosted Fireworks endpoint.",
                                required = true,
                            ) {
                                ConfigField(
                                    value = vm.llmBaseUrl,
                                    onValueChange = { vm.llmBaseUrl = it },
                                    placeholder = "https://api.fireworks.ai/inference",
                                    keyboardType = KeyboardType.Uri,
                                    isPassword = false,
                                )
                            }

                            FieldGroup(
                                icon = Icons.Default.Key,
                                accent = LL.tokens.cyan,
                                label = "Fireworks AI API key",
                                hint = "Bearer token from your Fireworks AI account.",
                                required = true,
                            ) {
                                ConfigField(
                                    value = vm.llmApiKey,
                                    onValueChange = { vm.llmApiKey = it },
                                    placeholder = "fw_…",
                                    keyboardType = KeyboardType.Password,
                                    isPassword = true,
                                )
                            }

                            FieldGroup(
                                icon = Icons.Default.AutoAwesome,
                                accent = LL.tokens.violet,
                                label = "Fireworks model",
                                hint = "Full model path, e.g. accounts/fireworks/models/llama-v3p1-8b-instruct.",
                                required = true,
                            ) {
                                ConfigField(
                                    value = vm.llmModel,
                                    onValueChange = { vm.llmModel = it },
                                    placeholder = "accounts/fireworks/models/…",
                                    keyboardType = KeyboardType.Uri,
                                    isPassword = false,
                                )
                            }

                            FieldGroup(
                                icon = Icons.Default.Mic,
                                accent = LL.tokens.violet,
                                label = "Deepgram API key",
                                hint = "Powers both speech-to-text and translated voice playback.",
                                required = true,
                            ) {
                                ConfigField(
                                    value = vm.deepgramKey,
                                    onValueChange = { vm.deepgramKey = it },
                                    placeholder = "Your Deepgram key",
                                    keyboardType = KeyboardType.Password,
                                    isPassword = true,
                                )
                            }

                            Spacer(Modifier.height(2.dp))

                            // Actions
                            if (compact) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    GradientButton(
                                        text = if (vm.isSaving) "Saving…" else "Save & continue",
                                        onClick = { vm.save(onSaved) },
                                        leading = Icons.Default.AutoAwesome,
                                        enabled = vm.canSave && !vm.isSaving,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    GhostButton(
                                        text = if (vm.isTesting) "Testing…" else "Test connection",
                                        onClick = { vm.testConnection() },
                                        leading = Icons.Default.Key,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    GhostButton(
                                        text = if (vm.isTesting) "Testing…" else "Test connection",
                                        onClick = { vm.testConnection() },
                                        leading = Icons.Default.Key,
                                        modifier = Modifier.weight(1f),
                                    )
                                    GradientButton(
                                        text = if (vm.isSaving) "Saving…" else "Save & continue",
                                        onClick = { vm.save(onSaved) },
                                        leading = Icons.Default.AutoAwesome,
                                        enabled = vm.canSave && !vm.isSaving,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                            // Status
                            vm.testStatus?.let { status -> StatusLine(status = status) }
                        }
                    }

                    SetupFooter()
                }
            }
        }
    }
}

// ───────────────────────────── Topbar ─────────────────────────────

@Composable
private fun SetupTopbar(onBack: (() -> Unit)?, compact: Boolean) {
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
            if (onBack != null) {
                IconBtn(icon = Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack, contentDescription = "Back")
            }

            Box(modifier = Modifier.sharedBoundsAcrossScreens(SharedKeys.BRAND_MARK)) {
                BrandMark(size = 28.dp)
            }
            Spacer(Modifier.width(2.dp))
            BrandWordmark(size = 16.dp)
            Spacer(Modifier.weight(1f))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "GET STARTED",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                )
                Text("First-run setup", color = t.text0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        GlowDivider(modifier = Modifier.fillMaxWidth())
    }
}

// ───────────────────────────── Hero ─────────────────────────────

@Composable
private fun SetupHero(compact: Boolean) {
    val t = LL.tokens
    val title = buildAnnotatedString {
        withStyle(SpanStyle(color = t.text0)) { append("Welcome to ") }
        withStyle(
            SpanStyle(
                brush = t.brandGradientHorizontal,
                fontWeight = FontWeight.SemiBold,
            )
        ) { append("LinguaLink") }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            title,
            fontSize = if (compact) 26.sp else 32.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
        Text(
            "Drop in your translation endpoint and API keys to bring the orb online.",
            color = t.text2,
            fontSize = if (compact) 13.sp else 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.widthIn(max = 520.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SetupFooter() {
    val t = LL.tokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf("Stored locally", "Never logged", "Edit anytime").forEachIndexed { i, item ->
            if (i > 0) Spacer(Modifier.width(18.dp))
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

// ───────────────────────────── Field group ─────────────────────────────

@Composable
private fun FieldGroup(
    icon: ImageVector,
    accent: Color,
    label: String,
    hint: String,
    required: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LL.tokens
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.18f), t.violet.copy(alpha = 0.08f))
                        )
                    )
                    .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(label, color = t.text0, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                    if (required) {
                        Text("REQUIRED", color = accent, fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    } else {
                        Text("OPTIONAL", color = t.text3, fontFamily = MonoFamily, fontSize = 9.sp, letterSpacing = 1.2.sp)
                    }
                }
                Text(hint, color = t.text2, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        content()
    }
}

// ───────────────────────────── Field ─────────────────────────────

@Composable
private fun ConfigField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isPassword: Boolean,
) {
    val t = LL.tokens
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = t.text3) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp, color = t.text0),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = t.cyan.copy(alpha = 0.50f),
            unfocusedBorderColor = t.border,
            focusedTextColor = t.text0,
            unfocusedTextColor = t.text0,
            cursorColor = t.cyan,
            focusedContainerColor = Color.White.copy(alpha = if (t.isDark) 0.03f else 0.0f),
            unfocusedContainerColor = Color.White.copy(alpha = if (t.isDark) 0.03f else 0.0f),
        ),
    )
}

// ───────────────────────────── Status line ─────────────────────────────

@Composable
private fun StatusLine(status: String) {
    val t = LL.tokens
    val isOk = status == "ok"
    val color = if (isOk) t.green else t.red
    val message = if (isOk) "Connected — translations are ready to flow." else status
    val icon = if (isOk) Icons.Default.CheckCircle else Icons.Default.ErrorOutline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Text(message, color = color, fontSize = 12.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
    }
}
