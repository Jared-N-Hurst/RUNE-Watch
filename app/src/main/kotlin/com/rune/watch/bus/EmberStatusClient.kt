// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val WATCH_API_BASE_URL = "https://api.rune-systems.com"

data class EmberTileStatus(
    val userId: String,
    val phaseLabel: String,
    val engagementSummary: String,
    val riskCount: Int,
    val isilmeResonance: Double,
    val lastUpdated: String,
)

class EmberStatusClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun fetchTileStatus(userId: String, authToken: String): EmberTileStatus? = withContext(Dispatchers.IO) {
        try {
            val url = "$WATCH_API_BASE_URL/api/watch/ember-status?userId=$userId"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            val resp = http.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null

            val body = resp.body?.string() ?: return@withContext null
            parseTileStatusBody(body, userId)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseTileStatusBody(body: String, fallbackUserId: String): EmberTileStatus? {
        val json = JSONObject(body)
        if (!json.optBoolean("ok", false)) return null

        val data = json.getJSONObject("data")
        return EmberTileStatus(
            userId = data.optString("userId", fallbackUserId),
            phaseLabel = data.optString("phaseLabel", "Ready"),
            engagementSummary = data.optString("engagementSummary", "Ember is synchronized and ready."),
            riskCount = data.optInt("riskCount", 0),
            isilmeResonance = data.optDouble("isilmeResonance", 0.0),
            lastUpdated = data.optString("lastUpdated", "")
        )
    }
}
