// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import org.json.JSONArray
import org.json.JSONObject

private const val WATCH_RELAY_TRANSCRIPT_MAX = 12

data class WatchRelayTranscriptSummary(
    val conversationId: String,
    val updatedAtMs: Long,
    val messageCount: Int,
    val receiptCount: Int,
    val latestMessageText: String? = null,
)

data class WatchRelayTranscriptEntry(
    val entryId: String,
    val text: String,
    val sourceLabel: String,
    val sourceSurface: String? = null,
    val threadId: String? = null,
    val status: String? = null,
    val fromWatch: Boolean = false,
    val updatedAtMs: Long,
)

private fun defaultTranscriptSourceLabel(sourceSurface: String?): String {
    return when (sourceSurface) {
        "desktop" -> "Desktop"
        "mobile" -> "Phone"
        "watch" -> "Watch"
        "web" -> "Web"
        else -> "Linked Surface"
    }
}

private fun trimTranscript(entries: List<WatchRelayTranscriptEntry>): List<WatchRelayTranscriptEntry> {
    return if (entries.size <= WATCH_RELAY_TRANSCRIPT_MAX) entries else entries.takeLast(WATCH_RELAY_TRANSCRIPT_MAX)
}

internal fun appendOutgoingRelayTranscript(
    current: List<WatchRelayTranscriptEntry>,
    text: String,
    threadId: String?,
    updatedAtMs: Long,
): List<WatchRelayTranscriptEntry> {
    val normalizedText = text.trim()
    if (normalizedText.isEmpty()) return current

    return trimTranscript(
        current + WatchRelayTranscriptEntry(
            entryId = "watch-out-$updatedAtMs-${current.size}",
            text = normalizedText,
            sourceLabel = "Watch",
            sourceSurface = "watch",
            threadId = threadId?.trim()?.ifEmpty { null },
            status = "sending",
            fromWatch = true,
            updatedAtMs = updatedAtMs,
        )
    )
}

internal fun applyRelayChatEvent(
    current: List<WatchRelayTranscriptEntry>,
    event: ParsedRelayChatEvent,
    updatedAtMs: Long,
): List<WatchRelayTranscriptEntry> {
    val normalizedStatus = event.status?.let { normalizeRelayStatus(it, fallback = "sending") }
    if (normalizedStatus != null) {
        val matchIndex = current.indexOfLast { entry ->
            when {
                event.threadId != null -> entry.threadId == event.threadId
                entry.fromWatch -> true
                else -> false
            }
        }.takeIf { it >= 0 } ?: return current

        return current.mapIndexed { index, entry ->
            if (index != matchIndex) {
                entry
            } else {
                entry.copy(
                    sourceLabel = event.sourceLabel ?: entry.sourceLabel,
                    sourceSurface = event.sourceSurface ?: entry.sourceSurface,
                    status = applyRelayReceiptStatus(entry.status, normalizedStatus),
                    updatedAtMs = updatedAtMs,
                )
            }
        }
    }

    val normalizedText = event.text.trim()
    if (normalizedText.isEmpty()) return current

    val sourceLabel = event.sourceLabel ?: defaultTranscriptSourceLabel(event.sourceSurface)
    return trimTranscript(
        current + WatchRelayTranscriptEntry(
            entryId = event.eventId,
            text = normalizedText,
            sourceLabel = sourceLabel,
            sourceSurface = event.sourceSurface,
            threadId = event.threadId,
            status = null,
            fromWatch = event.sourceSurface == "watch" || sourceLabel.equals("Watch", ignoreCase = true),
            updatedAtMs = updatedAtMs,
        )
    )
}

internal fun encodeRelayTranscript(entries: List<WatchRelayTranscriptEntry>): String {
    val payload = JSONArray()
    trimTranscript(entries).forEach { entry ->
        payload.put(JSONObject().apply {
            put("entryId", entry.entryId)
            put("text", entry.text)
            put("sourceLabel", entry.sourceLabel)
            put("sourceSurface", entry.sourceSurface)
            put("threadId", entry.threadId)
            put("status", entry.status)
            put("fromWatch", entry.fromWatch)
            put("updatedAtMs", entry.updatedAtMs)
        })
    }
    return payload.toString()
}

internal fun decodeRelayTranscript(raw: String?): List<WatchRelayTranscriptEntry> {
    if (raw.isNullOrBlank()) return emptyList()

    return runCatching {
        val payload = JSONArray(raw)
        buildList {
            for (index in 0 until payload.length()) {
                val item = payload.optJSONObject(index) ?: continue
                val entryId = item.optString("entryId").trim()
                val text = item.optString("text").trim()
                val sourceLabel = item.optString("sourceLabel").trim()
                val updatedAtMs = item.optLong("updatedAtMs", 0L)

                if (entryId.isEmpty() || text.isEmpty() || sourceLabel.isEmpty() || updatedAtMs <= 0L) {
                    continue
                }

                add(
                    WatchRelayTranscriptEntry(
                        entryId = entryId,
                        text = text,
                        sourceLabel = sourceLabel,
                        sourceSurface = item.optString("sourceSurface").trim().ifEmpty { null },
                        threadId = item.optString("threadId").trim().ifEmpty { null },
                        status = item.optString("status").trim().ifEmpty { null },
                        fromWatch = item.optBoolean("fromWatch", false),
                        updatedAtMs = updatedAtMs,
                    )
                )
            }
        }
    }.getOrDefault(emptyList()).let(::trimTranscript)
}

internal fun mergeSharedRelayTranscriptSnapshot(
    current: List<WatchRelayTranscriptEntry>,
    rawResponse: String?,
): List<WatchRelayTranscriptEntry>? {
    if (rawResponse.isNullOrBlank()) return null

    return runCatching {
        val root = JSONObject(rawResponse)
        val snapshot = root.optJSONObject("data")?.opt("snapshot")
        if (snapshot == null || snapshot == JSONObject.NULL) {
            return@runCatching null
        }

        val snapshotObject = snapshot as? JSONObject ?: return@runCatching null
        val snapshotConversationId = snapshotObject.optString("conversationId").trim().ifEmpty { null }
        val messages = snapshotObject.optJSONArray("messages") ?: JSONArray()
        val hydrated = buildList {
            for (index in 0 until messages.length()) {
                val message = messages.optJSONObject(index) ?: continue
                val entryId = message.optString("clientMessageId")
                    .trim()
                    .ifEmpty { message.optString("messageId").trim() }
                val text = message.optString("text").trim()
                if (entryId.isEmpty() || text.isEmpty()) {
                    continue
                }

                val sourceSurface = message.optString("sourceSurface").trim().ifEmpty { null }
                val sourceLabel = message.optString("sourceLabel")
                    .trim()
                    .ifEmpty { defaultTranscriptSourceLabel(sourceSurface) }
                val updatedAtMs = message.optLong("seenAtMs")
                    .takeIf { it > 0L }
                    ?: message.optLong("deliveredAtMs").takeIf { it > 0L }
                    ?: message.optLong("createdAtMs").takeIf { it > 0L }
                    ?: continue
                val status = if (message.optLong("seenAtMs") > 0L) {
                    "seen"
                } else {
                    normalizeRelayStatus(message.optString("deliveryStatus"), fallback = "sending")
                }

                add(
                    WatchRelayTranscriptEntry(
                        entryId = entryId,
                        text = text,
                        sourceLabel = sourceLabel,
                        sourceSurface = sourceSurface,
                        threadId = message.optString("conversationId").trim().ifEmpty { null },
                        status = status,
                        fromWatch = sourceSurface == "watch" || sourceLabel.equals("Watch", ignoreCase = true),
                        updatedAtMs = updatedAtMs,
                    )
                )
            }
        }

        val preservedSending = current.filter { entry ->
            entry.status == "sending" &&
                (snapshotConversationId == null || entry.threadId == snapshotConversationId) &&
                hydrated.none { hydratedEntry ->
                hydratedEntry.threadId == entry.threadId &&
                    hydratedEntry.text.equals(entry.text, ignoreCase = false)
            }
        }

        trimTranscript((hydrated + preservedSending).sortedBy { it.updatedAtMs })
    }.getOrNull()
}

internal fun deriveOutgoingRelayThreadId(
    current: List<WatchRelayTranscriptEntry>,
    fallbackThreadId: String,
): String {
    return current
        .asReversed()
        .firstNotNullOfOrNull { entry -> entry.threadId?.trim()?.ifEmpty { null } }
        ?: fallbackThreadId.trim().ifEmpty { "watch-thread" }
}

internal fun readSharedRelayTranscriptSummaries(rawResponse: String?): List<WatchRelayTranscriptSummary> {
    if (rawResponse.isNullOrBlank()) return emptyList()

    return runCatching {
        val data = JSONObject(rawResponse).optJSONObject("data") ?: return@runCatching emptyList()
        val recentSnapshots = data.optJSONArray("recentSnapshots") ?: JSONArray()
        val summaries = buildList {
            for (index in 0 until recentSnapshots.length()) {
                val item = recentSnapshots.optJSONObject(index) ?: continue
                val conversationId = item.optString("conversationId").trim()
                val updatedAtMs = item.optLong("updatedAtMs", 0L)
                if (conversationId.isEmpty() || updatedAtMs <= 0L) {
                    continue
                }

                add(
                    WatchRelayTranscriptSummary(
                        conversationId = conversationId,
                        updatedAtMs = updatedAtMs,
                        messageCount = item.optInt("messageCount", 0),
                        receiptCount = item.optInt("receiptCount", 0),
                        latestMessageText = item.optString("latestMessageText").trim().ifEmpty { null },
                    )
                )
            }
        }

        if (summaries.isNotEmpty()) {
            summaries
        } else {
            val snapshot = data.optJSONObject("snapshot") ?: return@runCatching emptyList()
            val conversationId = snapshot.optString("conversationId").trim()
            val updatedAtMs = snapshot.optLong("updatedAtMs", 0L)
            if (conversationId.isEmpty() || updatedAtMs <= 0L) {
                return@runCatching emptyList()
            }

            val messages = snapshot.optJSONArray("messages") ?: JSONArray()
            val receipts = snapshot.optJSONArray("receipts") ?: JSONArray()
            val latestMessageText = (messages.length() - 1)
                .takeIf { it >= 0 }
                ?.let(messages::optJSONObject)
                ?.optString("text")
                ?.trim()
                ?.ifEmpty { null }

            listOf(
                WatchRelayTranscriptSummary(
                    conversationId = conversationId,
                    updatedAtMs = updatedAtMs,
                    messageCount = messages.length(),
                    receiptCount = receipts.length(),
                    latestMessageText = latestMessageText,
                )
            )
        }
    }.getOrDefault(emptyList())
}