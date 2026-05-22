// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

private val RELAY_STATUS_ALIASES = mapOf(
    "sending" to "sending",
    "queued" to "queued",
    "sent" to "delivered",
    "delivered" to "delivered",
    "received" to "delivered",
    "seen" to "seen",
    "read" to "seen",
    "ack" to "seen",
    "acknowledged" to "seen",
    "responded" to "responded",
    "reply" to "responded",
    "replied" to "responded",
    "failed" to "failed",
)

private val RELAY_STATUS_RANK = mapOf(
    "sending" to 0,
    "queued" to 1,
    "delivered" to 2,
    "seen" to 3,
    "responded" to 4,
)

internal fun normalizeRelayStatus(value: String?, fallback: String = "failed"): String {
    val normalized = value?.trim()?.lowercase().orEmpty()
    if (normalized.isEmpty()) return fallback
    return RELAY_STATUS_ALIASES[normalized] ?: fallback
}

internal fun applyRelayReceiptStatus(current: String?, receipt: String?): String {
    val currentStatus = normalizeRelayStatus(current, fallback = "sending")
    val receiptStatus = normalizeRelayStatus(receipt, fallback = currentStatus)

    if (currentStatus == "failed") {
        return if (receiptStatus == "failed") "failed" else receiptStatus
    }

    val currentRank = RELAY_STATUS_RANK[currentStatus] ?: 0
    val receiptRank = RELAY_STATUS_RANK[receiptStatus] ?: currentRank
    return if (receiptRank >= currentRank) receiptStatus else currentStatus
}
