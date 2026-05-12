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

// Neo-retro phosphor (neon green on dark)
val PhosphorGreen = Color(0xFF00FF00)
val PhosphorGreenDim = Color(0xFF00CC00)
val EmberWatchPhosphorColors = Colors(
    primary          = PhosphorGreen,
    primaryVariant   = PhosphorGreenDim,
    secondary        = PhosphorGreen,
    secondaryVariant = PhosphorGreenDim,
    background       = Color(0xFF0A0A0A),
    surface          = Color(0xFF151515),
    error            = Color(0xFFFF3333),
    onPrimary        = Color.Black,
    onSecondary      = Color.Black,
    onBackground     = Color(0xFFE0FFE0),
    onSurface        = Color(0xFFE0FFE0),
    onSurfaceVariant = Color(0xFF88DD88),
    onError          = Color.Black
)

// Neo-retro jade (neon cyan on dark)
val JadeCyan = Color(0xFF00FFFF)
val JadeCyanDim = Color(0xFF00DDDD)
val EmberWatchJadeColors = Colors(
    primary          = JadeCyan,
    primaryVariant   = JadeCyanDim,
    secondary        = JadeCyan,
    secondaryVariant = JadeCyanDim,
    background       = Color(0xFF0A0F0F),
    surface          = Color(0xFF151A1A),
    error            = Color(0xFFFF3333),
    onPrimary        = Color.Black,
    onSecondary      = Color.Black,
    onBackground     = Color(0xFFE0FFFF),
    onSurface        = Color(0xFFE0FFFF),
    onSurfaceVariant = Color(0xFF88DDDD),
    onError          = Color.Black
)

// Neo-retro crimson (bright red on dark)
val CrimsonRed = Color(0xFFFF0040)
val CrimsonRedDim = Color(0xFFDD0033)
val EmberWatchCrimsonColors = Colors(
    primary          = CrimsonRed,
    primaryVariant   = CrimsonRedDim,
    secondary        = CrimsonRed,
    secondaryVariant = CrimsonRedDim,
    background       = Color(0xFF0F0A0A),
    surface          = Color(0xFF1A1515),
    error            = Color(0xFFFFFF33),
    onPrimary        = Color.White,
    onSecondary      = Color.White,
    onBackground     = Color(0xFFFFE0E8),
    onSurface        = Color(0xFFFFE0E8),
    onSurfaceVariant = Color(0xFFDD88AA),
    onError          = Color.Black
)
