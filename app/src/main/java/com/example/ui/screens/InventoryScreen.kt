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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.ProductCategory
import com.example.data.local.ProductEntity
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.OverdueCrimsonContainer
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald

@Composable
fun InventoryScreen(
    state: KasaUiState,
    onSaveProduct: (
        id: Long,
        name: String,
        category: String,
        costUsd: Double,
        costCdf: Double,
        priceUsd: Double,
        priceCdf: Double,
        stockQuantity: Int,
        videoUrl: String
    ) -> Unit,
    onAdjustStock: (ProductEntity, Int) -> Unit
) {
    val strings = LocalKasaStrings.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var editingProduct by remember { mutableStateOf<ProductEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(state.products, selectedFilter) {
        if (selectedFilter == "ALL") {
            state.products
        } else {
            state.products.filter { it.category == selectedFilter }
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddDialog = true
                },
                containerColor = SovereignGold,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.addProduct)
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
                text = strings.inventoryTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = SovereignGold,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))

            // V6.0 Category Filter Chips: All, Cuisine, Beauté, Maison
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val chips = listOf(
                    "ALL" to strings.categoryAll,
                    ProductCategory.CUISINE to strings.categoryCuisine,
                    ProductCategory.BEAUTE to strings.categoryBeaute,
                    ProductCategory.MAISON to strings.categoryMaison
                )
                chips.forEach { (key, label) ->
                    FilterChip(
                        selected = selectedFilter == key,
                        onClick = { selectedFilter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SovereignGold,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("category_chip_$key")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredProducts, key = { it.id }) { product ->
                    val lowStock = product.isLowStock
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (lowStock) OverdueCrimsonContainer else MaterialTheme.colorScheme.surface
                        ),
                        border = if (lowStock) BorderStroke(1.dp, OverdueCrimson) else null,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("product_card_${product.id}")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = product.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SovereignGold.copy(alpha = 0.18f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = localizedCategoryLabel(product.category, strings),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SovereignGold
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingProduct = product
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.testTag("edit_product_${product.id}")
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = strings.editProduct, tint = SovereignGold)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "USD: ${formatUsd(product.priceUsd)} (Coût ${formatUsd(product.costUsd)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = UsdEmerald
                                )
                            }
                            Text(
                                text = "CDF: ${formatCdf(product.priceCdf)} (Coût ${formatCdf(product.costCdf)})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (lowStock) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Filled.Warning,
                                            contentDescription = strings.lowStockAlert,
                                            tint = OverdueCrimson
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${strings.lowStockAlert}: ${product.stockQuantity}",
                                            color = OverdueCrimson,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${strings.stockQuantity}: ${product.stockQuantity}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = UsdEmerald
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onAdjustStock(product, -1) },
                                        modifier = Modifier.testTag("stock_minus_${product.id}")
                                    ) {
                                        Icon(Icons.Filled.Remove, contentDescription = "Decrease stock")
                                    }
                                    IconButton(
                                        onClick = { onAdjustStock(product, 1) },
                                        modifier = Modifier.testTag("stock_plus_${product.id}")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = "Increase stock")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ProductEditorDialog(
            initial = editingProduct,
            onDismiss = { showAddDialog = false },
            onSave = { id, name, category, costUsd, costCdf, priceUsd, priceCdf, stockQty, videoUrl ->
                onSaveProduct(id, name, category, costUsd, costCdf, priceUsd, priceCdf, stockQty, videoUrl)
                showAddDialog = false
            }
        )
    }
}

private fun localizedCategoryLabel(category: String, strings: com.example.ui.KasaStrings): String {
    return when (category) {
        ProductCategory.CUISINE -> "${ProductCategory.CUISINE} (${strings.categoryCuisine})"
        ProductCategory.BEAUTE -> "${ProductCategory.BEAUTE} (${strings.categoryBeaute})"
        ProductCategory.MAISON -> "${ProductCategory.MAISON} (${strings.categoryMaison})"
        else -> category
    }
}

@Composable
private fun ProductEditorDialog(
    initial: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, Double, Double, Double, Double, Int, String) -> Unit
) {
    val strings = LocalKasaStrings.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: ProductCategory.CUISINE) }
    var costUsd by remember { mutableStateOf(initial?.costUsd?.toString() ?: "") }
    var costCdf by remember { mutableStateOf(initial?.costCdf?.toString() ?: "") }
    var priceUsd by remember { mutableStateOf(initial?.priceUsd?.toString() ?: "") }
    var priceCdf by remember { mutableStateOf(initial?.priceCdf?.toString() ?: "") }
    var stockQty by remember { mutableStateOf(initial?.stockQuantity?.toString() ?: "10") }
    var videoUrl by remember { mutableStateOf(initial?.videoUrl ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addProduct else strings.editProduct) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.productName) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text(strings.productCategoryLabel, style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ProductCategory.ALL.forEach { cat ->
                            FilterChip(
                                selected = category == cat,
                                onClick = { category = cat },
                                label = { Text(cat) }
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costUsd,
                            onValueChange = { costUsd = it },
                            label = { Text(strings.costUsd) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceUsd,
                            onValueChange = { priceUsd = it },
                            label = { Text(strings.priceUsd) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = costCdf,
                            onValueChange = { costCdf = it },
                            label = { Text(strings.costCdf) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = priceCdf,
                            onValueChange = { priceCdf = it },
                            label = { Text(strings.priceCdf) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        value = stockQty,
                        onValueChange = { stockQty = it },
                        label = { Text(strings.stockQuantity) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = { Text(strings.videoUrl) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            initial?.id ?: 0L,
                            name,
                            category,
                            costUsd.toDoubleOrNull() ?: 0.0,
                            costCdf.toDoubleOrNull() ?: 0.0,
                            priceUsd.toDoubleOrNull() ?: 0.0,
                            priceCdf.toDoubleOrNull() ?: 0.0,
                            stockQty.toIntOrNull() ?: 0,
                            videoUrl
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
