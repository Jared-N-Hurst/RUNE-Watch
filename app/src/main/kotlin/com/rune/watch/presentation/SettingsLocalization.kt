package com.rune.watch.presentation

import com.rune.watch.bus.DeviceBusRuntime.HealthEvent
import com.rune.watch.settings.WatchSettingsStore
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private data class WatchSettingsCopy(
    val presenceDiagnosticsUnavailable: String,
    val lastReconnect: String,
    val lastAction: String,
    val none: String,
    val unknown: String,
    val stopDisabledUntilPairingComplete: String,
    val serviceAlreadyStopped: String,
    val controlLockActive: String,
)

private val WATCH_SETTINGS_COPY = mapOf(
    "en" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "Presence diagnostics unavailable",
        lastReconnect = "Last reconnect",
        lastAction = "Last action",
        none = "none",
        unknown = "unknown",
        stopDisabledUntilPairingComplete = "Stop disabled until pairing is complete",
        serviceAlreadyStopped = "Service already stopped",
        controlLockActive = "Control lock active...",
    ),
    "es" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "Diagnostico de presencia no disponible",
        lastReconnect = "Ultima reconexion",
        lastAction = "Ultima accion",
        none = "ninguna",
        unknown = "desconocido",
        stopDisabledUntilPairingComplete = "Detener deshabilitado hasta completar el emparejamiento",
        serviceAlreadyStopped = "El servicio ya esta detenido",
        controlLockActive = "Bloqueo de control activo...",
    ),
    "fr" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "Diagnostic de presence indisponible",
        lastReconnect = "Derniere reconnexion",
        lastAction = "Derniere action",
        none = "aucune",
        unknown = "inconnu",
        stopDisabledUntilPairingComplete = "Arret desactive tant que l'appairage n'est pas termine",
        serviceAlreadyStopped = "Le service est deja arrete",
        controlLockActive = "Verrou de controle actif...",
    ),
    "de" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "Praesenzdiagnose nicht verfuegbar",
        lastReconnect = "Letzte Wiederverbindung",
        lastAction = "Letzte Aktion",
        none = "keine",
        unknown = "unbekannt",
        stopDisabledUntilPairingComplete = "Stoppen deaktiviert bis Kopplung abgeschlossen ist",
        serviceAlreadyStopped = "Dienst ist bereits gestoppt",
        controlLockActive = "Steuersperre aktiv...",
    ),
    "pt" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "Diagnostico de presenca indisponivel",
        lastReconnect = "Ultima reconexao",
        lastAction = "Ultima acao",
        none = "nenhuma",
        unknown = "desconhecido",
        stopDisabledUntilPairingComplete = "Parada desativada ate concluir o pareamento",
        serviceAlreadyStopped = "O servico ja esta parado",
        controlLockActive = "Bloqueio de controle ativo...",
    ),
    "zh" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "存在诊断不可用",
        lastReconnect = "上次重连",
        lastAction = "上次操作",
        none = "无",
        unknown = "未知",
        stopDisabledUntilPairingComplete = "完成配对前无法停止",
        serviceAlreadyStopped = "服务已停止",
        controlLockActive = "控制锁已激活...",
    ),
    "hi" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "उपस्थिति निदान उपलब्ध नहीं है",
        lastReconnect = "पिछला पुन:कनेक्ट",
        lastAction = "पिछली कार्रवाई",
        none = "कोई नहीं",
        unknown = "अज्ञात",
        stopDisabledUntilPairingComplete = "पेयरिंग पूरी होने तक रोकना अक्षम है",
        serviceAlreadyStopped = "सेवा पहले से रुकी है",
        controlLockActive = "नियंत्रण लॉक सक्रिय है...",
    ),
    "ar" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "تشخيص الحضور غير متاح",
        lastReconnect = "آخر إعادة اتصال",
        lastAction = "آخر إجراء",
        none = "لا شيء",
        unknown = "غير معروف",
        stopDisabledUntilPairingComplete = "الإيقاف معطل حتى يكتمل الاقتران",
        serviceAlreadyStopped = "الخدمة متوقفة بالفعل",
        controlLockActive = "قفل التحكم نشط...",
    ),
    "ja" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "プレゼンス診断を利用できません",
        lastReconnect = "前回の再接続",
        lastAction = "前回の操作",
        none = "なし",
        unknown = "不明",
        stopDisabledUntilPairingComplete = "ペアリング完了まで停止できません",
        serviceAlreadyStopped = "サービスは既に停止しています",
        controlLockActive = "コントロールロックが有効です...",
    ),
    "ru" to WatchSettingsCopy(
        presenceDiagnosticsUnavailable = "Диагностика присутствия недоступна",
        lastReconnect = "Последнее переподключение",
        lastAction = "Последнее действие",
        none = "нет",
        unknown = "неизвестно",
        stopDisabledUntilPairingComplete = "Остановка недоступна до завершения сопряжения",
        serviceAlreadyStopped = "Сервис уже остановлен",
        controlLockActive = "Блокировка управления активна...",
    ),
)

private fun watchSettingsCopy(locale: Locale): WatchSettingsCopy {
    val language = locale.language.lowercase(Locale.ROOT)
    return WATCH_SETTINGS_COPY[language] ?: WATCH_SETTINGS_COPY.getValue("en")
}

private fun localizedTime(atMs: Long, locale: Locale): String =
    DateFormat.getTimeInstance(DateFormat.MEDIUM, locale).format(Date(atMs))

private fun localizedText(
    locale: Locale,
    en: String,
    es: String = en,
    fr: String = en,
    de: String = en,
    pt: String = en,
    zh: String = en,
    hi: String = en,
    ar: String = en,
    ja: String = en,
    ru: String = en,
): String = when (locale.language.lowercase(Locale.ROOT)) {
    "es" -> es
    "fr" -> fr
    "de" -> de
    "pt" -> pt
    "zh" -> zh
    "hi" -> hi
    "ar" -> ar
    "ja" -> ja
    "ru" -> ru
    else -> en
}

fun watchPresenceDiagnosticsUnavailable(locale: Locale = Locale.getDefault()): String =
    watchSettingsCopy(locale).presenceDiagnosticsUnavailable

fun watchLastReconnectText(lastReconnectAttemptMs: Long?, locale: Locale = Locale.getDefault()): String {
    val copy = watchSettingsCopy(locale)
    return if (lastReconnectAttemptMs == null) {
        "${copy.lastReconnect}: ${copy.none}"
    } else {
        "${copy.lastReconnect}: ${localizedTime(lastReconnectAttemptMs, locale)}"
    }
}

fun watchLastActionText(
    lastActionAtMs: Long?,
    lastAction: String?,
    lastActionResult: String?,
    locale: Locale = Locale.getDefault(),
): String {
    val copy = watchSettingsCopy(locale)
    return if (lastActionAtMs == null) {
        "${copy.lastAction}: ${copy.none}"
    } else {
        val action = lastAction ?: copy.unknown
        val result = lastActionResult ?: copy.unknown
        "${copy.lastAction}: ${action} (${result}) @ ${localizedTime(lastActionAtMs, locale)}"
    }
}

fun watchStopDisabledReason(
    paired: Boolean,
    serviceRunning: Boolean,
    locale: Locale = Locale.getDefault(),
): String? {
    val copy = watchSettingsCopy(locale)
    return when {
        !paired -> copy.stopDisabledUntilPairingComplete
        !serviceRunning -> copy.serviceAlreadyStopped
        else -> null
    }
}

fun watchControlLockActiveText(locale: Locale = Locale.getDefault()): String =
    watchSettingsCopy(locale).controlLockActive

fun watchConnectionSettingsTitle(locale: Locale = Locale.getDefault()): String =
    localizedText(
        locale,
        en = "Connection Settings",
        es = "Configuracion de conexion",
        fr = "Reglages de connexion",
        de = "Verbindungseinstellungen",
        pt = "Configuracoes de conexao",
        zh = "连接设置",
        hi = "कनेक्शन सेटिंग्स",
        ar = "إعدادات الاتصال",
        ja = "接続設定",
        ru = "Настройки подключения",
    )

fun watchSettingsUiVersionText(locale: Locale = Locale.getDefault()): String =
    localizedText(
        locale,
        en = "Settings UI v2",
        es = "IU de ajustes v2",
        fr = "Interface reglages v2",
        de = "Einstellungsoberflache v2",
        pt = "Interface de ajustes v2",
        zh = "设置界面 v2",
        hi = "सेटिंग्स UI v2",
        ar = "واجهة الإعدادات v2",
        ja = "設定UI v2",
        ru = "Интерфейс настроек v2",
    )

fun watchServiceConnectionText(connected: Boolean, locale: Locale = Locale.getDefault()): String =
    if (connected) {
        localizedText(
            locale,
            en = "Service: Connected",
            es = "Servicio: conectado",
            fr = "Service : connecte",
            de = "Dienst: verbunden",
            pt = "Servico: conectado",
            zh = "服务：已连接",
            hi = "सेवा: कनेक्टेड",
            ar = "الخدمة: متصلة",
            ja = "サービス: 接続済み",
            ru = "Сервис: подключен",
        )
    } else {
        localizedText(
            locale,
            en = "Service: Offline/Reconnecting",
            es = "Servicio: sin conexion/reconectando",
            fr = "Service : hors ligne/reconnexion",
            de = "Dienst: offline/wird neu verbunden",
            pt = "Servico: offline/reconectando",
            zh = "服务：离线/重连中",
            hi = "सेवा: ऑफलाइन/पुनःकनेक्ट हो रही है",
            ar = "الخدمة: غير متصلة/تعيد الاتصال",
            ja = "サービス: オフライン/再接続中",
            ru = "Сервис: офлайн/переподключение",
        )
    }

fun watchRuntimeStateText(serviceRunning: Boolean, locale: Locale = Locale.getDefault()): String =
    if (serviceRunning) {
        localizedText(
            locale,
            en = "Runtime: Running",
            es = "Runtime: activo",
            fr = "Runtime : actif",
            de = "Runtime: aktiv",
            pt = "Runtime: ativo",
            zh = "运行时：运行中",
            hi = "रनटाइम: चालू",
            ar = "وقت التشغيل: يعمل",
            ja = "ランタイム: 稼働中",
            ru = "Рантайм: работает",
        )
    } else {
        localizedText(
            locale,
            en = "Runtime: Stopped",
            es = "Runtime: detenido",
            fr = "Runtime : arrete",
            de = "Runtime: gestoppt",
            pt = "Runtime: parado",
            zh = "运行时：已停止",
            hi = "रनटाइम: रुका हुआ",
            ar = "وقت التشغيل: متوقف",
            ja = "ランタイム: 停止中",
            ru = "Рантайм: остановлен",
        )
    }

fun watchStartStopReconnectText(
    startCount: Int,
    stopCount: Int,
    reconnectCount: Int,
    locale: Locale = Locale.getDefault(),
): String = localizedText(
    locale,
    en = "Start/Stop/Reconnect: $startCount/$stopCount/$reconnectCount",
    es = "Inicio/Detencion/Reconexion: $startCount/$stopCount/$reconnectCount",
    fr = "Demarrage/Arret/Reconnexion : $startCount/$stopCount/$reconnectCount",
    de = "Start/Stopp/Neu verbinden: $startCount/$stopCount/$reconnectCount",
    pt = "Iniciar/Parar/Reconectar: $startCount/$stopCount/$reconnectCount",
    zh = "启动/停止/重连：$startCount/$stopCount/$reconnectCount",
    hi = "शुरू/रोकें/पुनःकनेक्ट: $startCount/$stopCount/$reconnectCount",
    ar = "بدء/إيقاف/إعادة اتصال: $startCount/$stopCount/$reconnectCount",
    ja = "開始/停止/再接続: $startCount/$stopCount/$reconnectCount",
    ru = "Старт/Стоп/Переподключение: $startCount/$stopCount/$reconnectCount",
)

fun watchDeviceIdText(deviceId: String, locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Device ID: ${deviceId.take(12)}",
    es = "ID del dispositivo: ${deviceId.take(12)}",
    fr = "ID appareil : ${deviceId.take(12)}",
    de = "Gerate-ID: ${deviceId.take(12)}",
    pt = "ID do dispositivo: ${deviceId.take(12)}",
    zh = "设备 ID：${deviceId.take(12)}",
    hi = "डिवाइस आईडी: ${deviceId.take(12)}",
    ar = "معرف الجهاز: ${deviceId.take(12)}",
    ja = "デバイス ID: ${deviceId.take(12)}",
    ru = "ID устройства: ${deviceId.take(12)}",
)

fun watchUserIdText(userId: String, locale: Locale = Locale.getDefault()): String =
    if (userId.isBlank()) {
        localizedText(
            locale,
            en = "User ID: unpaired",
            es = "ID de usuario: sin emparejar",
            fr = "ID utilisateur : non appaire",
            de = "Benutzer-ID: nicht gekoppelt",
            pt = "ID do usuario: nao pareado",
            zh = "用户 ID：未配对",
            hi = "यूज़र आईडी: अनपेयर",
            ar = "معرف المستخدم: غير مقترن",
            ja = "ユーザー ID: 未ペアリング",
            ru = "ID пользователя: не сопряжен",
        )
    } else {
        localizedText(
            locale,
            en = "User ID: ${userId.take(12)}",
            es = "ID de usuario: ${userId.take(12)}",
            fr = "ID utilisateur : ${userId.take(12)}",
            de = "Benutzer-ID: ${userId.take(12)}",
            pt = "ID do usuario: ${userId.take(12)}",
            zh = "用户 ID：${userId.take(12)}",
            hi = "यूज़र आईडी: ${userId.take(12)}",
            ar = "معرف المستخدم: ${userId.take(12)}",
            ja = "ユーザー ID: ${userId.take(12)}",
            ru = "ID пользователя: ${userId.take(12)}",
        )
    }

fun watchPresenceDiagnosticsTitle(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Presence diagnostics",
    es = "Diagnostico de presencia",
    fr = "Diagnostic de presence",
    de = "Praesenzdiagnose",
    pt = "Diagnostico de presenca",
    zh = "存在诊断",
    hi = "उपस्थिति निदान",
    ar = "تشخيص الحضور",
    ja = "プレゼンス診断",
    ru = "Диагностика присутствия",
)

fun watchRefreshPresenceText(presenceLoading: Boolean, locale: Locale = Locale.getDefault()): String =
    if (presenceLoading) {
        localizedText(
            locale,
            en = "Refreshing Presence...",
            es = "Actualizando presencia...",
            fr = "Actualisation de la presence...",
            de = "Praesenz wird aktualisiert...",
            pt = "Atualizando presenca...",
            zh = "正在刷新存在状态...",
            hi = "उपस्थिति रीफ्रेश हो रही है...",
            ar = "جار تحديث الحضور...",
            ja = "プレゼンスを更新中...",
            ru = "Обновление присутствия...",
        )
    } else {
        localizedText(
            locale,
            en = "Refresh Presence",
            es = "Actualizar presencia",
            fr = "Actualiser la presence",
            de = "Praesenz aktualisieren",
            pt = "Atualizar presenca",
            zh = "刷新存在状态",
            hi = "उपस्थिति रीफ्रेश करें",
            ar = "تحديث الحضور",
            ja = "プレゼンスを更新",
            ru = "Обновить присутствие",
        )
    }

fun watchStaleText(staleCause: String, locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Stale: $staleCause",
    es = "Obsoleto: $staleCause",
    fr = "Perime : $staleCause",
    de = "Veraltet: $staleCause",
    pt = "Desatualizado: $staleCause",
    zh = "陈旧：$staleCause",
    hi = "पुराना: $staleCause",
    ar = "قديم: $staleCause",
    ja = "古い状態: $staleCause",
    ru = "Устарело: $staleCause",
)

fun watchRelayHealthText(relayHealthy: Boolean, locale: Locale = Locale.getDefault()): String =
    if (relayHealthy) {
        localizedText(
            locale,
            en = "Relay: healthy",
            es = "Relay: saludable",
            fr = "Relais : sain",
            de = "Relay: gesund",
            pt = "Relay: saudavel",
            zh = "中继：健康",
            hi = "रिले: स्वस्थ",
            ar = "الترحيل: سليم",
            ja = "リレー: 正常",
            ru = "Релей: исправен",
        )
    } else {
        localizedText(
            locale,
            en = "Relay: unhealthy",
            es = "Relay: no saludable",
            fr = "Relais : en panne",
            de = "Relay: ungesund",
            pt = "Relay: com falha",
            zh = "中继：异常",
            hi = "रिले: अस्वस्थ",
            ar = "الترحيل: غير سليم",
            ja = "リレー: 異常",
            ru = "Релей: неисправен",
        )
    }

fun watchHeartbeatText(
    heartbeatPath: String,
    lastSeenLabel: String,
    locale: Locale = Locale.getDefault(),
): String = localizedText(
    locale,
    en = "Heartbeat: $heartbeatPath ($lastSeenLabel)",
    es = "Heartbeat: $heartbeatPath ($lastSeenLabel)",
    fr = "Heartbeat : $heartbeatPath ($lastSeenLabel)",
    de = "Heartbeat: $heartbeatPath ($lastSeenLabel)",
    pt = "Heartbeat: $heartbeatPath ($lastSeenLabel)",
    zh = "心跳：$heartbeatPath ($lastSeenLabel)",
    hi = "हार्टबीट: $heartbeatPath ($lastSeenLabel)",
    ar = "نبضة: $heartbeatPath ($lastSeenLabel)",
    ja = "ハートビート: $heartbeatPath ($lastSeenLabel)",
    ru = "Пульс: $heartbeatPath ($lastSeenLabel)",
)

fun watchFontTitle(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Font:",
    es = "Fuente:",
    fr = "Police :",
    de = "Schrift:",
    pt = "Fonte:",
    zh = "字体：",
    hi = "फ़ॉन्ट:",
    ar = "الخط:",
    ja = "フォント:",
    ru = "Шрифт:",
)

fun watchThemeOptionText(
    selected: Boolean,
    themeMode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val prefix = if (selected) "●" else "◇"
    val label = when (themeMode) {
        WatchSettingsStore.THEME_RUNE_DARK -> localizedText(
            locale,
            en = "RUNE-Dark",
            es = "RUNE-Oscuro",
            fr = "RUNE-Sombre",
            de = "RUNE-Dunkel",
            pt = "RUNE-Escuro",
            zh = "RUNE-暗色",
            hi = "RUNE-डार्क",
            ar = "RUNE-داكن",
            ja = "RUNE-ダーク",
            ru = "RUNE-Темная",
        )
        WatchSettingsStore.THEME_PHOSPHOR -> localizedText(
            locale,
            en = "Phosphor",
            es = "Fosforo",
            fr = "Phosphore",
            de = "Phosphor",
            pt = "Fosforo",
            zh = "荧光",
            hi = "फॉस्फर",
            ar = "فوسفور",
            ja = "蛍光",
            ru = "Фосфор",
        )
        WatchSettingsStore.THEME_JADE -> localizedText(
            locale,
            en = "Jade",
            es = "Jade",
            fr = "Jade",
            de = "Jade",
            pt = "Jade",
            zh = "玉石",
            hi = "जेड",
            ar = "يشم",
            ja = "翡翠",
            ru = "Нефрит",
        )
        WatchSettingsStore.THEME_CRIMSON -> localizedText(
            locale,
            en = "Crimson",
            es = "Carmesi",
            fr = "Cramoisi",
            de = "Karmesin",
            pt = "Carmesim",
            zh = "深红",
            hi = "गहरा लाल",
            ar = "قرمزي",
            ja = "深紅",
            ru = "Багряный",
        )
        WatchSettingsStore.THEME_VOID -> localizedText(
            locale,
            en = "Void",
            es = "Vacio",
            fr = "Vide",
            de = "Leere",
            pt = "Vazio",
            zh = "虚空",
            hi = "शून्य",
            ar = "فراغ",
            ja = "虚無",
            ru = "Пустота",
        )
        WatchSettingsStore.THEME_ASH -> localizedText(
            locale,
            en = "Ash",
            es = "Ceniza",
            fr = "Cendre",
            de = "Asche",
            pt = "Cinza",
            zh = "灰烬",
            hi = "राख",
            ar = "رماد",
            ja = "灰",
            ru = "Пепел",
        )
        else -> themeMode
    }
    return "$prefix $label"
}

fun watchFontOptionText(
    selected: Boolean,
    fontMode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val prefix = if (selected) "●" else "◇"
    val label = when (fontMode) {
        WatchSettingsStore.FONT_DEFAULT -> localizedText(
            locale,
            en = "Default",
            es = "Predeterminada",
            fr = "Par defaut",
            de = "Standard",
            pt = "Padrao",
            zh = "默认",
            hi = "डिफ़ॉल्ट",
            ar = "افتراضي",
            ja = "標準",
            ru = "Стандарт",
        )
        WatchSettingsStore.FONT_MONO -> localizedText(
            locale,
            en = "Mono",
            es = "Mono",
            fr = "Mono",
            de = "Mono",
            pt = "Mono",
            zh = "等宽",
            hi = "मोनो",
            ar = "أحادي",
            ja = "等幅",
            ru = "Моно",
        )
        else -> fontMode
    }
    return "$prefix $label"
}

fun watchBiometricToggleText(enabled: Boolean, locale: Locale = Locale.getDefault()): String =
    if (enabled) {
        localizedText(
            locale,
            en = "Biometric: ON",
            es = "Biometria: ACTIVADA",
            fr = "Biometrie : ACTIVE",
            de = "Biometrie: EIN",
            pt = "Biometria: ATIVA",
            zh = "生物识别：开",
            hi = "बायोमेट्रिक: चालू",
            ar = "القياسات الحيوية: تشغيل",
            ja = "生体認証: オン",
            ru = "Биометрия: ВКЛ",
        )
    } else {
        localizedText(
            locale,
            en = "Biometric: OFF",
            es = "Biometria: DESACTIVADA",
            fr = "Biometrie : DESACTIVEE",
            de = "Biometrie: AUS",
            pt = "Biometria: DESATIVA",
            zh = "生物识别：关",
            hi = "बायोमेट्रिक: बंद",
            ar = "القياسات الحيوية: إيقاف",
            ja = "生体認証: オフ",
            ru = "Биометрия: ВЫКЛ",
        )
    }

fun watchReopenPairingQrText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Reopen Pairing QR",
    es = "Reabrir QR de emparejamiento",
    fr = "Rouvrir le QR d'appairage",
    de = "Kopplungs-QR erneut offnen",
    pt = "Reabrir QR de pareamento",
    zh = "重新打开配对二维码",
    hi = "पेयरिंग QR फिर खोलें",
    ar = "إعادة فتح رمز الاقتران",
    ja = "ペアリングQRを再表示",
    ru = "Снова открыть QR сопряжения",
)

fun watchHealthSnapshotTitle(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Health snapshot",
    es = "Instantanea de salud",
    fr = "Instantane sante",
    de = "Gesundheits-Snapshot",
    pt = "Snapshot de saude",
    zh = "健康快照",
    hi = "स्वास्थ्य स्नैपशॉट",
    ar = "لقطة الصحة",
    ja = "ヘルススナップショット",
    ru = "Снимок здоровья",
)

fun watchSnapshotCopiedText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Snapshot copied",
    es = "Instantanea copiada",
    fr = "Instantane copie",
    de = "Snapshot kopiert",
    pt = "Snapshot copiado",
    zh = "快照已复制",
    hi = "स्नैपशॉट कॉपी हो गया",
    ar = "تم نسخ اللقطة",
    ja = "スナップショットをコピーしました",
    ru = "Снимок скопирован",
)

fun watchCopySnapshotText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Copy Snapshot",
    es = "Copiar instantanea",
    fr = "Copier l'instantane",
    de = "Snapshot kopieren",
    pt = "Copiar snapshot",
    zh = "复制快照",
    hi = "स्नैपशॉट कॉपी करें",
    ar = "نسخ اللقطة",
    ja = "スナップショットをコピー",
    ru = "Копировать снимок",
)

fun watchClearSnapshotText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Clear Snapshot",
    es = "Borrar instantanea",
    fr = "Effacer l'instantane",
    de = "Snapshot loschen",
    pt = "Limpar snapshot",
    zh = "清除快照",
    hi = "स्नैपशॉट साफ करें",
    ar = "مسح اللقطة",
    ja = "スナップショットを消去",
    ru = "Очистить снимок",
)

fun watchConfirmReconnectQuestion(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Confirm reconnect?",
    es = "Confirmar reconexion?",
    fr = "Confirmer la reconnexion ?",
    de = "Neu verbinden bestatigen?",
    pt = "Confirmar reconexao?",
    zh = "确认重新连接？",
    hi = "पुनःकनेक्ट की पुष्टि करें?",
    ar = "تأكيد إعادة الاتصال؟",
    ja = "再接続を確認しますか？",
    ru = "Подтвердить переподключение?",
)

fun watchConfirmReconnectText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Confirm Reconnect",
    es = "Confirmar reconexion",
    fr = "Confirmer la reconnexion",
    de = "Neu verbinden bestatigen",
    pt = "Confirmar reconexao",
    zh = "确认重连",
    hi = "पुनःकनेक्ट की पुष्टि करें",
    ar = "تأكيد إعادة الاتصال",
    ja = "再接続を確認",
    ru = "Подтвердить переподключение",
)

fun watchCancelText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Cancel",
    es = "Cancelar",
    fr = "Annuler",
    de = "Abbrechen",
    pt = "Cancelar",
    zh = "取消",
    hi = "रद्द करें",
    ar = "إلغاء",
    ja = "キャンセル",
    ru = "Отмена",
)

fun watchReconnectButtonText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Reconnect",
    es = "Reconectar",
    fr = "Reconnecter",
    de = "Neu verbinden",
    pt = "Reconectar",
    zh = "重新连接",
    hi = "पुनःकनेक्ट",
    ar = "إعادة الاتصال",
    ja = "再接続",
    ru = "Переподключить",
)

fun watchStopServiceText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Stop Service",
    es = "Detener servicio",
    fr = "Arreter le service",
    de = "Dienst stoppen",
    pt = "Parar servico",
    zh = "停止服务",
    hi = "सेवा रोकें",
    ar = "إيقاف الخدمة",
    ja = "サービスを停止",
    ru = "Остановить сервис",
)

fun watchStartServiceText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Start Service",
    es = "Iniciar servicio",
    fr = "Demarrer le service",
    de = "Dienst starten",
    pt = "Iniciar servico",
    zh = "启动服务",
    hi = "सेवा शुरू करें",
    ar = "بدء الخدمة",
    ja = "サービスを開始",
    ru = "Запустить сервис",
)

fun watchConfirmLogOutQuestion(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Confirm log out?",
    es = "Confirmar cierre de sesion?",
    fr = "Confirmer la deconnexion ?",
    de = "Abmeldung bestatigen?",
    pt = "Confirmar logout?",
    zh = "确认退出登录？",
    hi = "लॉग आउट की पुष्टि करें?",
    ar = "تأكيد تسجيل الخروج؟",
    ja = "ログアウトを確認しますか？",
    ru = "Подтвердить выход?",
)

fun watchLoggedOutText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Logged out",
    es = "Sesion cerrada",
    fr = "Deconnecte",
    de = "Abgemeldet",
    pt = "Logout concluido",
    zh = "已退出登录",
    hi = "लॉग आउट हो गया",
    ar = "تم تسجيل الخروج",
    ja = "ログアウトしました",
    ru = "Вы вышли из системы",
)

fun watchConfirmLogOutText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Confirm Log Out",
    es = "Confirmar cierre de sesion",
    fr = "Confirmer la deconnexion",
    de = "Abmeldung bestatigen",
    pt = "Confirmar logout",
    zh = "确认退出登录",
    hi = "लॉग आउट की पुष्टि करें",
    ar = "تأكيد تسجيل الخروج",
    ja = "ログアウトを確認",
    ru = "Подтвердить выход",
)

fun watchLogOutText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Log Out",
    es = "Cerrar sesion",
    fr = "Se deconnecter",
    de = "Abmelden",
    pt = "Sair",
    zh = "退出登录",
    hi = "लॉग आउट",
    ar = "تسجيل الخروج",
    ja = "ログアウト",
    ru = "Выйти",
)

fun watchBackText(locale: Locale = Locale.getDefault()): String = localizedText(
    locale,
    en = "Back",
    es = "Atras",
    fr = "Retour",
    de = "Zuruck",
    pt = "Voltar",
    zh = "返回",
    hi = "वापस",
    ar = "رجوع",
    ja = "戻る",
    ru = "Назад",
)

fun formatWatchHealthEvent(event: HealthEvent, locale: Locale = Locale.getDefault()): String {
    val at = localizedTime(event.atMs, locale)
    return "$at ${event.category}: ${event.message}"
}