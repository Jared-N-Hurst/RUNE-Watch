package com.rune.watch.bus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps the DeviceBusClient WebSocket alive
 * even when MainActivity is not in the foreground.
 *
 * The system shows a persistent notification while the service runs.
 * Users can dismiss it by stopping sync from the Ember watch app settings.
 */
class DeviceBusService : Service() {

    companion object {
        private const val CHANNEL_ID = "ember_sync"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Ember Sync",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps Ember connected to your other devices"
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Ember")
            .setContentText("Syncing with your devices…")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
}
