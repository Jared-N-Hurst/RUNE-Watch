package com.rune.watch.presentation

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class PairingLocalizationTest {

    @Test
    fun pairingScreenCopyUsesLanguageFallbacks() {
        assertEquals(
            "Emparejar con telefono",
            watchPairingTitle(Locale("es", "MX")),
        )
        assertEquals(
            "Escaneie no RUNE Client para vincular este relogio.",
            watchPairingScanHint(Locale("pt", "BR")),
        )
    }

    @Test
    fun pairingStatusAndActionsAreLocalized() {
        assertEquals(
            "時計のペアリングが完了しました。接続中...",
            watchPairingConnectingStatus(Locale.JAPAN),
        )
        assertEquals(
            "Продолжить",
            watchPairingContinueText(Locale("ru", "RU")),
        )
    }
}