// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.storage

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// Single app-wide DataStore instance for ember_prefs.
val Context.emberPrefsDataStore by preferencesDataStore("ember_prefs")
