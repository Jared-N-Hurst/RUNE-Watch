// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

internal suspend fun <T> runWithRetry(maxAttempts: Int = 2, operation: suspend () -> T?): T? {
    var attempts = 0
    while (attempts < maxAttempts) {
        val value = try {
            operation()
        } catch (_: Exception) {
            null
        }
        if (value != null) {
            return value
        }
        attempts += 1
    }
    return null
}
