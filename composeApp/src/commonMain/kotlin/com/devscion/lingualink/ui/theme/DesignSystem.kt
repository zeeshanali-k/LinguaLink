package com.devscion.lingualink.ui.theme

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.absoluteValue
import kotlin.math.sin
import kotlin.random.Random

// ───────────────────────────── Background ─────────────────────────────

@Composable
fun AmbientMeshBackground(modifier: Modifier = Modifier) {
    val t = LL.tokens
    Canvas(modifier = modifier) {
        // Base wash
        drawRect(brush = Brush.verticalGradient(listOf(t.bg0, t.bg1, t.bg0)))
        // Cyan radial top-left
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(t.cyan.copy(alpha = if (t.isDark) 0.18f else 0.22f), Color.Transparent),
                center = Offset(size.width * 0.12f, size.height * 0.18f),
                radius = maxOf(size.width, size.height) * 0.55f,
            )
        )
        // Violet radial bottom-right
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(t.violet.copy(alpha = if (t.isDark) 0.20f else 0.22f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.82f),
                radius = maxOf(size.width, size.height) * 0.55f,
            )
        )
        // Magenta accent top-right
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(t.magenta.copy(alpha = if (t.isDark) 0.10f else 0.10f), Color.Transparent),
                center = Offset(size.width * 0.6f, size.height * 0.10f),
                radius = maxOf(size.width, size.height) * 0.42f,
            )
        )
    }
}

// ───────────────────────────── Surfaces ─────────────────────────────

/**
 * Glass surface — translucent fill + subtle border. True backdrop blur isn't
 * portable in Compose Multiplatform, so we lean on the layered radial mesh
 * underneath plus a high-alpha translucent fill to evoke the same depth.
 */
fun Modifier.glass(
    tokens: LinguaLinkTokens,
    shape: Shape = RoundedCornerShape(18.dp),
    strong: Boolean = false,
): Modifier = this
    .clip(shape)
    .background(if (strong) tokens.surface2 else tokens.surface, shape)
    .border(1.dp, if (strong) tokens.borderStrong else tokens.border, shape)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
    strong: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit,
) {
    val t = LL.tokens
    Box(
        modifier = modifier
            .glass(t, shape, strong)
            .padding(contentPadding)
    ) { content() }
}

// ───────────────────────────── Brand mark ─────────────────────────────

@Composable
fun BrandMark(size: Dp = 28.dp) {
    val t = LL.tokens
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.sweepGradient(
                    listOf(t.cyan, t.violet, t.magenta, t.cyan),
                )
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding((size.value * 0.14f).dp)
                .clip(RoundedCornerShape((size.value * 0.18f).dp))
                .background(t.bg1),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(size * 0.42f)
                    .clip(CircleShape)
                    .background(t.brandGradient)
            )
        }
    }
}

@Composable
fun BrandWordmark(size: Dp = 16.dp) {
    val t = LL.tokens
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Lingua",
            color = t.text0,
            fontSize = size.value.sp,
            fontWeight = FontWeight.SemiBold,
        )
        // Apply gradient via a Box that draws the same text masked by the gradient.
        // Simpler: use a colored second token; gradient text on KMP requires graphicsLayer tricks.
        Text(
            "Link",
            color = t.cyan,
            fontSize = size.value.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

// ───────────────────────────── Chips & pills ─────────────────────────────

enum class ChipKind { Neutral, Cyan, Violet, Live }

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    kind: ChipKind = ChipKind.Neutral,
    leading: ImageVector? = null,
) {
    val t = LL.tokens
    val (fg, bg, bd) = when (kind) {
        ChipKind.Cyan -> Triple(t.cyan, t.cyan.copy(alpha = 0.06f), t.cyan.copy(alpha = 0.25f))
        ChipKind.Violet -> Triple(t.violet, t.violet.copy(alpha = 0.06f), t.violet.copy(alpha = 0.25f))
        ChipKind.Live -> Triple(t.red, t.red.copy(alpha = 0.08f), t.red.copy(alpha = 0.30f))
        ChipKind.Neutral -> Triple(t.text1, Color.White.copy(alpha = if (t.isDark) 0.04f else 0.0f), t.border)
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, bd, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (kind == ChipKind.Live) {
            PulseDot(color = t.red, size = 6.dp)
        }
        if (leading != null) {
            Icon(leading, null, tint = fg, modifier = Modifier.size(11.dp))
        }
        Text(
            text,
            color = fg,
            fontSize = 10.5.sp,
            fontFamily = MonoFamily,
            letterSpacing = 0.4.sp,
        )
    }
}

// ───────────────────────────── Pulse / live indicators ─────────────────────────────

@Composable
fun PulseDot(color: Color, size: Dp = 7.dp) {
    val transition = rememberInfiniteTransition(label = "pulseDot")
    val scale by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun LiveIndicator(timer: String) {
    val t = LL.tokens
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(t.red.copy(alpha = 0.08f))
            .border(1.dp, t.red.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PulseDot(color = t.red, size = 7.dp)
        Text("LIVE", color = t.red, fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Medium)
        Text("·", color = t.text3, fontSize = 10.sp)
        Text(timer, color = t.text1, fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 0.8.sp)
    }
}

@Composable
fun ModelPill(text: String, latencyMs: Int? = null, leading: ImageVector? = null) {
    val t = LL.tokens
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(t.cyan.copy(alpha = 0.06f))
            .border(1.dp, t.cyan.copy(alpha = 0.20f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) Icon(leading, null, tint = t.cyan, modifier = Modifier.size(11.dp))
        Text(text, color = t.cyan, fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 1.0.sp)
        if (latencyMs != null) {
            Text("·", color = t.text3.copy(alpha = 0.5f), fontSize = 10.sp)
            Text("${latencyMs}ms", color = t.text1, fontFamily = MonoFamily, fontSize = 10.sp, letterSpacing = 0.8.sp)
        }
    }
}

// ───────────────────────────── Mini waveform ─────────────────────────────

@Composable
fun MiniWave(
    bars: Int = 14,
    height: Dp = 18.dp,
    modifier: Modifier = Modifier,
) {
    val t = LL.tokens
    val phases = remember(bars) { List(bars) { Random.nextFloat() * 1.2f } }
    val transition = rememberInfiniteTransition(label = "miniWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    Row(
        modifier = modifier.height(height),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        for (i in 0 until bars) {
            val v = (sin((phase + phases[i]) * 6.28f) * 0.5f + 0.5f).absoluteValue
            val h = (0.30f + v * 0.70f).coerceIn(0.10f, 1f)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(height * h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(t.brandGradientVertical)
            )
        }
    }
}

// ───────────────────────────── Live waveform (large) ─────────────────────────────

enum class Hue { Cyan, Violet }

@Composable
fun LiveWaveform(
    modifier: Modifier = Modifier,
    bars: Int = 56,
    speaking: Boolean = true,
    hue: Hue = Hue.Cyan,
) {
    val t = LL.tokens
    val phases = remember(bars) { List(bars) { Random.nextFloat() * 1.2f } }
    val periods = remember(bars) { List(bars) { 0.9f + Random.nextFloat() * 0.6f } }
    val transition = rememberInfiniteTransition(label = "liveWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val (cTop, cBot) = when (hue) {
        Hue.Cyan -> t.cyan to t.violet
        Hue.Violet -> t.violet to t.magenta
    }
    Canvas(modifier = modifier) {
        val gap = 3f
        val barWidth = ((size.width - gap * (bars - 1)) / bars).coerceAtLeast(1f)
        for (i in 0 until bars) {
            val v = (sin((phase + phases[i]) * 6.28f / periods[i]) * 0.5f + 0.5f).absoluteValue
            val h = if (speaking) (0.18f + v * 0.82f).coerceIn(0.10f, 1f) else 0.18f
            val barH = size.height * h
            val x = i * (barWidth + gap)
            val y = (size.height - barH) / 2f
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(cTop, cBot),
                    startY = y,
                    endY = y + barH,
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth, barH),
            )
        }
    }
}

// ───────────────────────────── Buttons ─────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    enabled: Boolean = true,
) {
    val t = LL.tokens
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) t.brandGradient else Brush.linearGradient(listOf(t.bg3, t.bg3)))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) Icon(leading, null, tint = if (enabled) t.bg0 else t.text3, modifier = Modifier.size(14.dp))
        Text(
            text,
            color = if (enabled) t.bg0 else t.text3,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
        )
    }
}

@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
) {
    val t = LL.tokens
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = if (t.isDark) 0.04f else 0.04f))
            .border(1.dp, t.border, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leading != null) Icon(leading, null, tint = t.text0, modifier = Modifier.size(14.dp))
        Text(text, color = t.text0, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
    }
}

@Composable
fun IconBtn(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    size: Dp = 32.dp,
    selected: Boolean = false,
) {
    val t = LL.tokens
    val bg = if (selected) t.cyan.copy(alpha = 0.12f) else Color.White.copy(alpha = if (t.isDark) 0.04f else 0.0f)
    val bd = if (selected) t.cyan.copy(alpha = 0.35f) else t.border
    val fg = if (selected) t.cyan else t.text1
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, bd, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = fg, modifier = Modifier.size(15.dp))
    }
}

// ───────────────────────────── Section dividers ─────────────────────────────

@Composable
fun GlowDivider(modifier: Modifier = Modifier) {
    val t = LL.tokens
    Canvas(
        modifier
            .height(1.dp)
            .clip(RectangleShape)
    ) {
        drawRect(
            brush = Brush.horizontalGradient(
                0.0f to Color.Transparent,
                0.30f to t.cyan.copy(alpha = 0.35f),
                0.70f to t.violet.copy(alpha = 0.35f),
                1.0f to Color.Transparent,
            )
        )
    }
}

// ───────────────────────────── Avatar ─────────────────────────────

@Composable
fun GradientAvatar(
    initials: String,
    size: Dp = 32.dp,
    brush: Brush? = null,
    live: Boolean = false,
) {
    val t = LL.tokens
    val bg = brush ?: Brush.linearGradient(listOf(Color(0xFF6C7CE8), Color(0xFFB18CF5)))
    Box(modifier = Modifier.size(size + 4.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                initials,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.36f).sp,
            )
        }
        if (live) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.30f)
                    .clip(CircleShape)
                    .background(t.bg1)
                    .padding(2.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(t.green))
            }
        }
    }
}

// Prebaked avatar gradients used across the app.
object AvatarBrushes {
    val Coral get() = Brush.linearGradient(listOf(Color(0xFFFF9A76), Color(0xFFFF6B8A)))
    val Indigo get() = Brush.linearGradient(listOf(Color(0xFF6C7CE8), Color(0xFFB18CF5)))
    val Aqua   get() = Brush.linearGradient(listOf(Color(0xFF76D7FF), Color(0xFF5CF2E8)))
    val Violet get() = Brush.linearGradient(listOf(Color(0xFFB18CF5), Color(0xFFA98BFF)))
    val Amber  get() = Brush.linearGradient(listOf(Color(0xFFFFC26B), Color(0xFFFF7AD9)))
    val Mint   get() = Brush.linearGradient(listOf(Color(0xFF5AF2A8), Color(0xFF5CF2E8)))
    val Rose   get() = Brush.linearGradient(listOf(Color(0xFFFF6B8A), Color(0xFFB18CF5)))
}

// ───────────────────────────── Spacers ─────────────────────────────

@Composable
fun HSpacer(width: Dp) = Spacer(Modifier.width(width))
@Composable
fun VSpacer(height: Dp) = Spacer(Modifier.height(height))
