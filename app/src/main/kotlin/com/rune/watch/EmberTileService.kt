// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.ResourceBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.FontStyle
import androidx.wear.protolayout.TimelineBuilders
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rune.watch.bus.IdentityMapClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

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
class EmberTileService : TileService() {

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .build()
        )
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        // FUT-3: load compact identity map for non-mythic tile display
        val tile = runBlocking {
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
                else          -> "Ember ready"
            }
            val resonanceLabel = identityMap?.let {
                " R:${(it.isilmeResonance * 100).toInt()}%"
            } ?: ""

            val text = LayoutElementBuilders.Text.Builder()
                .setText("$displayText$resonanceLabel")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(16f))
                        .setColor(ColorBuilders.argb(0xFFFF7043.toInt()))
                        .build()
                )
                .build()

            val layout = LayoutElementBuilders.Box.Builder()
                .addContent(text)
                .build()

            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(5 * 60 * 1_000L) // refresh every 5 minutes
                .setTileTimeline(
                    TimelineBuilders.Timeline.fromLayoutElement(layout)
                )
                .build()
        }

        return Futures.immediateFuture(tile)
    }
}
