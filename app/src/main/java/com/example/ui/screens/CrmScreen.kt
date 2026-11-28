package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerSource
import com.example.data.local.CustomerStatus
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SovereignGoldContainer
import com.example.ui.theme.UsdEmerald
import com.example.utils.TimeUtils

@Composable
fun CrmScreen(
    state: KasaUiState,
    onSaveCustomer: (
        id: Long,
        name: String,
        phone: String,
        address: String,
        gpsLat: Double,
        gpsLng: Double,
        source: String,
        loyaltyPoints: Int
    ) -> Unit
) {
    val strings = LocalKasaStrings.current
    val context = LocalContext.current
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var showOnlyPotential by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<CustomerEntity?>(null) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var showCsvExportDialog by remember { mutableStateOf(false) }

    val potentialCount = remember(state.customers) {
        state.customers.count { it.status == CustomerStatus.POTENTIAL }
    }

    val filtered = remember(state.customers, searchQuery, showOnlyPotential) {
        val q = searchQuery.trim().lowercase()
        state.customers.filter { customer ->
            val matchesStatus = !showOnlyPotential || customer.status == CustomerStatus.POTENTIAL
            val matchesQuery = q.isEmpty() ||
                customer.name.lowercase().contains(q) ||
                customer.address.lowercase().contains(q) ||
                customer.phone.lowercase().contains(q) ||
                customer.source.lowercase().contains(q)
            matchesStatus && matchesQuery
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                containerColor = SovereignGold,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.addCustomer)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.crmTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = SovereignGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(strings.searchCustomers) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = strings.searchCustomers) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("crm_search_input")
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = !showOnlyPotential,
                    onClick = { showOnlyPotential = false },
                    label = {
                        Text(if (isArabic) "الكل (${state.customers.size})" else "Tous (${state.customers.size})")
                    },
                    modifier = Modifier.testTag("crm_filter_all")
                )
                FilterChip(
                    selected = showOnlyPotential,
                    onClick = { showOnlyPotential = true },
                    label = { Text("POTENTIAL ($potentialCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PromiseAmber.copy(alpha = 0.22f),
                        selectedLabelColor = PromiseAmber
                    ),
                    modifier = Modifier.testTag("crm_filter_potential")
                )
                OutlinedButton(
                    onClick = { showCsvExportDialog = true },
                    modifier = Modifier.testTag("crm_export_csv_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.TableChart,
                        contentDescription = "Export Potential Leads CSV",
                        tint = SovereignGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "تصدير CSV ($potentialCount)" else "CSV Leads ($potentialCount)",
                        color = SovereignGold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { customer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProfile = customer }
                            .testTag("customer_card_${customer.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = customer.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${customer.address} · ${customer.phone}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = customer.source,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    if (customer.status == CustomerStatus.POTENTIAL) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(PromiseAmber.copy(alpha = 0.18f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = CustomerStatus.POTENTIAL,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = PromiseAmber,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SovereignGoldContainer)
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = strings.loyaltyPointsLabel,
                                            tint = SovereignGold,
                                            modifier = Modifier.height(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${customer.loyaltyPoints} pts",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SovereignGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            IconButton(
                                onClick = { openDialer(context, customer.phone) },
                                modifier = Modifier.testTag("call_customer_${customer.id}")
                            ) {
                                Icon(
                                    Icons.Filled.Call,
                                    contentDescription = strings.callCustomer,
                                    tint = UsdEmerald
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedProfile != null) {
        val cust = selectedProfile!!
        val customerSales = state.sales.filter { it.customerId == cust.id }
        AlertDialog(
            onDismissRequest = { selectedProfile = null },
            title = { Text("${strings.customerProfileTitle}: ${cust.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${strings.phone}: ${cust.phone}")
                    Text("${strings.address}: ${cust.address}")
                    Text("${strings.customerSourceLabel}: ${cust.source}")
                    Text(
                        text = "${strings.loyaltyPointsLabel}: ${cust.loyaltyPoints} pts",
                        color = SovereignGold,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Achats / Ventes: ${customerSales.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    customerSales.take(3).forEach { s ->
                        Text(
                            text = "• ${s.productName} (${formatCurrency(s.totalPrice, s.currency)})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { openDialer(context, cust.phone) }) {
                    Text(strings.callCustomer)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProfile = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSave = { name, phone, address, lat, lng, source ->
                onSaveCustomer(0L, name, phone, address, lat, lng, source, 0)
                showAddCustomerDialog = false
            }
        )
    }

    if (showCsvExportDialog) {
        PotentialLeadsCsvExportDialog(
            potentialCount = potentialCount,
            isArabic = isArabic,
            onDismiss = { showCsvExportDialog = false },
            onExported = { message ->
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }
}

@Composable
private fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double, String) -> Unit
) {
    val strings = LocalKasaStrings.current
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+243 ") }
    var address by remember { mutableStateOf("Kolwezi") }
    var latText by remember { mutableStateOf(TimeUtils.GOLF_ISTM_LAT.toString()) }
    var lngText by remember { mutableStateOf(TimeUtils.GOLF_ISTM_LNG.toString()) }
    var source by remember { mutableStateOf(CustomerSource.WHATSAPP) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addCustomer) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.customerName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.phone) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.address) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(strings.customerSourceLabel, style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CustomerSource.ALL.forEach { src ->
                        FilterChip(
                            selected = source == src,
                            onClick = { source = src },
                            label = { Text(src) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("GPS Lat") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("GPS Lng") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name,
                            phone,
                            address,
                            latText.toDoubleOrNull() ?: TimeUtils.GOLF_ISTM_LAT,
                            lngText.toDoubleOrNull() ?: TimeUtils.GOLF_ISTM_LNG,
                            source
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SovereignGold)
            ) {
                Text(strings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
