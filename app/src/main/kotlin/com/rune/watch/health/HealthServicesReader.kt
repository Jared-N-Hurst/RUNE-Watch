// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.health

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.core.content.ContextCompat
import com.rune.watch.BuildConfig
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Wrapper for biometric sensor reading on Wear OS
 *
 * This layer handles:
 * - Permission checking (BODY_SENSORS)
 * - Device-specific sensor API integration
 * - Graceful degradation if sensors unavailable
 *
 * Note: Full Health Services API integration pending Galaxy Watch 7 physical testing.
 * Current implementation returns null with graceful fallback to PowerManager-based state.
 */
object HealthServicesReader {
    private const val HEART_RATE_MIN_BPM = 30
    private const val HEART_RATE_MAX_BPM = 240

    private var enabledOverrideForTests: Boolean? = null
    private var timeoutOverrideMsForTests: Long? = null

    internal fun setEnabledForTests(enabled: Boolean?) {
        enabledOverrideForTests = enabled
    }

    internal fun setTimeoutMsForTests(timeoutMs: Long?) {
        timeoutOverrideMsForTests = timeoutMs
    }

    internal fun resetForTests() {
        enabledOverrideForTests = null
        timeoutOverrideMsForTests = null
    }

    internal fun normalizeHeartRate(raw: Float?): Int? {
        if (raw == null || raw.isNaN() || !raw.isFinite()) return null
        val bpm = raw.toInt()
        if (bpm < HEART_RATE_MIN_BPM || bpm > HEART_RATE_MAX_BPM) return null
        return bpm
    }

    private fun isRuntimeEnabled(): Boolean {
        return enabledOverrideForTests ?: BuildConfig.HEART_RATE_SENSOR_ENABLED
    }

    private fun timeoutMs(): Long {
        return timeoutOverrideMsForTests ?: BuildConfig.HEART_RATE_READ_TIMEOUT_MS
    }

    /**
     * Attempt to read current heart rate from device sensors
     *
     * Returns null if:
     * - Device lacks heart rate sensor
     * - BODY_SENSORS permission not granted
     * - Health Services API unavailable
     * - Real-time data not currently available
     */
    suspend fun readHeartRate(context: Context): Int? {
        if (!isRuntimeEnabled()) {
            return null
        }

        if (!hasBodySensorPermission(context)) {
            return null
        }

        return readFromSensorWithTimeout(context, timeoutMs())
    }

    private suspend fun readFromSensorWithTimeout(context: Context, timeoutMs: Long): Int? {
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                val sensor = manager?.getDefaultSensor(Sensor.TYPE_HEART_RATE)
                if (manager == null || sensor == null) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        val value = normalizeHeartRate(event?.values?.firstOrNull())
                        if (value != null && cont.isActive) {
                            manager.unregisterListener(this)
                            cont.resume(value)
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        // No-op
                    }
                }

                val registered = runCatching {
                    manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                }.getOrDefault(false)

                if (!registered) {
                    cont.resume(null)
                    return@suspendCancellableCoroutine
                }

                cont.invokeOnCancellation {
                    runCatching { manager.unregisterListener(listener) }
                }
            }
        }
    }

    private fun hasBodySensorPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BODY_SENSORS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

