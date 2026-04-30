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
    fun parseDeviceBusMessage_readsNotificationWithSourceTags() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"notification",
              "data":{
                "id":"notif-1",
                "type":"relay",
                "title":"Message from Phone",
                "body":"Hello from mobile",
                "sourceLabel":"Phone",
                "sourceSurface":"phone",
                "actions":[]
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.notification)
        assertEquals("notif-1", parsed.notification?.id)
        assertEquals("relay", parsed.notification?.type)
        assertEquals("Message from Phone", parsed.notification?.title)
        assertEquals("Hello from mobile", parsed.notification?.body)
        assertEquals("Phone", parsed.notification?.sourceLabel)
        assertEquals("phone", parsed.notification?.sourceSurface)
    }

    @Test
    fun parseDeviceBusMessage_notificationSourceTagsOptional() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"notification",
              "data":{
                "id":"notif-2",
                "type":"status",
                "title":"Status Update",
                "body":"No source tags",
                "actions":[]
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.notification)
        assertEquals("notif-2", parsed.notification?.id)
        assertNull(parsed.notification?.sourceLabel)
        assertNull(parsed.notification?.sourceSurface)
    }

    @Test
    fun parseDeviceBusMessage_readsNotificationWithActions() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"notification",
              "data":{
                "id":"notif-3",
                "type":"alert",
                "title":"Action Available",
                "body":"Take action",
                "sourceLabel":"Desktop",
                "sourceSurface":"desktop",
                "actions":[
                  {"id":"ack","label":"Acknowledge","actionType":"primary"},
                  {"id":"dismiss","label":"Dismiss","actionType":"secondary"}
                ]
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.notification)
        assertEquals(2, parsed.notification?.actions?.size)
        assertEquals("Desktop", parsed.notification?.sourceLabel)
        assertEquals("desktop", parsed.notification?.sourceSurface)
        assertEquals("Acknowledge", parsed.notification?.actions?.get(0)?.label)
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
