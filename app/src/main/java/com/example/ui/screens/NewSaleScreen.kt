package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.Paint as AndroidPaint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.local.CurrencyCode
import com.example.data.local.CustomerEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleType
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald
import com.example.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSaleScreen(
    state: KasaUiState,
    onCreateSale: (
        customer: CustomerEntity,
        product: ProductEntity,
        quantity: Int,
        saleType: String,
        currency: String,
        paidAmountInput: Double,
        dueDaysAhead: Int,
        signaturePath: String,
        isGiveawayEntry: Boolean,
        onSuccess: (SaleEntity) -> Unit,
        onError: (String) -> Unit
    ) -> Unit
) {
    val strings = LocalKasaStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedCustomer by remember(state.customers) { mutableStateOf(state.customers.firstOrNull()) }
    var selectedProduct by remember(state.products) { mutableStateOf(state.products.firstOrNull()) }
    var customerExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }

    var quantity by remember { mutableIntStateOf(1) }
    var saleType by remember { mutableStateOf(SaleType.CASH) }
    var currency by remember { mutableStateOf(CurrencyCode.USD) }
    var paidInputText by remember { mutableStateOf("0") }
    var dueDaysText by remember { mutableStateOf("30") }
    var signaturePath by remember { mutableStateOf("") }
    var showSignatureDialog by remember { mutableStateOf(false) }
    var isGiveawayEntry by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastCreatedSale by remember { mutableStateOf(state.lastCreatedSale) }

    val unitPrice = selectedProduct?.unitPrice(currency) ?: 0.0
    val unitCost = selectedProduct?.unitCost(currency) ?: 0.0
    val total = unitPrice * quantity
    val profit = (unitPrice - unitCost) * quantity
    val paidAmount = if (saleType == SaleType.CASH) {
        total
    } else {
        (paidInputText.toDoubleOrNull() ?: 0.0).coerceIn(0.0, total)
    }
    val remaining = (total - paidAmount).coerceAtLeast(0.0)
    val stockInsufficient = selectedProduct != null && selectedProduct!!.stockQuantity < quantity
    val negativeProfit = selectedProduct != null && profit < 0.0
    val canSubmit = selectedCustomer != null && selectedProduct != null && !stockInsufficient && !negativeProfit

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = strings.newSaleTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = SovereignGold,
                fontWeight = FontWeight.Bold
            )
        }

        // Customer Dropdown
        item {
            ExposedDropdownMenuBox(
                expanded = customerExpanded,
                onExpandedChange = { customerExpanded = !customerExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCustomer?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.selectCustomer) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("sale_customer_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = customerExpanded,
                    onDismissRequest = { customerExpanded = false }
                ) {
                    state.customers.forEach { customer ->
                        DropdownMenuItem(
                            text = { Text("${customer.name} (${customer.source})") },
                            onClick = {
                                selectedCustomer = customer
                                customerExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Product Dropdown
        item {
            ExposedDropdownMenuBox(
                expanded = productExpanded,
                onExpandedChange = { productExpanded = !productExpanded }
            ) {
                OutlinedTextField(
                    value = selectedProduct?.let { "${it.name} [${it.category}] (Stock: ${it.stockQuantity})" } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(strings.selectProduct) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = productExpanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                        .testTag("sale_product_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = productExpanded,
                    onDismissRequest = { productExpanded = false }
                ) {
                    state.products.forEach { product ->
                        DropdownMenuItem(
                            text = { Text("${product.name} · ${product.category} (Stock: ${product.stockQuantity})") },
                            onClick = {
                                selectedProduct = product
                                productExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Quantity Stepper & Currency Radio
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(strings.quantity, style = MaterialTheme.typography.titleMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                modifier = Modifier.testTag("qty_minus_button")
                            ) {
                                Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                            }
                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.titleLarge,
                                color = SovereignGold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { quantity++ },
                                modifier = Modifier.testTag("qty_plus_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        listOf(CurrencyCode.USD, CurrencyCode.CDF).forEach { code ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { currency = code }
                                    .testTag("currency_radio_$code")
                            ) {
                                RadioButton(selected = currency == code, onClick = { currency = code })
                                CurrencyBadge(code)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { saleType = SaleType.CASH }
                                .testTag("sale_type_cash_radio")
                        ) {
                            RadioButton(selected = saleType == SaleType.CASH, onClick = { saleType = SaleType.CASH })
                            Text(strings.saleTypeCash, style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { saleType = SaleType.INSTALLMENT }
                                .testTag("sale_type_installment_radio")
                        ) {
                            RadioButton(
                                selected = saleType == SaleType.INSTALLMENT,
                                onClick = { saleType = SaleType.INSTALLMENT }
                            )
                            Text(strings.saleTypeInstallment, style = MaterialTheme.typography.bodyMedium)
                        }
                    }

                    if (saleType == SaleType.INSTALLMENT) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = paidInputText,
                                onValueChange = { paidInputText = it },
                                label = { Text("${strings.initialPaidAmount} ($currency)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("initial_paid_input")
                            )
                            OutlinedTextField(
                                value = dueDaysText,
                                onValueChange = { dueDaysText = it },
                                label = { Text(strings.dueDateLabel) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("due_days_input")
                            )
                        }
                    }
                }
            }
        }

        // Auto-Calculated Summary & Guard Warnings
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.autoTotal)
                        Text(
                            text = formatCurrency(total, currency),
                            color = SovereignGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.autoProfit)
                        Text(
                            text = formatCurrency(profit, currency),
                            color = if (negativeProfit) OverdueCrimson else UsdEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(strings.remainingBalance)
                        Text(
                            text = formatCurrency(remaining, currency),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (negativeProfit) {
                        Text(
                            text = strings.errorNegativeProfit,
                            color = OverdueCrimson,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (stockInsufficient) {
                        Text(
                            text = strings.errorInsufficientStock,
                            color = OverdueCrimson,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Digital Signature Pad Button & V5 Giveaway Checkbox
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
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
                            Text(strings.signatureLabel, style = MaterialTheme.typography.titleMedium)
                            if (signaturePath.isNotBlank()) {
                                Text(
                                    text = strings.signatureSaved,
                                    color = UsdEmerald,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { showSignatureDialog = true },
                            modifier = Modifier.testTag("open_signature_pad_button")
                        ) {
                            Icon(Icons.Filled.Draw, contentDescription = strings.openSignaturePad)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.openSignaturePad)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGiveawayEntry = !isGiveawayEntry }
                            .testTag("giveaway_checkbox_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isGiveawayEntry,
                            onCheckedChange = { isGiveawayEntry = it },
                            modifier = Modifier.testTag("giveaway_checkbox")
                        )
                        Text(
                            text = strings.giveawayCheckbox,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SovereignGold
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            item {
                Text(
                    text = errorMessage ?: "",
                    color = OverdueCrimson,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Submit Sale + Share PDF Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val cust = selectedCustomer
                        val prod = selectedProduct
                        if (cust == null || prod == null) {
                            errorMessage = strings.errorSelectCustomerProduct
                            return@Button
                        }
                        errorMessage = null
                        val dueDays = dueDaysText.toIntOrNull() ?: 30
                        onCreateSale(
                            cust,
                            prod,
                            quantity,
                            saleType,
                            currency,
                            paidAmount,
                            dueDays,
                            signaturePath,
                            isGiveawayEntry,
                            { created ->
                                lastCreatedSale = created
                            },
                            { err ->
                                errorMessage = err
                            }
                        )
                    },
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("confirm_sale_button")
                ) {
                    Text(strings.confirmSale, fontWeight = FontWeight.Bold)
                }

                if (lastCreatedSale != null) {
                    OutlinedButton(
                        onClick = {
                            val sale = lastCreatedSale ?: return@OutlinedButton
                            scope.launch {
                                val cust = state.customers.find { it.id == sale.customerId }
                                val uri = PdfGenerator.generateSaleInvoicePdf(context, sale, cust)
                                context.startActivity(PdfGenerator.buildSharePdfIntent(uri))
                            }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("sale_pdf_button")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = strings.generatePdf)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.generatePdf)
                    }
                }
            }
        }
    }

    if (showSignatureDialog) {
        SignaturePadDialog(
            onDismiss = { showSignatureDialog = false },
            onSaveBitmap = { bitmap ->
                val sigDir = File(context.filesDir, "signatures").apply { mkdirs() }
                val sigFile = File(sigDir, "sig_${System.currentTimeMillis()}.png")
                FileOutputStream(sigFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                signaturePath = sigFile.absolutePath
                showSignatureDialog = false
            }
        )
    }
}

@Composable
fun SignaturePadDialog(
    onDismiss: () -> Unit,
    onSaveBitmap: (Bitmap) -> Unit
) {
    val strings = LocalKasaStrings.current
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasSize by remember { mutableStateOf(IntSize(600, 300)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.signatureLabel) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.5.dp, SovereignGold, RoundedCornerShape(12.dp))
                        .onSizeChanged { canvasSize = it }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentStroke = listOf(offset)
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    currentStroke = currentStroke + change.position
                                },
                                onDragEnd = {
                                    if (currentStroke.isNotEmpty()) {
                                        strokes.add(currentStroke)
                                        currentStroke = emptyList()
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val all = strokes + listOf(currentStroke)
                        all.forEach { pts ->
                            if (pts.size > 1) {
                                val path = Path().apply {
                                    moveTo(pts.first().x, pts.first().y)
                                    for (i in 1 until pts.size) {
                                        lineTo(pts[i].x, pts[i].y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(0xFF0B3D2E),
                                    style = Stroke(
                                        width = 5f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = canvasSize.width.coerceAtLeast(300)
                    val h = canvasSize.height.coerceAtLeast(180)
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val androidCanvas = android.graphics.Canvas(bmp)
                    androidCanvas.drawColor(android.graphics.Color.WHITE)
                    val paint = AndroidPaint().apply {
                        color = android.graphics.Color.parseColor("#0B3D2E")
                        strokeWidth = 6f
                        style = AndroidPaint.Style.STROKE
                        strokeCap = AndroidPaint.Cap.ROUND
                        strokeJoin = AndroidPaint.Join.ROUND
                        isAntiAlias = true
                    }
                    strokes.forEach { pts ->
                        if (pts.size > 1) {
                            val p = AndroidPath().apply {
                                moveTo(pts.first().x, pts.first().y)
                                for (i in 1 until pts.size) {
                                    lineTo(pts[i].x, pts[i].y)
                                }
                            }
                            androidCanvas.drawPath(p, paint)
                        }
                    }
                    onSaveBitmap(bmp)
                },
                modifier = Modifier.testTag("save_signature_button")
            ) {
                Text(strings.saveSignature)
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        strokes.clear()
                        currentStroke = emptyList()
                    }
                ) {
                    Text(strings.clearSignature)
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.cancel)
                }
            }
        }
    )
}
