// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import com.rune.watch.settings.WatchSettingsStore

@Composable
fun EmberWatchTheme(
    themeMode: String = WatchSettingsStore.THEME_RUNE_DARK,
    content: @Composable () -> Unit,
) {
    val colors = when (themeMode) {
        WatchSettingsStore.THEME_PHOSPHOR -> EmberWatchPhosphorColors
        WatchSettingsStore.THEME_JADE -> EmberWatchJadeColors
        WatchSettingsStore.THEME_VOID -> EmberWatchVoidColors
        WatchSettingsStore.THEME_ASH -> EmberWatchAshColors
        WatchSettingsStore.THEME_CRIMSON -> EmberWatchCrimsonColors
        else -> EmberWatchRuneDarkColors
    }

    MaterialTheme(
        colors = colors,
        typography = EmberWatchTypography,
        content = content
    )
}
