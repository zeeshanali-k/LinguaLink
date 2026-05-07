package com.devscion.lingualink.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.model.SupportedLanguages
import com.devscion.lingualink.data.model.languageByCode
import com.devscion.lingualink.ui.theme.AmbientMeshBackground
import com.devscion.lingualink.ui.theme.BrandMark
import com.devscion.lingualink.ui.theme.BrandWordmark
import com.devscion.lingualink.ui.theme.Chip
import com.devscion.lingualink.ui.theme.ChipKind
import com.devscion.lingualink.ui.theme.GlassCard
import com.devscion.lingualink.ui.theme.GlowDivider
import com.devscion.lingualink.ui.theme.IconBtn
import com.devscion.lingualink.ui.theme.LL
import com.devscion.lingualink.ui.theme.MonoFamily
import com.devscion.lingualink.ui.theme.SharedKeys
import com.devscion.lingualink.ui.theme.sharedBoundsAcrossScreens

@Composable
fun SettingsScreen(
    configManager: ConfigManager,
    onBack: () -> Unit,
    onEditApiConfig: () -> Unit,
) {
    val cfg = remember { configManager.load() ?: ConfigManager.AppConfig() }
    val src = languageByCode(cfg.sourceLanguage)
    val tgt = languageByCode(cfg.targetLanguage)

    var voiceClone by remember { mutableStateOf(false) }
    var autoLang by remember { mutableStateOf(true) }
    var endToEnd by remember { mutableStateOf(true) }
    var recordCalls by remember { mutableStateOf(true) }
    var model by remember { mutableStateOf("haiku-4-5") }

    Box(modifier = Modifier.fillMaxSize()) {
        AmbientMeshBackground(modifier = Modifier.fillMaxSize())

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val compact = maxWidth < 760.dp
            val veryCompact = maxWidth < 460.dp

            Column(modifier = Modifier.fillMaxSize()) {
                SettingsTopbar(
                    sourceCode = src.code.uppercase(),
                    targetCode = tgt.code.uppercase(),
                    onBack = onBack,
                    compact = compact,
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            horizontal = if (veryCompact) 14.dp else if (compact) 18.dp else 28.dp,
                            vertical = 16.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SettingsHeader()

                    val cards: List<@Composable () -> Unit> = listOf(
                        {
                            SettingsCard(
                                icon = Icons.Default.Memory,
                                iconAccent = LL.tokens.cyan,
                                title = "Translation model",
                                description = "Bigger models = better nuance, more latency.",
                            ) {
                                ModelPicker(selected = model, onSelect = { model = it })
                                SettingsRow(label = "Streaming output", value = "word-by-word")
                                SettingsRow(label = "Avg. latency", value = "184ms", valueAccent = LL.tokens.cyan)
                            }
                        },
                        {
                            SettingsCard(
                                icon = Icons.Default.VolumeUp,
                                iconAccent = LL.tokens.violet,
                                title = "Voice & audio",
                                description = "How translated speech sounds in calls.",
                            ) {
                                ToggleRow(label = "Voice cloning", checked = voiceClone, onChange = { voiceClone = it })
                                ToggleRow(label = "Auto-detect language", checked = autoLang, onChange = { autoLang = it })
                                SettingsRow(label = "Voice profile", value = "Maya · trained 4 May")
                            }
                        },
                        {
                            SettingsCard(
                                icon = Icons.Default.Shield,
                                iconAccent = LL.tokens.green,
                                title = "Privacy",
                                description = "Your conversations stay yours.",
                            ) {
                                ToggleRow(label = "End-to-end encryption", checked = endToEnd, onChange = { endToEnd = it })
                                ToggleRow(label = "Record & summarize calls", checked = recordCalls, onChange = { recordCalls = it })
                                SettingsRow(label = "Train on my data", value = "never", valueAccent = LL.tokens.text3)
                            }
                        },
                        {
                            SettingsCard(
                                icon = Icons.Default.Language,
                                iconAccent = LL.tokens.amber,
                                title = "Languages",
                                description = "Default pair and quick-switch presets.",
                            ) {
                                SettingsRow(label = "My language", value = tgt.name, valueAccent = LL.tokens.cyan)
                                SettingsRow(label = "Their language", value = src.name, valueAccent = LL.tokens.violet)
                                SettingsRow(label = "Formality", value = "auto (match speaker)")
                                SettingsRow(label = "Custom glossary", value = "${SupportedLanguages.size} terms")
                            }
                        },
                    )
                    SettingsCardsGrid(cards = cards, compact = compact)

                    // API config entry
                    ApiConfigEntry(onClick = onEditApiConfig)

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ───────────────────────────── Topbar ─────────────────────────────

@Composable
private fun SettingsTopbar(
    sourceCode: String,
    targetCode: String,
    onBack: () -> Unit,
    compact: Boolean,
) {
    val t = LL.tokens
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(t.violet.copy(alpha = 0.025f), Color.Transparent))
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
                    "PREFERENCES",
                    color = t.text3,
                    fontFamily = MonoFamily,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                )
                Text("Settings", color = t.text0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(8.dp))
            Chip(
                text = "$sourceCode → $targetCode",
                kind = ChipKind.Violet,
                leading = Icons.Default.Language,
                modifier = Modifier.sharedBoundsAcrossScreens(SharedKeys.LANG_PILL),
            )
        }
        GlowDivider(modifier = Modifier.fillMaxWidth())
    }
}

// ───────────────────────────── Header ─────────────────────────────

@Composable
private fun SettingsHeader() {
    val t = LL.tokens
    Column {
        Text(
            "Tune the translator",
            color = t.text0,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        )
        Text(
            "Pick a model, set voice and privacy defaults, lock in your everyday language pair.",
            color = t.text2,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

// ───────────────────────────── Cards grid ─────────────────────────────

@Composable
private fun SettingsCardsGrid(cards: List<@Composable () -> Unit>, compact: Boolean) {
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cards.forEach { it() }
        }
    } else {
        // Two columns of cards on desktop; alternate cards into left vs right column.
        val left = cards.filterIndexed { i, _ -> i % 2 == 0 }
        val right = cards.filterIndexed { i, _ -> i % 2 == 1 }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                left.forEach { it() }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                right.forEach { it() }
            }
        }
    }
}

// ───────────────────────────── Settings card ─────────────────────────────

@Composable
private fun SettingsCard(
    icon: ImageVector,
    iconAccent: Color,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val t = LL.tokens
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(iconAccent.copy(alpha = 0.18f), t.violet.copy(alpha = 0.12f))
                            )
                        )
                        .border(1.dp, iconAccent.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, null, tint = iconAccent, modifier = Modifier.size(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = t.text0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(description, color = t.text2, fontSize = 12.5.sp, lineHeight = 17.sp)
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String, valueAccent: Color = LL.tokens.text2) {
    val t = LL.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = t.text1, fontSize = 13.sp)
        Text(value, color = valueAccent, fontFamily = MonoFamily, fontSize = 11.5.sp, letterSpacing = 0.4.sp)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val t = LL.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = t.text1, fontSize = 13.sp)
        GradientToggle(checked = checked, onChange = onChange)
    }
}

@Composable
private fun GradientToggle(checked: Boolean, onChange: (Boolean) -> Unit) {
    val t = LL.tokens
    Box(
        modifier = Modifier
            .size(width = 36.dp, height = 20.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(if (checked) t.brandGradientHorizontal else Brush.linearGradient(listOf(Color.White.copy(alpha = if (t.isDark) 0.08f else 0.12f), Color.White.copy(alpha = if (t.isDark) 0.08f else 0.12f))))
            .clickable { onChange(!checked) }
            .padding(2.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

@Composable
private fun ModelPicker(selected: String, onSelect: (String) -> Unit) {
    val models = listOf("haiku-4-5", "sonnet-4-5", "opus-4-5", "on-device")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        models.forEach { m ->
            ModelOption(name = m, selected = selected == m, onClick = { onSelect(m) })
        }
    }
}

@Composable
private fun ModelOption(name: String, selected: Boolean, onClick: () -> Unit) {
    val t = LL.tokens
    val bg = if (selected) t.cyan.copy(alpha = 0.08f) else Color.White.copy(alpha = if (t.isDark) 0.03f else 0.0f)
    val bd = if (selected) t.cyan.copy(alpha = 0.40f) else t.border
    val fg = if (selected) t.cyan else t.text1
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, bd, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(name, color = fg, fontFamily = MonoFamily, fontSize = 11.sp, letterSpacing = 0.4.sp)
    }
}

// ───────────────────────────── API config entry ─────────────────────────────

@Composable
private fun ApiConfigEntry(onClick: () -> Unit) {
    val t = LL.tokens
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(t.amber.copy(alpha = 0.18f), t.violet.copy(alpha = 0.12f))
                        )
                    )
                    .border(1.dp, t.amber.copy(alpha = 0.30f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Key, null, tint = t.amber, modifier = Modifier.size(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("API configuration", color = t.text0, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    "Update the Fireworks AI endpoint and Deepgram key.",
                    color = t.text2,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = t.text2, modifier = Modifier.size(20.dp))
        }
    }
}
