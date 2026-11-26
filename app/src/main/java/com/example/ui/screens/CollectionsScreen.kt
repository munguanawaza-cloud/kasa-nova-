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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.InstallmentPaymentOutcome
import com.example.data.local.SaleEntity
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald
import com.example.ui.theme.UsdEmeraldContainer
import com.example.utils.PdfGenerator
import com.example.utils.TimeUtils
import kotlinx.coroutines.launch

@Composable
fun CollectionsScreen(
    state: KasaUiState,
    onReceivePayment: (
        sale: SaleEntity,
        amount: Double,
        onSuccess: (InstallmentPaymentOutcome) -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    val strings = LocalKasaStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var payingSale by remember { mutableStateOf<SaleEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.collectionsTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = SovereignGold,
                fontWeight = FontWeight.Bold
            )
        }

        if (state.activeInstallments.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Text(
                        text = strings.noActiveInstallments,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(state.activeInstallments, key = { it.id }) { sale ->
                val now = TimeUtils.nowMillis()
                val earlyBirdEligible = TimeUtils.isEarlyBirdEligible(now, sale.dueDate)
                val isOverdue = TimeUtils.isOverdue(sale.dueDate, now)
                val customer = state.customers.find { it.id == sale.customerId }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("collection_card_${sale.id}")
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
                                    text = sale.customerName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${sale.productName} × ${sale.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            CurrencyBadge(sale.currency)
                        }

                        if (sale.dueDate > 0L) {
                            Text(
                                text = "Échéance / Due: ${TimeUtils.formatDateOnly(sale.dueDate)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isOverdue) OverdueCrimson else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (earlyBirdEligible) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(UsdEmeraldContainer)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = strings.earlyBirdDiscountBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = UsdEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Payé: ${formatCurrency(sale.paidAmount, sale.currency)}")
                            Text(
                                text = "Reste: ${formatCurrency(sale.remainingAmount, sale.currency)}",
                                color = SovereignGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { payingSale = sale },
                                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("receive_payment_button_${sale.id}")
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = strings.receivePayment)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.receivePayment)
                            }

                            OutlinedButton(
                                onClick = {
                                    val receiptText = buildString {
                                        append("KASA NOVA · HAMADA GROUP\n")
                                        append("Reçu Échéance #KN-${sale.id}\n")
                                        append("Client: ${sale.customerName}\n")
                                        append("Article: ${sale.productName}\n")
                                        append("Payé: ${formatCurrency(sale.paidAmount, sale.currency)}\n")
                                        append("Reste: ${formatCurrency(sale.remainingAmount, sale.currency)}\n")
                                        append("Support Kolwezi: +243 992 120 772 / +243 970 903 085")
                                    }
                                    val targetPhone = customer?.phone?.ifBlank { "+243 992 120 772" }
                                        ?: "+243 992 120 772"
                                    openWhatsAppMessage(context, targetPhone, receiptText)
                                },
                                modifier = Modifier.testTag("whatsapp_receipt_button_${sale.id}")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = strings.sendWhatsAppReceipt)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WA")
                            }

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        val uri = PdfGenerator.generateSaleInvoicePdf(context, sale, customer)
                                        context.startActivity(PdfGenerator.buildSharePdfIntent(uri))
                                    }
                                },
                                modifier = Modifier.testTag("pdf_receipt_button_${sale.id}")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = strings.sharePdfInvoice)
                            }
                        }
                    }
                }
            }
        }
    }

    if (payingSale != null) {
        ReceivePaymentDialog(
            sale = payingSale!!,
            onDismiss = { payingSale = null },
            onConfirm = { amount ->
                val target = payingSale ?: return@ReceivePaymentDialog
                onReceivePayment(
                    target,
                    amount,
                    { payingSale = null },
                    { /* error */ }
                )
            }
        )
    }
}

@Composable
private fun ReceivePaymentDialog(
    sale: SaleEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val strings = LocalKasaStrings.current
    var amountText by remember { mutableStateOf(sale.remainingAmount.toString()) }
    val requestedAmount = (amountText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, sale.remainingAmount)
    val earlyBird = TimeUtils.isEarlyBirdEligible(TimeUtils.nowMillis(), sale.dueDate)
    val discount = if (earlyBird) requestedAmount * 0.05 else 0.0
    val netPayable = (requestedAmount - discount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.receivePayment} · ${sale.customerName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("${strings.paymentAmount} (${sale.currency})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("payment_amount_input")
                )
                if (earlyBird && requestedAmount > 0.0) {
                    Text(
                        text = "${strings.earlyBirdDiscountBadge}\n-${formatCurrency(discount, sale.currency)} → Net: ${formatCurrency(netPayable, sale.currency)}",
                        color = UsdEmerald,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) onConfirm(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                modifier = Modifier.testTag("confirm_payment_button")
            ) {
                Text(strings.confirmPayment)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
