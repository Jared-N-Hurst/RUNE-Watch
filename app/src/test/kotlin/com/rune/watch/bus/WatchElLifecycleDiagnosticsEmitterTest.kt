// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WatchElLifecycleDiagnosticsEmitterTest {

    private class FakeTransport : WatchElLifecycleDiagnosticsEmitter.Transport {
        var lastUrl: String? = null
        var lastAuthToken: String? = null
        var lastBody: String? = null

        override fun post(url: String, authToken: String?, body: String) {
            lastUrl = url
            lastAuthToken = authToken
            lastBody = body
        }
    }

    @Test
    fun emit_builds_expected_payload_for_connected_state() {
        val transport = FakeTransport()
        val emitter = WatchElLifecycleDiagnosticsEmitter(
            apiBaseUrl = "https://api.test",
            transport = transport,
        )

        emitter.emit(
            state = "connected",
            deviceId = "watch-1",
            userId = "user-1",
            authToken = "token-1",
        )

        assertEquals("https://api.test/api/diagnostics/event", transport.lastUrl)
        assertEquals("token-1", transport.lastAuthToken)

        val body = JSONObject(transport.lastBody ?: "{}")
        assertEquals("watch-ember-event-stream", body.getString("source"))
        assertEquals("integration", body.getString("category"))
        assertEquals("info", body.getString("severity"))
        assertEquals("EL_STREAM_STATE:connected", body.getString("message"))
        assertEquals("user-1", body.getString("userId"))

        val details = body.getJSONObject("details")
        assertEquals("watch", details.getString("surface"))
        assertEquals("connected", details.getString("state"))
        assertEquals("watch-1", details.getString("deviceId"))
        assertEquals("ember-user-1", details.getString("emberId"))
    }

    @Test
    fun emit_uses_warning_severity_for_error_and_reconnecting() {
        val transport = FakeTransport()
        val emitter = WatchElLifecycleDiagnosticsEmitter(
            apiBaseUrl = "https://api.test",
            transport = transport,
        )

        emitter.emit(
            state = "error",
            deviceId = "watch-1",
            userId = "user-1",
            authToken = null,
        )
        val first = JSONObject(transport.lastBody ?: "{}")
        assertEquals("warning", first.getString("severity"))

        emitter.emit(
            state = "reconnecting",
            deviceId = "watch-1",
            userId = "user-1",
            authToken = null,
        )
        val second = JSONObject(transport.lastBody ?: "{}")
        assertEquals("warning", second.getString("severity"))
        assertNotNull(second.getJSONObject("details"))
    }
}
