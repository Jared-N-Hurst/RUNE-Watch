// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val IDENTITY_API_BASE_URL = "https://api.rune-systems.com"

/**
 * FUT-3: IdentityMapClient — fetches the compact identity map from RUNE-Backend.
 *
 * Returns a [CompactIdentityMap] for non-mythic Watch tile display.
 * Uses compact=true to suppress heavy topology/symbolic fields.
 */
data class CompactIdentityMap(
    val userId: String,
    val convergenceState: String,
    val isilmeResonance: Double,
    val readerInitiationLevel: Int,
    val awakenedFragmentsCount: Int,
)

class IdentityMapClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    /**
     * Fetch compact identity map for [userId].
     * Returns null if the request fails or the user has no identity yet.
     */
    suspend fun fetchCompact(
        userId: String,
        authToken: String,
    ): CompactIdentityMap? = withContext(Dispatchers.IO) {
        try {
            val url = "$IDENTITY_API_BASE_URL/api/identity/map?userId=${userId}&compact=true"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            val resp = http.newCall(req).execute()
            if (!resp.isSuccessful) return@withContext null

            val body = resp.body?.string() ?: return@withContext null
            parseCompactBody(body, userId)
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseCompactBody(body: String, fallbackUserId: String): CompactIdentityMap? {
        val json = JSONObject(body)
        if (!json.optBoolean("ok", false)) return null

        val data = json.getJSONObject("data")
        return CompactIdentityMap(
            userId = data.optString("userId", fallbackUserId),
            convergenceState = data.optString("convergenceState", "dormant"),
            isilmeResonance = data.optDouble("isilmeResonance", 0.0),
            readerInitiationLevel = data.optInt("readerInitiationLevel", 0),
            awakenedFragmentsCount = data.optInt("awakenedFragmentsCount", 0),
        )
    }
}
