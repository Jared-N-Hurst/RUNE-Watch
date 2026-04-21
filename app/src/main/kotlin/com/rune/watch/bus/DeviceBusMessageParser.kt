// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import org.json.JSONObject

data class ParsedDeviceBusMessage(
    val emberState: String? = null,
    val ringUpdate: String? = null,
)

internal fun parseDeviceBusMessage(text: String): ParsedDeviceBusMessage {
    return runCatching {
        val msg = JSONObject(text)
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
