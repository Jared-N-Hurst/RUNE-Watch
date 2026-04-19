// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.wear.tiles.*
import androidx.wear.tiles.material.*
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.android.horologist.tiles.SuspendingTileService
import com.rune.watch.bus.IdentityMapClient
import kotlinx.coroutines.flow.first

private val android.content.Context.dataStore by preferencesDataStore("ember_prefs")
private val KEY_USER_ID = stringPreferencesKey("user_id")
private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")

/**
 * Ember WearOS Tile — appears in the Watch's swipe-left pages.
 *
 * Shows the current Ember expression and two quick-action buttons:
 * "Desktop" and "Chat" that route commands back to the host device via
 * the device bus.
 */
class EmberTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion("1")
            .build()
    }

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        // FUT-3: load compact identity map for non-mythic tile display
        val prefs = dataStore.data.first()
        val userId = prefs[KEY_USER_ID] ?: ""
        val authToken = prefs[KEY_AUTH_TOKEN] ?: ""

        val identityMap = if (userId.isNotEmpty() && authToken.isNotEmpty()) {
            IdentityMapClient().fetchCompact(userId, authToken)
        } else null

        val displayText = when (identityMap?.convergenceState) {
            "complete"   -> "Cycle complete"
            "converging" -> "Converging"
            "stirring"   -> "Stirring"
            else         -> "Ember ready"
        }
        val resonanceLabel = identityMap?.let {
            " R:${(it.isilmeResonance * 100).toInt()}%"
        } ?: ""

        val layout = PrimaryLayout.Builder(requestParams.deviceConfiguration)
            .setContent(
                Text.Builder(this, "$displayText$resonanceLabel")
                    .setTypography(Typography.TYPOGRAPHY_BODY1)
                    .setColor(ColorBuilders.argb(0xFFFF7043.toInt()))
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setFreshnessIntervalMillis(5 * 60 * 1_000L) // refresh every 5 minutes
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(layout)
            )
            .build()
    }
}
