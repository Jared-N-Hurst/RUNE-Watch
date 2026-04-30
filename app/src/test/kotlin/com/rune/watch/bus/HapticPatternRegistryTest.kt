// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WDP-9A: HapticPatternRegistry unit tests.
 *
 * Verifies that named pattern IDs resolve deterministically and that unknown
 * names return null (caller no-ops gracefully).
 */
class HapticPatternRegistryTest {

    @Test
    fun resolve_ping_returnsKnownWaveform() {
        val waveform = HapticPatternRegistry.resolve(HapticPatternRegistry.PING)
        assertNotNull(waveform)
        assertTrue(waveform.size >= 2, "Waveform must have at least delay + vibrate entries")
    }

    @Test
    fun resolve_resonancePulse_returnsKnownWaveform() {
        val waveform = HapticPatternRegistry.resolve(HapticPatternRegistry.RESONANCE_PULSE)
        assertNotNull(waveform)
        assertEquals(0L, waveform[0], "Waveform must start with 0ms delay")
    }

    @Test
    fun resolve_flowStart_returnsKnownWaveform() {
        val waveform = HapticPatternRegistry.resolve(HapticPatternRegistry.FLOW_START)
        assertNotNull(waveform)
    }

    @Test
    fun resolve_tensionRelease_returnsKnownWaveform() {
        val waveform = HapticPatternRegistry.resolve(HapticPatternRegistry.TENSION_RELEASE)
        assertNotNull(waveform)
    }

    @Test
    fun resolve_alert_returnsKnownWaveform() {
        val waveform = HapticPatternRegistry.resolve(HapticPatternRegistry.ALERT)
        assertNotNull(waveform)
    }

    @Test
    fun resolve_unknownPatternId_returnsNull() {
        val waveform = HapticPatternRegistry.resolve("unknown_pattern_that_does_not_exist")
        assertNull(waveform)
    }

    @Test
    fun resolve_emptyString_returnsNull() {
        val waveform = HapticPatternRegistry.resolve("")
        assertNull(waveform)
    }

    @Test
    fun registeredPatternIds_containsAllNamedConstants() {
        val ids = HapticPatternRegistry.registeredPatternIds()
        assertTrue(ids.contains(HapticPatternRegistry.PING))
        assertTrue(ids.contains(HapticPatternRegistry.RESONANCE_PULSE))
        assertTrue(ids.contains(HapticPatternRegistry.FLOW_START))
        assertTrue(ids.contains(HapticPatternRegistry.TENSION_RELEASE))
        assertTrue(ids.contains(HapticPatternRegistry.ALERT))
    }

    @Test
    fun lfseSeam_disabledByDefaultInV1() {
        assertFalse(LfseSeam.ENABLED, "LfseSeam.ENABLED must be false for V1 (no behavior change)")
    }

    @Test
    fun lfseSeam_onSyncHint_doesNotThrowWhenDisabled() {
        // No-op path must never throw regardless of input
        LfseSeam.onSyncHint("", 0)
        LfseSeam.onSyncHint("session-abc", 42)
    }

    @Test
    fun lfseSeam_initialize_doesNotThrowWhenDisabled() {
        LfseSeam.initialize("watch-device-1", "user-1")
    }
}
