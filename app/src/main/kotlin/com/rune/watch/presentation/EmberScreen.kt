// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*

@Composable
fun EmberScreen(
    connected: Boolean,
    emberState: String,
    onSendCommand: (intent: String, payload: Map<String, Any>?) -> Unit
) {
    val bgColor = Color(0xFF0D0D0D)
    val emberOrange = Color(0xFFFF7043)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            // Ember avatar glyph
            Text(
                text = "E",
                fontSize = 40.sp,
                color = emberOrange,
                style = MaterialTheme.typography.display1
            )

            // Current Ember expression / state
            Text(
                text = emberState.ifBlank { "Listening…" },
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )

            // Connection indicator
            Text(
                text = if (connected) "● Connected" else "○ Offline",
                fontSize = 11.sp,
                color = if (connected) Color(0xFF4CAF50) else Color(0xFF757575)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Quick-action chip row
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(
                    onClick = { onSendCommand("show_help", null) },
                    label = { Text("Help", fontSize = 11.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF1E1E1E))
                )
                Chip(
                    onClick = { onSendCommand("toggle_desktop", null) },
                    label = { Text("Desktop", fontSize = 11.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF1E1E1E))
                )
            }
        }
    }
}
