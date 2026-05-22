package com.rune.watch.presentation

import java.util.Locale

private fun localizedPairingText(
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

fun watchPairingTitle(locale: Locale = Locale.getDefault()): String = localizedPairingText(
    locale,
    english = "Pair with phone",
    spanish = "Emparejar con telefono",
    french = "Associer au telephone",
    german = "Mit Telefon koppeln",
    portuguese = "Parear com o telefone",
    chinese = "与手机配对",
    hindi = "फोन से पेयर करें",
    arabic = "الاقتران مع الهاتف",
    japanese = "スマホとペア設定",
    russian = "Сопрячь с телефоном",
)

fun watchPairingQrDescription(locale: Locale = Locale.getDefault()): String = localizedPairingText(
    locale,
    english = "Pairing QR",
    spanish = "QR de emparejamiento",
    french = "QR d'appairage",
    german = "Kopplungs-QR",
    portuguese = "QR de pareamento",
    chinese = "配对二维码",
    hindi = "पेयरिंग क्यूआर",
    arabic = "رمز QR للاقتران",
    japanese = "ペアリングQR",
    russian = "QR-код сопряжения",
)

fun watchPairingGeneratingCode(locale: Locale = Locale.getDefault()): String = localizedPairingText(
    locale,
    english = "Generating code...",
    spanish = "Generando codigo...",
    french = "Generation du code...",
    german = "Code wird erstellt...",
    portuguese = "Gerando codigo...",
    chinese = "正在生成代码...",
    hindi = "कोड बनाया जा रहा है...",
    arabic = "جارٍ إنشاء الرمز...",
    japanese = "コードを生成中...",
    russian = "Код создается...",
)

fun watchPairingScanHint(locale: Locale = Locale.getDefault()): String = localizedPairingText(
    locale,
    english = "Scan in RUNE Client to link this watch.",
    spanish = "Escanealo en RUNE Client para vincular este reloj.",
    french = "Scannez dans RUNE Client pour lier cette montre.",
    german = "In RUNE Client scannen, um diese Uhr zu verbinden.",
    portuguese = "Escaneie no RUNE Client para vincular este relogio.",
    chinese = "在 RUNE Client 中扫描以连接此手表。",
    hindi = "इस घड़ी को जोड़ने के लिए RUNE Client में स्कैन करें।",
    arabic = "امسح في RUNE Client لربط هذه الساعة.",
    japanese = "この時計を連携するには RUNE Client でスキャンしてください。",
    russian = "Отсканируйте в RUNE Client, чтобы связать эти часы.",
)

fun watchPairingPrepareError(locale: Locale = Locale.getDefault()): String = localizedPairingText(
    locale,
    english = "Could not prepare pairing right now.",
    spanish = "No se pudo preparar el emparejamiento ahora.",
    french = "Impossible de preparer l'appairage maintenant.",
    german = "Kopplung konnte gerade nicht vorbereitet werden.",
    portuguese = "Nao foi possivel preparar o pareamento agora.",
    chinese = "当前无法准备配对。",
    hindi = "अभी पेयरिंग तैयार नहीं की जा सकी।",
    arabic = "تعذر تجهيز الاقتران الآن.",
    japanese = "今はペアリングを準備できません。",
    russian = "Сейчас не удалось подготовить сопряжение.",
)

fun watchPairingConnectedStatus(locale: Locale = Locale.getDefault()): String = localizedPairingText(
    locale,
    english = "Watch paired. Opening Ember...",
    spanish = "Reloj emparejado. Abriendo Ember...",
    french = "Montre associee. Ouverture d'Ember...",
    german = "Uhr gekoppelt. Ember wird geoeffnet...",
    portuguese = "Relogio pareado. Abrindo Ember...",
    chinese = "手表已配对。正在打开 Ember...",
    hindi = "घड़ी पेयर हो गई। Ember खोला जा रहा है...",
    arabic = "تم اقتران الساعة. جارٍ فتح Ember...",
    japanese = "時計のペアリングが完了しました。Emberを開いています...",
    russian = "Часы сопряжены. Открываем Ember...",
)

fun watchPairingConnectingStatus(locale: Locale = Locale.getDefault()): String = localizedPairingText(
    locale,
    english = "Watch paired. Connecting...",
    spanish = "Reloj emparejado. Conectando...",
    french = "Montre associee. Connexion...",
    german = "Uhr gekoppelt. Verbindung wird hergestellt...",
    portuguese = "Relogio pareado. Conectando...",
    chinese = "手表已配对。正在连接...",
    hindi = "घड़ी पेयर हो गई। कनेक्ट हो रही है...",
    arabic = "تم اقتران الساعة. جارٍ الاتصال...",
    japanese = "時計のペアリングが完了しました。接続中...",
    russian = "Часы сопряжены. Подключение...",
)

fun watchPairingContinueText(locale: Locale = Locale.getDefault()): String = localizedPairingText(
    locale,
    english = "Continue",
    spanish = "Continuar",
    french = "Continuer",
    german = "Weiter",
    portuguese = "Continuar",
    chinese = "继续",
    hindi = "जारी रखें",
    arabic = "متابعة",
    japanese = "続ける",
    russian = "Продолжить",
)