package com.devscion.lingualink.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Solar-system orb visualizer.
 *
 * Five layers, all driven by a single per-frame time tick:
 *  1. Ambient halo
 *  2. Outer ring + clockwise cyan dot (30s)
 *  3. Inner ring + counter-clockwise violet dot (20s)
 *  4. Off-center radial-gradient core with depth bloom and 6s breathing scale
 *  5. 14 orbiting particles (alternating cyan/violet) with staggered phases
 *
 * @param coreSize visible core diameter; canvas footprint is roughly 2.1× this
 * @param speaking when false, the core breathes much slower (idle look)
 */
@Composable
fun OrbVisualizer(
    coreSize: Dp,
    modifier: Modifier = Modifier,
    speaking: Boolean = true,
) {
    val t = LL.tokens
    val time by produceState(initialValue = 0L) {
        while (true) withFrameMillis { ms -> value = ms }
    }
    val breathPeriod = if (speaking) 6000.0 else 10_000.0
    val breath = (sin(time / breathPeriod * 2.0 * PI) * 0.5 + 0.5).toFloat()
    val coreScale = 1.0f + (if (speaking) 0.04f else 0.02f) * breath

    val canvasFootprint = coreSize * 2.1f
    Canvas(modifier = modifier.size(canvasFootprint)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val sPx = coreSize.toPx()
        val coreR = sPx * 0.42f * coreScale
        val ringOuterR = sPx * 0.68f
        val ringInnerR = sPx * 0.50f
        val orbitR = sPx * 0.85f
        val ringStrokeAlpha = if (t.isDark) 0.10f else 0.22f

        // Halo
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to t.cyan.copy(alpha = 0.16f),
                0.55f to t.violet.copy(alpha = 0.08f),
                1.0f to Color.Transparent,
                center = center,
                radius = sPx * 1.05f,
            ),
            radius = sPx * 1.05f,
            center = center,
        )

        // Outer ring (clockwise dot)
        drawCircle(
            color = Color.White.copy(alpha = ringStrokeAlpha),
            radius = ringOuterR,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawOrbitDot(
            center = center,
            radius = ringOuterR,
            angleDeg = ((time % 30_000L) / 30_000f) * 360f - 90f,
            color = t.cyan,
        )

        // Inner ring (counter-clockwise dot)
        drawCircle(
            color = Color.White.copy(alpha = ringStrokeAlpha),
            radius = ringInnerR,
            center = center,
            style = Stroke(width = 1.dp.toPx()),
        )
        drawOrbitDot(
            center = center,
            radius = ringInnerR,
            angleDeg = -((time % 20_000L) / 20_000f) * 360f - 90f,
            color = t.violet,
        )

        // Core (off-center radial gradient highlight)
        val highlight = Offset(center.x - coreR * 0.30f, center.y - coreR * 0.40f)
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color.White,
                0.25f to t.cyan,
                0.70f to t.violet,
                1.0f to Color(0xFF4A3A8A),
                center = highlight,
                radius = coreR * 1.6f,
            ),
            radius = coreR,
            center = center,
        )
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                center = highlight,
                radius = coreR * 0.9f,
            ),
            radius = coreR * 0.9f,
            center = highlight,
        )

        // Orbiting particles
        val particleCount = 14
        val particleDot = 1.8.dp.toPx()
        val particleGlow = 5.5.dp.toPx()
        for (i in 0 until particleCount) {
            val periodMs = 5500f + (i % 4) * 800f
            val staggerMs = i * 450L
            val phase = (((time + staggerMs) % periodMs.toLong()) / periodMs)
                .coerceIn(0f, 1f)
            val angleDeg = phase * 360f - 90f
            val rad = (angleDeg * PI / 180.0).toFloat()
            val pos = Offset(
                center.x + cos(rad) * orbitR,
                center.y + sin(rad) * orbitR,
            )
            val alpha = when {
                phase < 0.10f -> phase / 0.10f
                phase > 0.90f -> (1.0f - phase) / 0.10f
                else -> 1f
            }.coerceIn(0f, 1f)
            val color = if (i % 2 == 0) t.cyan else t.violet
            drawCircle(color = color.copy(alpha = alpha * 0.30f), radius = particleGlow, center = pos)
            drawCircle(color = color.copy(alpha = alpha), radius = particleDot, center = pos)
        }
    }
}

private fun DrawScope.drawOrbitDot(
    center: Offset,
    radius: Float,
    angleDeg: Float,
    color: Color,
) {
    val rad = (angleDeg * PI / 180.0).toFloat()
    val pos = Offset(
        center.x + cos(rad) * radius,
        center.y + sin(rad) * radius,
    )
    drawCircle(color = color.copy(alpha = 0.40f), radius = 6.dp.toPx(), center = pos)
    drawCircle(color = color, radius = 3.dp.toPx(), center = pos)
}
