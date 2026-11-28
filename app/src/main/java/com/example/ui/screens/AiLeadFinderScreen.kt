package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ai.AiLeadProfile
import com.example.data.local.CustomerStatus
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.CdfCobalt
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald
import com.example.ui.theme.UsdEmeraldContainer

@Composable
fun AiLeadFinderScreen(
    state: KasaUiState,
    onSearchLeads: (targetArea: String, productInterest: String, budgetCapability: String) -> Unit,
    onGenerateSwahiliPitch: (leadId: String) -> Unit,
    onSavePotentialLead: (AiLeadProfile) -> Unit
) {
    val strings = LocalKasaStrings.current
    val isArabic = LocalLayoutDirection.current == LayoutDirection.Rtl
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var targetArea by remember { mutableStateOf("Musompo, Kolwezi") }
    var productInterest by remember { mutableStateOf("Mixeur & Appareils de Cuisine") }
    var budgetCapability by remember { mutableStateOf("20$ - 40$ / mois (Acompte + Versements)") }
    var showCsvExportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    val potentialCount = remember(state.customers) {
        state.customers.count { it.status == CustomerStatus.POTENTIAL }
    }

    val quickAreas = listOf("Musompo", "Dilala", "Manika", "Golf ISTM", "Kasulo")
    val quickProducts = listOf(
        "Mixeurs / Blenders",
        "Aspirateurs / Cleaning",
        "Batterie Cuisine",
        "Fer Vapeur Maison"
    )
    val quickBudgets = listOf(
        "15$ - 25$ / sem.",
        "30$ - 50$ / mois",
        "60,000 - 150,000 CDF / mois"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = SovereignGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SovereignGold
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                icon = { Icon(Icons.Filled.PersonSearch, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text(if (isArabic) "البحث" else "Prospection", maxLines = 1) },
                modifier = Modifier.testTag("lead_tab_finder")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                icon = { Icon(Icons.Filled.QueryStats, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text(if (isArabic) "تحليلات" else "Leads Analytics", maxLines = 1) },
                modifier = Modifier.testTag("lead_tab_analytics")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                icon = { Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(18.dp)) },
                text = { Text(if (isArabic) "خريطة" else "Lead Map", maxLines = 1) },
                modifier = Modifier.testTag("lead_tab_map")
            )
        }

        when (selectedTab) {
            1 -> LeadsAnalyticsSection(
                state = state,
                isArabic = isArabic
            )

            2 -> LeadMapSection(
                state = state,
                isArabic = isArabic,
                onSavePotentialLead = onSavePotentialLead
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Screen Header
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.45f)),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(SovereignGold.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PersonSearch,
                                        contentDescription = "AI Lead Finder",
                                        tint = SovereignGold
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isArabic) {
                                            "البحث الذكي عن الزبائن · AI Lead Finder"
                                        } else {
                                            "AI Lead Finder · البحث الذكي عن الزبائن"
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = SovereignGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isArabic) {
                                            "اكتشاف عملاء كولويزي ذوي نية الشراء العالية بالتقسيط وتوليد رسائل واتساب بالسواحيلية"
                                        } else {
                                            "Prospection intelligente Kolwezi à forte intention d'achat à tempérament + pitch Swahili"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = { showCsvExportDialog = true },
                                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("lead_finder_export_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TableChart,
                                    contentDescription = "Export Potential Leads CSV",
                                    tint = SovereignGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isArabic) {
                                        "تصدير العملاء المحتملين CSV ($potentialCount)"
                                    } else {
                                        "Exporter Prospects POTENTIAL en CSV ($potentialCount)"
                                    },
                                    color = SovereignGold,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

        // Search Criteria Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetArea,
                        onValueChange = { targetArea = it },
                        label = {
                            Text(if (isArabic) "المنطقة المستهدفة (كولويزي)" else "Zone Cible (Quartiers Kolwezi)")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lead_area_input")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickAreas.forEach { area ->
                            FilterChip(
                                selected = targetArea.contains(area, ignoreCase = true),
                                onClick = { targetArea = "$area, Kolwezi" },
                                label = { Text(area) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SovereignGold.copy(alpha = 0.22f),
                                    selectedLabelColor = SovereignGold
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = productInterest,
                        onValueChange = { productInterest = it },
                        label = {
                            Text(if (isArabic) "الأجهزة المطلوبة" else "Intérêt Produit (Maison / Cuisine)")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lead_product_input")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickProducts.forEach { prod ->
                            FilterChip(
                                selected = productInterest == prod,
                                onClick = { productInterest = prod },
                                label = { Text(prod) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = budgetCapability,
                        onValueChange = { budgetCapability = it },
                        label = {
                            Text(if (isArabic) "القدرة المالية / ميزانية التقسيط" else "Budget / Capacité d'échelonnement")
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lead_budget_input")
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickBudgets.forEach { budget ->
                            FilterChip(
                                selected = budgetCapability == budget,
                                onClick = { budgetCapability = budget },
                                label = { Text(budget) }
                            )
                        }
                    }

                    Button(
                        onClick = { onSearchLeads(targetArea, productInterest, budgetCapability) },
                        enabled = !state.isSearchingLeads && targetArea.isNotBlank() && productInterest.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lead_search_button")
                    ) {
                        if (state.isSearchingLeads) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isArabic) "جاري البحث بالذكاء الاصطناعي..." else "Analyse IA des signaux Kolwezi..."
                            )
                        } else {
                            Icon(Icons.Filled.PersonSearch, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isArabic) "بحث ذكي عن الزبائن (Search)" else "Rechercher Prospects (Search)"
                            )
                        }
                    }
                }
            }
        }

        if (state.leadSearchError != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = OverdueCrimson.copy(alpha = 0.14f)),
                    border = BorderStroke(1.dp, OverdueCrimson.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = state.leadSearchError,
                        color = OverdueCrimson,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        if (state.discoveredLeads.isEmpty() && !state.isSearchingLeads) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = if (isArabic) {
                            "اختر المنطقة والمنتج والميزانية ثم اضغط على بحث ذكي لاكتشاف عملاء محتملين في كولويزي."
                        } else {
                            "Renseignez le quartier, l'équipement et la capacité de paiement puis lancez la recherche IA."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(state.discoveredLeads, key = { it.id }) { lead ->
                val isGeneratingPitch = lead.id in state.generatingPitchLeadIds
                val isSaved = lead.id in state.savedLeadIds
                LeadResultCard(
                    lead = lead,
                    isArabic = isArabic,
                    isGeneratingPitch = isGeneratingPitch,
                    isSaved = isSaved,
                    onGeneratePitch = { onGenerateSwahiliPitch(lead.id) },
                    onSaveLead = { onSavePotentialLead(lead) },
                    onCopyPitch = { pitch ->
                        clipboard.setText(AnnotatedString(pitch))
                    },
                    onSendWhatsApp = { pitch ->
                        openWhatsAppMessage(context, lead.phone, pitch)
                    }
                )
            }
        }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showCsvExportDialog) {
        PotentialLeadsCsvExportDialog(
            potentialCount = potentialCount,
            isArabic = isArabic,
            onDismiss = { showCsvExportDialog = false },
            onExported = { msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        )
    }
}

@Composable
private fun LeadResultCard(
    lead: AiLeadProfile,
    isArabic: Boolean,
    isGeneratingPitch: Boolean,
    isSaved: Boolean,
    onGeneratePitch: () -> Unit,
    onSaveLead: () -> Unit,
    onCopyPitch: (String) -> Unit,
    onSendWhatsApp: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lead_card_${lead.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.35f)),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = lead.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = SovereignGold,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = lead.neighborhood,
                            tint = CdfCobalt,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${lead.neighborhood} · ${lead.phone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(UsdEmeraldContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${lead.intentScore}% Intent",
                        style = MaterialTheme.typography.labelSmall,
                        color = UsdEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = lead.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = PromiseAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${lead.productInterest} · ${lead.installmentBudget}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = lead.signal,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onGeneratePitch,
                    enabled = !isGeneratingPitch,
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("generate_swahili_pitch_${lead.id}")
                ) {
                    if (isGeneratingPitch) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            Icons.Filled.Translate,
                            contentDescription = "Generate Swahili Pitch",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "رسالة بالسواحيلية" else "Generate Swahili Pitch",
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onSaveLead,
                    enabled = !isSaved,
                    modifier = Modifier.testTag("save_lead_button_${lead.id}")
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Filled.BookmarkAdded else Icons.Filled.PersonAdd,
                        contentDescription = "Save Lead",
                        tint = if (isSaved) UsdEmerald else SovereignGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSaved) {
                            if (isArabic) "محفوظ POTENTIAL" else "Saved POTENTIAL"
                        } else {
                            if (isArabic) "حفظ كعميل" else "Save Lead"
                        },
                        color = if (isSaved) UsdEmerald else SovereignGold,
                        maxLines = 1
                    )
                }
            }

            if (!lead.swahiliPitch.isNullOrBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "WhatsApp Pitch (Kiswahili · Kolwezi)",
                            style = MaterialTheme.typography.labelLarge,
                            color = UsdEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = lead.swahiliPitch,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onCopyPitch(lead.swahiliPitch) }) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy Swahili Pitch",
                                    tint = SovereignGold
                                )
                            }
                            Button(
                                onClick = { onSendWhatsApp(lead.swahiliPitch) },
                                colors = ButtonDefaults.buttonColors(containerColor = UsdEmerald)
                            ) {
                                Icon(
                                    Icons.Filled.Share,
                                    contentDescription = "Send WhatsApp",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp")
                            }
                        }
                    }
                }
            }
        }
    }
}
