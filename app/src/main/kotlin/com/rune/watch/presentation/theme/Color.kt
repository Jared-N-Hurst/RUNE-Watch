// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val EmberOrange     = Color(0xFFFF7043)
val EmberOrangeDark = Color(0xFFB71C1C)
val EmberSurface    = Color(0xFF141414)
val EmberBackground = Color(0xFF0D0D0D)
val EmberOnSurface  = Color(0xFFF0F0F0)

// Emotional colors (aligned with Portal Katiah system)
val EmotionalRed    = Color(0xFFFF7A72)    // Alert state
val EmotionalViolet = Color(0xFFC89DFF)    // Thinking state
val EmotionalBlue   = Color(0xFF8FD9FF)    // Contemplative
val EmotionalAmber  = Color(0xFFFFCF88)    // Idle/default

val EmberWatchColors = Colors(
    primary          = EmberOrange,
    primaryVariant   = EmberOrangeDark,
    secondary        = EmberOrange,
    secondaryVariant = EmberOrangeDark,
    background       = EmberBackground,
    surface          = EmberSurface,
    error            = Color(0xFFCF6679),
    onPrimary        = Color.Black,
    onSecondary      = Color.Black,
    onBackground     = EmberOnSurface,
    onSurface        = EmberOnSurface,
    onSurfaceVariant = Color(0xFFAAAAAA),
    onError          = Color.Black
)

val EmberWatchLightColors = Colors(
    primary = Color(0xFF1565C0),
    primaryVariant = Color(0xFF0D47A1),
    secondary = Color(0xFF039BE5),
    secondaryVariant = Color(0xFF0277BD),
    background = Color(0xFFF4F7FB),
    surface = Color(0xFFFFFFFF),
    error = Color(0xFFB00020),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF102338),
    onSurface = Color(0xFF102338),
    onSurfaceVariant = Color(0xFF4A627A),
    onError = Color.White,
)
