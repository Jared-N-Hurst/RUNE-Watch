// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import com.rune.watch.settings.WatchSettingsStore

@Composable
fun EmberWatchTheme(
    themeMode: String = WatchSettingsStore.THEME_GHOST,
    content: @Composable () -> Unit,
) {
    val colors = if (themeMode == WatchSettingsStore.THEME_LIGHT) {
        EmberWatchLightColors
    } else {
        EmberWatchColors
    }

    MaterialTheme(
        colors = colors,
        typography = EmberWatchTypography,
        content = content
    )
}
