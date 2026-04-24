// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import com.rune.watch.bus.DeviceBusClient
import com.rune.watch.bus.NotificationAction
import com.rune.watch.presentation.notifications.NotificationRenderer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun EmberScreen(
    connected: Boolean,
    emberState: String,
    onOpenSettings: () -> Unit,
    onSendCommand: (intent: String, payload: Map<String, Any>?) -> Unit,
    busClient: DeviceBusClient,
) {
    val scope = rememberCoroutineScope()
    val notification by busClient.currentNotification.collectAsState()
    
    val bgColor = Color(0xFF0D0D0D)
    val emberOrange = Color(0xFFFF7043)
    
    // Determine emotional color based on state (aligned with Portal)
    val emotionalColor = when {
        !connected -> Color(0xFF757575) // gray if offline
        emberState.contains("alert") -> Color(0xFFFF7A72) // red for alert
        emberState.contains("thinking") -> Color(0xFFC89DFF) // violet for thinking
        else -> Color(0xFFFF7043) // default orange
    }
    
    // Pulse settings based on state (aligned with Portal: alert/thinking/idle)
    val (pulseDuration, pulseStrength) = when {
        emberState.contains("alert") -> 1650 to 1.0f
        emberState.contains("thinking") -> 2350 to 1.0f
        else -> 3100 to 0.62f
    }

    if (notification != null) {
        NotificationRenderer(
            notification = notification!!,
            onActionSelected = { action: NotificationAction ->
                scope.launch {
                    busClient.respondToNotificationAction(notification!!.id, action.id)
                }
            },
            onDismiss = {
                scope.launch {
                    busClient.respondToNotificationAction(notification!!.id, "dismiss")
                }
            }
        )
        return
    }

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
            // Ember avatar glyph — Katiah "Being" with pulse and emotional coloring
            KatiahGlyph(
                emotionalColor = emotionalColor,
                pulseStrength = pulseStrength,
                pulseDuration = pulseDuration,
                size = 80.dp
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
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(
                    onClick = { onSendCommand("approve", null) },
                    label = { Text("Approve", fontSize = 10.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF1B5E20))
                )
                Chip(
                    onClick = { onSendCommand("dismiss", null) },
                    label = { Text("Dismiss", fontSize = 10.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF424242))
                )
                Chip(
                    onClick = onOpenSettings,
                    label = { Text("Settings", fontSize = 10.sp) },
                    colors = ChipDefaults.chipColors(backgroundColor = Color(0xFF1E1E1E))
                )
            }
        }
    }
}
