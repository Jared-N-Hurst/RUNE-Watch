// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private val Context.dataStore by preferencesDataStore("ember_prefs")

private val KEY_DEVICE_ID  = stringPreferencesKey("device_id")
private val KEY_USER_ID    = stringPreferencesKey("user_id")
private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")

private const val API_BASE_URL = "https://api.rune-systems.com"

data class PairingSession(
    val deviceId: String,
    val pairingCode: String,
    val timestamp: Long,
    val qrPayload: String,
)

/**
 * WebSocket client that connects this WearOS device to the RUNE-Backend device bus.
 *
 * Call [connect] when the app becomes visible and [disconnect] on destroy.
 * The client registers the device on first connect, reconnects automatically
 * on failure with exponential back-off, and dispatches incoming cross-device
 * commands as state updates to Compose collectors.
 */
class DeviceBusClient(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val http = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()

    // ── Public state ─────────────────────────────────────────────────────────

    private val _connected  = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    /** Latest Ember expression/state string pushed from any device. */
    private val _emberState = MutableStateFlow("")
    val emberState: StateFlow<String> = _emberState.asStateFlow()

    /** Most recent ring-action graph pushed by Ember (null = use default). */
    private val _ringUpdate = MutableStateFlow<String?>(null)
    val ringUpdate: StateFlow<String?> = _ringUpdate.asStateFlow()

    private val _paired = MutableStateFlow(false)
    val paired: StateFlow<Boolean> = _paired.asStateFlow()

    private val _pairingSession = MutableStateFlow<PairingSession?>(null)
    val pairingSession: StateFlow<PairingSession?> = _pairingSession.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var deviceId: String = ""
    private var userId: String = ""
    private var authToken: String = ""

    init {
        scope.launch { loadCredentials() }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun connect() {
        scope.launch { loadCredentials() }
        startReconnectLoop()
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "app closing")
        webSocket = null
        scope.cancel()
    }

    // ── Command dispatch ──────────────────────────────────────────────────────

    fun sendCommand(intent: String, payload: Map<String, Any>?) {
        scope.launch {
            if (userId.isBlank() || authToken.isBlank()) return@launch
            runCatching {
                val body = JSONObject().apply {
                    put("fromDevice", deviceId)
                    put("toDevice", "*")   // broadcast to all user's devices
                    put("intent", intent)
                    if (payload != null) put("payload", JSONObject(payload))
                }.toString()

                val req = Request.Builder()
                    .url("$API_BASE_URL/api/device/command?userId=$userId")
                    .addHeader("Authorization", "Bearer $authToken")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(req).execute()
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun startReconnectLoop() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var delaySecs = 2L
            while (isActive) {
                if (authToken.isNotBlank() && deviceId.isNotBlank()) {
                    registerDevice()
                    openWebSocket()
                    // openWebSocket blocks until the socket closes/errors
                    _connected.value = false
                }
                delay(delaySecs * 1000L)
                delaySecs = (delaySecs * 2).coerceAtMost(60L)
            }
        }
    }

    private suspend fun registerDevice() {
        runCatching {
            val caps = JSONObject().apply {
                put("deviceId",     deviceId)
                put("platform",     "wearos")
                put("capabilities", org.json.JSONArray(listOf("notifications", "haptics")))
                put("label",        "Galaxy Watch")
                put("deviceType",   "watch")
                put("trustLevel",   "personal")
            }.toString()

            val req = Request.Builder()
                .url("$API_BASE_URL/api/device/register?userId=$userId")
                .addHeader("Authorization", "Bearer $authToken")
                .post(caps.toRequestBody("application/json".toMediaType()))
                .build()
            http.newCall(req).execute()
        }
    }

    private suspend fun openWebSocket() = suspendCancellableCoroutine<Unit> { cont ->
        val wsUrl = API_BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://") +
                "/api/device/stream?deviceId=$deviceId&userId=$userId"

        val req = Request.Builder()
            .url(wsUrl)
            .addHeader("Authorization", "Bearer $authToken")
            .build()

        webSocket = http.newWebSocket(req, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                _connected.value = true
                startHeartbeat()
            }

            override fun onMessage(ws: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(1000, null)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                _connected.value = false
                stopHeartbeat()
                if (cont.isActive) cont.resume(Unit) {}
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connected.value = false
                stopHeartbeat()
                if (cont.isActive) cont.resume(Unit) {}
            }
        })

        cont.invokeOnCancellation { webSocket?.cancel() }
    }

    private fun handleMessage(text: String) {
        runCatching {
            val msg = JSONObject(text)
            if (msg.optString("type") == "command") {
                val commandId = msg.optString("commandId")
                if (commandId.isNotBlank()) {
                    scope.launch { acknowledgeCommand(commandId, "received") }
                }
            }
        }

        val parsed = parseDeviceBusMessage(text)
        if (parsed.ringUpdate != null) {
            _ringUpdate.value = parsed.ringUpdate
        }
        if (parsed.emberState != null) {
            _emberState.value = parsed.emberState
        }
    }

    private suspend fun loadCredentials() {
        context.dataStore.data.first().let { prefs ->
            deviceId  = prefs[KEY_DEVICE_ID]  ?: generateAndSaveDeviceId()
            userId    = prefs[KEY_USER_ID]    ?: ""
            authToken = prefs[KEY_AUTH_TOKEN] ?: ""
            _paired.value = userId.isNotBlank() && authToken.isNotBlank()
        }
    }

    private suspend fun generateAndSaveDeviceId(): String {
        val id = UUID.randomUUID().toString()
        context.dataStore.edit { it[KEY_DEVICE_ID] = id }
        return id
    }

    /** Called from settings / auth flow to persist credentials. */
    suspend fun saveCredentials(userId: String, authToken: String) {
        this.userId    = userId
        this.authToken = authToken
        context.dataStore.edit {
            it[KEY_USER_ID]    = userId
            it[KEY_AUTH_TOKEN] = authToken
        }
        _paired.value = true
        _pairingSession.value = null
    }

    suspend fun ensurePairingSession(forceRefresh: Boolean = false): PairingSession {
        loadCredentials()

        val current = _pairingSession.value
        if (!forceRefresh && current != null && (System.currentTimeMillis() - current.timestamp) < 60_000L) {
            return current
        }

        val resolvedDeviceId = if (deviceId.isBlank()) generateAndSaveDeviceId() else deviceId
        val pairingCode = Random.nextInt(0, 1_000_000).toString().padStart(6, '0')
        val timestamp = System.currentTimeMillis()
        val qrPayload = JSONObject()
            .put("deviceId", resolvedDeviceId)
            .put("pairingCode", pairingCode)
            .put("timestamp", timestamp)
            .toString()

        runCatching {
            val body = JSONObject().apply {
                put("deviceId", resolvedDeviceId)
                put("pairingCode", pairingCode)
                put("timestamp", timestamp)
            }.toString()

            val req = Request.Builder()
                .url("$API_BASE_URL/api/device/pair/register")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            http.newCall(req).execute().use { }
        }

        val session = PairingSession(
            deviceId = resolvedDeviceId,
            pairingCode = pairingCode,
            timestamp = timestamp,
            qrPayload = qrPayload,
        )
        _pairingSession.value = session
        return session
    }

    suspend fun pollPairingStatus(): Boolean {
        val session = _pairingSession.value ?: return false
        return runCatching {
            val url = "$API_BASE_URL/api/device/pair/status?deviceId=${session.deviceId}&pairingCode=${session.pairingCode}"
            val req = Request.Builder().url(url).get().build()
            http.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return@use false
                val raw = res.body?.string() ?: return@use false
                val body = JSONObject(raw)
                if (!body.optBoolean("ok", false)) return@use false
                val data = body.optJSONObject("data") ?: return@use false
                val paired = data.optBoolean("paired", false)
                if (!paired) return@use false

                val pairedUserId = data.optString("userId")
                val watchAuthToken = data.optString("watchAuthToken")
                if (pairedUserId.isBlank() || watchAuthToken.isBlank()) return@use false

                saveCredentials(pairedUserId, watchAuthToken)
                true
            }
        }.getOrDefault(false)
    }

    private suspend fun acknowledgeCommand(commandId: String, status: String) {
        if (userId.isBlank() || authToken.isBlank() || deviceId.isBlank()) return
        runCatching {
            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("commandId", commandId)
                put("status", status)
            }.toString()

            val req = Request.Builder()
                .url("$API_BASE_URL/api/device/command/ack?userId=$userId")
                .addHeader("Authorization", "Bearer $authToken")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(req).execute().use { }
        }
    }

    private fun startHeartbeat() {
        stopHeartbeat()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(30_000L) // 30 seconds
                emitHeartbeat()
            }
        }
    }

    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private suspend fun emitHeartbeat() {
        if (userId.isBlank() || authToken.isBlank() || deviceId.isBlank()) return
        runCatching {
            val body = JSONObject().apply {
                put("deviceId", deviceId)
            }.toString()

            val req = Request.Builder()
                .url("$API_BASE_URL/api/device/heartbeat?userId=$userId")
                .addHeader("Authorization", "Bearer $authToken")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(req).execute().use { }
        }
    }
}
