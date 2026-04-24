// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.rune.watch.bus.NotificationAction
import com.rune.watch.bus.ParsedNotification

@Composable
fun NotificationRenderer(
    notification: ParsedNotification,
    onActionSelected: (NotificationAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val accentColor = when (notification.type) {
        "alert" -> Color(0xFFFF7A72)
        "safety" -> Color(0xFFFF8A80)
        "prompt" -> Color(0xFFFFCF88)
        "reminder" -> Color(0xFF8FD9FF)
        else -> Color(0xFFFF7043)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Accent bar for notification type
        Text(
            text = notification.type.uppercase(),
            fontSize = 10.sp,
            color = accentColor,
            textAlign = TextAlign.Center,
        )

        // Title
        Text(
            text = notification.title,
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Body
        Text(
            text = notification.body,
            fontSize = 11.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Action buttons (max 2 per row on small screen)
        if (notification.actions.isNotEmpty()) {
            val actions = notification.actions.take(4)
            for (i in actions.indices step 2) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Button(
                        onClick = { onActionSelected(actions[i]) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = actions[i].label.take(8),
                            fontSize = 9.sp,
                        )
                    }

                    if (i + 1 < actions.size) {
                        Button(
                            onClick = { onActionSelected(actions[i + 1]) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = actions[i + 1].label.take(8),
                                fontSize = 9.sp,
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        } else {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Button(onClick = onDismiss) {
            Text("Dismiss")
        }
    }
}
