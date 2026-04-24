// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.rune.watch.bus.DeviceBusRuntime
import com.rune.watch.bus.DeviceBusRuntime.HealthEvent
import com.rune.watch.bus.DeviceBusService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    paired: Boolean,
    connected: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val serviceRunning by DeviceBusRuntime.serviceRunning.collectAsState()
    val lastReconnectAttemptMs by DeviceBusRuntime.lastReconnectAttemptMs.collectAsState()
    val startCount by DeviceBusRuntime.startCount.collectAsState()
    val stopCount by DeviceBusRuntime.stopCount.collectAsState()
    val reconnectCount by DeviceBusRuntime.reconnectCount.collectAsState()
    val lastAction by DeviceBusRuntime.lastAction.collectAsState()
    val lastActionResult by DeviceBusRuntime.lastActionResult.collectAsState()
    val lastActionAtMs by DeviceBusRuntime.lastActionAtMs.collectAsState()
    val healthEvents by DeviceBusRuntime.healthEvents.collectAsState()
    var actionLocked by remember { mutableStateOf(false) }
    var showReconnectConfirm by remember { mutableStateOf(false) }

    val reconnectText = lastReconnectAttemptMs?.let {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        "Last reconnect: ${fmt.format(Date(it))}"
    } ?: "Last reconnect: none"

    val lastActionText = lastActionAtMs?.let {
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        val action = lastAction ?: "unknown"
        val result = lastActionResult ?: "unknown"
        "Last action: ${action} (${result}) @ ${fmt.format(Date(it))}"
    } ?: "Last action: none"

    val stopDisabledReason = when {
        !paired -> "Stop disabled until pairing is complete"
        !serviceRunning -> "Service already stopped"
        else -> null
    }

    fun runDebounced(action: () -> Unit) {
        if (actionLocked) return
        actionLocked = true
        scope.launch {
            try {
                action()
            } finally {
                delay(1_200L)
                actionLocked = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Connection Settings",
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Text(
            text = if (connected) "Service: Connected" else "Service: Offline/Reconnecting",
            fontSize = 11.sp,
            color = if (connected) Color(0xFF4CAF50) else Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Text(
            text = if (serviceRunning) "Runtime: Running" else "Runtime: Stopped",
            fontSize = 10.sp,
            color = if (serviceRunning) Color(0xFF8BC34A) else Color(0xFFFFA726),
            textAlign = TextAlign.Center,
        )

        Text(
            text = reconnectText,
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Start/Stop/Reconnect: ${startCount}/${stopCount}/${reconnectCount}",
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Text(
            text = lastActionText,
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        if (healthEvents.isNotEmpty()) {
            Text(
                text = "Health snapshot",
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
            )

            Button(
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    val exported = DeviceBusRuntime.exportHealthSnapshot()
                    clipboard.setPrimaryClip(ClipData.newPlainText("health_snapshot", exported))
                    Toast.makeText(context, "Snapshot copied", Toast.LENGTH_SHORT).show()
                },
                enabled = !actionLocked,
            ) {
                Text("Copy Snapshot")
            }

            Button(
                onClick = { DeviceBusRuntime.clearHealthEvents() },
                enabled = !actionLocked,
            ) {
                Text("Clear Snapshot")
            }

            val preview = healthEvents.take(6)
            for (event in preview) {
                Text(
                    text = formatHealthEvent(event),
                    fontSize = 9.sp,
                    color = Color(0xFF90A4AE),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        if (showReconnectConfirm) {
            Text(
                text = "Confirm reconnect?",
                fontSize = 10.sp,
                color = Color(0xFFFFCC80),
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = {
                    showReconnectConfirm = false
                    runDebounced { DeviceBusService.reconnect(context) }
                },
                enabled = !actionLocked,
            ) {
                Text("Confirm Reconnect")
            }
            Button(
                onClick = { showReconnectConfirm = false },
                enabled = !actionLocked,
            ) {
                Text("Cancel")
            }
        } else {
            Button(
                onClick = { showReconnectConfirm = true },
                enabled = !actionLocked,
            ) {
                Text("Reconnect")
            }
        }

        Button(
            onClick = {
                showReconnectConfirm = false
                runDebounced { DeviceBusService.stop(context) }
            },
            enabled = stopDisabledReason == null && !actionLocked,
        ) {
            Text("Stop Service")
        }

        if (stopDisabledReason != null) {
            Text(
                text = stopDisabledReason,
                fontSize = 10.sp,
                color = Color(0xFFFFB74D),
                textAlign = TextAlign.Center,
            )
        }

        Button(
            onClick = {
                showReconnectConfirm = false
                runDebounced { DeviceBusService.start(context) }
            },
            enabled = !actionLocked,
        ) {
            Text("Start Service")
        }

        if (actionLocked) {
            Text(
                text = "Control lock active...",
                fontSize = 10.sp,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center,
            )
        }

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}

private fun formatHealthEvent(event: HealthEvent): String {
    val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
    val at = fmt.format(Date(event.atMs))
    return "$at ${event.category}: ${event.message}"
}
