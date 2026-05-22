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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.rune.watch.bus.DeviceBusClient
import com.rune.watch.bus.DeviceBusRuntime
import com.rune.watch.bus.DeviceBusRuntime.HealthEvent
import com.rune.watch.bus.DeviceBusService
import com.rune.watch.bus.WatchPresenceDiagnosticsSnapshot
import com.rune.watch.settings.WatchSettingsStore
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    busClient: DeviceBusClient,
    paired: Boolean,
    connected: Boolean,
    onOpenPairing: () -> Unit,
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
    val deviceId by busClient.deviceIdState.collectAsState()
    val userId by busClient.userIdState.collectAsState()
    val themeMode by WatchSettingsStore.themeModeFlow(context)
        .collectAsState(initial = WatchSettingsStore.THEME_RUNE_DARK)
    val fontMode by WatchSettingsStore.fontModeFlow(context)
        .collectAsState(initial = WatchSettingsStore.FONT_DEFAULT)
    val biometricIngestEnabled by WatchSettingsStore.biometricIngestEnabledFlow(context)
        .collectAsState(initial = true)
    val localePreference by WatchSettingsStore.localePreferenceFlow(context)
        .collectAsState(initial = null)
    var actionLocked by remember { mutableStateOf(false) }
    var showReconnectConfirm by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var presenceSnapshot by remember { mutableStateOf<WatchPresenceDiagnosticsSnapshot?>(null) }
    var presenceLoading by remember { mutableStateOf(false) }
    var presenceError by remember { mutableStateOf<String?>(null) }
    val currentLocale = WatchSettingsStore.resolveLocale(localePreference)

    fun refreshPresenceSnapshot() {
        scope.launch {
            presenceLoading = true
            presenceError = null
            val snapshot = busClient.fetchPresenceDiagnosticsSnapshot()
            if (snapshot == null) {
                presenceError = watchPresenceDiagnosticsUnavailable(currentLocale)
            }
            presenceSnapshot = snapshot
            presenceLoading = false
        }
    }

    LaunchedEffect(userId, deviceId) {
        if (userId.isNotBlank() && deviceId.isNotBlank()) {
            refreshPresenceSnapshot()
        }
    }

    val reconnectText = watchLastReconnectText(lastReconnectAttemptMs, currentLocale)

    val lastActionText = watchLastActionText(lastActionAtMs, lastAction, lastActionResult, currentLocale)

    val stopDisabledReason = watchStopDisabledReason(paired, serviceRunning, currentLocale)
    val wideButtonModifier = Modifier.fillMaxWidth(0.88f)

    fun runDebounced(action: suspend () -> Unit) {
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
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = watchConnectionSettingsTitle(currentLocale),
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Text(
            text = watchSettingsUiVersionText(currentLocale),
            fontSize = 10.sp,
            color = Color(0xFF90CAF9),
            textAlign = TextAlign.Center,
        )

        Text(
            text = watchServiceConnectionText(connected, currentLocale),
            fontSize = 11.sp,
            color = if (connected) Color(0xFF4CAF50) else Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Text(
            text = watchRuntimeStateText(serviceRunning, currentLocale),
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
            text = watchStartStopReconnectText(startCount, stopCount, reconnectCount, currentLocale),
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

        Text(
            text = watchDeviceIdText(deviceId, currentLocale),
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Text(
            text = watchUserIdText(userId, currentLocale),
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Text(
            text = watchPresenceDiagnosticsTitle(currentLocale),
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Button(
            modifier = wideButtonModifier,
            onClick = { refreshPresenceSnapshot() },
            enabled = !actionLocked && !presenceLoading,
        ) {
            Text(watchRefreshPresenceText(presenceLoading, currentLocale))
        }

        if (presenceSnapshot != null) {
            val snapshot = presenceSnapshot!!
            Text(
                text = watchStaleText(snapshot.staleCause, currentLocale),
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
            )
            Text(
                text = watchRelayHealthText(snapshot.relayHealthy, currentLocale),
                fontSize = 10.sp,
                color = if (snapshot.relayHealthy) Color(0xFF4CAF50) else Color(0xFFD28B6A),
                textAlign = TextAlign.Center,
            )
            Text(
                text = watchHeartbeatText(snapshot.heartbeatPath, snapshot.lastSeenLabel, currentLocale),
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
            )
        }

        if (presenceError != null) {
            Text(
                text = presenceError ?: watchPresenceDiagnosticsUnavailable(currentLocale),
                fontSize = 10.sp,
                color = Color(0xFFFFB74D),
                textAlign = TextAlign.Center,
            )
        }

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.setThemeMode(context, WatchSettingsStore.THEME_RUNE_DARK) } },
            enabled = !actionLocked,
        ) {
            Text(watchThemeOptionText(themeMode == WatchSettingsStore.THEME_RUNE_DARK, WatchSettingsStore.THEME_RUNE_DARK, currentLocale))
        }

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.setThemeMode(context, WatchSettingsStore.THEME_PHOSPHOR) } },
            enabled = !actionLocked,
        ) {
            Text(watchThemeOptionText(themeMode == WatchSettingsStore.THEME_PHOSPHOR, WatchSettingsStore.THEME_PHOSPHOR, currentLocale))
        }

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.setThemeMode(context, WatchSettingsStore.THEME_JADE) } },
            enabled = !actionLocked,
        ) {
            Text(watchThemeOptionText(themeMode == WatchSettingsStore.THEME_JADE, WatchSettingsStore.THEME_JADE, currentLocale))
        }

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.setThemeMode(context, WatchSettingsStore.THEME_CRIMSON) } },
            enabled = !actionLocked,
        ) {
            Text(watchThemeOptionText(themeMode == WatchSettingsStore.THEME_CRIMSON, WatchSettingsStore.THEME_CRIMSON, currentLocale))
        }

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.setThemeMode(context, WatchSettingsStore.THEME_VOID) } },
            enabled = !actionLocked,
        ) {
            Text(watchThemeOptionText(themeMode == WatchSettingsStore.THEME_VOID, WatchSettingsStore.THEME_VOID, currentLocale))
        }

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.setThemeMode(context, WatchSettingsStore.THEME_ASH) } },
            enabled = !actionLocked,
        ) {
            Text(watchThemeOptionText(themeMode == WatchSettingsStore.THEME_ASH, WatchSettingsStore.THEME_ASH, currentLocale))
        }

        Text(
            text = watchFontTitle(currentLocale),
            fontSize = 12.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.setFontMode(context, WatchSettingsStore.FONT_DEFAULT) } },
            enabled = !actionLocked,
        ) {
            Text(watchFontOptionText(fontMode == WatchSettingsStore.FONT_DEFAULT, WatchSettingsStore.FONT_DEFAULT, currentLocale))
        }

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.setFontMode(context, WatchSettingsStore.FONT_MONO) } },
            enabled = !actionLocked,
        ) {
            Text(watchFontOptionText(fontMode == WatchSettingsStore.FONT_MONO, WatchSettingsStore.FONT_MONO, currentLocale))
        }

        Button(
            modifier = wideButtonModifier,
            onClick = { runDebounced { WatchSettingsStore.toggleBiometricIngest(context) } },
            enabled = !actionLocked,
        ) {
            Text(watchBiometricToggleText(biometricIngestEnabled, currentLocale))
        }

        Button(
            modifier = wideButtonModifier,
            onClick = onOpenPairing,
            enabled = !actionLocked,
        ) {
            Text(watchReopenPairingQrText(currentLocale))
        }

        if (healthEvents.isNotEmpty()) {
            Text(
                text = watchHealthSnapshotTitle(currentLocale),
                fontSize = 10.sp,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center,
            )

            Button(
                modifier = wideButtonModifier,
                onClick = {
                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                    val exported = DeviceBusRuntime.exportHealthSnapshot()
                    clipboard.setPrimaryClip(ClipData.newPlainText("health_snapshot", exported))
                    Toast.makeText(context, watchSnapshotCopiedText(currentLocale), Toast.LENGTH_SHORT).show()
                },
                enabled = !actionLocked,
            ) {
                Text(watchCopySnapshotText(currentLocale))
            }

            Button(
                modifier = wideButtonModifier,
                onClick = { DeviceBusRuntime.clearHealthEvents() },
                enabled = !actionLocked,
            ) {
                Text(watchClearSnapshotText(currentLocale))
            }

            val preview = healthEvents.take(6)
            for (event in preview) {
                Text(
                    text = formatHealthEvent(event, currentLocale),
                    fontSize = 9.sp,
                    color = Color(0xFF90A4AE),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        if (showReconnectConfirm) {
            Text(
                text = watchConfirmReconnectQuestion(currentLocale),
                fontSize = 10.sp,
                color = Color(0xFFFFCC80),
                textAlign = TextAlign.Center,
            )
            Button(
                modifier = wideButtonModifier,
                onClick = {
                    showReconnectConfirm = false
                    runDebounced { DeviceBusService.reconnect(context) }
                },
                enabled = !actionLocked,
            ) {
                Text(watchConfirmReconnectText(currentLocale))
            }
            Button(
                modifier = wideButtonModifier,
                onClick = { showReconnectConfirm = false },
                enabled = !actionLocked,
            ) {
                Text(watchCancelText(currentLocale))
            }
        } else {
            Button(
                modifier = wideButtonModifier,
                onClick = { showReconnectConfirm = true },
                enabled = !actionLocked,
            ) {
                Text(watchReconnectButtonText(currentLocale))
            }
        }

        Button(
            modifier = wideButtonModifier,
            onClick = {
                showReconnectConfirm = false
                runDebounced { DeviceBusService.stop(context) }
            },
            enabled = stopDisabledReason == null && !actionLocked,
        ) {
            Text(watchStopServiceText(currentLocale))
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
            modifier = wideButtonModifier,
            onClick = {
                showReconnectConfirm = false
                runDebounced { DeviceBusService.start(context) }
            },
            enabled = !actionLocked,
        ) {
            Text(watchStartServiceText(currentLocale))
        }

        if (showLogoutConfirm) {
            Text(
                text = watchConfirmLogOutQuestion(currentLocale),
                fontSize = 10.sp,
                color = Color(0xFFFFCC80),
                textAlign = TextAlign.Center,
            )
            Button(
                modifier = wideButtonModifier,
                onClick = {
                    showLogoutConfirm = false
                    runDebounced {
                        DeviceBusService.stop(context)
                        busClient.clearCredentials()
                        Toast.makeText(context, watchLoggedOutText(currentLocale), Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !actionLocked,
            ) {
                Text(watchConfirmLogOutText(currentLocale))
            }
            Button(
                modifier = wideButtonModifier,
                onClick = { showLogoutConfirm = false },
                enabled = !actionLocked,
            ) {
                Text(watchCancelText(currentLocale))
            }
        } else {
            Button(
                modifier = wideButtonModifier,
                onClick = { showLogoutConfirm = true },
                enabled = !actionLocked,
            ) {
                Text(watchLogOutText(currentLocale))
            }
        }

        if (actionLocked) {
            Text(
                text = watchControlLockActiveText(currentLocale),
                fontSize = 10.sp,
                color = Color(0xFF90CAF9),
                textAlign = TextAlign.Center,
            )
        }

        Button(modifier = wideButtonModifier, onClick = onBack) {
            Text(watchBackText(currentLocale))
        }
    }
}

private fun formatHealthEvent(event: HealthEvent, locale: Locale): String {
    return formatWatchHealthEvent(event, locale)
}

