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
)

data class ParsedDeviceBusMessage(
    val emberState: String? = null,
    val ringUpdate: String? = null,
    val notification: ParsedNotification? = null,
)

internal fun parseDeviceBusMessage(text: String): ParsedDeviceBusMessage {
    return runCatching {
        val msg = JSONObject(text)
        
        // Handle notification type
        if (msg.optString("type") == "notification") {
            val data = msg.optJSONObject("data") ?: return@runCatching ParsedDeviceBusMessage()
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
                    actions = actions
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
            else -> ParsedDeviceBusMessage(emberState = if (intent.isNotEmpty()) intent else null)
        }
    }.getOrDefault(ParsedDeviceBusMessage())
}
