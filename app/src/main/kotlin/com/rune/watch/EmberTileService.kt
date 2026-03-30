package com.rune.watch

import androidx.wear.tiles.*
import androidx.wear.tiles.material.*
import androidx.wear.tiles.material.layouts.PrimaryLayout
import com.google.android.horologist.tiles.SuspendingTileService

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
        val deviceResourcesVersion = requestParams.currentState.lastClickableId

        val layout = PrimaryLayout.Builder(requestParams.deviceConfiguration)
            .setContent(
                Text.Builder(this, "Ember ready")
                    .setTypography(Typography.TYPOGRAPHY_BODY1)
                    .setColor(ColorBuilders.argb(0xFFFF7043.toInt()))
                    .build()
            )
            .build()

        return TileBuilders.Tile.Builder()
            .setResourcesVersion("1")
            .setTileTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(layout)
            )
            .build()
    }
}
