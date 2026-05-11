// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.rune.watch.settings.WatchSettingsStore
import kotlinx.coroutines.launch

@Composable
fun BiometricsScreen(
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs by WatchSettingsStore.biometricStreamPrefsFlow(context)
        .collectAsState(
            initial = WatchSettingsStore.BiometricStreamPrefs(
                enabled = true,
                heartRate = true,
                hrv = true,
                stress = true,
                sleep = true,
                movement = true,
            )
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Biometrics",
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Text(
            text = if (prefs.enabled) "Streaming enabled" else "Streaming disabled",
            fontSize = 10.sp,
            color = if (prefs.enabled) Color(0xFF4CAF50) else Color(0xFFFFA726),
            textAlign = TextAlign.Center,
        )

        Button(
            onClick = { scope.launch { WatchSettingsStore.toggleBiometricIngest(context) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (prefs.enabled) "Master: ON" else "Master: OFF")
        }

        Button(
            onClick = { scope.launch { WatchSettingsStore.toggleHeartRate(context) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = prefs.enabled,
        ) {
            Text(if (prefs.heartRate) "Heart Rate: ON" else "Heart Rate: OFF")
        }

        Button(
            onClick = { scope.launch { WatchSettingsStore.toggleHrv(context) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = prefs.enabled,
        ) {
            Text(if (prefs.hrv) "HRV: ON" else "HRV: OFF")
        }

        Button(
            onClick = { scope.launch { WatchSettingsStore.toggleStress(context) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = prefs.enabled,
        ) {
            Text(if (prefs.stress) "Stress: ON" else "Stress: OFF")
        }

        Button(
            onClick = { scope.launch { WatchSettingsStore.toggleSleep(context) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = prefs.enabled,
        ) {
            Text(if (prefs.sleep) "Sleep: ON" else "Sleep: OFF")
        }

        Button(
            onClick = { scope.launch { WatchSettingsStore.toggleMovement(context) } },
            modifier = Modifier.fillMaxWidth(),
            enabled = prefs.enabled,
        ) {
            Text(if (prefs.movement) "Movement: ON" else "Movement: OFF")
        }

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Back")
        }
    }
}
