// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rune.watch.storage.emberPrefsDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

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

    private val _currentNotification = MutableStateFlow<ParsedNotification?>(null)
    val currentNotification: StateFlow<ParsedNotification?> = _currentNotification.asStateFlow()

    private val _paired = MutableStateFlow(false)
    val paired: StateFlow<Boolean> = _paired.asStateFlow()

    private val _deviceIdState = MutableStateFlow("")
    val deviceIdState: StateFlow<String> = _deviceIdState.asStateFlow()

    private val _userIdState = MutableStateFlow("")
    val userIdState: StateFlow<String> = _userIdState.asStateFlow()

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

    suspend fun uploadBiometricSnapshot(data: Map<String, Any>): Boolean {
        if (userId.isBlank() || authToken.isBlank()) return false

        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("data", JSONObject(data))
                }.toString()

                val req = Request.Builder()
                    .url("$API_BASE_URL/api/rune-watch/biometric?userId=$userId")
                    .addHeader("Authorization", "Bearer $authToken")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(req).execute().use { it.isSuccessful }
            }.getOrDefault(false)
        }
    }

    suspend fun respondToNotificationAction(notificationId: String, actionId: String): Boolean {
        if (userId.isBlank() || authToken.isBlank()) return false

        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("notificationId", notificationId)
                    put("actionId", actionId)
                    put("timestamp", System.currentTimeMillis())
                }.toString()

                val req = Request.Builder()
                    .url("$API_BASE_URL/api/rune-watch/action?userId=$userId")
                    .addHeader("Authorization", "Bearer $authToken")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(req).execute().use { it.isSuccessful }
            }.getOrDefault(false)
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
                if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                _connected.value = false
                stopHeartbeat()
                if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
            }
        })

        cont.invokeOnCancellation { webSocket?.cancel() }
    }

    private fun handleMessage(text: String) {
        val command = runCatching {
            val msg = JSONObject(text)
            if (msg.optString("type") == "command") {
                val commandId = msg.optString("commandId")
                if (commandId.isNotBlank()) {
                    scope.launch { acknowledgeCommand(commandId, "received") }
                }
                msg
            } else {
                null
            }
        }.getOrNull()

        if (command != null) {
            handleCommandIntent(command)
        }

        val parsed = parseDeviceBusMessage(text)
        if (parsed.ringUpdate != null) {
            _ringUpdate.value = parsed.ringUpdate
        }
        if (parsed.emberState != null) {
            _emberState.value = parsed.emberState
        }
        if (parsed.notification != null) {
            _currentNotification.value = parsed.notification
        }
    }

    private fun handleCommandIntent(command: JSONObject) {
        val intent = command.optString("intent")
        val payload = command.optJSONObject("payload")
        val fromDevice = command.optString("fromDevice")

        when (intent) {
            "haptic_ping" -> {
                triggerHaptic(longArrayOf(0L, 80L, 40L, 90L))
                _currentNotification.value = ParsedNotification(
                    id = command.optString("commandId").ifBlank { "haptic-${System.currentTimeMillis()}" },
                    type = "status",
                    title = payload?.optString("title").orEmpty().ifBlank { "Phone Ping" },
                    body = payload?.optString("body").orEmpty().ifBlank { "Haptic check from linked phone." },
                    actions = emptyList(),
                )
            }
            "alert_notify", "process_completion" -> {
                triggerHaptic(longArrayOf(0L, 60L, 30L, 60L, 30L, 90L))
                _currentNotification.value = ParsedNotification(
                    id = command.optString("commandId").ifBlank { "alert-${System.currentTimeMillis()}" },
                    type = "alert",
                    title = payload?.optString("title").orEmpty().ifBlank {
                        if (intent == "process_completion") "Process Complete" else "Ember Alert"
                    },
                    body = payload?.optString("body").orEmpty().ifBlank { "A linked surface sent an update." },
                    actions = emptyList(),
                )
            }
            "presence_probe" -> {
                if (fromDevice.isNotBlank()) {
                    scope.launch {
                        sendDirectCommand(
                            toDevice = fromDevice,
                            intent = "presence_probe_ack",
                            payload = JSONObject().apply {
                                put("sourceDeviceId", deviceId)
                                put("role", "watch")
                                put("acknowledgedAtMs", System.currentTimeMillis())
                            },
                        )
                    }
                }
            }
        }
    }

    private suspend fun sendDirectCommand(toDevice: String, intent: String, payload: JSONObject) {
        if (userId.isBlank() || authToken.isBlank() || deviceId.isBlank()) return
        runCatching {
            val body = JSONObject().apply {
                put("fromDevice", deviceId)
                put("toDevice", toDevice)
                put("intent", intent)
                put("payload", payload)
            }.toString()

            val req = Request.Builder()
                .url("$API_BASE_URL/api/device/command?userId=$userId")
                .addHeader("Authorization", "Bearer $authToken")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            http.newCall(req).execute().use { }
        }
    }

    private fun triggerHaptic(pattern: LongArray) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (!vibrator.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    private suspend fun loadCredentials() {
        context.emberPrefsDataStore.data.first().let { prefs ->
            deviceId  = prefs[KEY_DEVICE_ID]  ?: generateAndSaveDeviceId()
            userId    = prefs[KEY_USER_ID]    ?: ""
            authToken = prefs[KEY_AUTH_TOKEN] ?: ""
            _deviceIdState.value = deviceId
            _userIdState.value = userId
            _paired.value = userId.isNotBlank() && authToken.isNotBlank()
        }
    }

    private suspend fun generateAndSaveDeviceId(): String {
        val id = UUID.randomUUID().toString()
        context.emberPrefsDataStore.edit { it[KEY_DEVICE_ID] = id }
        return id
    }

    /** Called from settings / auth flow to persist credentials. */
    suspend fun saveCredentials(userId: String, authToken: String) {
        this.userId    = userId
        this.authToken = authToken
        context.emberPrefsDataStore.edit {
            it[KEY_USER_ID]    = userId
            it[KEY_AUTH_TOKEN] = authToken
        }
        _userIdState.value = userId
        _paired.value = true
        _pairingSession.value = null
    }

    suspend fun clearCredentials() {
        this.userId = ""
        this.authToken = ""
        context.emberPrefsDataStore.edit {
            it.remove(KEY_USER_ID)
            it.remove(KEY_AUTH_TOKEN)
        }
        _connected.value = false
        _userIdState.value = ""
        _paired.value = false
        _pairingSession.value = null
        _currentNotification.value = null
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
