package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.OverdueCrimsonContainer
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SovereignGoldContainer

@Composable
fun AiAssistantScreen(
    state: KasaUiState,
    onAnalyzeToday: () -> Unit,
    onAskAi: (String) -> Unit
) {
    val strings = LocalKasaStrings.current
    var promptText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = strings.aiTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = SovereignGold,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = strings.aiSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = strings.aiReadOnlyRule,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onAnalyzeToday,
            enabled = !state.isAiGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("ai_analyze_today_button")
        ) {
            if (state.isAiGenerating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = strings.aiAnalyzeToday)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(strings.aiAnalyzeToday, fontWeight = FontWeight.Bold)
        }

        if (state.aiError != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(OverdueCrimsonContainer)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.WifiOff, contentDescription = strings.aiOfflineError, tint = OverdueCrimson)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.aiError,
                    color = OverdueCrimson,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.aiMessages, key = { it.id }) { bubble ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (bubble.fromUser) SovereignGoldContainer else MaterialTheme.colorScheme.surface
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = if (bubble.fromUser) "Owner" else "KASA NOVA AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = SovereignGold,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = bubble.text.ifBlank { "..." },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = { Text(strings.aiPromptPlaceholder) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input")
            )
            IconButton(
                onClick = {
                    val q = promptText.trim()
                    if (q.isNotEmpty()) {
                        onAskAi(q)
                        promptText = ""
                    }
                },
                enabled = !state.isAiGenerating,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SovereignGold),
                modifier = Modifier.testTag("ai_send_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = strings.sendMessage,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
