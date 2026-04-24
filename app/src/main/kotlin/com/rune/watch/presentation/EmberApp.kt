// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.rune.watch.bus.DeviceBusClient
import com.rune.watch.presentation.theme.EmberWatchTheme

@Composable
fun EmberApp(busClient: DeviceBusClient) {
    EmberWatchTheme {
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
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    paired = paired,
                    connected = connected,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
