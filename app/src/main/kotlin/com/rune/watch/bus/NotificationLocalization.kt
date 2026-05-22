package com.rune.watch.bus

import com.rune.watch.settings.WatchSettingsStore
import java.util.Locale

private fun notificationLocale(localePreference: String?): Locale =
    WatchSettingsStore.resolveLocale(localePreference)

private fun localizedNotificationText(
    localePreference: String?,
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
): String = when (notificationLocale(localePreference).language.lowercase(Locale.ROOT)) {
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

internal fun watchNotificationOkText(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "OK",
    es = "OK",
    fr = "OK",
    de = "OK",
    pt = "OK",
    zh = "确定",
    hi = "ठीक",
    ar = "حسنًا",
    ja = "OK",
    ru = "ОК",
)

internal fun watchNotificationDismissText(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "Dismiss",
    es = "Cerrar",
    fr = "Fermer",
    de = "Schliessen",
    pt = "Fechar",
    zh = "关闭",
    hi = "बंद करें",
    ar = "إغلاق",
    ja = "閉じる",
    ru = "Закрыть",
)

internal fun watchNotificationAcknowledgeText(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "Acknowledge",
    es = "Confirmar",
    fr = "Confirmer",
    de = "Bestaetigen",
    pt = "Confirmar",
    zh = "确认",
    hi = "पुष्टि",
    ar = "تأكيد",
    ja = "確認",
    ru = "Подтв",
)

internal fun watchLinkedSurfaceMessageText(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "New message from linked surface.",
    es = "Nuevo mensaje desde una superficie vinculada.",
    fr = "Nouveau message provenant d'une surface liee.",
    de = "Neue Nachricht von einer verbundenen Oberflaeche.",
    pt = "Nova mensagem de uma superficie vinculada.",
    zh = "来自已连接界面的新消息。",
    hi = "जुड़ी हुई सतह से नया संदेश।",
    ar = "رسالة جديدة من سطح مرتبط.",
    ja = "接続された画面から新しいメッセージです。",
    ru = "Новое сообщение с подключенной поверхности.",
)

internal fun watchPhonePingTitle(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "Phone Ping",
    es = "Ping del telefono",
    fr = "Ping du telephone",
    de = "Telefon-Ping",
    pt = "Ping do telefone",
    zh = "手机 Ping",
    hi = "फ़ोन पिंग",
    ar = "تنبيه الهاتف",
    ja = "スマホ Ping",
    ru = "Пинг телефона",
)

internal fun watchPhonePingBody(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "Haptic check from linked phone.",
    es = "Comprobacion haptica desde el telefono vinculado.",
    fr = "Verification haptique depuis le telephone lie.",
    de = "Haptikpruefung vom verbundenen Telefon.",
    pt = "Verificacao haptica do telefone vinculado.",
    zh = "来自已连接手机的触觉检查。",
    hi = "जुड़े हुए फ़ोन से हैप्टिक जांच।",
    ar = "فحص اهتزازي من الهاتف المرتبط.",
    ja = "接続されたスマホからの触覚チェックです。",
    ru = "Тактильная проверка с подключенного телефона.",
)

internal fun watchProcessCompleteTitle(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "Process Complete",
    es = "Proceso completado",
    fr = "Processus termine",
    de = "Prozess abgeschlossen",
    pt = "Processo concluido",
    zh = "流程已完成",
    hi = "प्रक्रिया पूर्ण",
    ar = "اكتملت العملية",
    ja = "処理完了",
    ru = "Процесс завершен",
)

internal fun watchEmberAlertTitle(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "Ember Alert",
    es = "Alerta de Ember",
    fr = "Alerte Ember",
    de = "Ember-Warnung",
    pt = "Alerta do Ember",
    zh = "Ember 提醒",
    hi = "Ember चेतावनी",
    ar = "تنبيه Ember",
    ja = "Emberアラート",
    ru = "Оповещение Ember",
)

internal fun watchLinkedSurfaceUpdateBody(localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "A linked surface sent an update.",
    es = "Una superficie vinculada envio una actualizacion.",
    fr = "Une surface liee a envoye une mise a jour.",
    de = "Eine verbundene Oberflaeche hat ein Update gesendet.",
    pt = "Uma superficie vinculada enviou uma atualizacao.",
    zh = "已连接界面发送了一条更新。",
    hi = "जुड़ी हुई सतह ने एक अपडेट भेजा।",
    ar = "أرسل سطح مرتبط تحديثًا.",
    ja = "接続された画面から更新が届きました。",
    ru = "Подключенная поверхность отправила обновление.",
)

internal fun watchIncidentSeverityLabel(severity: String, localePreference: String?): String = when (severity.lowercase()) {
    "critical" -> localizedNotificationText(localePreference, en = "Critical", es = "Critica", fr = "Critique", de = "Kritisch", pt = "Critico", zh = "严重", hi = "गंभीर", ar = "حرج", ja = "重大", ru = "Критично")
    "warning" -> localizedNotificationText(localePreference, en = "Warning", es = "Advertencia", fr = "Alerte", de = "Warnung", pt = "Aviso", zh = "警告", hi = "चेतावनी", ar = "تحذير", ja = "警告", ru = "Предупреждение")
    else -> localizedNotificationText(localePreference, en = "Info", es = "Info", fr = "Info", de = "Info", pt = "Info", zh = "信息", hi = "जानकारी", ar = "معلومة", ja = "情報", ru = "Инфо")
}

internal fun watchIncidentTitle(severity: String, localePreference: String?): String = localizedNotificationText(
    localePreference,
    en = "Incident ${watchIncidentSeverityLabel(severity, localePreference)}",
    es = "Incidente ${watchIncidentSeverityLabel(severity, localePreference)}",
    fr = "Incident ${watchIncidentSeverityLabel(severity, localePreference)}",
    de = "Vorfall ${watchIncidentSeverityLabel(severity, localePreference)}",
    pt = "Incidente ${watchIncidentSeverityLabel(severity, localePreference)}",
    zh = "事件 ${watchIncidentSeverityLabel(severity, localePreference)}",
    hi = "घटना ${watchIncidentSeverityLabel(severity, localePreference)}",
    ar = "حادثة ${watchIncidentSeverityLabel(severity, localePreference)}",
    ja = "インシデント ${watchIncidentSeverityLabel(severity, localePreference)}",
    ru = "Инцидент ${watchIncidentSeverityLabel(severity, localePreference)}",
)