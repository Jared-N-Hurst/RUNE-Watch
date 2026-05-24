// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.bus

import android.content.ContextWrapper
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeDeviceBusPersistence(
    initialState: DeviceBusPersistedState = DeviceBusPersistedState(),
) : DeviceBusPersistence {
    var state: DeviceBusPersistedState = initialState

    override suspend fun loadState(): DeviceBusPersistedState = state

    override suspend fun saveDeviceId(deviceId: String) {
        state = state.copy(deviceId = deviceId)
    }

    override suspend fun saveCredentials(userId: String, authToken: String, localePreference: String?) {
        state = state.copy(
            userId = userId,
            authToken = authToken,
            localePreference = localePreference?.trim()?.ifBlank { null }?.lowercase(),
        )
    }

    override suspend fun saveRelayTranscript(raw: String?) {
        state = state.copy(relayTranscriptRaw = raw)
    }

    override suspend fun clearCredentials() {
        state = state.copy(
            userId = null,
            authToken = null,
            localePreference = null,
            relayTranscriptRaw = null,
        )
    }
}

private class FakeTranscriptSnapshotFetcher(
    private val response: String?,
) : DeviceBusTranscriptSnapshotFetcher {
    var calls: Int = 0
    var lastConversationId: String? = null

    override suspend fun fetchSnapshot(userId: String, authToken: String, conversationId: String?): String? {
        calls += 1
        lastConversationId = conversationId
        return response
    }
}

class DeviceBusTranscriptHydrationTest {

    @Test
    fun refreshStoredState_hydratesPersistedRelayTranscript() = runBlocking {
        val persistedTranscript = encodeRelayTranscript(
            listOf(
                WatchRelayTranscriptEntry(
                    entryId = "persisted-1",
                    text = "Saved desktop update",
                    sourceLabel = "Desktop",
                    sourceSurface = "desktop",
                    threadId = "thread-27",
                    status = "seen",
                    fromWatch = false,
                    updatedAtMs = 42L,
                )
            )
        )
        val persistence = FakeDeviceBusPersistence(
            DeviceBusPersistedState(
                deviceId = "watch-1",
                relayTranscriptRaw = persistedTranscript,
            )
        )
        val client = DeviceBusClient(
            context = ContextWrapper(null),
            persistence = persistence,
            autoLoad = false,
        )

        client.refreshStoredState()

        assertEquals("watch-1", client.deviceIdState.value)
        assertEquals(1, client.relayTranscript.value.size)
        assertEquals("persisted-1", client.relayTranscript.value.first().entryId)
        assertEquals("thread-27", client.relayTranscript.value.first().threadId)
    }

    @Test
    fun clearCredentials_clearsHydratedRelayTranscriptButKeepsDeviceId() = runBlocking {
        val persistence = FakeDeviceBusPersistence(
            DeviceBusPersistedState(
                deviceId = "watch-2",
                relayTranscriptRaw = encodeRelayTranscript(
                    listOf(
                        WatchRelayTranscriptEntry(
                            entryId = "persisted-2",
                            text = "Saved watch message",
                            sourceLabel = "Watch",
                            sourceSurface = "watch",
                            threadId = "thread-28",
                            status = "sending",
                            fromWatch = true,
                            updatedAtMs = 84L,
                        )
                    )
                ),
            )
        )
        val client = DeviceBusClient(
            context = ContextWrapper(null),
            persistence = persistence,
            autoLoad = false,
        )

        client.refreshStoredState()
        client.clearCredentials()

        assertEquals("watch-2", client.deviceIdState.value)
        assertTrue(client.relayTranscript.value.isEmpty())
        assertNull(persistence.state.relayTranscriptRaw)
        assertEquals("watch-2", persistence.state.deviceId)
    }

    @Test
    fun refreshStoredState_hydratesSharedBackendTranscriptAndKeepsLocalSendingEntries() = runBlocking {
        val persistence = FakeDeviceBusPersistence(
            DeviceBusPersistedState(
                deviceId = "watch-3",
                userId = "user-3",
                authToken = "token-3",
                relayTranscriptRaw = encodeRelayTranscript(
                    listOf(
                        WatchRelayTranscriptEntry(
                            entryId = "local-pending-1",
                            text = "Pending watch reply",
                            sourceLabel = "Watch",
                            sourceSurface = "watch",
                            threadId = "thread-29",
                            status = "sending",
                            fromWatch = true,
                            updatedAtMs = 50L,
                        )
                    )
                ),
            )
        )
        val snapshotFetcher = FakeTranscriptSnapshotFetcher(
            """
            {"ok":true,"data":{"snapshot":{"conversationId":"thread-29","syncSource":"backend","syncVersion":1,"updatedAtMs":120,"messages":[{"messageId":"msg-29","clientMessageId":"msg-29","conversationId":"thread-29","text":"Desktop confirmed","sourceLabel":"Desktop","sourceSurface":"desktop","deliveryStatus":"delivered","createdAtMs":75,"deliveredAtMs":100,"seenAtMs":110}],"receipts":[{"clientMessageId":"msg-29","status":"seen","byDevice":"device-desktop"}]}}}
            """.trimIndent()
        )
        val client = DeviceBusClient(
            context = ContextWrapper(null),
            persistence = persistence,
            snapshotFetcherOverride = snapshotFetcher,
            autoLoad = false,
        )

        client.refreshStoredState()

        assertEquals(1, snapshotFetcher.calls)
        assertNull(snapshotFetcher.lastConversationId)
        assertEquals(2, client.relayTranscript.value.size)
        assertEquals("msg-29", client.relayTranscript.value[1].entryId)
        assertEquals("seen", client.relayTranscript.value[1].status)
        assertEquals("thread-29", client.relayTranscript.value[1].threadId)
        assertEquals("local-pending-1", client.relayTranscript.value[0].entryId)
        assertEquals("thread-29", client.selectedRelayConversationId.value)
        assertEquals(listOf("thread-29"), client.relayTranscriptSummaries.value.map { it.conversationId })
        assertTrue(persistence.state.relayTranscriptRaw?.contains("msg-29") == true)
    }
}