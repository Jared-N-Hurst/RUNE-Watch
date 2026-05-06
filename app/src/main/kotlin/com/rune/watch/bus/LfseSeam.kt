// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import com.rune.watch.BuildConfig
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

private const val TAG = "RUNE_LfseSeam"

/**
 * WDP-9A: LFSE micro-surface seam stub for Watch.
 *
 * This object provides the extension point for on-device LFSE (Localized Field
 * Synchronization Engine) initialization. Currently a no-op gated behind
 * [LfseSeam.ENABLED]. When the KLLM roadmap activates on-device cognition,
 * replace [initialize] and [onSyncHint] with real implementations without
 * changing callers.
 *
 * Acceptance: no behavior change when ENABLED = false (default for V1).
 */
object LfseSeam {

    data class HandshakeRequest(
        val userId: String,
        val authToken: String,
        val emitterId: String,
        val scopeType: String,
        val scopeId: String,
        val protocolVersion: String,
        val capabilities: List<String>,
    )

    data class HandshakeResponse(
        val sessionId: String,
    )

    data class SyncRequest(
        val userId: String,
        val authToken: String,
        val sessionId: String,
        val conceptDelta: List<Map<String, Any>>,
        val planDelta: List<Map<String, Any>>,
    )

    interface Transport {
        fun handshake(request: HandshakeRequest): HandshakeResponse?
        fun sync(request: SyncRequest): Boolean
    }

    private class HttpTransport(private val apiBaseUrl: String) : Transport {
        private val http = OkHttpClient()

        override fun handshake(request: HandshakeRequest): HandshakeResponse? {
            val body = JSONObject().apply {
                put("emitterId", request.emitterId)
                put("scopeType", request.scopeType)
                put("scopeId", request.scopeId)
                put("protocolVersion", request.protocolVersion)
                put("capabilities", JSONArray(request.capabilities))
            }.toString()

            val req = Request.Builder()
                .url("$apiBaseUrl/api/lfse/handshake?userId=${request.userId}")
                .addHeader("Authorization", "Bearer ${request.authToken}")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            return runCatching {
                http.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return null
                    val text = res.body?.string() ?: return null
                    val json = JSONObject(text)
                    val data = json.optJSONObject("data") ?: return null
                    val sessionId = data.optString("sessionId").orEmpty()
                    if (sessionId.isBlank()) return null
                    HandshakeResponse(sessionId)
                }
            }.getOrNull()
        }

        override fun sync(request: SyncRequest): Boolean {
            val body = JSONObject().apply {
                put("sessionId", request.sessionId)
                put("conceptDelta", JSONArray(request.conceptDelta.map { JSONObject(it) }))
                put("planDelta", JSONArray(request.planDelta.map { JSONObject(it) }))
            }.toString()

            val req = Request.Builder()
                .url("$apiBaseUrl/api/lfse/sync?userId=${request.userId}")
                .addHeader("Authorization", "Bearer ${request.authToken}")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            return runCatching {
                http.newCall(req).execute().use { res -> res.isSuccessful }
            }.getOrDefault(false)
        }
    }

    private var transport: Transport? = null
    private var initializedUserId: String = ""
    private var initializedToken: String = ""
    private var activeSessionId: String = ""

    private var enabledOverrideForTests: Boolean? = null

    /**
     * Feature gate — set to true only when on-device LFSE runtime is ready.
     * V1: always false (no-op). KLLM roadmap: flip to true after integration.
     */
    val ENABLED: Boolean
        get() = enabledOverrideForTests ?: BuildConfig.LFSE_SEAM_ENABLED

    /**
     * Initialize the LFSE seam. Call once from application/service startup.
     * No-op in V1 (ENABLED = false).
     */
    fun initialize(
        deviceId: String,
        userId: String,
        authToken: String,
        apiBaseUrl: String = "https://api.rune-systems.com",
    ) {
        if (!ENABLED) {
            Log.d(TAG, "LFSE seam initialized (no-op, ENABLED=false) deviceId=$deviceId userId=$userId")
            return
        }

        if (deviceId.isBlank() || userId.isBlank() || authToken.isBlank()) {
            Log.d(TAG, "LFSE seam init skipped (missing credentials)")
            return
        }

        if (activeSessionId.isNotBlank() && initializedUserId == userId && initializedToken == authToken) {
            Log.d(TAG, "LFSE seam already initialized for userId=$userId sessionId=$activeSessionId")
            return
        }

        val tx = transport ?: HttpTransport(apiBaseUrl)
        val handshake = tx.handshake(
            HandshakeRequest(
                userId = userId,
                authToken = authToken,
                emitterId = deviceId,
                scopeType = "user",
                scopeId = userId,
                protocolVersion = "1",
                capabilities = listOf("watch_haptics", "lfse_hint_sync"),
            )
        )

        if (handshake == null) {
            Log.w(TAG, "LFSE seam handshake failed for userId=$userId")
            return
        }

        initializedUserId = userId
        initializedToken = authToken
        activeSessionId = handshake.sessionId
        Log.d(TAG, "LFSE seam initialized sessionId=${handshake.sessionId}")
    }

    /**
     * Called when a device command carrying an LFSE hint envelope arrives.
     * The hint is advisory — it signals that new concept/plan deltas are available
     * on the backend. In V1 this is a no-op; in the KLLM path it triggers a sync.
     *
     * @param sessionId  The LFSE session ID from the hint envelope (may be blank).
     * @param deltaCount Approximate number of pending deltas (informational).
     */
    fun onSyncHint(sessionId: String, deltaCount: Int) {
        if (!ENABLED) {
            Log.d(TAG, "LFSE sync hint received (no-op) sessionId=$sessionId deltaCount=$deltaCount")
            return
        }

        val targetSession = sessionId.ifBlank { activeSessionId }
        if (targetSession.isBlank() || initializedUserId.isBlank() || initializedToken.isBlank()) {
            Log.d(TAG, "LFSE sync hint ignored (not initialized) sessionId=$sessionId")
            return
        }

        val tx = transport ?: HttpTransport("https://api.rune-systems.com")
        val ok = tx.sync(
            SyncRequest(
                userId = initializedUserId,
                authToken = initializedToken,
                sessionId = targetSession,
                conceptDelta = listOf(
                    mapOf(
                        "type" to "watch_hint",
                        "deltaCount" to deltaCount,
                        "source" to "watch",
                    )
                ),
                planDelta = emptyList(),
            )
        )

        if (ok) {
            Log.d(TAG, "LFSE sync hint processed sessionId=$targetSession deltaCount=$deltaCount")
        } else {
            Log.w(TAG, "LFSE sync hint failed sessionId=$targetSession")
        }
    }

    internal fun setTransportForTests(testTransport: Transport?) {
        transport = testTransport
    }

    internal fun setEnabledForTests(enabled: Boolean?) {
        enabledOverrideForTests = enabled
    }

    internal fun resetForTests() {
        transport = null
        initializedUserId = ""
        initializedToken = ""
        activeSessionId = ""
        enabledOverrideForTests = null
    }
}
