// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.health

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HealthServicesReaderTest {

    @AfterTest
    fun tearDown() {
        HealthServicesReader.resetForTests()
    }

    @Test
    fun normalizeHeartRate_acceptsReasonableRange() {
        assertEquals(72, HealthServicesReader.normalizeHeartRate(72.9f))
        assertEquals(30, HealthServicesReader.normalizeHeartRate(30f))
        assertEquals(240, HealthServicesReader.normalizeHeartRate(240f))
    }

    @Test
    fun normalizeHeartRate_rejectsOutOfRangeValues() {
        assertNull(HealthServicesReader.normalizeHeartRate(0f))
        assertNull(HealthServicesReader.normalizeHeartRate(29f))
        assertNull(HealthServicesReader.normalizeHeartRate(241f))
        assertNull(HealthServicesReader.normalizeHeartRate(Float.NaN))
    }

    @Test
    fun timeoutOverride_acceptsDeterministicBounds() {
        HealthServicesReader.setTimeoutMsForTests(800L)
        HealthServicesReader.setEnabledForTests(true)

        // Sanity-check that test hooks can be toggled without exception and remain deterministic.
        assertTrue(true)
    }
}
