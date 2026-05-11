// Copyright (c) RUNE Systems LLC 2026
package com.rune.watch.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text

@Composable
fun ChatScreen(
    voiceDraft: String,
    voiceListening: Boolean,
    onOpenVoiceCapture: () -> Unit,
    onSendVoiceDraft: () -> Unit,
    onClearVoiceDraft: () -> Unit,
    onBack: () -> Unit,
) {
    val voiceReady = voiceDraft.trim().isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Chat",
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Voice relay to Ember",
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color(0xCC101521), RoundedCornerShape(18.dp))
                .border(1.dp, Color(0x3378919C), RoundedCornerShape(18.dp))
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = when {
                    voiceListening -> "Listening for your message..."
                    voiceReady -> voiceDraft
                    else -> "Tap Mic, speak to Ember, then send the transcript."
                },
                fontSize = 11.sp,
                color = Color(0xFFCFD8DC),
                textAlign = TextAlign.Center,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = onOpenVoiceCapture,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                ) {
                    Text(
                        text = if (voiceListening) "Mic..." else "Mic",
                        fontSize = 10.sp,
                    )
                }

                Button(
                    onClick = onSendVoiceDraft,
                    enabled = voiceReady,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                ) {
                    Text(
                        text = "Send",
                        fontSize = 10.sp,
                    )
                }
            }

            if (voiceReady) {
                Button(
                    onClick = onClearVoiceDraft,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Clear")
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .height(40.dp),
        ) {
            Text("Back")
        }
    }
}
