// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.rune.watch.bus.DeviceBusClient
import com.rune.watch.presentation.EmberApp

class MainActivity : ComponentActivity() {

    private lateinit var busClient: DeviceBusClient

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        busClient = DeviceBusClient(applicationContext)
        setContent {
            EmberApp(busClient = busClient)
        }
    }

    override fun onResume() {
        super.onResume()
        busClient.connect()
    }

    override fun onPause() {
        super.onPause()
        // Keep connection alive in the background service; don't disconnect here.
    }

    override fun onDestroy() {
        super.onDestroy()
        busClient.disconnect()
    }
}
