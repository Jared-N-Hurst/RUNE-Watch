package com.rune.watch.presentation

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class EmberSurfaceLocalizationTest {

    @Test
    fun homeLabelsAndStatusLocalizeByLanguage() {
        assertEquals("Conf", watchHomeSettingsLabel(Locale("es", "MX")))
        assertEquals("○ オフライン", watchHomeConnectionStatus(false, Locale.JAPAN))
    }

    @Test
    fun chatAndVoiceCopyLocalizeByLanguage() {
        assertEquals("Fale com Ember", watchVoicePrompt(Locale("pt", "BR")))
        assertEquals("Отпр", watchChatSendButton(Locale("ru", "RU")))
        assertEquals("メッセージを聞き取っています...", watchChatListeningText(Locale.JAPAN))
    }
}