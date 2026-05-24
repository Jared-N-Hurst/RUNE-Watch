// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rune.watch.settings.WatchSettingsStore
import com.rune.watch.storage.emberPrefsDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random

private const val TAG = "RUNE_DeviceBus"

private val KEY_DEVICE_ID  = stringPreferencesKey("device_id")
private val KEY_USER_ID    = stringPreferencesKey("user_id")
private val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
private val KEY_LOCALE_PREFERENCE = stringPreferencesKey("locale_preference")
private val KEY_RELAY_TRANSCRIPT = stringPreferencesKey("relay_transcript")

private const val API_BASE_URL = "https://api.rune-systems.com"
private const val INCIDENT_SCHEMA_VERSION = "incident-v1"
private val INCIDENT_SUPPORTED_CLASSES = setOf("auth", "network", "runtime", "state", "capability", "custom")

internal fun extractLocalePreferenceSyncPayload(payloadRaw: Any?): String? {
    if (payloadRaw !is JSONObject) return null
    if (payloadRaw.isNull("localePreference")) return null
    return payloadRaw.optString("localePreference").trim().ifBlank { null }?.lowercase()
}

data class PairingSession(
    val deviceId: String,
    val pairingCode: String,
    val timestamp: Long,
    val qrPayload: String,
)

data class WatchPresenceDiagnosticsSnapshot(
    val staleCause: String,
    val relayHealthy: Boolean,
    val heartbeatPath: String,
    val lastSeenLabel: String,
    val lastRelayReceiptAtMs: Long?,
)

internal data class DeviceBusPersistedState(
    val deviceId: String? = null,
    val userId: String? = null,
    val authToken: String? = null,
    val localePreference: String? = null,
    val relayTranscriptRaw: String? = null,
)

internal interface DeviceBusPersistence {
    suspend fun loadState(): DeviceBusPersistedState
    suspend fun saveDeviceId(deviceId: String)
    suspend fun saveCredentials(userId: String, authToken: String, localePreference: String?)
    suspend fun saveRelayTranscript(raw: String?)
    suspend fun clearCredentials()
}

internal interface DeviceBusTranscriptSnapshotFetcher {
    suspend fun fetchSnapshot(userId: String, authToken: String, conversationId: String? = null): String?
}

internal class DataStoreDeviceBusPersistence(private val context: Context) : DeviceBusPersistence {
    override suspend fun loadState(): DeviceBusPersistedState {
        val prefs = context.applicationContext.emberPrefsDataStore.data.first()
        return DeviceBusPersistedState(
            deviceId = prefs[KEY_DEVICE_ID],
            userId = prefs[KEY_USER_ID],
            authToken = prefs[KEY_AUTH_TOKEN],
            localePreference = prefs[KEY_LOCALE_PREFERENCE],
            relayTranscriptRaw = prefs[KEY_RELAY_TRANSCRIPT],
        )
    }

    override suspend fun saveDeviceId(deviceId: String) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            prefs[KEY_DEVICE_ID] = deviceId
        }
    }

    override suspend fun saveCredentials(userId: String, authToken: String, localePreference: String?) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            prefs[KEY_USER_ID] = userId
            prefs[KEY_AUTH_TOKEN] = authToken
            val normalizedLocale = localePreference?.trim()?.lowercase().orEmpty()
            if (normalizedLocale.isBlank()) {
                prefs.remove(KEY_LOCALE_PREFERENCE)
            } else {
                prefs[KEY_LOCALE_PREFERENCE] = normalizedLocale
            }
        }
    }

    override suspend fun saveRelayTranscript(raw: String?) {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            if (raw.isNullOrBlank()) {
                prefs.remove(KEY_RELAY_TRANSCRIPT)
            } else {
                prefs[KEY_RELAY_TRANSCRIPT] = raw
            }
        }
    }

    override suspend fun clearCredentials() {
        context.applicationContext.emberPrefsDataStore.edit { prefs ->
            prefs.remove(KEY_USER_ID)
            prefs.remove(KEY_AUTH_TOKEN)
            prefs.remove(KEY_LOCALE_PREFERENCE)
            prefs.remove(KEY_RELAY_TRANSCRIPT)
        }
    }
}

private class OkHttpDeviceBusTranscriptSnapshotFetcher(
    private val http: OkHttpClient,
) : DeviceBusTranscriptSnapshotFetcher {
    override suspend fun fetchSnapshot(userId: String, authToken: String, conversationId: String?): String? {
        return withContext(Dispatchers.IO) {
            runCatching {
                val encodedUserId = URLEncoder.encode(userId, Charsets.UTF_8.name())
                val encodedConversationId = conversationId
                    ?.trim()
                    ?.ifEmpty { null }
                    ?.let { URLEncoder.encode(it, Charsets.UTF_8.name()) }
                val req = Request.Builder()
                    .url(
                        buildString {
                            append("$API_BASE_URL/api/device/transcript?userId=")
                            append(encodedUserId)
                            if (!encodedConversationId.isNullOrBlank()) {
                                append("&conversationId=")
                                append(encodedConversationId)
                            }
                        }
                    )
                    .addHeader("Authorization", "Bearer $authToken")
                    .get()
                    .build()

                http.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    response.body?.string()
                }
            }.getOrNull()
        }
    }
}

/**
 * WebSocket client that connects this WearOS device to the RUNE-Backend device bus.
 *
 * Call [connect] when the app becomes visible and [disconnect] on destroy.
 * The client registers the device on first connect, reconnects automatically
 * on failure with exponential back-off, and dispatches incoming cross-device
 * commands as state updates to Compose collectors.
 */
class DeviceBusClient internal constructor(
    private val context: Context,
    private val persistence: DeviceBusPersistence = DataStoreDeviceBusPersistence(context),
    private val snapshotFetcherOverride: DeviceBusTranscriptSnapshotFetcher? = null,
    autoLoad: Boolean = true,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val http = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS)
        .build()
    private val transcriptSnapshotFetcher: DeviceBusTranscriptSnapshotFetcher by lazy {
        snapshotFetcherOverride ?: OkHttpDeviceBusTranscriptSnapshotFetcher(http)
    }

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

    private val _relayTranscript = MutableStateFlow<List<WatchRelayTranscriptEntry>>(emptyList())
    val relayTranscript: StateFlow<List<WatchRelayTranscriptEntry>> = _relayTranscript.asStateFlow()

    private val _relayTranscriptSummaries = MutableStateFlow<List<WatchRelayTranscriptSummary>>(emptyList())
    val relayTranscriptSummaries: StateFlow<List<WatchRelayTranscriptSummary>> = _relayTranscriptSummaries.asStateFlow()

    private val _selectedRelayConversationId = MutableStateFlow<String?>(null)
    val selectedRelayConversationId: StateFlow<String?> = _selectedRelayConversationId.asStateFlow()

    private val _paired = MutableStateFlow(false)
    val paired: StateFlow<Boolean> = _paired.asStateFlow()

    private val _adaptiveFallbackProfile = MutableStateFlow<String?>(null)
    val adaptiveFallbackProfile: StateFlow<String?> = _adaptiveFallbackProfile.asStateFlow()

    private val _deviceIdState = MutableStateFlow("")
    val deviceIdState: StateFlow<String> = _deviceIdState.asStateFlow()

    private val _userIdState = MutableStateFlow("")
    val userIdState: StateFlow<String> = _userIdState.asStateFlow()

    private val _localePreferenceState = MutableStateFlow<String?>(null)
    val localePreferenceState: StateFlow<String?> = _localePreferenceState.asStateFlow()

    private val _pairingSession = MutableStateFlow<PairingSession?>(null)
    val pairingSession: StateFlow<PairingSession?> = _pairingSession.asStateFlow()

    // ── Internal ──────────────────────────────────────────────────────────────

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var heartbeatJob: Job? = null
    private var deviceId: String = ""
    private var userId: String = ""
    private var authToken: String = ""
    private val lifecycleDiagnostics = WatchElLifecycleDiagnosticsEmitter()

    private fun normalizeSourceSurface(value: String): String {
        return when (value.trim().lowercase()) {
            "desktop", "laptop", "pc" -> "desktop"
            "mobile", "phone", "android", "ios" -> "mobile"
            "watch", "wearos", "wearable" -> "watch"
            "web", "browser" -> "web"
            else -> ""
        }
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

    private fun defaultSourceLabel(surface: String): String {
        return when (surface) {
            "desktop" -> "Desktop"
            "mobile" -> "Phone"
            "watch" -> "Watch"
            "web" -> "Web"
            else -> "Linked Surface"
        }
    }

    private fun relayStatusCopy(status: String): String {
        return when (status) {
            "sending" -> "Relay sending"
            "queued" -> "Relay queued"
            "delivered" -> "Relay delivered"
            "seen" -> "Relay seen"
            "responded" -> "Relay responded"
            else -> "Relay update"
        }
    }

    init {
        if (autoLoad) {
            scope.launch { refreshStoredState() }
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    fun connect() {
        scope.launch {
            refreshStoredState()
            startReconnectLoop()
        }
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "app closing")
        webSocket = null
        scope.cancel()
    }

    // ── Command dispatch ──────────────────────────────────────────────────────

    fun sendCommand(intent: String, payload: Map<String, Any>?) {
        if (intent == "relay_message") {
            val messageText = (payload?.get("text") as? String).orEmpty().trim()
            val threadId = (payload?.get("threadId") as? String)?.trim()?.ifEmpty { null }
            setRelayTranscript(
                appendOutgoingRelayTranscript(
                    current = _relayTranscript.value,
                    text = messageText,
                    threadId = threadId,
                    updatedAtMs = System.currentTimeMillis(),
                )
            )
        }
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
        // Clear the notification immediately so the UI updates before the network round-trip
        _currentNotification.value = null

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

    suspend fun respondToIncidentAction(notification: ParsedNotification, actionId: String): Boolean {
        _currentNotification.value = null

        val incidentId = notification.incidentId?.trim().orEmpty()
        if (incidentId.isBlank()) {
            return respondToNotificationAction(notification.id, actionId)
        }

        val incidentClass = notification.incidentClass?.trim()?.lowercase().orEmpty()
        if (incidentClass.isBlank() || !INCIDENT_SUPPORTED_CLASSES.contains(incidentClass)) {
            Log.w(TAG, "respondToIncidentAction: ignoring unsupported incident class '$incidentClass'")
            return false
        }

        val acked = actionId != "dismiss"
        val posted = postIncidentAckSignal(
            incidentId = incidentId,
            acked = acked,
            tenantId = notification.incidentTenantId,
        )

        if (!posted) {
            return false
        }

        return if (acked) {
            true
        } else {
            respondToNotificationAction(notification.id, actionId)
        }
    }

    suspend fun fetchPresenceDiagnosticsSnapshot(): WatchPresenceDiagnosticsSnapshot? {
        if (userId.isBlank() || authToken.isBlank() || deviceId.isBlank()) return null

        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "$API_BASE_URL/api/device/presence-diagnostics?userId=$userId&limit=200"
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $authToken")
                    .get()
                    .build()

                http.newCall(req).execute().use { res ->
                    if (!res.isSuccessful) return@use null
                    val raw = res.body?.string().orEmpty()
                    val root = JSONObject(raw)
                    val data = root.optJSONObject("data") ?: return@use null
                    val devices = data.optJSONArray("devices") ?: return@use null

                    var row: JSONObject? = null
                    for (i in 0 until devices.length()) {
                        val candidate = devices.optJSONObject(i) ?: continue
                        if (candidate.optString("deviceId").trim() == deviceId) {
                            row = candidate
                            break
                        }
                    }
                    if (row == null) {
                        for (i in 0 until devices.length()) {
                            val candidate = devices.optJSONObject(i) ?: continue
                            if (candidate.optString("platform").trim() == "watch") {
                                row = candidate
                                break
                            }
                        }
                    }
                    if (row == null) return@use null

                    WatchPresenceDiagnosticsSnapshot(
                        staleCause = row.optString("staleCause").ifBlank { "unknown" },
                        relayHealthy = row.optBoolean("relayHealthy", false),
                        heartbeatPath = row.optString("heartbeatPath").ifBlank { "unknown" },
                        lastSeenLabel = row.optString("lastSeenLabel").ifBlank { "unknown" },
                        lastRelayReceiptAtMs = if (row.has("lastRelayReceiptAtMs") && !row.isNull("lastRelayReceiptAtMs")) {
                            row.optLong("lastRelayReceiptAtMs")
                        } else {
                            null
                        },
                    )
                }
            }.getOrNull()
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun startReconnectLoop() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var delaySecs = 2L
            var attemptedOnce = false
            while (isActive) {
                if (authToken.isNotBlank() && userId.isNotBlank() && deviceId.isNotBlank()) {
                    if (attemptedOnce) {
                        runCatching {
                            lifecycleDiagnostics.emit(
                                state = "reconnecting",
                                deviceId = deviceId,
                                userId = userId,
                                authToken = authToken,
                            )
                        }
                    }
                    attemptedOnce = true
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
            val capabilities = mutableListOf("notifications", "haptics")
            if (LfseSeam.ENABLED) capabilities += "lfse-v1"

            val caps = JSONObject().apply {
                put("deviceId",     deviceId)
                put("platform",     "wearos")
                put("capabilities", org.json.JSONArray(capabilities))
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
                Log.i(TAG, "openWebSocket: connected")
                _connected.value = true
                scope.launch {
                    runCatching {
                        lifecycleDiagnostics.emit(
                            state = "connected",
                            deviceId = deviceId,
                            userId = userId,
                            authToken = authToken,
                        )
                    }
                }
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
                scope.launch {
                    runCatching {
                        lifecycleDiagnostics.emit(
                            state = "closed",
                            deviceId = deviceId,
                            userId = userId,
                            authToken = authToken,
                        )
                    }
                }
                if (cont.isActive) cont.resume(Unit) { _, _, _ -> }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(
                    TAG,
                    "openWebSocket: failed code=${response?.code} msg=${response?.message} err=${t.message}",
                    t,
                )
                _connected.value = false
                stopHeartbeat()
                scope.launch {
                    runCatching {
                        lifecycleDiagnostics.emit(
                            state = "error",
                            deviceId = deviceId,
                            userId = userId,
                            authToken = authToken,
                        )
                    }
                }
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
        if (parsed.relayChatEvent != null) {
            setRelayTranscript(
                applyRelayChatEvent(
                    current = _relayTranscript.value,
                    event = parsed.relayChatEvent,
                    updatedAtMs = System.currentTimeMillis(),
                )
            )
        }
        if (parsed.adaptiveFallbackProfile != null) {
            _adaptiveFallbackProfile.value = parsed.adaptiveFallbackProfile
        }
    }

    private fun setRelayTranscript(next: List<WatchRelayTranscriptEntry>, persist: Boolean = true) {
        _relayTranscript.value = next
        if (!persist) return

        scope.launch {
            persistRelayTranscript(next)
        }
    }

    private suspend fun persistRelayTranscript(entries: List<WatchRelayTranscriptEntry>) {
        persistence.saveRelayTranscript(
            if (entries.isEmpty()) null else encodeRelayTranscript(entries)
        )
    }

    private fun handleCommandIntent(command: JSONObject) {
        val intent = command.optString("intent")
        val payload = command.optJSONObject("payload")
        val payloadRaw = command.opt("payload")
        val fromDevice = command.optString("fromDevice")

        when (intent) {
            "watch_locale_preference_sync" -> {
                scope.launch {
                    WatchSettingsStore.setLocalePreference(context, extractLocalePreferenceSyncPayload(payloadRaw))
                }
            }
            "haptic_ping" -> {
                triggerHaptic(longArrayOf(0L, 80L, 40L, 90L))
                _currentNotification.value = ParsedNotification(
                    id = command.optString("commandId").ifBlank { "haptic-${System.currentTimeMillis()}" },
                    type = "status",
                    title = payload?.optString("title").orEmpty().ifBlank { watchPhonePingTitle(_localePreferenceState.value) },
                    body = payload?.optString("body").orEmpty().ifBlank { watchPhonePingBody(_localePreferenceState.value) },
                    actions = emptyList(),
                )
            }
            // WDP-9A: named pattern dispatch via HapticPatternRegistry
            "haptic.pattern" -> {
                val patternId = payload?.optString("patternId").orEmpty()
                val waveform = HapticPatternRegistry.resolve(patternId)
                if (waveform != null) {
                    triggerHaptic(waveform)
                } else {
                    Log.d(TAG, "haptic.pattern: unknown patternId '$patternId' — no-op")
                }
            }
            "alert_notify", "process_completion" -> {
                triggerHaptic(longArrayOf(0L, 60L, 30L, 60L, 30L, 90L))
                _currentNotification.value = ParsedNotification(
                    id = command.optString("commandId").ifBlank { "alert-${System.currentTimeMillis()}" },
                    type = "alert",
                    title = payload?.optString("title").orEmpty().ifBlank {
                        if (intent == "process_completion") watchProcessCompleteTitle(_localePreferenceState.value) else watchEmberAlertTitle(_localePreferenceState.value)
                    },
                    body = payload?.optString("body").orEmpty().ifBlank { watchLinkedSurfaceUpdateBody(_localePreferenceState.value) },
                    actions = emptyList(),
                )
            }
            "incident_notify" -> {
                val incidentPayload = payload?.optJSONObject("incident")
                val incidentId = incidentPayload?.optString("incidentId").orEmpty().trim()
                val incidentClass = incidentPayload?.optString("class").orEmpty().trim().lowercase()
                val incidentSeverity = incidentPayload?.optString("severity").orEmpty().trim().lowercase()
                val incidentSummary = incidentPayload?.optString("summary")
                    .orEmpty()
                    .trim()
                    .ifBlank { payload?.optString("body").orEmpty().trim() }

                if (incidentId.isBlank() || incidentSummary.isBlank() || !INCIDENT_SUPPORTED_CLASSES.contains(incidentClass)) {
                    Log.w(TAG, "incident_notify: ignored malformed or unsupported incident payload")
                    return
                }

                val severityLabel = watchIncidentSeverityLabel(incidentSeverity, _localePreferenceState.value)

                triggerHaptic(longArrayOf(0L, 70L, 30L, 90L, 30L, 120L))
                _currentNotification.value = ParsedNotification(
                    id = command.optString("commandId").ifBlank { "incident-${System.currentTimeMillis()}" },
                    type = "alert",
                    title = payload?.optString("title").orEmpty().ifBlank { watchIncidentTitle(incidentSeverity, _localePreferenceState.value) },
                    body = incidentSummary,
                    actions = listOf(
                        NotificationAction(id = "ack", label = watchNotificationAcknowledgeText(_localePreferenceState.value), actionType = "primary"),
                        NotificationAction(id = "dismiss", label = watchNotificationDismissText(_localePreferenceState.value), actionType = "secondary"),
                    ),
                    incidentId = incidentId,
                    incidentClass = incidentClass,
                    incidentSeverity = incidentSeverity.ifBlank { "warning" },
                    incidentTenantId = incidentPayload?.optString("tenantId")?.trim()?.ifBlank { null },
                )
            }
            "relay_message" -> {
                var messageText = ""
                var sourceLabel = ""
                var sourceSurface = ""
                var clientMessageId = ""
                var threadId: String? = null

                if (payloadRaw is JSONObject) {
                    messageText = payloadRaw.optString("text").orEmpty().trim()
                    sourceLabel = payloadRaw.optString("sourceLabel").orEmpty().trim()
                    sourceSurface = normalizeSourceSurface(payloadRaw.optString("sourceSurface").orEmpty())
                    clientMessageId = payloadRaw.optString("clientMessageId").orEmpty().trim()
                    threadId = deriveThreadId(payloadRaw)
                } else if (payloadRaw is String) {
                    messageText = payloadRaw.trim()
                }

                if (messageText.isBlank()) {
                    messageText = watchLinkedSurfaceMessageText(_localePreferenceState.value)
                }

                triggerHaptic(longArrayOf(0L, 50L, 30L, 70L, 30L, 70L))
                _currentNotification.value = ParsedNotification(
                    id = command.optString("commandId").ifBlank { "relay-${System.currentTimeMillis()}" },
                    type = "prompt",
                    title = sourceLabel.ifBlank { defaultSourceLabel(sourceSurface) },
                    body = messageText,
                    actions = emptyList(),
                    threadId = threadId,
                )

                if (fromDevice.isNotBlank() && clientMessageId.isNotBlank()) {
                    scope.launch {
                        sendDirectCommand(
                            toDevice = fromDevice,
                            intent = "relay_receipt",
                            payload = JSONObject().apply {
                                put("clientMessageId", clientMessageId)
                                put("status", normalizeRelayStatus("seen", fallback = "seen"))
                                put("byDevice", deviceId)
                                put("respondedFromLabel", "Watch")
                                put("respondedFromSurface", "watch")
                            },
                        )
                    }
                }
            }
            "relay_receipt" -> {
                if (payloadRaw is JSONObject) {
                    val status = applyRelayReceiptStatus(
                        current = null,
                        receipt = payloadRaw.optString("status").orEmpty(),
                    )
                    val threadId = deriveThreadId(payloadRaw)
                    val sourceSurface = normalizeSourceSurface(
                        payloadRaw.optString("respondedFromSurface")
                            .ifBlank { payloadRaw.optString("sourceSurface") }
                            .orEmpty(),
                    )
                    val sourceLabel = payloadRaw.optString("respondedFromLabel")
                        .ifBlank { payloadRaw.optString("sourceLabel") }
                        .trim()

                    _currentNotification.value = ParsedNotification(
                        id = command.optString("commandId").ifBlank { "relay-receipt-${System.currentTimeMillis()}" },
                        type = "status",
                        title = sourceLabel.ifBlank { defaultSourceLabel(sourceSurface) },
                        body = relayStatusCopy(status),
                        actions = emptyList(),
                        threadId = threadId,
                    )
                }
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

        // WDP-9A: process optional LFSE hint envelope (no-op when LfseSeam.ENABLED=false)
        val lfseHint = command.optJSONObject("lfseHint")
        if (lfseHint != null) {
            val sessionId = lfseHint.optString("sessionId").orEmpty()
            val deltaCount = lfseHint.optInt("deltaCount", 0)
            LfseSeam.onSyncHint(sessionId, deltaCount)
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

    internal suspend fun refreshStoredState() {
        val state = persistence.loadState()
        deviceId = state.deviceId ?: generateAndSaveDeviceId()
        userId = state.userId ?: ""
        authToken = state.authToken ?: ""
        _localePreferenceState.value = state.localePreference?.trim()?.ifBlank { null }
        _relayTranscript.value = decodeRelayTranscript(state.relayTranscriptRaw)
        _deviceIdState.value = deviceId
        _userIdState.value = userId
        _paired.value = userId.isNotBlank() && authToken.isNotBlank()

        if (_paired.value) {
            LfseSeam.initialize(deviceId = deviceId, userId = userId, authToken = authToken)
            hydrateRelayTranscriptFromBackend()
        }
    }

    private suspend fun generateAndSaveDeviceId(): String {
        val id = UUID.randomUUID().toString()
        persistence.saveDeviceId(id)
        return id
    }

    /** Called from settings / auth flow to persist credentials. */
    suspend fun saveCredentials(userId: String, authToken: String, localePreference: String? = null) {
        this.userId    = userId
        this.authToken = authToken
        persistence.saveCredentials(userId, authToken, localePreference)
        _userIdState.value = userId
        _localePreferenceState.value = localePreference?.trim()?.ifBlank { null }?.lowercase()
        _paired.value = true
        _pairingSession.value = null

        LfseSeam.initialize(deviceId = deviceId, userId = userId, authToken = authToken)
        hydrateRelayTranscriptFromBackend()

        // Credentials became available after pairing; restart reconnect loop now
        // instead of waiting for the current backoff window to expire.
        startReconnectLoop()
    }

    suspend fun clearCredentials() {
        this.userId = ""
        this.authToken = ""
        persistence.clearCredentials()
        _connected.value = false
        _userIdState.value = ""
        _localePreferenceState.value = null
        _paired.value = false
        _pairingSession.value = null
        _currentNotification.value = null
        _relayTranscript.value = emptyList()
        _relayTranscriptSummaries.value = emptyList()
        _selectedRelayConversationId.value = null
    }

    fun cycleRelayTranscriptConversation(direction: Int) {
        val summaries = _relayTranscriptSummaries.value
        if (summaries.size <= 1) return

        val currentIndex = summaries.indexOfFirst { it.conversationId == _selectedRelayConversationId.value }
            .takeIf { it >= 0 }
            ?: 0
        val nextIndex = ((currentIndex + direction) % summaries.size + summaries.size) % summaries.size
        val nextConversationId = summaries[nextIndex].conversationId

        scope.launch {
            hydrateRelayTranscriptFromBackend(nextConversationId)
        }
    }

    private suspend fun hydrateRelayTranscriptFromBackend(conversationId: String? = _selectedRelayConversationId.value) {
        if (userId.isBlank() || authToken.isBlank()) return

        val requestedConversationId = conversationId?.trim()?.ifEmpty { null }
        val rawResponse = transcriptSnapshotFetcher.fetchSnapshot(userId, authToken, requestedConversationId)
        val summaries = readSharedRelayTranscriptSummaries(rawResponse)
        _relayTranscriptSummaries.value = summaries
        _selectedRelayConversationId.value = when {
            !requestedConversationId.isNullOrBlank() && summaries.any { it.conversationId == requestedConversationId } -> requestedConversationId
            !_selectedRelayConversationId.value.isNullOrBlank() && summaries.any { it.conversationId == _selectedRelayConversationId.value } -> _selectedRelayConversationId.value
            else -> summaries.firstOrNull()?.conversationId
        }

        val merged = mergeSharedRelayTranscriptSnapshot(
            current = _relayTranscript.value,
            rawResponse = rawResponse,
        ) ?: return

        _relayTranscript.value = merged
        persistRelayTranscript(merged)
    }

    suspend fun ensurePairingSession(forceRefresh: Boolean = false): PairingSession {
        refreshStoredState()

        val current = _pairingSession.value
        if (!forceRefresh && current != null && (System.currentTimeMillis() - current.timestamp) < 60_000L) {
            Log.i(TAG, "ensurePairingSession: returning cached session code=${current.pairingCode}")
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

        Log.i(TAG, "ensurePairingSession: registering new session deviceId=$resolvedDeviceId code=$pairingCode")
        withContext(Dispatchers.IO) {
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
                http.newCall(req).execute().use { res ->
                    Log.i(TAG, "ensurePairingSession: register response HTTP ${res.code}")
                }
            }.onFailure { e ->
                Log.e(TAG, "ensurePairingSession: register FAILED: ${e}")
            }
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
        val session = _pairingSession.value ?: run {
            Log.w(TAG, "pollPairingStatus: no active session")
            return false
        }
        Log.d(TAG, "pollPairingStatus: checking deviceId=${session.deviceId} code=${session.pairingCode}")
        return withContext(Dispatchers.IO) {
            runCatching {
                val url = "$API_BASE_URL/api/device/pair/status?deviceId=${session.deviceId}&pairingCode=${session.pairingCode}"
                val req = Request.Builder().url(url).get().build()
                http.newCall(req).execute().use { res ->
                    val raw = res.body?.string() ?: ""
                    Log.i(TAG, "pollPairingStatus: HTTP ${res.code} body=$raw")
                    if (!res.isSuccessful) return@use false
                    val body = JSONObject(raw)
                    if (!body.optBoolean("ok", false)) return@use false
                    val data = body.optJSONObject("data") ?: return@use false
                    val paired = data.optBoolean("paired", false)
                    if (!paired) return@use false

                    val pairedUserId = data.optString("userId")
                    val watchAuthToken = data.optString("watchAuthToken")
                    val localePreference = data.optString("localePreference").trim().ifBlank { null }
                    Log.i(TAG, "pollPairingStatus: paired=true userId=$pairedUserId hasToken=${watchAuthToken.isNotBlank()}")
                    if (pairedUserId.isBlank() || watchAuthToken.isBlank()) return@use false

                    saveCredentials(pairedUserId, watchAuthToken, localePreference)
                    true
                }
            }.onFailure { e ->
                Log.e(TAG, "pollPairingStatus: FAILED: ${e}")
            }.getOrDefault(false)
        }
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

    private suspend fun postIncidentAckSignal(
        incidentId: String,
        acked: Boolean,
        tenantId: String? = null,
    ): Boolean {
        if (userId.isBlank() || authToken.isBlank() || deviceId.isBlank()) return false

        return withContext(Dispatchers.IO) {
            runCatching {
                val body = JSONObject().apply {
                    put("incidentSchemaVersion", INCIDENT_SCHEMA_VERSION)
                    put("incidentId", incidentId)
                    put("deviceId", deviceId)
                    put("acked", acked)
                    put("ackedAt", java.time.Instant.now().toString())
                    put("confidence", if (acked) 0.9 else 0.2)
                    if (!tenantId.isNullOrBlank()) put("tenantId", tenantId)
                }.toString()

                val req = Request.Builder()
                    .url("$API_BASE_URL/api/diagnostics/incidents/ack?userId=$userId")
                    .addHeader("Authorization", "Bearer $authToken")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(req).execute().use { it.isSuccessful }
            }.getOrDefault(false)
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
