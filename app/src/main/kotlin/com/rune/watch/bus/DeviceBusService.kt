// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import android.content.Context
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.rune.watch.health.HealthMonitor
import com.rune.watch.settings.WatchSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the DeviceBusClient WebSocket alive
 * even when MainActivity is not in the foreground.
 *
 * The system shows a persistent notification while the service runs.
 * Users can dismiss it by stopping sync from the Ember watch app settings.
 */
class DeviceBusService : Service() {

    companion object {
        const val ACTION_START = "com.rune.watch.bus.START"
        const val ACTION_STOP = "com.rune.watch.bus.STOP"
        const val ACTION_RECONNECT = "com.rune.watch.bus.RECONNECT"
        private const val CHANNEL_ID = "ember_sync"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, DeviceBusService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DeviceBusService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun reconnect(context: Context) {
            val intent = Intent(context, DeviceBusService::class.java).apply {
                action = ACTION_RECONNECT
            }
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var connectedJob: Job? = null
    private var biometricJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        DeviceBusRuntime.markServiceRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                biometricJob?.cancel()
                biometricJob = null
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                DeviceBusRuntime.markServiceRunning(false)
                return START_NOT_STICKY
            }

            ACTION_RECONNECT -> {
                startForeground(NOTIFICATION_ID, buildNotification(connected = false))
                DeviceBusRuntime.restart(applicationContext)
                observeConnectionState()
                startBiometricLoop()
            }

            else -> {
                startForeground(NOTIFICATION_ID, buildNotification(connected = false))
                DeviceBusRuntime.start(applicationContext)
                DeviceBusRuntime.markServiceRunning(true)
                observeConnectionState()
                startBiometricLoop()
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        connectedJob?.cancel()
        connectedJob = null
        biometricJob?.cancel()
        biometricJob = null
        serviceScope.cancel()
        DeviceBusRuntime.stop()
        DeviceBusRuntime.markServiceRunning(false)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeConnectionState() {
        if (connectedJob != null) return
        connectedJob = serviceScope.launch {
            DeviceBusRuntime.client(applicationContext).connected.collect { connected ->
                DeviceBusRuntime.logConnectionState(connected)
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(connected))
            }
        }
    }

    private fun startBiometricLoop() {
        if (biometricJob != null) return

        biometricJob = serviceScope.launch {
            while (isActive) {
                val enabled = WatchSettingsStore.biometricIngestEnabledFlow(applicationContext).first()
                val client = DeviceBusRuntime.client(applicationContext)
                if (enabled && client.paired.value) {
                    val snapshot = HealthMonitor.readSnapshot(applicationContext)
                    val uploaded = client.uploadBiometricSnapshot(snapshot.toApiData())
                    DeviceBusRuntime.logBiometricUpload(uploaded)
                }
                delay(20_000L)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "RUNE-Watch Connection",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Ember connected to your other devices"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(connected: Boolean): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RUNE-Watch Connection")
            .setContentText(if (connected) "Connected to Ember bus" else "Reconnecting to Ember bus…")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
}
