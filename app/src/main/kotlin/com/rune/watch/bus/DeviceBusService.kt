// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
        private const val CHANNEL_ID = "ember_sync"
        private const val NOTIFICATION_ID = 1001
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var connectedJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification(connected = false))
        DeviceBusRuntime.start(applicationContext)
        observeConnectionState()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        connectedJob?.cancel()
        connectedJob = null
        serviceScope.cancel()
        DeviceBusRuntime.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeConnectionState() {
        if (connectedJob != null) return
        connectedJob = serviceScope.launch {
            DeviceBusRuntime.client(applicationContext).connected.collect { connected ->
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification(connected))
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
