package com.rune.watch.presentation

import com.rune.watch.bus.DeviceBusRuntime
import com.rune.watch.settings.WatchSettingsStore
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettingsLocalizationTest {

    @Test
    fun spanishStatusStringsUseLocalizedCopy() {
        assertEquals(
            "Diagnostico de presenca indisponivel",
            watchPresenceDiagnosticsUnavailable(Locale("pt", "BR")),
        )
        assertEquals(
            "Bloqueio de controle ativo...",
            watchControlLockActiveText(Locale("pt", "BR")),
        )
    }

    @Test
    fun stopDisabledReasonReflectsLocalizedState() {
        assertEquals(
            "Stoppen deaktiviert bis Kopplung abgeschlossen ist",
            watchStopDisabledReason(paired = false, serviceRunning = true, locale = Locale.GERMANY),
        )
        assertEquals(
            "Dienst ist bereits gestoppt",
            watchStopDisabledReason(paired = true, serviceRunning = false, locale = Locale.GERMANY),
        )
    }

    @Test
    fun lastActionFallsBackToLocalizedUnknownValues() {
        val text = watchLastActionText(
            lastActionAtMs = 1_716_372_930_000,
            lastAction = null,
            lastActionResult = null,
            locale = Locale("es", "MX"),
        )

        assertTrue(text.startsWith("Ultima accion: desconocido (desconocido) @ "))
    }

    @Test
    fun healthEventUsesLocalizedTimeFormatter() {
        val event = DeviceBusRuntime.HealthEvent(
            atMs = 1_716_372_930_000,
            category = "relay",
            message = "connected",
        )

        val formatted = formatWatchHealthEvent(event, Locale.JAPAN)
        assertTrue(formatted.contains("relay: connected"))
        assertTrue(formatted.isNotBlank())
    }

    @Test
    fun serviceAndPresenceLabelsAreLocalized() {
        assertEquals(
            "Servico: conectado",
            watchServiceConnectionText(connected = true, locale = Locale("pt", "BR")),
        )
        assertEquals(
            "Atualizar presenca",
            watchRefreshPresenceText(presenceLoading = false, locale = Locale("pt", "BR")),
        )
        assertEquals(
            "Snapshot de saude",
            watchHealthSnapshotTitle(Locale("pt", "BR")),
        )
    }

    @Test
    fun actionAndIdentityLabelsAreLocalized() {
        assertEquals(
            "ID de usuario: sin emparejar",
            watchUserIdText("", Locale("es", "MX")),
        )
        assertEquals(
            "Confirmar cierre de sesion",
            watchConfirmLogOutText(Locale("es", "MX")),
        )
        assertEquals(
            "Atras",
            watchBackText(Locale("es", "MX")),
        )
    }

    @Test
    fun themeAndFontOptionRowsAreLocalized() {
        assertEquals(
            "● Fosforo",
            watchThemeOptionText(selected = true, themeMode = WatchSettingsStore.THEME_PHOSPHOR, locale = Locale("es", "MX")),
        )
        assertEquals(
            "◇ 灰烬",
            watchThemeOptionText(selected = false, themeMode = WatchSettingsStore.THEME_ASH, locale = Locale.CHINA),
        )
        assertEquals(
            "● 標準",
            watchFontOptionText(selected = true, fontMode = WatchSettingsStore.FONT_DEFAULT, locale = Locale.JAPAN),
        )
        assertEquals(
            "◇ Моно",
            watchFontOptionText(selected = false, fontMode = WatchSettingsStore.FONT_MONO, locale = Locale("ru", "RU")),
        )
    }

    @Test
    fun storedLocalePreferenceOverridesDefaultLocaleWhenValid() {
        val resolved = WatchSettingsStore.resolveLocale("fr", Locale.US)
        assertEquals("fr", resolved.language)
    }

    @Test
    fun invalidStoredLocaleFallsBackToDeviceLocale() {
        val resolved = WatchSettingsStore.resolveLocale("", Locale.JAPAN)
        assertEquals(Locale.JAPAN.language, resolved.language)
    }
}