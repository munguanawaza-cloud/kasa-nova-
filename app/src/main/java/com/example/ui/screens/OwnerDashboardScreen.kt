package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Dataset
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.utils.KasaSqliteDbTarget
import com.example.data.local.CurrencyCode
import com.example.data.local.CustomerSource
import com.example.data.sync.CashDropRecord
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.CdfCobalt
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald

@Composable
fun OwnerDashboardScreen(
    state: KasaUiState,
    onSyncNow: () -> Unit,
    onSeedSampleData: () -> Unit,
    onAddExpense: (String, Double, String) -> Unit,
    onConfirmCashDropOtp: (CashDropRecord, String) -> Unit,
    onNavigateInventory: () -> Unit,
    onNavigateCrm: () -> Unit,
    onNavigateNewSale: () -> Unit,
    onNavigateCollections: () -> Unit,
    onNavigateCashVault: () -> Unit,
    onNavigateMap: () -> Unit,
    onNavigateChat: () -> Unit,
    onNavigateAi: () -> Unit,
    onNavigateCashFlow: () -> Unit = {},
    onNavigateLeadFinder: () -> Unit = {}
) {
    val strings = LocalKasaStrings.current
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    var showExpenseDialog by remember { mutableStateOf(false) }
    var showSqliteExportDialog by remember { mutableStateOf(false) }
    val otpInputs = remember { mutableStateMapOf<String, String>() }
    val pendingDrops = state.cashDrops.filter { it.status == "PENDING" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Brand Hero Banner
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(144.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kolwezi_hero),
                        contentDescription = strings.brandTagline,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xEB07291F),
                                        Color(0xC20B3D2E),
                                        Color(0x5207291F)
                                    )
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_kasa_nova_icon),
                            contentDescription = strings.appTitle,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.5.dp, SovereignGold, RoundedCornerShape(14.dp))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.dashboardTitle,
                                style = MaterialTheme.typography.headlineMedium,
                                color = SovereignGold,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.brandTagline,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (state.isOnline) {
                                    "Online · ${state.unsyncedCount} pending"
                                } else {
                                    strings.syncOffline
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (state.isOnline) UsdEmerald else PromiseAmber
                            )
                        }
                    }
                }
            }
        }

        // Strict Currency Separation Notice
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = strings.strictCurrencyRule,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Dual Currency KPI Grid (2 rows x 2 cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DualCurrencyStatCard(
                    title = strings.statsSales,
                    usdValue = state.todaySalesUsd,
                    cdfValue = state.todaySalesCdf,
                    accentColor = SovereignGold,
                    modifier = Modifier.weight(1f)
                )
                DualCurrencyStatCard(
                    title = strings.statsCollections,
                    usdValue = state.todayCollectionsUsd,
                    cdfValue = state.todayCollectionsCdf,
                    accentColor = UsdEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DualCurrencyStatCard(
                    title = strings.statsOverdue,
                    usdValue = state.overdueUsd,
                    cdfValue = state.overdueCdf,
                    accentColor = OverdueCrimson,
                    modifier = Modifier.weight(1f)
                )
                DualCurrencyStatCard(
                    title = strings.statsProfit,
                    usdValue = state.netProfitUsd,
                    cdfValue = state.netProfitCdf,
                    accentColor = UsdEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // $50 USD Daily Net Profit Target & AI Coach Progress Card
        item {
            val targetUsd = 50.0
            val progressFraction = (state.netProfitUsd / targetUsd).toFloat().coerceIn(0f, 1f)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    SovereignGold.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.medium
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
                        Text(
                            text = strings.dailyTargetTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = SovereignGold,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${formatUsd(state.netProfitUsd)} / $50",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (state.netProfitUsd >= targetUsd) UsdEmerald else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (state.netProfitUsd >= targetUsd) UsdEmerald else SovereignGold,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onNavigateAi,
                            modifier = Modifier.testTag("dashboard_ai_coach_button")
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = strings.moduleAiAssistant,
                                modifier = Modifier.size(16.dp),
                                tint = SovereignGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.aiModeDailyTarget, color = SovereignGold)
                        }
                    }
                }
            }
        }

        // Quick Operational Actions Row (Cloud Sync, Expense, Sample Data)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSyncNow,
                    enabled = !state.isSyncing,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sync_cloud_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
                ) {
                    if (state.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = if (state.unsyncedCount == 0) Icons.Filled.CloudDone else Icons.Filled.CloudSync,
                            contentDescription = strings.syncNow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.syncNow, maxLines = 1)
                }

                OutlinedButton(
                    onClick = { showExpenseDialog = true },
                    modifier = Modifier.testTag("add_expense_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = strings.addExpense,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.addExpense, maxLines = 1)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showSqliteExportDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_sqlite_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Storage,
                    contentDescription = "Export SQLite DB",
                    tint = SovereignGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isArabic) "تصدير قاعدة البيانات (SQLite .db)" else "Exporter Base SQLite (.db)",
                    color = SovereignGold
                )
            }
            if (state.products.isEmpty() && state.customers.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSeedSampleData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("load_sample_data_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Dataset,
                        contentDescription = strings.loadSampleData,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.loadSampleData)
                }
            }
        }

        // 8-Module Navigation Grid (Inventory, CRM, New Sale, Collections, Cash Vault, Map, Chat, AI)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardModuleTile(
                        title = strings.moduleNewSale,
                        icon = Icons.Filled.AddShoppingCart,
                        accent = SovereignGold,
                        tag = "tile_new_sale",
                        onClick = onNavigateNewSale,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardModuleTile(
                        title = strings.moduleInventory,
                        badge = if (state.lowStockCount > 0) "${state.lowStockCount}!" else null,
                        icon = Icons.Filled.Inventory2,
                        accent = if (state.lowStockCount > 0) OverdueCrimson else UsdEmerald,
                        tag = "tile_inventory",
                        onClick = onNavigateInventory,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardModuleTile(
                        title = strings.moduleCollections,
                        badge = state.activeInstallments.size.takeIf { it > 0 }?.toString(),
                        icon = Icons.Filled.Payments,
                        accent = PromiseAmber,
                        tag = "tile_collections",
                        onClick = onNavigateCollections,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardModuleTile(
                        title = strings.moduleCrm,
                        badge = state.customers.size.takeIf { it > 0 }?.toString(),
                        icon = Icons.Filled.People,
                        accent = CdfCobalt,
                        tag = "tile_crm",
                        onClick = onNavigateCrm,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardModuleTile(
                        title = strings.moduleCashVault,
                        badge = pendingDrops.size.takeIf { it > 0 }?.toString(),
                        icon = Icons.Filled.Lock,
                        accent = SovereignGold,
                        tag = "tile_cash_vault",
                        onClick = onNavigateCashVault,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardModuleTile(
                        title = strings.moduleMapRadar,
                        icon = Icons.Filled.Map,
                        accent = UsdEmerald,
                        tag = "tile_map",
                        onClick = onNavigateMap,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardModuleTile(
                        title = strings.moduleChat,
                        icon = Icons.AutoMirrored.Filled.Chat,
                        accent = CdfCobalt,
                        tag = "tile_chat",
                        onClick = onNavigateChat,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardModuleTile(
                        title = strings.moduleAiAssistant,
                        icon = Icons.Filled.AutoAwesome,
                        accent = SovereignGold,
                        tag = "tile_ai",
                        onClick = onNavigateAi,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DashboardModuleTile(
                        title = "AI Lead Finder · البحث عن زبائن",
                        icon = Icons.Filled.PersonSearch,
                        accent = SovereignGold,
                        tag = "tile_lead_finder",
                        onClick = onNavigateLeadFinder,
                        modifier = Modifier.weight(1f)
                    )
                    DashboardModuleTile(
                        title = "Cash on Hand · السيولة (Room)",
                        icon = Icons.Filled.Payments,
                        accent = UsdEmerald,
                        tag = "tile_cash_flow",
                        onClick = onNavigateCashFlow,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Pending Cash Drops OTP Verification Section
        if (pendingDrops.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.pendingCashDropsTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = SovereignGold,
                            fontWeight = FontWeight.Bold
                        )
                        pendingDrops.forEach { drop ->
                            val enteredOtp = otpInputs[drop.id] ?: ""
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${drop.agentName} · ${formatCurrency(drop.amount, drop.currency)}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (drop.note.isNotBlank()) {
                                        Text(
                                            text = drop.note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = enteredOtp,
                                        onValueChange = { if (it.length <= 4) otpInputs[drop.id] = it },
                                        label = { Text(strings.enterOtpPlaceholder) },
                                        singleLine = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("dashboard_otp_input_${drop.id}")
                                    )
                                }
                                Button(
                                    onClick = { onConfirmCashDropOtp(drop, enteredOtp) },
                                    modifier = Modifier.testTag("dashboard_otp_confirm_${drop.id}")
                                ) {
                                    Text(strings.confirmOtpButton)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sales by Source Marketing Tracker Chart
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = strings.salesBySourceTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = SovereignGold,
                        fontWeight = FontWeight.Bold
                    )
                    val totalSalesCount = state.salesBySource.values.sum().coerceAtLeast(1)
                    CustomerSource.ALL.forEach { src ->
                        val count = state.salesBySource[src] ?: 0
                        val fraction = count.toFloat() / totalSalesCount.toFloat()
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = src,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SovereignGold
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = when (src) {
                                    CustomerSource.FACEBOOK -> CdfCobalt
                                    CustomerSource.TIKTOK -> SovereignGold
                                    CustomerSource.WHATSAPP -> UsdEmerald
                                    else -> PromiseAmber
                                },
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showExpenseDialog = false },
            onSave = { title, amount, currency ->
                onAddExpense(title, amount, currency)
                showExpenseDialog = false
            }
        )
    }

    if (showSqliteExportDialog) {
        SqliteExportDialog(
            initialTarget = KasaSqliteDbTarget.MAIN_OFFLINE,
            isArabic = isArabic,
            onDismiss = { showSqliteExportDialog = false }
        )
    }
}

@Composable
private fun DashboardModuleTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    badge: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(92.dp)
            .testTag(tag)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accent.copy(alpha = 0.2f))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String) -> Unit
) {
    val strings = LocalKasaStrings.current
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf(CurrencyCode.USD) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addExpense) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Montant") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    listOf(CurrencyCode.USD, CurrencyCode.CDF).forEach { code ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { currency = code }
                        ) {
                            RadioButton(selected = currency == code, onClick = { currency = code })
                            Text(code)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && amt > 0.0) {
                        onSave(title, amt, currency)
                    }
                }
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
