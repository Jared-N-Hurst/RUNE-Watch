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

    fun client(context: Context): DeviceBusClient {
        val existing = clientInstance
        if (existing != null) return existing

        val created = DeviceBusClient(context.applicationContext)
        clientInstance = created
        return created
    }

    fun start(context: Context) {
        if (started) return
        _lastReconnectAttemptMs.value = System.currentTimeMillis()
        client(context).connect()
        started = true
    }

    fun stop() {
        val existing = clientInstance ?: return
        existing.disconnect()
        clientInstance = null
        started = false
    }

    fun restart(context: Context) {
        _lastReconnectAttemptMs.value = System.currentTimeMillis()
        stop()
        start(context)
    }

    fun markServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }
}
