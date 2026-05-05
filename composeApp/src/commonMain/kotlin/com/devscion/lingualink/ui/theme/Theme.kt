package com.devscion.lingualink.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object LinguaLinkColors {
    val Background     = Color(0xFF0A0C14)
    val Surface        = Color(0xFF12151F)
    val SurfaceVariant = Color(0xFF1A1E2E)
    val Primary        = Color(0xFF4F8EF7)
    val PrimaryVariant = Color(0xFF2D5FC4)
    val Accent         = Color(0xFF7C4DFF)
    val UserA          = Color(0xFF4F8EF7)
    val UserB          = Color(0xFF1DB97F)
    val Error          = Color(0xFFFF4D4D)
    val Success        = Color(0xFF1DB97F)
    val TextPrimary    = Color(0xFFF0F2FF)
    val TextSecondary  = Color(0xFF8A8FA8)
    val Divider        = Color(0xFF252A3D)
}

private val LinguaLinkColorScheme = darkColorScheme(
    primary          = LinguaLinkColors.Primary,
    onPrimary        = Color.White,
    background       = LinguaLinkColors.Background,
    onBackground     = LinguaLinkColors.TextPrimary,
    surface          = LinguaLinkColors.Surface,
    onSurface        = LinguaLinkColors.TextPrimary,
    surfaceVariant   = LinguaLinkColors.SurfaceVariant,
    onSurfaceVariant = LinguaLinkColors.TextSecondary,
    error            = LinguaLinkColors.Error,
    onError          = Color.White
)

@Composable
fun LinguaLinkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LinguaLinkColorScheme,
        typography = LinguaLinkTypography,
        content = content
    )
}
