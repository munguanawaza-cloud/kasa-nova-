package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiQuickAction
import com.example.data.local.ProductEntity
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.OverdueCrimsonContainer
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SovereignGoldContainer
import com.example.ui.theme.UsdEmerald

@Composable
fun AiAssistantScreen(
    state: KasaUiState,
    onAnalyzeToday: () -> Unit,
    onRunQuickAction: (AiQuickAction, ProductEntity?) -> Unit,
    onAskAi: (String) -> Unit
) {
    val strings = LocalKasaStrings.current
    val context = LocalContext.current
    var promptText by remember { mutableStateOf("") }
    var selectedPromoProduct by remember(state.products) {
        mutableStateOf(state.products.firstOrNull())
    }

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

        // Quick AI Tools Carousel (4 specialized capabilities)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = { onRunQuickAction(AiQuickAction.DAILY_TARGET_LIQUIDITY, null) },
                enabled = !state.isAiGenerating,
                label = { Text(strings.aiModeDailyTarget) },
                leadingIcon = {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = strings.aiModeDailyTarget,
                        tint = SovereignGold
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("ai_chip_daily_target")
            )
            AssistChip(
                onClick = { onRunQuickAction(AiQuickAction.COLLECTIONS_PRIORITY, null) },
                enabled = !state.isAiGenerating,
                label = { Text(strings.aiModeCollectionsPlan) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Payments,
                        contentDescription = strings.aiModeCollectionsPlan,
                        tint = UsdEmerald
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("ai_chip_collections")
            )
            AssistChip(
                onClick = { onRunQuickAction(AiQuickAction.SALES_OFFER_COMPOSER, selectedPromoProduct) },
                enabled = !state.isAiGenerating,
                label = { Text(strings.aiModeSalesOffer) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Campaign,
                        contentDescription = strings.aiModeSalesOffer,
                        tint = SovereignGold
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("ai_chip_sales_offer")
            )
            AssistChip(
                onClick = { onRunQuickAction(AiQuickAction.RESTOCK_ADVISOR, null) },
                enabled = !state.isAiGenerating,
                label = { Text(strings.aiModeRestock) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Inventory2,
                        contentDescription = strings.aiModeRestock,
                        tint = OverdueCrimson
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.testTag("ai_chip_restock")
            )
        }

        if (state.products.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                state.products.take(6).forEach { prod ->
                    FilterChip(
                        selected = selectedPromoProduct?.id == prod.id,
                        onClick = { selectedPromoProduct = prod },
                        label = { Text(prod.name, maxLines = 1) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SovereignGoldContainer,
                            selectedLabelColor = SovereignGold
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onAnalyzeToday,
            enabled = !state.isAiGenerating,
            colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
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
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
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
                        if (!bubble.fromUser && bubble.text.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    openWhatsAppMessage(context, "+243 992 120 772", bubble.text)
                                },
                                modifier = Modifier.testTag("ai_share_wa_${bubble.id}")
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = strings.aiShareWhatsApp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.aiShareWhatsApp)
                            }
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
