package com.rune.watch.presentation

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class BiometricsLocalizationTest {

    @Test
    fun titleAndStatusLocalizeByLanguage() {
        assertEquals("Biometria", watchBiometricsTitle(Locale("es", "MX")))
        assertEquals("ストリーミング無効", watchBiometricsStreamingStatus(false, Locale.JAPAN))
    }

    @Test
    fun toggleCopyLocalizesByLanguage() {
        assertEquals("Frequencia cardiaca: ON", watchBiometricsHeartRateToggle(true, Locale("pt", "BR")))
        assertEquals("الحركة: إيقاف", watchBiometricsMovementToggle(false, Locale("ar", "EG")))
        assertEquals("Назад", watchBiometricsBack(Locale("ru", "RU")))
    }
}