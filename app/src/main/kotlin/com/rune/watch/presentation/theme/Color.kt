package com.rune.watch.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

val EmberOrange     = Color(0xFFFF7043)
val EmberOrangeDark = Color(0xFFB71C1C)
val EmberSurface    = Color(0xFF141414)
val EmberBackground = Color(0xFF0D0D0D)
val EmberOnSurface  = Color(0xFFF0F0F0)

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
