// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.health

import android.content.Context
import android.os.PowerManager
import java.time.Instant

data class BiometricSnapshot(
    val heartRate: Int? = null,
    val hrv: Int? = null,
    val stressLevel: Double? = null,
    val sleepState: String,
    val movementState: String,
    val lastUpdated: String,
    val source: String = "rune-watch",
) {
    fun toApiData(
        includeHeartRate: Boolean = true,
        includeHrv: Boolean = true,
        includeStress: Boolean = true,
        includeSleep: Boolean = true,
        includeMovement: Boolean = true,
    ): Map<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "lastUpdated" to lastUpdated,
            "source" to source,
        )

        if (includeSleep) payload["sleepState"] = sleepState
        if (includeMovement) payload["movementState"] = movementState
        if (includeHeartRate) heartRate?.let { payload["heartRate"] = it }
        if (includeHrv) hrv?.let { payload["hrv"] = it }
        if (includeStress) stressLevel?.let { payload["stressLevel"] = it }

        return payload
    }
}

object HealthMonitor {
    suspend fun readSnapshotAsync(context: Context): BiometricSnapshot {
        val powerManager = context.getSystemService(PowerManager::class.java)
        val sleepState = if (powerManager?.isInteractive == true) "awake" else "light"

        // Attempt to read heart rate from Health Services API
        val heartRate = HealthServicesReader.readHeartRate(context)

        // Movement state and HRV will be connected via device-specific sensor APIs in field testing
        return BiometricSnapshot(
            heartRate = heartRate,
            hrv = null,
            stressLevel = null,
            sleepState = sleepState,
            movementState = "unknown",
            lastUpdated = Instant.now().toString(),
        )
    }
}
