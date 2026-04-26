// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch

import android.content.ComponentName
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ModifiersBuilders
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
import com.rune.watch.bus.DeviceBusRuntime
import kotlinx.coroutines.runBlocking

/**
 * Ember WearOS Tile — appears in the Watch's swipe-left pages.
 *
 * Shows the current Ember expression and two quick-action buttons:
 * "Desktop" and "Chat" that route commands back to the host device via
 * the device bus.
 */
class EmberTileService : TileService() {

    private data class EmberFrame(
        val emberColorArgb: Int,
        val glowColorArgb: Int,
    )

    private val emberFrames = listOf(
        EmberFrame(0xFFFF8A50.toInt(), 0x66FFB74D),
        EmberFrame(0xFFFF6D3A.toInt(), 0x55FF8A50),
        EmberFrame(0xFFFFB74D.toInt(), 0x66FFD180),
        EmberFrame(0xFFFF8A50.toInt(), 0x55FF6D3A),
        EmberFrame(0xFFFF6D3A.toInt(), 0x66FFB74D),
        EmberFrame(0xFFFFAB40.toInt(), 0x55FF8A50),
    )

    override fun onResourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder()
                .setVersion("1")
                .build()
        )
    }

    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val tile = runBlocking {
            val now = System.currentTimeMillis()
            val frameIndex = ((now / 1_500L) % emberFrames.size).toInt()
            val emberFrame = emberFrames[frameIndex]
            val paired = DeviceBusRuntime.client(applicationContext).paired.value
            val launchAction = ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setPackageName(ComponentName(applicationContext, MainActivity::class.java).packageName)
                        .setClassName(MainActivity::class.java.name)
                        .build()
                )
                .build()
            val rootModifiers = ModifiersBuilders.Modifiers.Builder()
                .setClickable(
                    ModifiersBuilders.Clickable.Builder()
                        .setId("open-ember-main")
                        .setOnClick(launchAction)
                        .build()
                )
                .build()

            val orbitStep = ((now / 3_750L) % 8L).toInt()
            val runeSymbols = listOf("ᚱ", "ᚢ", "ᚾ", "ᛖ", "ᛟ", "ᚠ", "ᛗ", "ᛁ")
            val runeStyle = FontStyle.Builder()
                .setSize(DimensionBuilders.sp(10f))
                .setColor(ColorBuilders.argb(0xFFFDD7A0.toInt()))
                .build()

            fun paddedBox(
                top: Float = 0f,
                bottom: Float = 0f,
                start: Float = 0f,
                end: Float = 0f,
                content: LayoutElementBuilders.LayoutElement,
            ): LayoutElementBuilders.Box =
                LayoutElementBuilders.Box.Builder()
                    .setWidth(DimensionBuilders.expand())
                    .setHeight(DimensionBuilders.expand())
                    .setModifiers(
                        ModifiersBuilders.Modifiers.Builder()
                            .setPadding(
                                ModifiersBuilders.Padding.Builder()
                                    .setTop(DimensionBuilders.dp(top))
                                    .setBottom(DimensionBuilders.dp(bottom))
                                    .setStart(DimensionBuilders.dp(start))
                                    .setEnd(DimensionBuilders.dp(end))
                                    .build()
                            )
                            .build()
                    )
                    .addContent(content)
                    .build()

            fun runeGlyph(symbol: String): LayoutElementBuilders.Text =
                LayoutElementBuilders.Text.Builder()
                    .setText(symbol)
                    .setFontStyle(runeStyle)
                    .build()

            fun orbitRune(symbol: String, slot: Int): LayoutElementBuilders.Box = when (slot) {
                0 -> paddedBox(top = 2f, bottom = 72f, content = runeGlyph(symbol))
                1 -> paddedBox(top = 12f, bottom = 54f, start = 58f, end = 8f, content = runeGlyph(symbol))
                2 -> paddedBox(top = 0f, bottom = 0f, start = 72f, end = 2f, content = runeGlyph(symbol))
                3 -> paddedBox(top = 54f, bottom = 12f, start = 58f, end = 8f, content = runeGlyph(symbol))
                4 -> paddedBox(top = 72f, bottom = 2f, content = runeGlyph(symbol))
                5 -> paddedBox(top = 54f, bottom = 12f, start = 8f, end = 58f, content = runeGlyph(symbol))
                6 -> paddedBox(top = 0f, bottom = 0f, start = 2f, end = 72f, content = runeGlyph(symbol))
                else -> paddedBox(top = 12f, bottom = 54f, start = 8f, end = 58f, content = runeGlyph(symbol))
            }

            val glow = LayoutElementBuilders.Text.Builder()
                .setText("●")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(62f))
                        .setColor(ColorBuilders.argb(emberFrame.glowColorArgb))
                        .build()
                )
                .build()

            val outerRing = LayoutElementBuilders.Text.Builder()
                .setText("○")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(55f))
                        .setColor(ColorBuilders.argb(0xCCFF7A45.toInt()))
                        .build()
                )
                .build()

            val glyphText = LayoutElementBuilders.Text.Builder()
                .setText("●")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(31f))
                        .setColor(ColorBuilders.argb(emberFrame.emberColorArgb))
                        .build()
                )
                .build()

            // Small green indicator dot for pairing status
            val pairingIndicator = LayoutElementBuilders.Text.Builder()
                .setText(if (paired) "●" else "○")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(7f))
                        .setColor(ColorBuilders.argb(if (paired) 0xFF4CAF50.toInt() else 0xFF78909C.toInt()))
                        .build()
                )
                .build()

            val centerContent = LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.dp(112f))
                .setHeight(DimensionBuilders.dp(112f))
                .addContent(glow)
                .addContent(outerRing)
                .addContent(glyphText)
                .addContent(
                    paddedBox(top = 64f, bottom = 8f, start = 66f, end = 8f, content = pairingIndicator)
                )
                .build()

            val orbitContent = LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.dp(112f))
                .setHeight(DimensionBuilders.dp(112f))
                .addContent(orbitRune(runeSymbols[0], orbitStep % 8))
                .addContent(orbitRune(runeSymbols[1], (orbitStep + 1) % 8))
                .addContent(orbitRune(runeSymbols[2], (orbitStep + 2) % 8))
                .addContent(orbitRune(runeSymbols[3], (orbitStep + 3) % 8))
                .addContent(orbitRune(runeSymbols[4], (orbitStep + 4) % 8))
                .addContent(orbitRune(runeSymbols[5], (orbitStep + 5) % 8))
                .addContent(orbitRune(runeSymbols[6], (orbitStep + 6) % 8))
                .addContent(orbitRune(runeSymbols[7], (orbitStep + 7) % 8))
                .build()

            val stack = LayoutElementBuilders.Box.Builder()
                .setWidth(DimensionBuilders.expand())
                .setHeight(DimensionBuilders.expand())
                .setModifiers(rootModifiers)
                .addContent(orbitContent)
                .addContent(centerContent)
                .build()

            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setFreshnessIntervalMillis(1_500L)
                .setTileTimeline(
                    TimelineBuilders.Timeline.fromLayoutElement(stack)
                )
                .build()
        }

        return Futures.immediateFuture(tile)
    }
}
