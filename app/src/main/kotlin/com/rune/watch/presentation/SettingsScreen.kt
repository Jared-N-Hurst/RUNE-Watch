// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.rune.watch.bus.DeviceBusService

@Composable
fun SettingsScreen(
    connected: Boolean,
    onBack: () -> Unit,
) {
    val context = LocalContext.current

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

        Spacer(modifier = Modifier.height(2.dp))

        Button(onClick = { DeviceBusService.reconnect(context) }) {
            Text("Reconnect")
        }

        Button(onClick = { DeviceBusService.stop(context) }) {
            Text("Stop Service")
        }

        Button(onClick = { DeviceBusService.start(context) }) {
            Text("Start Service")
        }

        Button(onClick = onBack) {
            Text("Back")
        }
    }
}
