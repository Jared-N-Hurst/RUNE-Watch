// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import kotlin.test.Test
import kotlin.test.assertEquals

class RelayStatusNormalizationTest {

    @Test
    fun normalizeRelayStatus_mapsLegacyAliases() {
        assertEquals("delivered", normalizeRelayStatus("sent"))
        assertEquals("delivered", normalizeRelayStatus("received"))
        assertEquals("seen", normalizeRelayStatus("read"))
        assertEquals("responded", normalizeRelayStatus("reply"))
    }

    @Test
    fun normalizeRelayStatus_usesFallbackWhenUnknown() {
        assertEquals("queued", normalizeRelayStatus("mystery", fallback = "queued"))
        assertEquals("failed", normalizeRelayStatus(null))
    }

    @Test
    fun applyRelayReceiptStatus_advancesAndNeverRegresses() {
        assertEquals("seen", applyRelayReceiptStatus(current = "delivered", receipt = "read"))
        assertEquals("responded", applyRelayReceiptStatus(current = "seen", receipt = "responded"))
        assertEquals("seen", applyRelayReceiptStatus(current = "seen", receipt = "queued"))
    }
}
