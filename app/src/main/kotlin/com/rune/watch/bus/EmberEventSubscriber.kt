// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

data class WatchEventSubscriptionConfig(
    val allowActionExecutionEvents: Boolean = false,
    val allowedSurfaces: Set<String> = setOf("watch", "mobile", "portal", "api", "desktop"),
    val allowedIncidentClasses: Set<String> = setOf("auth", "network", "runtime", "state", "capability", "custom"),
)

private val EXECUTION_EVENT_TYPES = setOf("ActionRequested", "ActionStarted")

fun canEmitToWatch(
    event: WatchEmberEventEnvelopeV1,
    config: WatchEventSubscriptionConfig = WatchEventSubscriptionConfig(),
): Boolean {
    if (!config.allowedSurfaces.contains(event.surface)) {
        return false
    }
    if (!config.allowActionExecutionEvents && EXECUTION_EVENT_TYPES.contains(event.eventType)) {
        return false
    }
    if (event.incidentClass != null && !config.allowedIncidentClasses.contains(event.incidentClass)) {
        return false
    }
    return true
}

fun filterWatchSupportedEvents(
    events: List<WatchEmberEventEnvelopeV1>,
    config: WatchEventSubscriptionConfig = WatchEventSubscriptionConfig(),
): List<WatchEmberEventEnvelopeV1> {
    val deduped = mutableMapOf<String, WatchEmberEventEnvelopeV1>()
    events.forEach { event ->
        val prior = deduped[event.eventId]
        if (prior == null || event.sequence >= prior.sequence) {
            deduped[event.eventId] = event
        }
    }

    val ordered = deduped.values
        .asSequence()
        .filter { canEmitToWatch(it, config) }
        .sortedBy { it.sequence }
        .toList()

    val monotonic = mutableListOf<WatchEmberEventEnvelopeV1>()
    var lastSequence = Long.MIN_VALUE
    ordered.forEach { event ->
        if (event.sequence < lastSequence) {
            return@forEach
        }
        monotonic.add(event)
        lastSequence = event.sequence
    }

    return monotonic
}

fun resolveReplayCursor(
    previousCursor: Long?,
    deliveredEvents: List<WatchEmberEventEnvelopeV1>,
): Long? {
    if (deliveredEvents.isEmpty()) return previousCursor
    val candidate = deliveredEvents.maxOf { it.sequence }
    if (previousCursor != null && candidate < previousCursor) {
        return previousCursor
    }
    return candidate
}
