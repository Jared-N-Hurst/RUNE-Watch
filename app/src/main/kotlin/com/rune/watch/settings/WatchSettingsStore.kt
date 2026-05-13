// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rune.watch.storage.emberPrefsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

object WatchSettingsStore {
    // Phone-aligned theme constants
    const val THEME_RUNE_DARK = "rune-dark"
    const val THEME_PHOSPHOR = "phosphor"
    const val THEME_JADE = "jade"
    const val THEME_VOID = "void"
    const val THEME_ASH = "ash"
    const val THEME_CRIMSON = "crimson"

    // Legacy values preserved for migration-only normalization
    private const val THEME_GHOST_LEGACY = "ghost"
    private const val THEME_LIGHT_LEGACY = "light"

    // Font constants
    const val FONT_DEFAULT = "default"
    const val FONT_MONO = "mono"

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_FONT_MODE = stringPreferencesKey("font_mode")
    private val KEY_BIOMETRIC_INGEST_ENABLED = booleanPreferencesKey("biometric_ingest_enabled")
    private val KEY_BIO_HEART_RATE_ENABLED = booleanPreferencesKey("bio_heart_rate_enabled")
    private val KEY_BIO_HRV_ENABLED = booleanPreferencesKey("bio_hrv_enabled")
    private val KEY_BIO_STRESS_ENABLED = booleanPreferencesKey("bio_stress_enabled")
    private val KEY_BIO_SLEEP_ENABLED = booleanPreferencesKey("bio_sleep_enabled")
    private val KEY_BIO_MOVEMENT_ENABLED = booleanPreferencesKey("bio_movement_enabled")

    data class BiometricStreamPrefs(
        val enabled: Boolean,
        val heartRate: Boolean,
        val hrv: Boolean,
        val stress: Boolean,
        val sleep: Boolean,
        val movement: Boolean,
    )

    fun themeModeFlow(context: Context): Flow<String> =
        context.applicationContext.emberPrefsDataStore.data
            .map { prefs ->
                when (val raw = prefs[KEY_THEME_MODE] ?: THEME_RUNE_DARK) {
                    THEME_GHOST_LEGACY, THEME_LIGHT_LEGACY -> THEME_RUNE_DARK
                    else -> raw
                }
            }
            .distinctUntilChanged()

    fun fontModeFlow(context: Context): Flow<String> =
        context.applicationContext.emberPrefsDataStore.data
            .map { prefs -> prefs[KEY_FONT_MODE] ?: FONT_DEFAULT }
            .distinctUntilChanged()

    fun biometricIngestEnabledFlow(context: Context): Flow<Boolean> =
        context.applicationContext.emberPrefsDataStore.data
            .map { prefs -> prefs[KEY_BIOMETRIC_INGEST_ENABLED] ?: true }
            .distinctUntilChanged()

    fun biometricStreamPrefsFlow(context: Context): Flow<BiometricStreamPrefs> =
        context.applicationContext.emberPrefsDataStore.data
            .map { prefs ->
                BiometricStreamPrefs(
                    enabled = prefs[KEY_BIOMETRIC_INGEST_ENABLED] ?: true,
                    heartRate = prefs[KEY_BIO_HEART_RATE_ENABLED] ?: true,
                    hrv = prefs[KEY_BIO_HRV_ENABLED] ?: true,
                    stress = prefs[KEY_BIO_STRESS_ENABLED] ?: true,
                    sleep = prefs[KEY_BIO_SLEEP_ENABLED] ?: true,
                    movement = prefs[KEY_BIO_MOVEMENT_ENABLED] ?: true,
                )
            }
            .distinctUntilChanged()

    suspend fun setThemeMode(context: Context, theme: String) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = theme
        }
    }

    suspend fun setFontMode(context: Context, font: String) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            prefs[KEY_FONT_MODE] = font
        }
    }

    suspend fun toggleBiometricIngest(context: Context) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_BIOMETRIC_INGEST_ENABLED] ?: true
            prefs[KEY_BIOMETRIC_INGEST_ENABLED] = !current
        }
    }

    suspend fun toggleHeartRate(context: Context) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_BIO_HEART_RATE_ENABLED] ?: true
            prefs[KEY_BIO_HEART_RATE_ENABLED] = !current
        }
    }

    suspend fun toggleHrv(context: Context) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_BIO_HRV_ENABLED] ?: true
            prefs[KEY_BIO_HRV_ENABLED] = !current
        }
    }

    suspend fun toggleStress(context: Context) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_BIO_STRESS_ENABLED] ?: true
            prefs[KEY_BIO_STRESS_ENABLED] = !current
        }
    }

    suspend fun toggleSleep(context: Context) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_BIO_SLEEP_ENABLED] ?: true
            prefs[KEY_BIO_SLEEP_ENABLED] = !current
        }
    }

    suspend fun toggleMovement(context: Context) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_BIO_MOVEMENT_ENABLED] ?: true
            prefs[KEY_BIO_MOVEMENT_ENABLED] = !current
        }
    }
}