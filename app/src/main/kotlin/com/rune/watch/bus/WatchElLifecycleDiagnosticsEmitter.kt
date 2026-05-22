// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val DIAGNOSTICS_SOURCE = "watch-ember-event-stream"

typealias WatchElLifecycleState = String

class WatchElLifecycleDiagnosticsEmitter(
    private val apiBaseUrl: String = "https://api.rune-systems.com",
    private val transport: Transport = OkHttpTransport(),
) {

    interface Transport {
        fun post(url: String, authToken: String?, body: String)
    }

    private class OkHttpTransport : Transport {
        private val http = OkHttpClient()

        override fun post(url: String, authToken: String?, body: String) {
            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .apply {
                    if (!authToken.isNullOrBlank()) {
                        addHeader("Authorization", "Bearer $authToken")
                    }
                }
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(request).execute().use { }
        }
    }

    fun emit(
        state: WatchElLifecycleState,
        deviceId: String,
        userId: String,
        authToken: String?,
    ) {
        val severity = when (state) {
            "error", "reconnecting" -> "warning"
            else -> "info"
        }

        val payload = JSONObject().apply {
            put("source", DIAGNOSTICS_SOURCE)
            put("severity", severity)
            put("category", "integration")
            put("message", "EL_STREAM_STATE:$state")
            put("userId", userId)
            put(
                "details",
                JSONObject().apply {
                    put("surface", "watch")
                    put("state", state)
                    put("deviceId", deviceId)
                    put("emberId", "ember-$userId")
                },
            )
        }

        transport.post("$apiBaseUrl/api/diagnostics/event", authToken, payload.toString())
    }
}
