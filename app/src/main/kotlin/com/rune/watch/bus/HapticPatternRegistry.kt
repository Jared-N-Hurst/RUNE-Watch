// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

/**
 * Named haptic pattern registry for WDP-9A future-hooks seam.
 *
 * Each entry maps a symbolic pattern name (sent as `hapticPatternId` in device commands)
 * to a WearOS waveform LongArray suitable for [android.os.VibrationEffect.createWaveform].
 * Pattern format: [delay, vibrate, pause, vibrate, ...] in milliseconds.
 *
 * Extend this registry as new LFSE-driven or Katiah-driven haptic vocabularies are defined.
 * All entries are no-ops if haptics are disabled on the device.
 */
object HapticPatternRegistry {

    // ── Named pattern constants ───────────────────────────────────────────────

    /** Short confirming pulse — Ember acknowledgement. */
    const val RESONANCE_PULSE = "resonance_pulse"

    /** Two-tap initiation — flow state entering. */
    const val FLOW_START = "flow_start"

    /** Long → short release — tension resolved. */
    const val TENSION_RELEASE = "tension_release"

    /** Single brief tap — ping / attention check. */
    const val PING = "ping"

    /** Escalating triple tap — alert / process completion. */
    const val ALERT = "alert"

    // ── Waveform definitions ──────────────────────────────────────────────────

    private val patterns: Map<String, LongArray> = mapOf(
        RESONANCE_PULSE to longArrayOf(0L, 40L, 20L, 80L),
        FLOW_START      to longArrayOf(0L, 60L, 40L, 60L),
        TENSION_RELEASE to longArrayOf(0L, 120L, 30L, 40L),
        PING            to longArrayOf(0L, 80L, 40L, 90L),
        ALERT           to longArrayOf(0L, 60L, 30L, 60L, 30L, 90L),
    )

    /**
     * Resolve a named pattern to its waveform.
     * Returns [null] if the pattern name is not registered (caller should no-op).
     */
    fun resolve(patternId: String): LongArray? = patterns[patternId]

    /** All registered pattern names — useful for capability negotiation. */
    fun registeredPatternIds(): Set<String> = patterns.keys
}
