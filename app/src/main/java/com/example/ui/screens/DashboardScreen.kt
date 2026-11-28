package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.CurrencyCode
import com.example.data.local.CurrencyType
import com.example.data.local.PaymentChannel
import com.example.data.local.TransactionEntity
import com.example.data.local.TransactionType
import com.example.ui.AppLanguage
import com.example.ui.DashboardViewModel
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.CdfCobalt
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald
import com.example.utils.KasaSqliteDbTarget
import com.example.utils.TimeUtils
import kotlinx.coroutines.launch

private enum class ChannelChoice(val labelFr: String, val labelAr: String) {
    CASH("Espèces / Cash", "نقداً"),
    MOBILE_MONEY("Mobile Money", "موبايل موني"),
    BANK("Banque", "تحويل بنكي")
}

@Composable
fun DashboardScreen(
    dashboardViewModel: DashboardViewModel = viewModel()
) {
    val strings = LocalKasaStrings.current
    val isArabic = strings.navHome == "الرئيسية"
    val cashBalanceUsd by dashboardViewModel.cashBalanceUsd.collectAsStateWithLifecycle()
    val cashBalanceCdf by dashboardViewModel.cashBalanceCdf.collectAsStateWithLifecycle()
    val totalSalesUsd by dashboardViewModel.totalSalesUsd.collectAsStateWithLifecycle()
    val totalExpensesUsd by dashboardViewModel.totalExpensesUsd.collectAsStateWithLifecycle()
    val totalSalesCdf by dashboardViewModel.totalSalesCdf.collectAsStateWithLifecycle()
    val totalExpensesCdf by dashboardViewModel.totalExpensesCdf.collectAsStateWithLifecycle()
    val transactions by dashboardViewModel.transactions.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SovereignGold,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = if (isArabic) "إضافة معاملة" else "Ajouter opération"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Section: Prominent Cash on Hand Card (USD & CDF strictly separated)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("cash_on_hand_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.55f)),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SovereignGold.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalanceWallet,
                                        contentDescription = "Cash on Hand",
                                        tint = SovereignGold,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (isArabic) "السيولة الفعلية · Cash on Hand" else "Cash on Hand · السيولة الفعلية",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SovereignGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = strings.brandTagline,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(
                                onClick = { showExportDialog = true },
                                modifier = Modifier.testTag("cashflow_export_sqlite_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storage,
                                    contentDescription = "Export SQLite",
                                    tint = UsdEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SQLite",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = UsdEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cash_balance_usd"),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurrencyBadge(CurrencyCode.USD)
                                Text(
                                    text = formatUsd(cashBalanceUsd),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = UsdEmerald,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(48.dp)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cash_balance_cdf"),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CurrencyBadge(CurrencyCode.CDF)
                                Text(
                                    text = formatCdf(cashBalanceCdf),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = CdfCobalt,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Text(
                            text = strings.strictCurrencyRule,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Middle Section: Total Sales / Revenue vs Total Expenses
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CashFlowSummaryCard(
                        title = if (isArabic) "إجمالي المبيعات" else "Total Sales / Revenue",
                        usdAmount = totalSalesUsd,
                        cdfAmount = totalSalesCdf,
                        accent = SovereignGold,
                        icon = Icons.Filled.ArrowUpward,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("total_sales_card")
                    )
                    CashFlowSummaryCard(
                        title = if (isArabic) "إجمالي المصروفات" else "Total Expenses",
                        usdAmount = totalExpensesUsd,
                        cdfAmount = totalExpensesCdf,
                        accent = OverdueCrimson,
                        icon = Icons.Filled.ArrowDownward,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("total_expenses_card")
                    )
                }
            }

            // Lower Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "سجل العمليات الأخيرة" else "Historique des Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        color = SovereignGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${transactions.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = SovereignGold.copy(alpha = 0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = if (isArabic) {
                                    "لا توجد عمليات مسجلة بعد. اضغط على + لإضافة عملية."
                                } else {
                                    "Aucune transaction enregistrée. Appuyez sur + pour commencer."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(transactions, key = { it.id }) { tx ->
                    TransactionHistoryItem(transaction = tx, isArabic = isArabic)
                }
            }

            item { Spacer(modifier = Modifier.height(72.dp)) }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            isArabic = isArabic,
            onDismiss = { showAddDialog = false },
            onSave = { type, amount, currency, channel, customerName, description ->
                dashboardViewModel.addTransaction(
                    type = type,
                    amount = amount,
                    currency = currency,
                    paymentChannel = channel,
                    customerName = customerName,
                    description = description
                )
                showAddDialog = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(
                        if (isArabic) "تم حفظ المعاملة محلياً بنجاح" else "Opération enregistrée hors-ligne"
                    )
                }
            }
        )
    }

    if (showExportDialog) {
        SqliteExportDialog(
            initialTarget = KasaSqliteDbTarget.CASH_FLOW,
            isArabic = isArabic,
            onDismiss = { showExportDialog = false },
            onExported = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }
}

@Composable
private fun CashFlowSummaryCard(
    title: String,
    usdAmount: Double,
    cdfAmount: Double,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text = formatUsd(usdAmount),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = formatCdf(cdfAmount),
                style = MaterialTheme.typography.bodyMedium,
                color = CdfCobalt,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TransactionHistoryItem(
    transaction: TransactionEntity,
    isArabic: Boolean
) {
    val (typeLabel, typeColor) = when (transaction.type) {
        TransactionType.SALE -> (if (isArabic) "بيع · SALE" else "VENTE") to SovereignGold
        TransactionType.COLLECTION -> (if (isArabic) "تحصيل · COLLECTION" else "RECOUVREMENT") to UsdEmerald
        TransactionType.EXPENSE -> (if (isArabic) "مصروف · EXPENSE" else "DÉPENSE") to OverdueCrimson
    }
    val channelLabel = when (transaction.paymentChannel) {
        PaymentChannel.CASH_USD -> "Cash USD"
        PaymentChannel.CASH_CDF -> "Cash CDF"
        PaymentChannel.MOBILE_MONEY -> "Mobile Money"
        PaymentChannel.BANK_TRANSFER -> if (isArabic) "تحويل بنكي" else "Banque"
    }
    val sign = if (transaction.type == TransactionType.EXPENSE) "-" else "+"
    val formattedAmount = if (transaction.currency == CurrencyType.USD) {
        "$sign${formatUsd(transaction.amount)} USD"
    } else {
        "$sign${formatCdf(transaction.amount)}"
    }
    val titleText = listOfNotNull(
        transaction.customerName?.takeIf { it.isNotBlank() },
        transaction.description?.takeIf { it.isNotBlank() }
    ).joinToString(" · ").ifBlank { typeLabel }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("transaction_row_${transaction.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(typeColor.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = typeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = channelLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = PromiseAmber
                    )
                }
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = TimeUtils.formatDateTime(transaction.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formattedAmount,
                    style = MaterialTheme.typography.titleMedium,
                    color = typeColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                CurrencyBadge(transaction.currency.name)
            }
        }
    }
}

@Composable
private fun AddTransactionDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        type: TransactionType,
        amount: Double,
        currency: CurrencyType,
        channel: PaymentChannel,
        customerName: String?,
        description: String?
    ) -> Unit
) {
    val strings = LocalKasaStrings.current
    var selectedType by remember { mutableStateOf(TransactionType.SALE) }
    var amountText by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(CurrencyType.USD) }
    var selectedChannel by remember { mutableStateOf(ChannelChoice.CASH) }
    var customerName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showAmountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Payments, contentDescription = null, tint = SovereignGold)
                Text(
                    text = if (isArabic) "إضافة معاملة مالية" else "Nouvelle Opération",
                    color = SovereignGold,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) "نوع العملية" else "Type d'opération",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        TransactionType.SALE to (if (isArabic) "بيع" else "Vente"),
                        TransactionType.COLLECTION to (if (isArabic) "تحصيل" else "Encaiss."),
                        TransactionType.EXPENSE to (if (isArabic) "مصروف" else "Dépense")
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label, maxLines = 1) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SovereignGold.copy(alpha = 0.24f),
                                selectedLabelColor = SovereignGold
                            ),
                            modifier = Modifier.testTag("tx_type_${type.name}")
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        showAmountError = false
                    },
                    label = { Text(if (isArabic) "المبلغ (${selectedCurrency.name})" else "Montant (${selectedCurrency.name})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = showAmountError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_amount_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CurrencyType.entries.forEach { curr ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { selectedCurrency = curr }
                                .testTag("tx_currency_${curr.name}")
                        ) {
                            RadioButton(
                                selected = selectedCurrency == curr,
                                onClick = { selectedCurrency = curr }
                            )
                            CurrencyBadge(curr.name)
                        }
                    }
                }

                Text(
                    text = if (isArabic) "قناة الدفع" else "Canal de paiement",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChannelChoice.entries.forEach { choice ->
                        FilterChip(
                            selected = selectedChannel == choice,
                            onClick = { selectedChannel = choice },
                            label = {
                                Text(
                                    text = if (isArabic) choice.labelAr else choice.labelFr,
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("tx_channel_${choice.name}")
                        )
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = {
                        Text(if (isArabic) "اسم العميل (اختياري)" else "Client (Optionnel)")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_customer_input")
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = {
                        Text(if (isArabic) "الوصف / الملاحظة (اختياري)" else "Description (Optionnel)")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tx_description_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (amount <= 0.0) {
                        showAmountError = true
                        return@Button
                    }
                    val mappedChannel = when (selectedChannel) {
                        ChannelChoice.CASH -> if (selectedCurrency == CurrencyType.USD) {
                            PaymentChannel.CASH_USD
                        } else {
                            PaymentChannel.CASH_CDF
                        }
                        ChannelChoice.MOBILE_MONEY -> PaymentChannel.MOBILE_MONEY
                        ChannelChoice.BANK -> PaymentChannel.BANK_TRANSFER
                    }
                    onSave(
                        selectedType,
                        amount,
                        selectedCurrency,
                        mappedChannel,
                        customerName,
                        description
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                modifier = Modifier.testTag("tx_save_button")
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}
