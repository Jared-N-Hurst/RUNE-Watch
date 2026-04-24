// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import android.content.Context

/**
 * Process-local runtime holder so the foreground service owns the live bus
 * connection while UI layers only observe/send through the same client instance.
 */
object DeviceBusRuntime {
    private var clientInstance: DeviceBusClient? = null
    private var started = false

    fun client(context: Context): DeviceBusClient {
        val existing = clientInstance
        if (existing != null) return existing

        val created = DeviceBusClient(context.applicationContext)
        clientInstance = created
        return created
    }

    fun start(context: Context) {
        if (started) return
        client(context).connect()
        started = true
    }

    fun stop() {
        val existing = clientInstance ?: return
        existing.disconnect()
        clientInstance = null
        started = false
    }
}
