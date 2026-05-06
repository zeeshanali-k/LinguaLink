package com.devscion.lingualink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ───────────────────────────── Tokens ─────────────────────────────

@Immutable
data class LinguaLinkTokens(
    val isDark: Boolean,
    // Surfaces
    val bg0: Color,
    val bg1: Color,
    val bg2: Color,
    val bg3: Color,
    val surface: Color,
    val surface2: Color,
    val border: Color,
    val borderStrong: Color,
    // Text
    val text0: Color,
    val text1: Color,
    val text2: Color,
    val text3: Color,
    // Accents
    val cyan: Color,
    val cyanGlow: Color,
    val violet: Color,
    val violetGlow: Color,
    val magenta: Color,
    // Status
    val green: Color,
    val amber: Color,
    val red: Color,
) {
    val brandGradient: Brush
        get() = Brush.linearGradient(listOf(cyan, violet))
    val brandGradientHorizontal: Brush
        get() = Brush.horizontalGradient(listOf(cyan, violet))
    val brandGradientVertical: Brush
        get() = Brush.verticalGradient(listOf(cyan, violet))
}

private val DarkTokens = LinguaLinkTokens(
    isDark = true,
    bg0 = Color(0xFF07080D),
    bg1 = Color(0xFF0B0D15),
    bg2 = Color(0xFF11141F),
    bg3 = Color(0xFF171B29),
    surface = Color(0x8C141826),       // rgba(20,24,38,0.55)
    surface2 = Color(0xB31C2134),      // rgba(28,33,52,0.7)
    border = Color(0x14FFFFFF),        // rgba(255,255,255,0.08)
    borderStrong = Color(0x24FFFFFF),  // rgba(255,255,255,0.14)
    text0 = Color(0xFFF3F5FB),
    text1 = Color(0xFFC8CEE0),
    text2 = Color(0xFF8B93AD),
    text3 = Color(0xFF5B6280),
    cyan = Color(0xFF5CF2E8),
    cyanGlow = Color(0x8C5CF2E8),
    violet = Color(0xFFA98BFF),
    violetGlow = Color(0x8CA98BFF),
    magenta = Color(0xFFFF7AD9),
    green = Color(0xFF5AF2A8),
    amber = Color(0xFFFFC26B),
    red = Color(0xFFFF6B8A),
)

private val LightTokens = LinguaLinkTokens(
    isDark = false,
    bg0 = Color(0xFFF4F5F9),
    bg1 = Color(0xFFECEEF5),
    bg2 = Color(0xFFFFFFFF),
    bg3 = Color(0xFFF8F9FC),
    surface = Color(0xB3FFFFFF),       // rgba(255,255,255,0.7)
    surface2 = Color(0xD9FFFFFF),      // rgba(255,255,255,0.85)
    border = Color(0x140F1428),
    borderStrong = Color(0x290F1428),
    text0 = Color(0xFF0D1224),
    text1 = Color(0xFF2D3450),
    text2 = Color(0xFF5B6480),
    text3 = Color(0xFF8B93AD),
    cyan = Color(0xFF12B5A8),
    cyanGlow = Color(0x6612B5A8),
    violet = Color(0xFF7C5CFF),
    violetGlow = Color(0x807C5CFF),
    magenta = Color(0xFFE062B8),
    green = Color(0xFF1DB97F),
    amber = Color(0xFFE0942F),
    red = Color(0xFFE2435F),
)

val LocalLinguaLinkTokens = staticCompositionLocalOf { DarkTokens }
val LocalIsCompactWidth = compositionLocalOf { false }

object LL {
    val tokens: LinguaLinkTokens
        @Composable get() = LocalLinguaLinkTokens.current
    val isCompactWidth: Boolean
        @Composable get() = LocalIsCompactWidth.current
}

// Legacy alias kept so older screens still compile while we migrate.
@Deprecated("Use LL.tokens instead", ReplaceWith("LL.tokens"))
object LinguaLinkColors {
    val Background     get() = DarkTokens.bg0
    val Surface        get() = DarkTokens.bg2
    val SurfaceVariant get() = DarkTokens.bg3
    val Primary        get() = DarkTokens.cyan
    val Accent         get() = DarkTokens.violet
    val UserA          get() = DarkTokens.cyan
    val UserB          get() = DarkTokens.violet
    val Error          get() = DarkTokens.red
    val Success        get() = DarkTokens.green
    val TextPrimary    get() = DarkTokens.text0
    val TextSecondary  get() = DarkTokens.text2
    val Divider        get() = DarkTokens.border
}

// ───────────────────────────── Material3 wiring ─────────────────────────────

private fun materialDark(t: LinguaLinkTokens) = darkColorScheme(
    primary       = t.cyan,
    onPrimary     = t.bg0,
    secondary     = t.violet,
    onSecondary   = t.bg0,
    background    = t.bg0,
    onBackground  = t.text0,
    surface       = t.bg2,
    onSurface     = t.text0,
    surfaceVariant   = t.bg3,
    onSurfaceVariant = t.text2,
    outline       = t.border,
    error         = t.red,
    onError       = t.bg0,
)

private fun materialLight(t: LinguaLinkTokens) = lightColorScheme(
    primary       = t.cyan,
    onPrimary     = Color.White,
    secondary     = t.violet,
    onSecondary   = Color.White,
    background    = t.bg0,
    onBackground  = t.text0,
    surface       = t.bg2,
    onSurface     = t.text0,
    surfaceVariant   = t.bg3,
    onSurfaceVariant = t.text2,
    outline       = t.border,
    error         = t.red,
    onError       = Color.White,
)

@Composable
fun LinguaLinkTheme(
    darkTheme: Boolean = true,
    isCompactWidth: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (darkTheme) DarkTokens else LightTokens
    CompositionLocalProvider(
        LocalLinguaLinkTokens provides tokens,
        LocalIsCompactWidth provides isCompactWidth,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) materialDark(tokens) else materialLight(tokens),
            typography = LinguaLinkTypography,
            content = content
        )
    }
}
