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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SovereignGoldContainer
import com.example.utils.TimeUtils

@Composable
fun ChatScreen(
    state: KasaUiState,
    onSendMessage: (senderRole: String, senderName: String, text: String) -> Unit
) {
    val strings = LocalKasaStrings.current
    var senderRole by remember { mutableStateOf("OWNER") }
    var messageInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.chatMessages.size) {
        if (state.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(state.chatMessages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = strings.chatTitle,
            style = MaterialTheme.typography.headlineMedium,
            color = SovereignGold,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Role Switcher (Owner / Agent)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = senderRole == "OWNER",
                onClick = { senderRole = "OWNER" },
                label = { Text(strings.roleOwner) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SovereignGold,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("chat_role_owner")
            )
            FilterChip(
                selected = senderRole == "AGENT",
                onClick = { senderRole = "AGENT" },
                label = { Text(strings.roleAgent) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SovereignGold,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.testTag("chat_role_agent")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.chatMessages, key = { it.id }) { msg ->
                val isOwnerMsg = msg.senderRole == "OWNER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isOwnerMsg) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isOwnerMsg) SovereignGoldContainer else MaterialTheme.colorScheme.surface
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Text(
                                text = "${msg.senderName} · ${TimeUtils.formatDateTime(msg.createdAtMillis)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = SovereignGold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
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
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text(strings.chatPlaceholder) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_message_input")
            )
            IconButton(
                onClick = {
                    val trimmed = messageInput.trim()
                    if (trimmed.isNotEmpty()) {
                        val name = if (senderRole == "OWNER") strings.roleOwner else strings.roleAgent
                        onSendMessage(senderRole, name, trimmed)
                        messageInput = ""
                    }
                },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = SovereignGold),
                modifier = Modifier.testTag("chat_send_button")
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
