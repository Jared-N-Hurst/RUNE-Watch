package com.rune.watch.presentation.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

@Composable
fun EmberWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = EmberWatchColors,
        typography = EmberWatchTypography,
        content = content
    )
}
