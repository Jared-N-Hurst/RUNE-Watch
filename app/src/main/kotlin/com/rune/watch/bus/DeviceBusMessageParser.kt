// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import org.json.JSONObject

data class NotificationAction(
    val id: String,
    val label: String,
    val actionType: String,
)

data class ParsedNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val actions: List<NotificationAction> = emptyList(),
    val sourceLabel: String? = null,
    val sourceSurface: String? = null,
    val threadId: String? = null,
    val incidentId: String? = null,
    val incidentClass: String? = null,
    val incidentSeverity: String? = null,
    val incidentTenantId: String? = null,
)

data class ParsedRelayChatEvent(
    val eventId: String,
    val text: String,
    val sourceLabel: String? = null,
    val sourceSurface: String? = null,
    val threadId: String? = null,
    val status: String? = null,
)

data class ParsedDeviceBusMessage(
    val emberState: String? = null,
    val ringUpdate: String? = null,
    val notification: ParsedNotification? = null,
    val adaptiveFallbackProfile: String? = null,
    val relayChatEvent: ParsedRelayChatEvent? = null,
)

data class WatchEmberEventEnvelopeV1(
    val eventSchemaVersion: String,
    val eventId: String,
    val emberId: String,
    val userId: String,
    val eventType: String,
    val provenance: String,
    val surface: String,
    val sequence: Long,
    val causalParentIds: List<String> = emptyList(),
    val occurredAt: String,
    val recordedAt: String,
    val incidentClass: String? = null,
)

private val EL1_EVENT_TYPES = setOf(
    "UserMessage",
    "IntentDetected",
    "IntentRouted",
    "ActionRequested",
    "ActionStarted",
    "ActionCompleted",
    "EmberReply",
    "CognitionStep",
    "FieldUpdate",
    "GhostIntervention"
)

private val EL1_PROVENANCES = setOf("user", "ember", "ghost", "lfse", "kllm")

private val EL1_SURFACES = setOf("desktop", "portal", "mobile", "watch", "api")
private val INCIDENT_SUPPORTED_CLASSES = setOf("auth", "network", "runtime", "state", "capability", "custom")
private val INCIDENT_SUPPORTED_SEVERITIES = setOf("info", "warning", "critical")

private val SOURCE_SURFACE_ALIASES = mapOf(
    "desktop" to "desktop",
    "laptop" to "desktop",
    "pc" to "desktop",
    "mobile" to "mobile",
    "phone" to "mobile",
    "android" to "mobile",
    "ios" to "mobile",
    "watch" to "watch",
    "wearos" to "watch",
    "wearable" to "watch",
    "web" to "web",
    "browser" to "web",
)

private fun normalizeSourceSurface(value: String?): String? {
    val normalized = value?.trim()?.lowercase().orEmpty()
    if (normalized.isEmpty()) return null
    return SOURCE_SURFACE_ALIASES[normalized]
}

private fun normalizeThreadId(value: String?): String? {
    val normalized = value?.trim().orEmpty()
    return normalized.ifEmpty { null }?.take(128)
}

private fun deriveThreadId(payload: JSONObject): String? {
    return normalizeThreadId(payload.optString("threadId"))
        ?: normalizeThreadId(payload.optString("conversationId"))
        ?: normalizeThreadId(payload.optString("groupId"))
}

internal fun parseEmberEventEnvelopeV1(text: String): WatchEmberEventEnvelopeV1? {
    return runCatching {
        val root = JSONObject(text)
        if (root.optString("eventSchemaVersion") != "el-1.v1") {
            return@runCatching null
        }

        val eventId = root.optString("eventId").trim()
        val emberId = root.optString("emberId").trim()
        val userId = root.optString("userId").trim()
        val eventType = root.optString("eventType").trim()
        val provenance = root.optString("provenance").trim()
        val surface = root.optString("surface").trim()
        val occurredAt = root.optString("occurredAt").trim()
        val recordedAt = root.optString("recordedAt").trim()
        val sequence = root.optLong("sequence", -1L)

        if (eventId.isEmpty() || emberId.isEmpty() || userId.isEmpty()) return@runCatching null
        if (!EL1_EVENT_TYPES.contains(eventType)) return@runCatching null
        if (!EL1_PROVENANCES.contains(provenance)) return@runCatching null
        if (!EL1_SURFACES.contains(surface)) return@runCatching null
        if (occurredAt.isEmpty() || recordedAt.isEmpty() || sequence < 0) return@runCatching null

        val causalParentIds = mutableListOf<String>()
        val causalParentArray = root.optJSONArray("causalParentIds")
        if (causalParentArray != null) {
            for (i in 0 until causalParentArray.length()) {
                val parent = causalParentArray.optString(i).trim()
                if (parent.isNotEmpty()) {
                    causalParentIds.add(parent)
                }
            }
        }

        val payload = root.optJSONObject("payload")
        val incidentClass = payload
            ?.optJSONObject("incident")
            ?.optString("class")
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() }

        if (incidentClass != null && !INCIDENT_SUPPORTED_CLASSES.contains(incidentClass)) {
            return@runCatching null
        }

        WatchEmberEventEnvelopeV1(
            eventSchemaVersion = "el-1.v1",
            eventId = eventId,
            emberId = emberId,
            userId = userId,
            eventType = eventType,
            provenance = provenance,
            surface = surface,
            sequence = sequence,
            causalParentIds = causalParentIds,
            occurredAt = occurredAt,
            recordedAt = recordedAt,
            incidentClass = incidentClass,
        )
    }.getOrNull()
}

internal fun parseDeviceBusMessage(text: String): ParsedDeviceBusMessage {
    return runCatching {
        val msg = JSONObject(text)
        
        // Handle notification type
        if (msg.optString("type") == "notification") {
            val data = msg.optJSONObject("data") ?: return@runCatching ParsedDeviceBusMessage()
            val incident = data.optJSONObject("incident")
            val incidentClass = incident
                ?.optString("class")
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }
            val incidentSeverity = incident
                ?.optString("severity")
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() }

            if (incidentClass != null && !INCIDENT_SUPPORTED_CLASSES.contains(incidentClass)) {
                return@runCatching ParsedDeviceBusMessage()
            }
            if (incidentSeverity != null && !INCIDENT_SUPPORTED_SEVERITIES.contains(incidentSeverity)) {
                return@runCatching ParsedDeviceBusMessage()
            }

            val actionsArray = data.optJSONArray("actions") ?: org.json.JSONArray()
            val actions = mutableListOf<NotificationAction>()
            
            for (i in 0 until actionsArray.length()) {
                val action = actionsArray.optJSONObject(i)
                if (action != null) {
                    actions.add(NotificationAction(
                        id = action.optString("id"),
                        label = action.optString("label"),
                        actionType = action.optString("actionType")
                    ))
                }
            }
            
            return@runCatching ParsedDeviceBusMessage(
                notification = ParsedNotification(
                    id = data.optString("id"),
                    type = data.optString("type"),
                    title = data.optString("title"),
                    body = data.optString("body"),
                    actions = actions,
                    sourceLabel = data.optString("sourceLabel").takeIf { it.isNotEmpty() },
                    sourceSurface = normalizeSourceSurface(data.optString("sourceSurface")),
                    threadId = deriveThreadId(data),
                    incidentId = incident?.optString("incidentId")?.trim()?.takeIf { it.isNotEmpty() },
                    incidentClass = incidentClass,
                    incidentSeverity = incidentSeverity,
                    incidentTenantId = incident?.optString("tenantId")?.trim()?.takeIf { it.isNotEmpty() },
                )
            )
        }
        
        // Handle command type (existing logic)
        if (msg.optString("type") != "command") {
            return@runCatching ParsedDeviceBusMessage()
        }

        when (val intent = msg.optString("intent")) {
            "ring_update" -> ParsedDeviceBusMessage(ringUpdate = msg.optString("payload"))
            "ember_state" -> ParsedDeviceBusMessage(
                emberState = msg.optJSONObject("payload")?.optString("expression") ?: ""
            )
            "adaptive_fallback_profile" -> ParsedDeviceBusMessage(
                adaptiveFallbackProfile = msg.optJSONObject("payload")?.toString()
            )
            "relay_message" -> {
                val payload = msg.optJSONObject("payload")
                val text = when {
                    payload != null -> payload.optString("text").trim()
                    msg.opt("payload") is String -> msg.optString("payload").trim()
                    else -> ""
                }
                val sourceSurface = normalizeSourceSurface(payload?.optString("sourceSurface"))
                val sourceLabel = payload?.optString("sourceLabel")
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                ParsedDeviceBusMessage(
                    relayChatEvent = ParsedRelayChatEvent(
                        eventId = msg.optString("commandId").trim().ifEmpty { "relay-${System.currentTimeMillis()}" },
                        text = text.ifEmpty { "Linked surface update" },
                        sourceLabel = sourceLabel,
                        sourceSurface = sourceSurface,
                        threadId = payload?.let(::deriveThreadId),
                    )
                )
            }
            "relay_receipt" -> {
                val payload = msg.optJSONObject("payload")
                val status = payload?.optString("status")
                    ?.trim()
                    ?.lowercase()
                    ?.takeIf { it.isNotEmpty() }
                val sourceSurface = normalizeSourceSurface(
                    payload?.optString("respondedFromSurface")
                        ?.ifBlank { payload.optString("sourceSurface") }
                )
                val sourceLabel = payload?.optString("respondedFromLabel")
                    ?.ifBlank { payload.optString("sourceLabel") }
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                ParsedDeviceBusMessage(
                    relayChatEvent = ParsedRelayChatEvent(
                        eventId = msg.optString("commandId").trim().ifEmpty { "relay-receipt-${System.currentTimeMillis()}" },
                        text = "",
                        sourceLabel = sourceLabel,
                        sourceSurface = sourceSurface,
                        threadId = payload?.let(::deriveThreadId),
                        status = status,
                    )
                )
            }
            else -> ParsedDeviceBusMessage(emberState = if (intent.isNotEmpty()) intent else null)
        }
    }.getOrDefault(ParsedDeviceBusMessage())
}
