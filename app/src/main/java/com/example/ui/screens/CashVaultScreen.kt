package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.CurrencyCode
import com.example.data.sync.CashDropRecord
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.PromiseAmberContainer
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SovereignGoldContainer
import com.example.ui.theme.UsdEmerald
import com.example.ui.theme.UsdEmeraldContainer
import com.example.utils.TimeUtils

@Composable
fun CashVaultScreen(
    state: KasaUiState,
    onCreateCashDrop: (amount: Double, currency: String, agentName: String, note: String) -> Unit,
    onConfirmCashDropOtp: (drop: CashDropRecord, enteredOtp: String) -> Unit
) {
    val strings = LocalKasaStrings.current
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CurrencyCode.USD) }
    var agentName by remember { mutableStateOf("Agent Kolwezi") }
    var note by remember { mutableStateOf("") }
    val otpInputs = remember { mutableStateMapOf<String, String>() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.cashVaultTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = SovereignGold,
                fontWeight = FontWeight.Bold
            )
        }

        // Agent Cash Drop Form -> Generates 4-digit OTP
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = agentName,
                        onValueChange = { agentName = it },
                        label = { Text(strings.agentNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("${strings.paymentAmount} ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cash_drop_amount_input")
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        listOf(CurrencyCode.USD, CurrencyCode.CDF).forEach { code ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { currency = code }
                            ) {
                                RadioButton(selected = currency == code, onClick = { currency = code })
                                CurrencyBadge(code)
                            }
                        }
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text(strings.dropNoteLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onCreateCashDrop(amt, currency, agentName, note)
                                amountText = ""
                                note = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_cash_drop_button")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = strings.submitCashDrop)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.submitCashDrop, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Generated Secret OTP Card (for Agent to hand to Owner)
        state.latestCashDropOtp?.let { latest ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SovereignGoldContainer),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.generatedOtpLabel,
                            style = MaterialTheme.typography.titleMedium,
                            color = SovereignGold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = latest.otp,
                            style = MaterialTheme.typography.displayLarge,
                            color = SovereignGold,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${latest.agentName} · ${formatCurrency(latest.amount, latest.currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Firestore Cash Drops List (Pending OTP confirmation + Completed)
        items(state.cashDrops, key = { it.id }) { drop ->
            val isPending = drop.status == "PENDING"
            val enteredOtp = otpInputs[drop.id] ?: ""

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cash_drop_card_${drop.id}")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${drop.agentName} · ${formatCurrency(drop.amount, drop.currency)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = TimeUtils.formatDateTime(drop.createdAtMillis),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPending) PromiseAmberContainer else UsdEmeraldContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPending) strings.statusPending else strings.statusCompleted,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isPending) PromiseAmber else UsdEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (drop.note.isNotBlank()) {
                        Text(drop.note, style = MaterialTheme.typography.bodyMedium)
                    }

                    if (isPending) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = enteredOtp,
                                onValueChange = { if (it.length <= 4) otpInputs[drop.id] = it },
                                label = { Text(strings.enterOtpPlaceholder) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vault_otp_input_${drop.id}")
                            )
                            Button(
                                onClick = { onConfirmCashDropOtp(drop, enteredOtp) },
                                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                                modifier = Modifier.testTag("vault_otp_confirm_${drop.id}")
                            ) {
                                Text(strings.confirmOtpButton)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = strings.statusCompleted,
                                tint = UsdEmerald
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.statusCompleted,
                                color = UsdEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
