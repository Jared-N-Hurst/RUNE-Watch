package com.rune.watch.presentation

import java.util.Locale

private fun localizedWatchSurfaceText(
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

fun watchHomeChatLabel(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Chat",
    spanish = "Chat",
    french = "Chat",
    german = "Chat",
    portuguese = "Chat",
    chinese = "聊天",
    hindi = "चैट",
    arabic = "دردشة",
    japanese = "チャット",
    russian = "Чат",
)

fun watchHomeSettingsLabel(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Set",
    spanish = "Conf",
    french = "Regl",
    german = "Einst",
    portuguese = "Conf",
    chinese = "设置",
    hindi = "सेट",
    arabic = "ضبط",
    japanese = "設定",
    russian = "Наст",
)

fun watchHomeBiometricsLabel(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Bio",
    spanish = "Bio",
    french = "Bio",
    german = "Bio",
    portuguese = "Bio",
    chinese = "生体",
    hindi = "बायो",
    arabic = "حيوي",
    japanese = "生体",
    russian = "Био",
)

fun watchHomeMicLabel(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Mic",
    spanish = "Mic",
    french = "Mic",
    german = "Mik",
    portuguese = "Mic",
    chinese = "麦克",
    hindi = "माइक",
    arabic = "ميك",
    japanese = "マイク",
    russian = "Мик",
)

fun watchHomePairLabel(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Pair",
    spanish = "Vinc",
    french = "Lier",
    german = "Paar",
    portuguese = "Par",
    chinese = "配对",
    hindi = "पेयर",
    arabic = "ربط",
    japanese = "ペア",
    russian = "Пара",
)

fun watchHomeConnectionStatus(connected: Boolean, locale: Locale = Locale.getDefault()): String = if (connected) {
    localizedWatchSurfaceText(
        locale,
        english = "● Connected",
        spanish = "● Conectado",
        french = "● Connectee",
        german = "● Verbunden",
        portuguese = "● Conectado",
        chinese = "● 已连接",
        hindi = "● कनेक्टेड",
        arabic = "● متصل",
        japanese = "● 接続済み",
        russian = "● Подключено",
    )
} else {
    localizedWatchSurfaceText(
        locale,
        english = "○ Offline",
        spanish = "○ Sin conexion",
        french = "○ Hors ligne",
        german = "○ Offline",
        portuguese = "○ Offline",
        chinese = "○ 离线",
        hindi = "○ ऑफलाइन",
        arabic = "○ غير متصل",
        japanese = "○ オフライン",
        russian = "○ Не в сети",
    )
}

fun watchVoicePrompt(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Speak to Ember",
    spanish = "Habla con Ember",
    french = "Parlez a Ember",
    german = "Sprich mit Ember",
    portuguese = "Fale com Ember",
    chinese = "对 Ember 说话",
    hindi = "Ember से बोलें",
    arabic = "تحدث إلى Ember",
    japanese = "Emberに話しかける",
    russian = "Скажите Ember",
)

fun watchVoiceUnavailable(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Voice input is unavailable on this watch right now.",
    spanish = "La entrada de voz no esta disponible en este reloj ahora.",
    french = "La saisie vocale est indisponible sur cette montre pour le moment.",
    german = "Spracheingabe ist auf dieser Uhr gerade nicht verfuegbar.",
    portuguese = "A entrada por voz nao esta disponivel neste relogio agora.",
    chinese = "此手表当前无法使用语音输入。",
    hindi = "इस घड़ी पर अभी वॉइस इनपुट उपलब्ध नहीं है।",
    arabic = "إدخال الصوت غير متاح على هذه الساعة الآن.",
    japanese = "この時計では現在音声入力を利用できません。",
    russian = "Голосовой ввод сейчас недоступен на этих часах.",
)

fun watchChatTitle(locale: Locale = Locale.getDefault()): String = watchHomeChatLabel(locale)

fun watchChatSubtitle(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Voice relay to Ember",
    spanish = "Relay de voz a Ember",
    french = "Relais vocal vers Ember",
    german = "Sprachweitergabe an Ember",
    portuguese = "Relay de voz para Ember",
    chinese = "向 Ember 发送语音",
    hindi = "Ember को वॉइस रिले",
    arabic = "ترحيل صوتي إلى Ember",
    japanese = "Emberへの音声リレー",
    russian = "Голосовая передача Ember",
)

fun watchChatListeningText(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Listening for your message...",
    spanish = "Escuchando tu mensaje...",
    french = "Ecoute de votre message...",
    german = "Nachricht wird abgehoert...",
    portuguese = "Ouvindo sua mensagem...",
    chinese = "正在聆听你的消息...",
    hindi = "आपका संदेश सुना जा रहा है...",
    arabic = "جارٍ الاستماع إلى رسالتك...",
    japanese = "メッセージを聞き取っています...",
    russian = "Слушаю ваше сообщение...",
)

fun watchChatInstructionText(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Tap Mic, speak to Ember, then send the transcript.",
    spanish = "Toca Mic, habla con Ember y luego envia la transcripcion.",
    french = "Touchez Mic, parlez a Ember puis envoyez la transcription.",
    german = "Tippe auf Mik, sprich mit Ember und sende dann das Transkript.",
    portuguese = "Toque em Mic, fale com Ember e depois envie a transcricao.",
    chinese = "点击麦克，对 Ember 说话，然后发送转写。",
    hindi = "माइक दबाएं, Ember से बोलें, फिर ट्रांसक्रिप्ट भेजें।",
    arabic = "اضغط على الميك، تحدث إلى Ember، ثم أرسل النص.",
    japanese = "マイクを押してEmberに話しかけ、文字起こしを送信してください。",
    russian = "Нажмите микрофон, скажите сообщение Ember и отправьте расшифровку.",
)

fun watchChatMicButton(listening: Boolean, locale: Locale = Locale.getDefault()): String = if (listening) {
    localizedWatchSurfaceText(
        locale,
        english = "Mic...",
        spanish = "Mic...",
        french = "Mic...",
        german = "Mik...",
        portuguese = "Mic...",
        chinese = "麦克...",
        hindi = "माइक...",
        arabic = "ميك...",
        japanese = "マイク...",
        russian = "Мик...",
    )
} else {
    watchHomeMicLabel(locale)
}

fun watchChatSendButton(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Send",
    spanish = "Enviar",
    french = "Envoyer",
    german = "Senden",
    portuguese = "Enviar",
    chinese = "发送",
    hindi = "भेजें",
    arabic = "إرسال",
    japanese = "送信",
    russian = "Отпр",
)

fun watchChatClearButton(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
    locale,
    english = "Clear",
    spanish = "Borrar",
    french = "Effacer",
    german = "Loesch",
    portuguese = "Limpar",
    chinese = "清除",
    hindi = "साफ",
    arabic = "مسح",
    japanese = "クリア",
    russian = "Очист",
)

fun watchChatBackButton(locale: Locale = Locale.getDefault()): String = localizedWatchSurfaceText(
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