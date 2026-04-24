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
    fun toApiData(): Map<String, Any> {
        val payload = mutableMapOf<String, Any>(
            "sleepState" to sleepState,
            "movementState" to movementState,
            "lastUpdated" to lastUpdated,
            "source" to source,
        )

        heartRate?.let { payload["heartRate"] = it }
        hrv?.let { payload["hrv"] = it }
        stressLevel?.let { payload["stressLevel"] = it }

        return payload
    }
}

object HealthMonitor {
    fun readSnapshot(context: Context): BiometricSnapshot {
        val powerManager = context.getSystemService(PowerManager::class.java)
        val sleepState = if (powerManager?.isInteractive == true) "awake" else "light"

        // Hardware heart-rate/HRV ingestion will be connected via Health Services API in next pass.
        return BiometricSnapshot(
            heartRate = null,
            hrv = null,
            stressLevel = null,
            sleepState = sleepState,
            movementState = "unknown",
            lastUpdated = Instant.now().toString(),
        )
    }
}
