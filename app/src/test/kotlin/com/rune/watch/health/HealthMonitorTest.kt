// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.health

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HealthMonitorTest {

    @Test
    fun biometricSnapshotToApiDataIncludesRequiredFields() {
        val snapshot = BiometricSnapshot(
            heartRate = 72,
            hrv = 50,
            stressLevel = 0.3,
            sleepState = "awake",
            movementState = "walking",
            lastUpdated = "2026-04-23T10:00:00Z",
        )

        val data = snapshot.toApiData()

        assertEquals("awake", data["sleepState"])
        assertEquals("walking", data["movementState"])
        assertEquals("rune-watch", data["source"])
        assertEquals(72, data["heartRate"])
        assertEquals("2026-04-23T10:00:00Z", data["lastUpdated"])
    }

    @Test
    fun biometricSnapshotHandlesNullSensorValues() {
        val snapshot = BiometricSnapshot(
            heartRate = null,
            hrv = null,
            stressLevel = null,
            sleepState = "awake",
            movementState = "unknown",
            lastUpdated = "2026-04-23T10:00:00Z",
        )

        val data = snapshot.toApiData()

        assertEquals("awake", data["sleepState"])
        assertEquals("unknown", data["movementState"])
        // Null values should not be in the payload
        assertEquals(false, "heartRate" in data)
        assertEquals(false, "hrv" in data)
        assertEquals(false, "stressLevel" in data)
    }

    @Test
    fun biometricSnapshotDefaultSourceIsRuneWatch() {
        val snapshot = BiometricSnapshot(
            sleepState = "light",
            movementState = "still",
            lastUpdated = "2026-04-23T10:00:00Z",
        )

        val data = snapshot.toApiData()
        assertEquals("rune-watch", data["source"])
    }

    @Test
    fun biometricSnapshotStressLevelInValidRange() {
        val snapshot = BiometricSnapshot(
            stressLevel = 0.65,
            sleepState = "awake",
            movementState = "active",
            lastUpdated = "2026-04-23T10:00:00Z",
        )

        val data = snapshot.toApiData()
        val stress = data["stressLevel"] as? Double
        assertNotNull(stress)
        assertEquals(true, stress >= 0.0 && stress <= 1.0)
    }
}
