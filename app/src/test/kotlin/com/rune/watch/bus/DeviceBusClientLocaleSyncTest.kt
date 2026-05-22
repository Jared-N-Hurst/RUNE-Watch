package com.rune.watch.bus

import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeviceBusClientLocaleSyncTest {

    @Test
    fun extractsNormalizedLocalePreferenceFromSyncPayload() {
        val payload = JSONObject().put("localePreference", " FR-CA ")

        assertEquals("fr-ca", extractLocalePreferenceSyncPayload(payload))
    }

    @Test
    fun ignoresMissingOrBlankLocalePreference() {
        assertNull(extractLocalePreferenceSyncPayload(JSONObject()))
        assertNull(extractLocalePreferenceSyncPayload(JSONObject().put("localePreference", "   ")))
        assertNull(extractLocalePreferenceSyncPayload("fr"))
    }
}