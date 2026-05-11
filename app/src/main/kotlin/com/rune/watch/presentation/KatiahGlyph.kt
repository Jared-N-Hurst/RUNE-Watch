// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import android.graphics.Paint
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

/**
 * Katiah Glyph Renderer for WearOS
 *
 * Renders Katiah concept glyphs (being, knowledge, unity, etc.) as animated vector graphics.
 * On the watch, we render a simplified "being" glyph representing Ember's presence.
 *
 * Animation states:
 * - disconnected: dim pulse, faint color
 * - connected+idle: normal pulse (3.1s cycle)
 * - connected+thinking: orbit animation (rotating outer rings, 2.35s cycle)
 * - connected+alert: rapid pulse (1.65s cycle, full energy)
 */

/**
 * KatiahGlyph — Animated rendering of a Katiah concept glyph
 *
 * @param emotionalColor The emotional color driving the glyph's glow (red, violet, blue, amber)
 * @param pulseStrength Pulse animation strength (0.62 for idle, 1.0 for active)
 * @param pulseDuration Duration of pulse animation in milliseconds
 * @param size Size of the glyph (default 80.dp for watch)
 * @param connected Connection state (affects animation intensity)
 * @param modifier Modifier to apply to the composable
 */
@Composable
fun KatiahGlyph(
    emotionalColor: Color = Color(0xFFFF7043), // Ember Orange by default
    pulseStrength: Float = 0.62f,
    pulseDuration: Int = 3100, // idle pulse: 3.1s
    size: Dp = 100.dp,
    connected: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glyph_pulse")

    val baseOpacity = if (connected) 1.0f else 0.5f
    val effectiveStrength = if (connected) pulseStrength else pulseStrength * 0.4f

    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_fraction"
    )

    val pulseWave = sin(pulseFraction * PI).toFloat()
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    pulseStrength > 0.95f -> 5328
                    pulseStrength > 0.8f -> 6584
                    else -> 9099
                },
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_rotation"
    )

    val scaleMin = 1f - (0.012f * effectiveStrength)
    val scaleMax = 1f + (0.06f * effectiveStrength)
    val scale = scaleMin + (pulseFraction * (scaleMax - scaleMin))
    val pulseRingScale = 1.08f + (pulseWave * 0.16f * effectiveStrength)
    val opacity = (0.84f + (pulseWave * 0.16f)) * baseOpacity
    val runeSymbols = listOf("ᚱ", "ᚢ", "ᚾ", "ᛖ", "ᛟ", "ᚠ", "ᛗ", "ᛁ")
    val runeColor = lerp(emotionalColor, Color(0xFFFFFFFF), 0.46f)
    val runePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = size.value * 0.13f
        color = runeColor.copy(alpha = 0.96f * baseOpacity).toArgb()
        setShadowLayer(size.value * 0.03f, 0f, 0f, Color.Black.copy(alpha = 0.45f).toArgb())
    }
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(size * scale)
                .align(Alignment.Center)
        ) {
            val canvasSize = this.size.minDimension
            val center = canvasSize / 2f
            val coreRadius = canvasSize * 0.285f
            val outerRingRadius = canvasSize * 0.355f
            val strokeWidth = canvasSize * 0.021f
            val compactGlyph = canvasSize <= 100f
            val orbitCenterX = center

            runePaint.textSize = canvasSize * if (compactGlyph) 0.080f else 0.090f

            drawCircle(
                color = emotionalColor.copy(alpha = 0.13f * opacity),
                radius = coreRadius * 1.45f,
            )

            drawCircle(
                color = emotionalColor.copy(alpha = 0.22f * opacity),
                radius = outerRingRadius * pulseRingScale,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )

            drawCircle(
                color = emotionalColor.copy(alpha = opacity * 0.95f),
                radius = coreRadius,
            )

            drawCircle(
                color = emotionalColor.copy(alpha = opacity * 0.34f),
                radius = coreRadius * 0.72f,
            )

            drawCircle(
                color = emotionalColor.copy(alpha = opacity * 0.62f),
                radius = coreRadius * 0.24f,
            )

            drawCircle(
                color = emotionalColor.copy(alpha = opacity * 0.32f),
                radius = outerRingRadius,
                style = Stroke(
                    width = strokeWidth * 0.75f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                )
            )

            val runeRadius = canvasSize * if (compactGlyph) 0.54f else 0.48f
            runeSymbols.forEachIndexed { index, symbol ->
                val angle = ((orbitRotation + (index * 45f)) * PI / 180f).toFloat()
                val x = orbitCenterX + cos(angle) * runeRadius
                val y = center + sin(angle) * runeRadius + (runePaint.textSize * 0.35f)
                drawContext.canvas.nativeCanvas.drawText(symbol, x, y, runePaint)
            }

            if (connected) {
                val orbitDotRadius = canvasSize * if (compactGlyph) 0.50f else 0.44f
                repeat(3) { index ->
                    val angle = ((orbitRotation + index * 120f + 18f) * PI / 180f).toFloat()
                    drawCircle(
                        color = emotionalColor.copy(alpha = opacity * 0.55f),
                        radius = strokeWidth * 1.15f,
                        center = androidx.compose.ui.geometry.Offset(
                            orbitCenterX + cos(angle) * orbitDotRadius,
                            center + sin(angle) * orbitDotRadius,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Variant: Simple dot representation of Ember for ultra-minimal displays
 */
@Composable
fun KatiahGlyphSimple(
    emotionalColor: Color = Color(0xFFFF7043),
    size: Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "simple_pulse")
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "simple_pulse_fraction"
    )
    
    val opacity = 0.7f + (pulseFraction * 0.3f)
    
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            drawCircle(
                color = emotionalColor.copy(alpha = opacity),
                radius = this.size.minDimension * 0.3f
            )
        }
    }
}
