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
    const val THEME_GHOST = "ghost"
    const val THEME_LIGHT = "light"

    private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    private val KEY_BIOMETRIC_INGEST_ENABLED = booleanPreferencesKey("biometric_ingest_enabled")

    fun themeModeFlow(context: Context): Flow<String> =
        context.applicationContext.emberPrefsDataStore.data
            .map { prefs -> prefs[KEY_THEME_MODE] ?: THEME_GHOST }
            .distinctUntilChanged()

    fun biometricIngestEnabledFlow(context: Context): Flow<Boolean> =
        context.applicationContext.emberPrefsDataStore.data
            .map { prefs -> prefs[KEY_BIOMETRIC_INGEST_ENABLED] ?: true }
            .distinctUntilChanged()

    suspend fun toggleThemeMode(context: Context) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_THEME_MODE] ?: THEME_GHOST
            prefs[KEY_THEME_MODE] =
                if (current == THEME_GHOST) THEME_LIGHT else THEME_GHOST
        }
    }

    suspend fun toggleBiometricIngest(context: Context) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            val current = prefs[KEY_BIOMETRIC_INGEST_ENABLED] ?: true
            prefs[KEY_BIOMETRIC_INGEST_ENABLED] = !current
        }
    }
}