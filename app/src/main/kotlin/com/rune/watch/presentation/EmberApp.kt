// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.rune.watch.bus.DeviceBusClient
import com.rune.watch.settings.WatchSettingsStore
import com.rune.watch.presentation.theme.EmberWatchTheme

@Composable
fun EmberApp(busClient: DeviceBusClient) {
    val context = LocalContext.current
    val themeMode by WatchSettingsStore.themeModeFlow(context)
        .collectAsState(initial = WatchSettingsStore.THEME_GHOST)

    EmberWatchTheme(themeMode = themeMode) {
        val paired by busClient.paired.collectAsState()

        if (!paired) {
            PairingScreen(busClient = busClient)
            return@EmberWatchTheme
        }

        val navController = rememberSwipeDismissableNavController()
        val connected by busClient.connected.collectAsState()
        val emberState by busClient.emberState.collectAsState()

        SwipeDismissableNavHost(
            navController = navController,
            startDestination = "ember"
        ) {
            composable("ember") {
                EmberScreen(
                    connected = connected,
                    emberState = emberState,
                    onOpenSettings = {
                        navController.navigate("settings")
                    },
                    onSendCommand = { intent, payload ->
                        busClient.sendCommand(intent, payload)
                    },
                    busClient = busClient,
                )
            }

            composable("settings") {
                SettingsScreen(
                    busClient = busClient,
                    paired = paired,
                    connected = connected,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
