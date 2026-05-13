// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.rune.watch.bus.DeviceBusClient
import com.rune.watch.presentation.theme.EmberWatchTheme
import com.rune.watch.settings.WatchSettingsStore
import java.util.Locale
import java.util.UUID

@Composable
fun EmberApp(busClient: DeviceBusClient) {
    val context = LocalContext.current
    val themeMode by WatchSettingsStore.themeModeFlow(context)
        .collectAsState(initial = WatchSettingsStore.THEME_RUNE_DARK)

    EmberWatchTheme(themeMode = themeMode) {
        val paired by busClient.paired.collectAsState()
        val navController = rememberSwipeDismissableNavController()
        val connected by busClient.connected.collectAsState()
        val emberState by busClient.emberState.collectAsState()
        val watchDeviceId by busClient.deviceIdState.collectAsState()
        var voiceDraftState by rememberSaveable { mutableStateOf("") }
        var voiceListening by rememberSaveable { mutableStateOf(false) }

        LaunchedEffect(paired) {
            if (!paired) {
                navController.navigate("pairing") {
                    launchSingleTop = true
                }
            }
        }

        val speechLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult(),
        ) { result ->
            voiceListening = false
            val transcript = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (transcript.isNotBlank()) {
                voiceDraftState = transcript
            }
        }

        fun launchVoiceCapture() {
            voiceListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Ember")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
            runCatching {
                speechLauncher.launch(intent)
            }.onFailure {
                voiceListening = false
                voiceDraftState = "Voice input is unavailable on this watch right now."
            }
        }

        fun sendVoiceDraft() {
            val text = voiceDraftState.trim()
            if (text.isBlank()) return
            busClient.sendCommand(
                intent = "relay_message",
                payload = mapOf(
                    "text" to text,
                    "clientMessageId" to UUID.randomUUID().toString(),
                    "sourceDeviceId" to watchDeviceId,
                    "sourceLabel" to "Watch",
                    "sourceSurface" to "watch",
                ),
            )
            voiceDraftState = ""
        }

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = if (paired) "ember" else "pairing"
        ) {
            composable("ember") {
                EmberScreen(
                    connected = connected,
                    emberState = emberState,
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onOpenBiometrics = {
                        navController.navigate("biometrics")
                    },
                    onOpenPairing = {
                        navController.navigate("pairing")
                    },
                    onOpenChat = {
                        navController.navigate("chat")
                    },
                    onOpenChatVoice = {
                        launchVoiceCapture()
                        navController.navigate("chat")
                    },
                    busClient = busClient,
                )
            }

            composable("chat") {
                ChatScreen(
                    voiceDraft = voiceDraftState,
                    voiceListening = voiceListening,
                    onOpenVoiceCapture = ::launchVoiceCapture,
                    onSendVoiceDraft = ::sendVoiceDraft,
                    onClearVoiceDraft = {
                        voiceDraftState = ""
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable("pairing") {
                PairingScreen(
                    busClient = busClient,
                    onContinue = {
                        if (paired) {
                            navController.navigate("ember") {
                                popUpTo("pairing") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                )
            }

            composable("settings") {
                SettingsScreen(
                    busClient = busClient,
                    paired = paired,
                    connected = connected,
                    onOpenPairing = {
                        navController.navigate("pairing")
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable("biometrics") {
                BiometricsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
