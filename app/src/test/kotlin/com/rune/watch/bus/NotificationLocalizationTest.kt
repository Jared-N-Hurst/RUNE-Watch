package com.rune.watch.bus

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationLocalizationTest {

    @Test
    fun notificationChromeAndFallbacksLocalizeFromLocalePreference() {
        assertEquals("关闭", watchNotificationDismissText("zh"))
        assertEquals("Confirmer", watchNotificationAcknowledgeText("fr"))
        assertEquals("Nuevo mensaje desde una superficie vinculada.", watchLinkedSurfaceMessageText("es"))
    }

    @Test
    fun syntheticTitlesAndBodiesLocalizeFromLocalePreference() {
        assertEquals("Процесс завершен", watchProcessCompleteTitle("ru"))
        assertEquals("手机 Ping", watchPhonePingTitle("zh-hant"))
        assertEquals("インシデント 警告", watchIncidentTitle("warning", "ja"))
    }
}