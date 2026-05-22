// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmberEventSubscriberTest {

    private fun event(
        eventType: String,
        sequence: Long,
        surface: String = "watch",
        incidentClass: String? = null,
    ): WatchEmberEventEnvelopeV1 {
        return WatchEmberEventEnvelopeV1(
            eventSchemaVersion = "el-1.v1",
            eventId = "evt-$sequence",
            emberId = "ember-1",
            userId = "user-1",
            eventType = eventType,
            provenance = "ember",
            surface = surface,
            sequence = sequence,
            occurredAt = "2026-05-17T00:00:00Z",
            recordedAt = "2026-05-17T00:00:01Z",
            incidentClass = incidentClass,
        )
    }

    @Test
    fun canEmitToWatch_blocks_execution_events_by_default() {
        val supported = event("EmberReply", 2)
        val blocked = event("ActionStarted", 3)

        assertTrue(canEmitToWatch(supported))
        assertFalse(canEmitToWatch(blocked))
    }

    @Test
    fun filterWatchSupportedEvents_keeps_supported_and_orders_sequence() {
        val input = listOf(
            event("ActionRequested", 10),
            event("FieldUpdate", 2),
            event("UserMessage", 1),
            event("ActionCompleted", 8),
        )

        val filtered = filterWatchSupportedEvents(input)

        assertEquals(3, filtered.size)
        assertEquals(listOf(1L, 2L, 8L), filtered.map { it.sequence })
        assertEquals(listOf("UserMessage", "FieldUpdate", "ActionCompleted"), filtered.map { it.eventType })
    }

    @Test
    fun canEmitToWatch_allows_supported_incident_class_and_rejects_unsupported() {
        val supportedIncident = event("GhostIntervention", 20, incidentClass = "auth")
        val unsupportedIncident = event("GhostIntervention", 21, incidentClass = "database")

        assertTrue(canEmitToWatch(supportedIncident))
        assertFalse(canEmitToWatch(unsupportedIncident))
    }

    @Test
    fun filterWatchSupportedEvents_dedupes_event_ids_and_keeps_latest_sequence() {
        val original = event("UserMessage", 2)
        val upgraded = event("UserMessage", 6)
        val input = listOf(
            original,
            event("ActionRequested", 9),
            upgraded.copy(eventId = original.eventId),
            event("FieldUpdate", 4),
        )

        val filtered = filterWatchSupportedEvents(input)

        assertEquals(2, filtered.size)
        assertEquals(listOf(4L, 6L), filtered.map { it.sequence })
    }

    @Test
    fun resolveReplayCursor_advances_to_latest_sequence() {
        val filtered = filterWatchSupportedEvents(
            listOf(
                event("UserMessage", 3),
                event("FieldUpdate", 6),
            ),
        )

        val cursor = resolveReplayCursor(2, filtered)

        assertEquals(6L, cursor)
    }

    @Test
    fun resolveReplayCursor_ignores_regressive_candidate_under_chaos() {
        val filtered = filterWatchSupportedEvents(
            listOf(
                event("UserMessage", 2),
                event("FieldUpdate", 4),
            ),
        )

        val cursor = resolveReplayCursor(9, filtered)

        assertEquals(9L, cursor)
    }
}
