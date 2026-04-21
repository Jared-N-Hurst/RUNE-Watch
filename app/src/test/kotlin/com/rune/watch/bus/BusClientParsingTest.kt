// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking

class BusClientParsingTest {

    @Test
    fun parseDeviceBusMessage_readsRingUpdateCommand() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"command",
              "intent":"ring_update",
              "payload":"ring:v2"
            }
            """.trimIndent()
        )

        assertEquals("ring:v2", parsed.ringUpdate)
        assertNull(parsed.emberState)
    }

    @Test
    fun parseDeviceBusMessage_readsEmberStateCommand() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"command",
              "intent":"ember_state",
              "payload":{"expression":"focused"}
            }
            """.trimIndent()
        )

        assertEquals("focused", parsed.emberState)
        assertNull(parsed.ringUpdate)
    }

    @Test
    fun parseDeviceBusMessage_returnsEmptyForInvalidJson() {
        val parsed = parseDeviceBusMessage("not-json")
        assertNull(parsed.emberState)
        assertNull(parsed.ringUpdate)
    }

    @Test
    fun emberStatusClient_parsesSuccessfulPayload() {
        val client = EmberStatusClient()
        val parsed = client.parseTileStatusBody(
            """
            {
              "ok": true,
              "data": {
                "userId": "u-1",
                "phaseLabel": "Listening",
                "engagementSummary": "Ready",
                "riskCount": 1,
                "isilmeResonance": 0.42,
                "lastUpdated": "2026-04-20T00:00:00Z"
              }
            }
            """.trimIndent(),
            fallbackUserId = "fallback"
        )

        assertNotNull(parsed)
        assertEquals("u-1", parsed.userId)
        assertEquals("Listening", parsed.phaseLabel)
        assertEquals(1, parsed.riskCount)
    }

    @Test
    fun identityMapClient_parsesSuccessfulPayload() {
        val client = IdentityMapClient()
        val parsed = client.parseCompactBody(
            """
            {
              "ok": true,
              "data": {
                "userId": "u-2",
                "convergenceState": "integrating",
                "isilmeResonance": 0.9,
                "readerInitiationLevel": 3,
                "awakenedFragmentsCount": 5
              }
            }
            """.trimIndent(),
            fallbackUserId = "fallback"
        )

        assertNotNull(parsed)
        assertEquals("u-2", parsed.userId)
        assertEquals("integrating", parsed.convergenceState)
        assertEquals(5, parsed.awakenedFragmentsCount)
    }

      @Test
      fun retryPolicy_returnsValueAfterRetry() = runBlocking {
        var attempts = 0
        val result = runWithRetry(maxAttempts = 3) {
          attempts += 1
          if (attempts < 2) null else "ok"
        }

        assertEquals("ok", result)
        assertEquals(2, attempts)
      }

      @Test
      fun retryPolicy_returnsNullAfterAllAttemptsFail() = runBlocking {
        var attempts = 0
        val result = runWithRetry(maxAttempts = 2) {
          attempts += 1
          null
        }

        assertNull(result)
        assertEquals(2, attempts)
      }

      @Test
      fun emberStatusClient_handlesPartialPayloadWithDefaults() {
        val client = EmberStatusClient()
        val parsed = client.parseTileStatusBody(
          """
          {
            "ok": true,
            "data": {
            "userId": "u-3"
            }
          }
          """.trimIndent(),
          fallbackUserId = "fallback"
        )

        assertNotNull(parsed)
        assertEquals("Ready", parsed.phaseLabel)
        assertEquals(0, parsed.riskCount)
      }

      @Test
      fun identityMapClient_returnsNullForInvalidJson() {
        val client = IdentityMapClient()
        val parsed = client.parseCompactBody("not-json", fallbackUserId = "fallback")
        assertNull(parsed)
      }
}
