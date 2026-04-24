// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.rune.watch.bus.DeviceBusClient
import com.rune.watch.bus.PairingSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun PairingScreen(busClient: DeviceBusClient) {
    var session by remember { mutableStateOf<PairingSession?>(null) }
    var sessionError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        session = runCatching { busClient.ensurePairingSession() }
            .onFailure { sessionError = "Could not prepare pairing right now." }
            .getOrNull()
    }

    LaunchedEffect(session?.pairingCode, session?.timestamp) {
        if (session == null) return@LaunchedEffect
        while (isActive) {
            val paired = busClient.pollPairingStatus()
            if (paired) {
                break
            }
            delay(3_000L)
        }
    }

    val qrBitmap = remember(session?.qrPayload) {
        session?.let { generateQrBitmap(it.qrPayload) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            KatiahGlyph(
                emotionalColor = Color(0xFFFF7043),
                pulseStrength = 0.8f,
                pulseDuration = 2300,
                size = 58.dp,
            )

            Text(
                text = "Pair with phone",
                fontSize = 13.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )

            if (qrBitmap != null) {
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Pairing QR",
                    modifier = Modifier.size(120.dp),
                )
            }

            Text(
                text = session?.pairingCode ?: "Generating code...",
                fontSize = 16.sp,
                color = Color(0xFFFFCCBC),
            )

            Text(
                text = "Scan in RUNE Client to link this watch.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )

            if (sessionError != null) {
                Text(
                    text = sessionError!!,
                    fontSize = 10.sp,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun generateQrBitmap(payload: String, sizePx: Int = 240): Bitmap? {
    return runCatching {
        val bits = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    }.getOrNull()
}
