// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class WatchRelayTranscriptTest {

    @Test
    fun appendOutgoingRelayTranscript_addsWatchEntryWithSendingStatus() {
        val transcript = appendOutgoingRelayTranscript(
            current = emptyList(),
            text = "Send update to desktop",
            threadId = "thread-4",
            updatedAtMs = 42L,
        )

        assertEquals(1, transcript.size)
        assertEquals("Watch", transcript.first().sourceLabel)
        assertEquals("sending", transcript.first().status)
        assertEquals("thread-4", transcript.first().threadId)
    }

    @Test
    fun applyRelayChatEvent_appendsInboundMessage() {
        val transcript = applyRelayChatEvent(
            current = emptyList(),
            event = ParsedRelayChatEvent(
                eventId = "relay-in-1",
                text = "Desktop reply",
                sourceLabel = "Desktop",
                sourceSurface = "desktop",
                threadId = "thread-7",
            ),
            updatedAtMs = 100L,
        )

        assertEquals(1, transcript.size)
        assertEquals("Desktop", transcript.first().sourceLabel)
        assertEquals("Desktop reply", transcript.first().text)
        assertNull(transcript.first().status)
    }

    @Test
    fun applyRelayChatEvent_updatesLatestMatchingEntryForReceipt() {
        val seed = appendOutgoingRelayTranscript(
            current = emptyList(),
            text = "Take screenshot",
            threadId = "thread-9",
            updatedAtMs = 1L,
        )

        val transcript = applyRelayChatEvent(
            current = seed,
            event = ParsedRelayChatEvent(
                eventId = "relay-receipt-1",
                text = "",
                sourceLabel = "Desktop",
                sourceSurface = "desktop",
                threadId = "thread-9",
                status = "seen",
            ),
            updatedAtMs = 2L,
        )

        assertEquals(1, transcript.size)
        assertEquals("seen", transcript.first().status)
        assertEquals("Desktop", transcript.first().sourceLabel)
    }

    @Test
    fun applyRelayChatEvent_ignoresReceiptWithoutMatchingEntry() {
        val transcript = applyRelayChatEvent(
            current = emptyList(),
            event = ParsedRelayChatEvent(
                eventId = "relay-receipt-2",
                text = "",
                sourceLabel = "Phone",
                sourceSurface = "mobile",
                threadId = "thread-11",
                status = "seen",
            ),
            updatedAtMs = 4L,
        )

        assertEquals(0, transcript.size)
    }

    @Test
    fun relayTranscript_roundTripsThroughJsonEncoding() {
        val encoded = encodeRelayTranscript(
            listOf(
                WatchRelayTranscriptEntry(
                    entryId = "entry-1",
                    text = "Desktop update",
                    sourceLabel = "Desktop",
                    sourceSurface = "desktop",
                    threadId = "thread-14",
                    status = "seen",
                    fromWatch = false,
                    updatedAtMs = 20L,
                )
            )
        )

        val decoded = decodeRelayTranscript(encoded)
        assertEquals(1, decoded.size)
        assertEquals("entry-1", decoded.first().entryId)
        assertEquals("Desktop update", decoded.first().text)
        assertEquals("thread-14", decoded.first().threadId)
        assertEquals("seen", decoded.first().status)
        assertFalse(decoded.first().fromWatch)
    }

    @Test
    fun relayTranscript_decodeSkipsMalformedEntries() {
        val decoded = decodeRelayTranscript("""
            [
              {"entryId":"","text":"ignored","sourceLabel":"Desktop","updatedAtMs":10},
              {"entryId":"ok-1","text":"Saved item","sourceLabel":"Watch","updatedAtMs":11,"fromWatch":true}
            ]
        """.trimIndent())

        assertEquals(1, decoded.size)
        assertEquals("ok-1", decoded.first().entryId)
        assertEquals("Saved item", decoded.first().text)
    }

    @Test
    fun deriveOutgoingRelayThreadId_prefersLatestKnownThread() {
        val transcript = listOf(
            WatchRelayTranscriptEntry(
                entryId = "entry-1",
                text = "Earlier message",
                sourceLabel = "Desktop",
                sourceSurface = "desktop",
                threadId = "thread-18",
                status = null,
                fromWatch = false,
                updatedAtMs = 1L,
            ),
            WatchRelayTranscriptEntry(
                entryId = "entry-2",
                text = "Recent watch reply",
                sourceLabel = "Watch",
                sourceSurface = "watch",
                threadId = "thread-22",
                status = "seen",
                fromWatch = true,
                updatedAtMs = 2L,
            ),
        )

        assertEquals("thread-22", deriveOutgoingRelayThreadId(transcript, "fallback-thread-1"))
    }

    @Test
    fun deriveOutgoingRelayThreadId_usesFallbackWhenTranscriptHasNoThread() {
        val transcript = listOf(
            WatchRelayTranscriptEntry(
                entryId = "entry-3",
                text = "Untagged message",
                sourceLabel = "Watch",
                sourceSurface = "watch",
                threadId = null,
                status = "sending",
                fromWatch = true,
                updatedAtMs = 3L,
            ),
        )

        assertEquals("fallback-thread-7", deriveOutgoingRelayThreadId(transcript, "fallback-thread-7"))
    }

    @Test
    fun mergeSharedRelayTranscriptSnapshot_dropsPendingEntryFromDifferentConversation() {
        val merged = mergeSharedRelayTranscriptSnapshot(
            current = listOf(
                WatchRelayTranscriptEntry(
                    entryId = "local-pending-2",
                    text = "Pending watch reply",
                    sourceLabel = "Watch",
                    sourceSurface = "watch",
                    threadId = "thread-other",
                    status = "sending",
                    fromWatch = true,
                    updatedAtMs = 50L,
                )
            ),
            rawResponse = """
                {"ok":true,"data":{"snapshot":{"conversationId":"thread-44","syncSource":"backend","syncVersion":1,"updatedAtMs":120,"messages":[{"messageId":"msg-44","clientMessageId":"msg-44","conversationId":"thread-44","text":"Desktop confirmed","sourceLabel":"Desktop","sourceSurface":"desktop","deliveryStatus":"delivered","createdAtMs":75}],"receipts":[]}}}
            """.trimIndent(),
        )

        assertEquals(listOf("msg-44"), merged?.map { it.entryId })
    }

    @Test
    fun readSharedRelayTranscriptSummaries_readsRecentSnapshotMetadata() {
        val summaries = readSharedRelayTranscriptSummaries(
            """
                {"ok":true,"data":{"recentSnapshots":[{"conversationId":"thread-55","updatedAtMs":155,"messageCount":2,"receiptCount":1,"latestMessageText":"Desktop follow-up"}]}}
            """.trimIndent()
        )

        assertEquals(1, summaries.size)
        assertEquals("thread-55", summaries.first().conversationId)
        assertEquals("Desktop follow-up", summaries.first().latestMessageText)
    }
}