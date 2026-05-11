// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

@Composable
fun EmberScreen(
    connected: Boolean,
    emberState: String,
    onOpenSettings: () -> Unit,
    onOpenBiometrics: () -> Unit,
    onOpenPairing: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenChatVoice: () -> Unit,
    busClient: DeviceBusClient,
) {
    val scope = rememberCoroutineScope()
    val notification by busClient.currentNotification.collectAsState()
    var radialMenuOpen by rememberSaveable { mutableStateOf(false) }

    val emotionalColor = when {
        !connected -> Color(0xFF7E8A98)
        emberState.contains("alert", ignoreCase = true) -> Color(0xFFFF7A72)
        emberState.contains("thinking", ignoreCase = true) -> Color(0xFFC89DFF)
        else -> Color(0xFFFF8A4C)
    }

    val (pulseDuration, pulseStrength) = when {
        emberState.contains("alert", ignoreCase = true) -> 1650 to 1.0f
        emberState.contains("thinking", ignoreCase = true) -> 2350 to 0.94f
        else -> 3100 to 0.72f
    }
    val backgroundBrush = Brush.radialGradient(
        colors = listOf(
            emotionalColor.copy(alpha = 0.26f),
            Color(0xFF12131A),
            Color(0xFF080A10),
        ),
    )
    val orbContainerSize = 186.dp
    val orbTouchSize = 132.dp
    val orbGlyphSize = 115.dp

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
            .background(backgroundBrush),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(orbContainerSize),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(orbTouchSize)
                            .clickable { radialMenuOpen = !radialMenuOpen },
                        contentAlignment = Alignment.Center,
                    ) {
                        KatiahGlyph(
                            emotionalColor = emotionalColor,
                            pulseStrength = pulseStrength,
                            pulseDuration = pulseDuration,
                            size = orbGlyphSize,
                            connected = connected,
                        )
                    }

                    if (radialMenuOpen) {
                        RadialActionButton(
                            icon = "✦",
                            label = "Chat",
                            offsetX = (-63).dp,
                            offsetY = (-20).dp,
                            onClick = {
                                radialMenuOpen = false
                                onOpenChat()
                            },
                        )
                        RadialActionButton(
                            icon = "⚙",
                            label = "Set",
                            offsetX = 63.dp,
                            offsetY = (-20).dp,
                            onClick = {
                                radialMenuOpen = false
                                onOpenSettings()
                            },
                        )
                        RadialActionButton(
                            icon = "♥",
                            label = "Bio",
                            offsetX = 39.dp,
                            offsetY = 54.dp,
                            onClick = {
                                radialMenuOpen = false
                                onOpenBiometrics()
                            },
                            accent = Color(0xFFA3D5FF),
                        )
                        RadialActionButton(
                            icon = "🎤",
                            label = "Mic",
                            offsetX = (-39).dp,
                            offsetY = 54.dp,
                            onClick = {
                                radialMenuOpen = false
                                onOpenChatVoice()
                            },
                            accent = Color(0xFF9FD7FF),
                        )
                        RadialActionButton(
                            icon = "⌁",
                            label = "Pair",
                            offsetX = 0.dp,
                            offsetY = (-66).dp,
                            onClick = {
                                radialMenuOpen = false
                                onOpenPairing()
                            },
                            accent = if (connected) Color(0xFF87E6AE) else Color(0xFFFFC785),
                        )
                    }
                }
            }

            Text(
                text = if (connected) "● Connected" else "○ Offline",
                fontSize = 11.sp,
                color = if (connected) Color(0xFF4CAF50) else Color(0xFF757575)
            )
        }
    }
}

@Composable
private fun RadialActionButton(
    icon: String,
    label: String,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    accent: Color = Color(0xFFFFC785),
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(50.dp),
        colors = ButtonDefaults.secondaryButtonColors(
            backgroundColor = Color(0xCC151A24),
            contentColor = accent,
        ),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = icon,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
            Text(
                text = label,
                fontSize = 7.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
