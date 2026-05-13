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
import com.rune.watch.settings.WatchSettingsStore
import kotlinx.coroutines.flow.first
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

    private fun themeAccentArgb(themeMode: String): Int = when (themeMode) {
        WatchSettingsStore.THEME_PHOSPHOR -> 0xFFFFB300.toInt()
        WatchSettingsStore.THEME_JADE     -> 0xFF00E676.toInt()
        WatchSettingsStore.THEME_VOID     -> 0xFFB388FF.toInt()
        WatchSettingsStore.THEME_ASH      -> 0xFFCFD8DC.toInt()
        WatchSettingsStore.THEME_CRIMSON  -> 0xFFFF1744.toInt()
        else                              -> 0xFF00E5FF.toInt() // RUNE_DARK
    }

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
            val themeMode = WatchSettingsStore.themeModeFlow(applicationContext).first()
            val accentArgb = themeAccentArgb(themeMode)
            val accentRgb = accentArgb and 0x00FFFFFF
            val emberFrames = listOf(
                EmberFrame(accentArgb, accentRgb or 0x66000000.toInt()),
                EmberFrame(accentArgb, accentRgb or 0x44000000.toInt()),
                EmberFrame(accentArgb, accentRgb or 0x77000000.toInt()),
                EmberFrame(accentArgb, accentRgb or 0x55000000.toInt()),
                EmberFrame(accentArgb, accentRgb or 0x66000000.toInt()),
                EmberFrame(accentArgb, accentRgb or 0x44000000.toInt()),
            )
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

            val orbitStep = ((now / 8_131L) % 8L).toInt()
            val runeSymbols = listOf("ᚱ", "ᚢ", "ᚾ", "ᛖ", "ᛟ", "ᚠ", "ᛗ", "ᛁ")
            val runeStyle = FontStyle.Builder()
                .setSize(DimensionBuilders.sp(10f))
                .setColor(ColorBuilders.argb(accentRgb or 0xCC000000.toInt()))
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
                0 -> paddedBox(top = 6f, bottom = 68f, content = runeGlyph(symbol))
                1 -> paddedBox(top = 16f, bottom = 50f, start = 54f, end = 12f, content = runeGlyph(symbol))
                2 -> paddedBox(top = 0f, bottom = 0f, start = 68f, end = 6f, content = runeGlyph(symbol))
                3 -> paddedBox(top = 50f, bottom = 16f, start = 54f, end = 12f, content = runeGlyph(symbol))
                4 -> paddedBox(top = 68f, bottom = 6f, content = runeGlyph(symbol))
                5 -> paddedBox(top = 50f, bottom = 16f, start = 12f, end = 54f, content = runeGlyph(symbol))
                6 -> paddedBox(top = 0f, bottom = 0f, start = 6f, end = 68f, content = runeGlyph(symbol))
                else -> paddedBox(top = 16f, bottom = 50f, start = 12f, end = 54f, content = runeGlyph(symbol))
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
                        .setSize(DimensionBuilders.sp(50f))
                        .setColor(ColorBuilders.argb(accentRgb or 0xAA000000.toInt()))
                        .build()
                )
                .build()

            val glyphText = LayoutElementBuilders.Text.Builder()
                .setText("●")
                .setFontStyle(
                    FontStyle.Builder()
                        .setSize(DimensionBuilders.sp(30f))
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
                .setFreshnessIntervalMillis(2_200L)
                .setTileTimeline(
                    TimelineBuilders.Timeline.fromLayoutElement(stack)
                )
                .build()
        }

        return Futures.immediateFuture(tile)
    }
}
