package com.devscion.lingualink.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.devscion.lingualink.ui.theme.LinguaLinkColors
import kotlin.math.sin

@Composable
fun WaveformIndicator(level: Float, isActive: Boolean, modifier: Modifier = Modifier) {
    val animatedLevel by animateFloatAsState(
        targetValue = if (isActive) level else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "waveform"
    )

    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(isActive) {
        while (isActive) {
            kotlinx.coroutines.delay(80)
            tick++
        }
    }

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val barCount = 20
        val spacing = size.width / (barCount * 2f)
        val barWidth = spacing * 0.9f
        val maxBarHeight = size.height

        repeat(barCount) { i ->
            val jitter = if (isActive) {
                (sin((tick + i) * 0.8) * 0.25 + 0.1).toFloat()
            } else 0f
            val barHeight = if (isActive)
                (animatedLevel + jitter).coerceIn(0.08f, 1f) * maxBarHeight
            else maxBarHeight * 0.06f

            val x = i * spacing * 2f + spacing / 2
            val color = if (isActive) LinguaLinkColors.Primary else LinguaLinkColors.TextSecondary

            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - barHeight) / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}
