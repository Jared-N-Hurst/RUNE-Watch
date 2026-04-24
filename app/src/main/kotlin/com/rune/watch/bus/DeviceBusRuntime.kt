// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-local runtime holder so the foreground service owns the live bus
 * connection while UI layers only observe/send through the same client instance.
 */
object DeviceBusRuntime {
    private var clientInstance: DeviceBusClient? = null
    private var started = false

    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning.asStateFlow()

    private val _lastReconnectAttemptMs = MutableStateFlow<Long?>(null)
    val lastReconnectAttemptMs: StateFlow<Long?> = _lastReconnectAttemptMs.asStateFlow()

    private val _startCount = MutableStateFlow(0)
    val startCount: StateFlow<Int> = _startCount.asStateFlow()

    private val _stopCount = MutableStateFlow(0)
    val stopCount: StateFlow<Int> = _stopCount.asStateFlow()

    private val _reconnectCount = MutableStateFlow(0)
    val reconnectCount: StateFlow<Int> = _reconnectCount.asStateFlow()

    private val _lastAction = MutableStateFlow<String?>(null)
    val lastAction: StateFlow<String?> = _lastAction.asStateFlow()

    private val _lastActionResult = MutableStateFlow<String?>(null)
    val lastActionResult: StateFlow<String?> = _lastActionResult.asStateFlow()

    private val _lastActionAtMs = MutableStateFlow<Long?>(null)
    val lastActionAtMs: StateFlow<Long?> = _lastActionAtMs.asStateFlow()

    fun client(context: Context): DeviceBusClient {
        val existing = clientInstance
        if (existing != null) return existing

        val created = DeviceBusClient(context.applicationContext)
        clientInstance = created
        return created
    }

    fun start(context: Context) {
        if (started) {
            recordAction("start", "ignored_already_started")
            return
        }
        _lastReconnectAttemptMs.value = System.currentTimeMillis()
        client(context).connect()
        started = true
        _startCount.value += 1
        recordAction("start", "requested")
    }

    fun stop() {
        val existing = clientInstance
        if (existing == null) {
            recordAction("stop", "ignored_not_running")
            return
        }
        existing.disconnect()
        clientInstance = null
        started = false
        _stopCount.value += 1
        recordAction("stop", "requested")
    }

    fun restart(context: Context) {
        _lastReconnectAttemptMs.value = System.currentTimeMillis()
        _reconnectCount.value += 1
        recordAction("reconnect", "requested")
        stop()
        start(context)
    }

    fun markServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }

    private fun recordAction(action: String, result: String) {
        _lastAction.value = action
        _lastActionResult.value = result
        _lastActionAtMs.value = System.currentTimeMillis()
    }
}
