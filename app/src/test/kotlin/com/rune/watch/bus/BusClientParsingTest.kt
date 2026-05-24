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
      fun parseDeviceBusMessage_readsAdaptiveFallbackProfileCommand() {
        val parsed = parseDeviceBusMessage(
          """
          {
            "type":"command",
            "intent":"adaptive_fallback_profile",
            "payload":{"strategy":"conservative","cooldownSeconds":90}
          }
          """.trimIndent()
        )

        assertNotNull(parsed.adaptiveFallbackProfile)
        assertEquals(true, parsed.adaptiveFallbackProfile!!.contains("conservative"))
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
        assertEquals("mobile", parsed.notification?.sourceSurface)
    }

    @Test
    fun parseDeviceBusMessage_normalizesNotificationSourceSurfaceAliases() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"notification",
              "data":{
                "id":"notif-1a",
                "type":"relay",
                "title":"Message from desktop",
                "body":"Hello from laptop",
                "sourceLabel":"Desktop",
                "sourceSurface":"laptop",
                "actions":[]
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.notification)
        assertEquals("desktop", parsed.notification?.sourceSurface)
    }

    @Test
    fun parseDeviceBusMessage_prefersCanonicalThreadIdOverLegacyAliases() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"notification",
              "data":{
                "id":"notif-thread-1",
                "type":"relay",
                "title":"Message from desktop",
                "body":"Canonical thread should win",
                "threadId":"canonical-thread-7",
                "conversationId":"legacy-conversation-8",
                "groupId":"group-thread-9",
                "actions":[]
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.notification)
        assertEquals("canonical-thread-7", parsed.notification?.threadId)
    }

    @Test
    fun parseDeviceBusMessage_usesLegacyConversationIdWhenThreadIdMissing() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"notification",
              "data":{
                "id":"notif-thread-2",
                "type":"relay",
                "title":"Message from phone",
                "body":"Legacy alias fallback",
                "conversationId":"legacy-conversation-11",
                "actions":[]
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.notification)
        assertEquals("legacy-conversation-11", parsed.notification?.threadId)
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
    fun parseDeviceBusMessage_readsRelayMessageAsChatEvent() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"command",
              "intent":"relay_message",
              "commandId":"relay-cmd-1",
              "payload":{
                "text":"Hello from desktop",
                "sourceLabel":"Desktop",
                "sourceSurface":"laptop",
                "threadId":"thread-22"
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.relayChatEvent)
        assertEquals("relay-cmd-1", parsed.relayChatEvent?.eventId)
        assertEquals("Hello from desktop", parsed.relayChatEvent?.text)
        assertEquals("Desktop", parsed.relayChatEvent?.sourceLabel)
        assertEquals("desktop", parsed.relayChatEvent?.sourceSurface)
        assertEquals("thread-22", parsed.relayChatEvent?.threadId)
        assertNull(parsed.relayChatEvent?.status)
    }

    @Test
    fun parseDeviceBusMessage_readsRelayReceiptAsChatEventStatus() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"command",
              "intent":"relay_receipt",
              "commandId":"relay-receipt-1",
              "payload":{
                "status":"seen",
                "respondedFromLabel":"Phone",
                "respondedFromSurface":"android",
                "conversationId":"legacy-thread-18"
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.relayChatEvent)
        assertEquals("relay-receipt-1", parsed.relayChatEvent?.eventId)
        assertEquals("seen", parsed.relayChatEvent?.status)
        assertEquals("Phone", parsed.relayChatEvent?.sourceLabel)
        assertEquals("mobile", parsed.relayChatEvent?.sourceSurface)
        assertEquals("legacy-thread-18", parsed.relayChatEvent?.threadId)
    }

    @Test
    fun parseDeviceBusMessage_acceptsIncidentNotificationForSupportedClass() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"notification",
              "data":{
                "id":"notif-incident-1",
                "type":"alert",
                "title":"Incident Warning",
                "body":"Token refresh failed repeatedly.",
                "incident":{
                  "incidentId":"inc-auth-1",
                  "class":"auth",
                  "severity":"critical",
                  "tenantId":"global"
                },
                "actions":[{"id":"ack","label":"Acknowledge","actionType":"primary"}]
              }
            }
            """.trimIndent()
        )

        assertNotNull(parsed.notification)
        assertEquals("inc-auth-1", parsed.notification?.incidentId)
        assertEquals("auth", parsed.notification?.incidentClass)
        assertEquals("critical", parsed.notification?.incidentSeverity)
        assertEquals("global", parsed.notification?.incidentTenantId)
    }

    @Test
    fun parseDeviceBusMessage_rejectsIncidentNotificationForUnsupportedClass() {
        val parsed = parseDeviceBusMessage(
            """
            {
              "type":"notification",
              "data":{
                "id":"notif-incident-2",
                "type":"alert",
                "title":"Incident Unsupported",
                "body":"Unsupported incident class should be ignored.",
                "incident":{
                  "incidentId":"inc-unsupported-1",
                  "class":"database",
                  "severity":"warning"
                }
              }
            }
            """.trimIndent()
        )

        assertNull(parsed.notification)
    }

    @Test
    fun parseEmberEventEnvelopeV1_accepts_valid_el1_event() {
        val parsed = parseEmberEventEnvelopeV1(
            """
            {
              "eventSchemaVersion": "el-1.v1",
              "eventId": "evt-1",
              "emberId": "ember-1",
              "userId": "user-1",
              "eventType": "UserMessage",
              "provenance": "user",
              "surface": "watch",
              "sequence": 7,
              "causalParentIds": ["evt-0"],
              "occurredAt": "2026-05-17T00:00:00Z",
              "recordedAt": "2026-05-17T00:00:01Z",
              "payload": { "text": "hello" }
            }
            """.trimIndent()
        )

        assertNotNull(parsed)
        assertEquals("el-1.v1", parsed?.eventSchemaVersion)
        assertEquals("UserMessage", parsed?.eventType)
        assertEquals(listOf("evt-0"), parsed?.causalParentIds)
    }

    @Test
    fun parseEmberEventEnvelopeV1_rejects_invalid_type_or_surface() {
        val invalidType = parseEmberEventEnvelopeV1(
            """
            {
              "eventSchemaVersion": "el-1.v1",
              "eventId": "evt-1",
              "emberId": "ember-1",
              "userId": "user-1",
              "eventType": "NotARealType",
              "provenance": "user",
              "surface": "watch",
              "sequence": 7,
              "occurredAt": "2026-05-17T00:00:00Z",
              "recordedAt": "2026-05-17T00:00:01Z",
              "payload": {}
            }
            """.trimIndent()
        )
        assertNull(invalidType)

        val invalidSurface = parseEmberEventEnvelopeV1(
            """
            {
              "eventSchemaVersion": "el-1.v1",
              "eventId": "evt-1",
              "emberId": "ember-1",
              "userId": "user-1",
              "eventType": "UserMessage",
              "provenance": "user",
              "surface": "browser",
              "sequence": 7,
              "occurredAt": "2026-05-17T00:00:00Z",
              "recordedAt": "2026-05-17T00:00:01Z",
              "payload": {}
            }
            """.trimIndent()
        )
        assertNull(invalidSurface)
    }

    @Test
    fun parseEmberEventEnvelopeV1_rejects_unsupported_incident_class() {
        val parsed = parseEmberEventEnvelopeV1(
            """
            {
              "eventSchemaVersion": "el-1.v1",
              "eventId": "evt-incident-1",
              "emberId": "ember-1",
              "userId": "user-1",
              "eventType": "GhostIntervention",
              "provenance": "ghost",
              "surface": "watch",
              "sequence": 11,
              "occurredAt": "2026-05-17T00:00:00Z",
              "recordedAt": "2026-05-17T00:00:01Z",
              "payload": {
                "incident": {
                  "class": "database"
                }
              }
            }
            """.trimIndent()
        )

        assertNull(parsed)
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
