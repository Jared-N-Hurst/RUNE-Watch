// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.rune.watch.bus.EmberStatusClient
import com.rune.watch.bus.IdentityMapClient
import com.rune.watch.storage.emberPrefsDataStore
import kotlinx.coroutines.flow.first
private val KEY_USER_ID = stringPreferencesKey("user_id")
private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")

/**
 * Provides Ember status as a watch face complication.
 * Any WearOS watch face that supports complications can show Ember state
 * (expression, online indicator, etc.) directly on the dial.
 */
class EmberComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        when (type) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("E").build(),
                contentDescription = PlainComplicationText.Builder("Ember").build()
            ).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Ember").build(),
                contentDescription = PlainComplicationText.Builder("Ember AI assistant").build()
            ).setTitle(PlainComplicationText.Builder("Ready").build()).build()

            else -> null
        }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? =
        run {
            val prefs = applicationContext.emberPrefsDataStore.data.first()
            val userId = prefs[KEY_USER_ID] ?: ""
            val authToken = prefs[KEY_AUTH_TOKEN] ?: ""

            val emberStatus = if (userId.isNotEmpty() && authToken.isNotEmpty()) {
                EmberStatusClient().fetchTileStatus(userId, authToken)
            } else null
            val fallbackIdentity = if (emberStatus == null && userId.isNotEmpty() && authToken.isNotEmpty()) {
                IdentityMapClient().fetchCompact(userId, authToken)
            } else null

            when (request.complicationType) {
                ComplicationType.SHORT_TEXT -> {
                    val shortLabel = emberStatus?.let {
                        if (it.riskCount > 0) "!${it.riskCount}" else it.phaseLabel.take(1)
                    } ?: fallbackIdentity?.convergenceState?.take(1)?.uppercase() ?: "E"
                    ShortTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(shortLabel).build(),
                        contentDescription = PlainComplicationText.Builder("Ember").build()
                    ).build()
                }

                ComplicationType.LONG_TEXT -> {
                    val title = emberStatus?.phaseLabel
                        ?: when (fallbackIdentity?.convergenceState) {
                            "complete" -> "Complete"
                            "converging" -> "Converging"
                            "stirring" -> "Stirring"
                            else -> "Ready"
                        }
                    val body = emberStatus?.engagementSummary ?: "Ember AI assistant"
                    LongTextComplicationData.Builder(
                        text = PlainComplicationText.Builder(body).build(),
                        contentDescription = PlainComplicationText.Builder("Ember AI assistant").build()
                    ).setTitle(PlainComplicationText.Builder(title).build()).build()
                }

                else -> null
            }
        }
}
