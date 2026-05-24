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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Text
import com.rune.watch.bus.WatchRelayTranscriptEntry
import com.rune.watch.bus.WatchRelayTranscriptSummary
import java.util.Locale

@Composable
fun ChatScreen(
    relayTranscript: List<WatchRelayTranscriptEntry>,
    relayConversationSummaries: List<WatchRelayTranscriptSummary>,
    selectedConversationId: String?,
    voiceDraft: String,
    voiceListening: Boolean,
    currentLocale: Locale,
    onPreviousConversation: () -> Unit,
    onNextConversation: () -> Unit,
    onOpenVoiceCapture: () -> Unit,
    onSendVoiceDraft: () -> Unit,
    onClearVoiceDraft: () -> Unit,
    onBack: () -> Unit,
) {
    val voiceReady = voiceDraft.trim().isNotBlank()
    val selectedConversationSummary = relayConversationSummaries.firstOrNull { it.conversationId == selectedConversationId }
        ?: relayConversationSummaries.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = watchChatTitle(currentLocale),
            fontSize = 13.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
        )

        Text(
            text = watchChatSubtitle(currentLocale),
            fontSize = 10.sp,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center,
        )

        if (relayConversationSummaries.size > 1 && selectedConversationSummary != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPreviousConversation,
                    modifier = Modifier.height(34.dp),
                ) {
                    Text(text = "◀", fontSize = 10.sp)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = "Shared thread",
                        fontSize = 8.sp,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = selectedConversationSummary.latestMessageText
                            ?: "Thread ${selectedConversationSummary.conversationId.take(8)}",
                        fontSize = 9.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                }

                Button(
                    onClick = onNextConversation,
                    modifier = Modifier.height(34.dp),
                ) {
                    Text(text = "▶", fontSize = 10.sp)
                }
            }
        }

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
            if (relayTranscript.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    relayTranscript.takeLast(6).forEach { entry ->
                        ChatTranscriptBubble(entry = entry)
                    }
                }
            }

            Text(
                text = when {
                    voiceListening -> watchChatListeningText(currentLocale)
                    voiceReady -> voiceDraft
                    else -> watchChatInstructionText(currentLocale)
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
                        text = watchChatMicButton(voiceListening, currentLocale),
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
                        text = watchChatSendButton(currentLocale),
                        fontSize = 10.sp,
                    )
                }
            }

            if (voiceReady) {
                Button(
                    onClick = onClearVoiceDraft,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(watchChatClearButton(currentLocale))
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
            Text(watchChatBackButton(currentLocale))
        }
    }
}

@Composable
private fun ChatTranscriptBubble(entry: WatchRelayTranscriptEntry) {
    val bubbleColor = if (entry.fromWatch) Color(0xFF12344A) else Color(0xFF1A2331)
    val borderColor = if (entry.fromWatch) Color(0x554FC3F7) else Color(0x3378919C)
    val statusLabel = entry.status?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    Column(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .background(bubbleColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .then(if (entry.fromWatch) Modifier else Modifier),
        horizontalAlignment = if (entry.fromWatch) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = buildString {
                append(entry.sourceLabel)
                if (!statusLabel.isNullOrBlank()) {
                    append(" • ")
                    append(statusLabel)
                }
            },
            fontSize = 9.sp,
            color = Color(0xFFB0BEC5),
        )
        Text(
            text = entry.text,
            fontSize = 11.sp,
            color = Color(0xFFCFD8DC),
            textAlign = if (entry.fromWatch) TextAlign.End else TextAlign.Start,
        )
        if (!entry.threadId.isNullOrBlank()) {
            Text(
                text = "Thread ${entry.threadId.take(8)}",
                fontSize = 9.sp,
                color = Color(0xFFFFCC80),
            )
        }
    }
}
