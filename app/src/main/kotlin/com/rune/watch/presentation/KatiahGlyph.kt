// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.math.PI

/**
 * Katiah Glyph Renderer for WearOS
 *
 * Renders Katiah concept glyphs (being, knowledge, unity, etc.) as animated vector graphics.
 * On the watch, we render a simplified "being" glyph representing Ember's presence.
 */

/**
 * KatiahGlyph — Animated rendering of a Katiah concept glyph
 *
 * @param emotionalColor The emotional color driving the glyph's glow (red, violet, blue, amber)
 * @param pulseStrength Pulse animation strength (0.62 for idle, 1.0 for active)
 * @param pulseDuration Duration of pulse animation in milliseconds
 * @param size Size of the glyph (default 80.dp for watch)
 * @param modifier Modifier to apply to the composable
 */
@Composable
fun KatiahGlyph(
    emotionalColor: Color = Color(0xFFFF7043), // Ember Orange by default
    pulseStrength: Float = 0.62f,
    pulseDuration: Int = 3100, // idle pulse: 3.1s
    size: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glyph_pulse")
    
    // Pulse animation: oscillate from 0 to 1
    val pulseFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_fraction"
    )
    
    // Calculate scale: oscillate between 1 - 0.012*strength and 1 + 0.06*strength
    val scaleMin = 1f - (0.012f * pulseStrength)
    val scaleMax = 1f + (0.06f * pulseStrength)
    val scale = scaleMin + (pulseFraction * (scaleMax - scaleMin))
    
    // Calculate opacity: 0.84 at start/end, 1.0 at peak (using sine for smooth curve)
    val opacityPulse = sin(pulseFraction * PI).toFloat()
    val opacity = 0.84f + (opacityPulse * 0.16f)
    
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
            val radius = canvasSize * 0.35f
            val strokeWidth = canvasSize * 0.08f
            
            // Outer circle
            drawCircle(
                color = emotionalColor.copy(alpha = opacity),
                radius = radius,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Inner circle (smaller)
            drawCircle(
                color = emotionalColor.copy(alpha = opacity * 0.7f),
                radius = radius * 0.4f,
                style = Stroke(
                    width = strokeWidth * 0.6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            
            // Vertical line
            drawLine(
                color = emotionalColor.copy(alpha = opacity * 0.6f),
                start = androidx.compose.ui.geometry.Offset(center, center - radius * 0.8f),
                end = androidx.compose.ui.geometry.Offset(center, center + radius * 0.8f),
                strokeWidth = strokeWidth * 0.7f,
                cap = StrokeCap.Round
            )
            
            // Horizontal line
            drawLine(
                color = emotionalColor.copy(alpha = opacity * 0.6f),
                start = androidx.compose.ui.geometry.Offset(center - radius * 0.8f, center),
                end = androidx.compose.ui.geometry.Offset(center + radius * 0.8f, center),
                strokeWidth = strokeWidth * 0.7f,
                cap = StrokeCap.Round
            )
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
