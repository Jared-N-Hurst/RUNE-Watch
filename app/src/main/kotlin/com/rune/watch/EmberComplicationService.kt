package com.rune.watch

import androidx.wear.watchface.complications.data.*
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

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
        when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder("E").build(),
                contentDescription = PlainComplicationText.Builder("Ember").build()
            ).build()

            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text = PlainComplicationText.Builder("Ember").build(),
                contentDescription = PlainComplicationText.Builder("Ember AI assistant").build()
            ).setTitle(PlainComplicationText.Builder("Listening…").build()).build()

            else -> null
        }
}
