package com.rune.watch.presentation

import java.util.Locale

private fun localizedBiometricsText(
    locale: Locale,
    english: String,
    spanish: String,
    french: String,
    german: String,
    portuguese: String,
    chinese: String,
    hindi: String,
    arabic: String,
    japanese: String,
    russian: String,
): String = when (locale.language.lowercase(Locale.ROOT)) {
    "es" -> spanish
    "fr" -> french
    "de" -> german
    "pt" -> portuguese
    "zh" -> chinese
    "hi" -> hindi
    "ar" -> arabic
    "ja" -> japanese
    "ru" -> russian
    else -> english
}

fun watchBiometricsTitle(locale: Locale = Locale.getDefault()): String = localizedBiometricsText(
    locale,
    english = "Biometrics",
    spanish = "Biometria",
    french = "Biometrie",
    german = "Biometrie",
    portuguese = "Biometria",
    chinese = "生物指标",
    hindi = "बायोमेट्रिक्स",
    arabic = "القياسات الحيوية",
    japanese = "生体情報",
    russian = "Биометрия",
)

fun watchBiometricsStreamingStatus(enabled: Boolean, locale: Locale = Locale.getDefault()): String = if (enabled) {
    localizedBiometricsText(
        locale,
        english = "Streaming enabled",
        spanish = "Transmision activada",
        french = "Transmission activee",
        german = "Streaming aktiviert",
        portuguese = "Transmissao ativada",
        chinese = "流传输已启用",
        hindi = "स्ट्रीमिंग चालू है",
        arabic = "تم تفعيل البث",
        japanese = "ストリーミング有効",
        russian = "Передача включена",
    )
} else {
    localizedBiometricsText(
        locale,
        english = "Streaming disabled",
        spanish = "Transmision desactivada",
        french = "Transmission desactivee",
        german = "Streaming deaktiviert",
        portuguese = "Transmissao desativada",
        chinese = "流传输已关闭",
        hindi = "स्ट्रीमिंग बंद है",
        arabic = "تم تعطيل البث",
        japanese = "ストリーミング無効",
        russian = "Передача отключена",
    )
}

fun watchBiometricsMasterToggle(enabled: Boolean, locale: Locale = Locale.getDefault()): String = localizedBiometricsToggle(
    label = localizedBiometricsText(
        locale,
        english = "Master",
        spanish = "General",
        french = "Global",
        german = "Master",
        portuguese = "Geral",
        chinese = "总开关",
        hindi = "मुख्य",
        arabic = "الرئيسي",
        japanese = "全体",
        russian = "Общий",
    ),
    enabled = enabled,
    locale = locale,
)

fun watchBiometricsHeartRateToggle(enabled: Boolean, locale: Locale = Locale.getDefault()): String = localizedBiometricsToggle(
    label = localizedBiometricsText(
        locale,
        english = "Heart Rate",
        spanish = "Frecuencia cardiaca",
        french = "Frequence cardiaque",
        german = "Herzfrequenz",
        portuguese = "Frequencia cardiaca",
        chinese = "心率",
        hindi = "हृदय गति",
        arabic = "معدل القلب",
        japanese = "心拍数",
        russian = "Пульс",
    ),
    enabled = enabled,
    locale = locale,
)

fun watchBiometricsHrvToggle(enabled: Boolean, locale: Locale = Locale.getDefault()): String = localizedBiometricsToggle(
    label = "HRV",
    enabled = enabled,
    locale = locale,
)

fun watchBiometricsStressToggle(enabled: Boolean, locale: Locale = Locale.getDefault()): String = localizedBiometricsToggle(
    label = localizedBiometricsText(
        locale,
        english = "Stress",
        spanish = "Estres",
        french = "Stress",
        german = "Stress",
        portuguese = "Estresse",
        chinese = "压力",
        hindi = "तनाव",
        arabic = "التوتر",
        japanese = "ストレス",
        russian = "Стресс",
    ),
    enabled = enabled,
    locale = locale,
)

fun watchBiometricsSleepToggle(enabled: Boolean, locale: Locale = Locale.getDefault()): String = localizedBiometricsToggle(
    label = localizedBiometricsText(
        locale,
        english = "Sleep",
        spanish = "Sueno",
        french = "Sommeil",
        german = "Schlaf",
        portuguese = "Sono",
        chinese = "睡眠",
        hindi = "नींद",
        arabic = "النوم",
        japanese = "睡眠",
        russian = "Сон",
    ),
    enabled = enabled,
    locale = locale,
)

fun watchBiometricsMovementToggle(enabled: Boolean, locale: Locale = Locale.getDefault()): String = localizedBiometricsToggle(
    label = localizedBiometricsText(
        locale,
        english = "Movement",
        spanish = "Movimiento",
        french = "Mouvement",
        german = "Bewegung",
        portuguese = "Movimento",
        chinese = "运动",
        hindi = "गतिविधि",
        arabic = "الحركة",
        japanese = "動作",
        russian = "Движение",
    ),
    enabled = enabled,
    locale = locale,
)

fun watchBiometricsBack(locale: Locale = Locale.getDefault()): String = localizedBiometricsText(
    locale,
    english = "Back",
    spanish = "Atras",
    french = "Retour",
    german = "Zurueck",
    portuguese = "Voltar",
    chinese = "返回",
    hindi = "वापस",
    arabic = "رجوع",
    japanese = "戻る",
    russian = "Назад",
)

private fun localizedBiometricsToggle(label: String, enabled: Boolean, locale: Locale): String {
    val state = if (enabled) {
        localizedBiometricsText(
            locale,
            english = "ON",
            spanish = "ON",
            french = "ON",
            german = "AN",
            portuguese = "ON",
            chinese = "开",
            hindi = "चालू",
            arabic = "تشغيل",
            japanese = "オン",
            russian = "ВКЛ",
        )
    } else {
        localizedBiometricsText(
            locale,
            english = "OFF",
            spanish = "OFF",
            french = "OFF",
            german = "AUS",
            portuguese = "OFF",
            chinese = "关",
            hindi = "बंद",
            arabic = "إيقاف",
            japanese = "オフ",
            russian = "ВЫКЛ",
        )
    }
    return "$label: $state"
}