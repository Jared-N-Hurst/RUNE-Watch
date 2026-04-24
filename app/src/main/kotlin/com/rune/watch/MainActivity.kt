// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import com.rune.watch.bus.DeviceBusClient
import com.rune.watch.bus.DeviceBusRuntime
import com.rune.watch.bus.DeviceBusService
import com.rune.watch.presentation.EmberApp

class MainActivity : ComponentActivity() {

    private lateinit var busClient: DeviceBusClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val serviceIntent = Intent(this, DeviceBusService::class.java).apply {
            action = DeviceBusService.ACTION_START
        }
        ContextCompat.startForegroundService(this, serviceIntent)

        busClient = DeviceBusRuntime.client(applicationContext)
        setContent {
            EmberApp(busClient = busClient)
        }
    }
}
