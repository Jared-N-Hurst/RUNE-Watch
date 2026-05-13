// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

private fun runeColors(
    accent: Color,
    line: Color,
    bg: Color,
    bgAlt: Color,
    text: Color,
    textSoft: Color,
    danger: Color,
    success: Color,
): Colors = Colors(
    primary = accent,
    primaryVariant = line,
    secondary = accent,
    secondaryVariant = line,
    background = bg,
    surface = bgAlt,
    error = danger,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = text,
    onSurface = text,
    onSurfaceVariant = textSoft,
    onError = Color.Black,
)

val EmberWatchRuneDarkColors = runeColors(
    accent = Color(0xFF00E5FF),
    line = Color(0xFF173145),
    bg = Color(0xFF000000),
    bgAlt = Color(0xFF0A0A0A),
    text = Color(0xFFFFFFFF),
    textSoft = Color(0xFF7A9BAC),
    danger = Color(0xFFEF5350),
    success = Color(0xFF66BB6A),
)

val EmberWatchPhosphorColors = runeColors(
    accent = Color(0xFFFFB300),
    line = Color(0xFF3D2E00),
    bg = Color(0xFF080600),
    bgAlt = Color(0xFF110E00),
    text = Color(0xFFFFE082),
    textSoft = Color(0xFF8A7040),
    danger = Color(0xFFFF6D00),
    success = Color(0xFFC6FF00),
)

val EmberWatchJadeColors = runeColors(
    accent = Color(0xFF00E676),
    line = Color(0xFF003314),
    bg = Color(0xFF000A03),
    bgAlt = Color(0xFF001206),
    text = Color(0xFFCCFFE0),
    textSoft = Color(0xFF4A7A5A),
    danger = Color(0xFFFF5252),
    success = Color(0xFFB9F6CA),
)

val EmberWatchVoidColors = runeColors(
    accent = Color(0xFFB388FF),
    line = Color(0xFF2D0057),
    bg = Color(0xFF050009),
    bgAlt = Color(0xFF0A000F),
    text = Color(0xFFE8D5FF),
    textSoft = Color(0xFF6A4A8A),
    danger = Color(0xFFFF5252),
    success = Color(0xFF69F0AE),
)

val EmberWatchAshColors = runeColors(
    accent = Color(0xFFCFD8DC),
    line = Color(0xFF2A2A2E),
    bg = Color(0xFF0C0C0D),
    bgAlt = Color(0xFF141416),
    text = Color(0xFFECEFF1),
    textSoft = Color(0xFF78909C),
    danger = Color(0xFFEF5350),
    success = Color(0xFF66BB6A),
)

val EmberWatchCrimsonColors = runeColors(
    accent = Color(0xFFFF1744),
    line = Color(0xFF4A0010),
    bg = Color(0xFF0A0002),
    bgAlt = Color(0xFF140004),
    text = Color(0xFFFFCDD2),
    textSoft = Color(0xFF8A2040),
    danger = Color(0xFFFF6D00),
    success = Color(0xFF69F0AE),
)
