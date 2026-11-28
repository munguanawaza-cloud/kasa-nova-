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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.AiLeadProfile
import com.example.data.local.CustomerStatus
import com.example.ui.KasaUiState
import com.example.ui.theme.CdfCobalt
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald
import com.example.ui.theme.UsdEmeraldContainer
import com.example.utils.TimeUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private enum class LeadMapFilter {
    ALL,
    POTENTIAL,
    AI_DISCOVERED,
    CONVERTED
}

private sealed class LeadMapPin {
    abstract val key: String
    abstract val title: String
    abstract val subtitle: String
    abstract val phone: String
    abstract val source: String
    abstract val lat: Double
    abstract val lng: Double

    data class SavedPotential(
        val id: Long,
        override val title: String,
        override val subtitle: String,
        override val phone: String,
        override val source: String,
        override val lat: Double,
        override val lng: Double
    ) : LeadMapPin() {
        override val key: String = "potential_$id"
    }

    data class AiDiscovered(
        val profile: AiLeadProfile,
        val isSaved: Boolean
    ) : LeadMapPin() {
        override val key: String = "ai_${profile.id}"
        override val title: String = profile.name
        override val subtitle: String = "${profile.neighborhood} · ${profile.productInterest} (${profile.intentScore}%)"
        override val phone: String = profile.phone
        override val source: String = profile.source
        override val lat: Double = profile.gpsLat
        override val lng: Double = profile.gpsLng
    }

    data class ConvertedCustomer(
        val id: Long,
        override val title: String,
        override val subtitle: String,
        override val phone: String,
        override val source: String,
        val loyaltyPoints: Int,
        override val lat: Double,
        override val lng: Double
    ) : LeadMapPin() {
        override val key: String = "active_$id"
    }
}

@Composable
fun LeadMapSection(
    state: KasaUiState,
    isArabic: Boolean,
    onSavePotentialLead: (AiLeadProfile) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val kolweziCenter = remember { LatLng(TimeUtils.GOLF_ISTM_LAT, TimeUtils.GOLF_ISTM_LNG) }

    var activeFilter by remember { mutableStateOf(LeadMapFilter.ALL) }
    var selectedPin by remember { mutableStateOf<LeadMapPin?>(null) }

    val potentialPins = remember(state.customers) {
        state.customers
            .filter { it.status == CustomerStatus.POTENTIAL }
            .map {
                LeadMapPin.SavedPotential(
                    id = it.id,
                    title = it.name,
                    subtitle = it.address,
                    phone = it.phone,
                    source = it.source,
                    lat = it.gpsLat,
                    lng = it.gpsLng
                )
            }
    }

    val aiPins = remember(state.discoveredLeads, state.savedLeadIds) {
        state.discoveredLeads.map { lead ->
            LeadMapPin.AiDiscovered(
                profile = lead,
                isSaved = lead.id in state.savedLeadIds
            )
        }
    }

    val convertedPins = remember(state.customers) {
        state.customers
            .filter { it.status == CustomerStatus.ACTIVE }
            .map {
                LeadMapPin.ConvertedCustomer(
                    id = it.id,
                    title = it.name,
                    subtitle = it.address,
                    phone = it.phone,
                    source = it.source,
                    loyaltyPoints = it.loyaltyPoints,
                    lat = it.gpsLat,
                    lng = it.gpsLng
                )
            }
    }

    val visiblePins = remember(activeFilter, potentialPins, aiPins, convertedPins) {
        when (activeFilter) {
            LeadMapFilter.ALL -> aiPins + potentialPins + convertedPins
            LeadMapFilter.POTENTIAL -> potentialPins
            LeadMapFilter.AI_DISCOVERED -> aiPins
            LeadMapFilter.CONVERTED -> convertedPins
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        val initialTarget = visiblePins.firstOrNull()?.let { LatLng(it.lat, it.lng) } ?: kolweziCenter
        position = CameraPosition.fromLatLngZoom(initialTarget, 12.2f)
    }

    val kolweziNeighborhoods = remember {
        listOf(
            "Golf ISTM" to LatLng(TimeUtils.GOLF_ISTM_LAT, TimeUtils.GOLF_ISTM_LNG),
            "Musompo" to LatLng(-10.7310, 25.4480),
            "Dilala" to LatLng(-10.7112, 25.4615),
            "Manika" to LatLng(-10.7235, 25.4920)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("lead_map_container")
    ) {
        // Filter Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalCount = potentialPins.size + aiPins.size + convertedPins.size
            FilterChip(
                selected = activeFilter == LeadMapFilter.ALL,
                onClick = { activeFilter = LeadMapFilter.ALL },
                label = { Text(if (isArabic) "الكل ($totalCount)" else "Tous ($totalCount)") },
                modifier = Modifier.testTag("lead_map_filter_all")
            )
            FilterChip(
                selected = activeFilter == LeadMapFilter.POTENTIAL,
                onClick = { activeFilter = LeadMapFilter.POTENTIAL },
                label = { Text("POTENTIAL (${potentialPins.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PromiseAmber.copy(alpha = 0.22f),
                    selectedLabelColor = PromiseAmber
                ),
                modifier = Modifier.testTag("lead_map_filter_potential")
            )
            FilterChip(
                selected = activeFilter == LeadMapFilter.AI_DISCOVERED,
                onClick = { activeFilter = LeadMapFilter.AI_DISCOVERED },
                label = { Text("AI Leads (${aiPins.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CdfCobalt.copy(alpha = 0.22f),
                    selectedLabelColor = CdfCobalt
                ),
                modifier = Modifier.testTag("lead_map_filter_ai")
            )
            FilterChip(
                selected = activeFilter == LeadMapFilter.CONVERTED,
                onClick = { activeFilter = LeadMapFilter.CONVERTED },
                label = { Text(if (isArabic) "عملاء فعليون (${convertedPins.size})" else "Convertis (${convertedPins.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = UsdEmerald.copy(alpha = 0.22f),
                    selectedLabelColor = UsdEmerald
                ),
                modifier = Modifier.testTag("lead_map_filter_converted")
            )
        }

        // Quick Neighborhood Pan Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            kolweziNeighborhoods.forEach { (label, coords) ->
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(coords, 13.0f))
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(label, style = MaterialTheme.typography.labelSmall, color = SovereignGold)
                }
            }
        }

        // Interactive Google Map
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("lead_google_map"),
                cameraPositionState = cameraPositionState
            ) {
                Circle(
                    center = kolweziCenter,
                    radius = TimeUtils.RADAR_ZONE_RADIUS_METERS,
                    fillColor = Color(0x1AD4AF37),
                    strokeColor = SovereignGold.copy(alpha = 0.7f),
                    strokeWidth = 3f
                )

                visiblePins.forEach { pin ->
                    val hue = when (pin) {
                        is LeadMapPin.SavedPotential -> BitmapDescriptorFactory.HUE_YELLOW
                        is LeadMapPin.AiDiscovered -> BitmapDescriptorFactory.HUE_AZURE
                        is LeadMapPin.ConvertedCustomer -> BitmapDescriptorFactory.HUE_GREEN
                    }
                    val prefix = when (pin) {
                        is LeadMapPin.SavedPotential -> "POTENTIAL"
                        is LeadMapPin.AiDiscovered -> "AI ${pin.profile.intentScore}%"
                        is LeadMapPin.ConvertedCustomer -> "ACTIVE"
                    }
                    Marker(
                        state = MarkerState(position = LatLng(pin.lat, pin.lng)),
                        title = "[$prefix] ${pin.title}",
                        snippet = "${pin.subtitle} · ${pin.phone}",
                        icon = BitmapDescriptorFactory.defaultMarker(hue),
                        onClick = {
                            selectedPin = pin
                            false
                        }
                    )
                }
            }

            // Recenter Kolwezi FAB
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(kolweziCenter, 12.0f))
                    }
                },
                containerColor = SovereignGold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(44.dp)
                    .testTag("lead_map_recenter_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "Recenter Kolwezi",
                    tint = Color.Black
                )
            }

            // Selected Pin Quick-Action Card
            val currentSelected = selectedPin
            if (currentSelected != null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(12.dp)
                        .testTag("lead_map_selected_card"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.65f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSelected.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${currentSelected.subtitle} · ${currentSelected.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            PinStatusBadge(currentSelected)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { openDialer(context, currentSelected.phone) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp), tint = UsdEmerald)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isArabic) "اتصال" else "Appeler", color = UsdEmerald)
                            }
                            Button(
                                onClick = {
                                    val defaultMsg = if (currentSelected is LeadMapPin.AiDiscovered && !currentSelected.profile.swahiliPitch.isNullOrBlank()) {
                                        currentSelected.profile.swahiliPitch
                                    } else {
                                        "Habari ${currentSelected.title}, hapa ni KASA NOVA · HAMADA GROUP Kolwezi. Tunapatikana kwa malipo ya awamu!"
                                    }
                                    openWhatsAppMessage(context, currentSelected.phone, defaultMsg)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = UsdEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp")
                            }
                            if (currentSelected is LeadMapPin.AiDiscovered) {
                                IconButton(
                                    onClick = { onSavePotentialLead(currentSelected.profile) },
                                    enabled = !currentSelected.isSaved
                                ) {
                                    Icon(
                                        imageVector = if (currentSelected.isSaved) Icons.Filled.BookmarkAdded else Icons.Filled.PersonAdd,
                                        contentDescription = "Save Lead",
                                        tint = if (currentSelected.isSaved) UsdEmerald else SovereignGold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Pin Strip for fast map navigation
        if (visiblePins.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(visiblePins, key = { it.key }) { pin ->
                    Card(
                        onClick = {
                            selectedPin = pin
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(pin.lat, pin.lng), 14.0f)
                                )
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPin?.key == pin.key) {
                                SovereignGold.copy(alpha = 0.16f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (selectedPin?.key == pin.key) SovereignGold else Color.Transparent
                        ),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            PinStatusBadge(pin)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pin.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = pin.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinStatusBadge(pin: LeadMapPin) {
    val (label, bg, fg) = when (pin) {
        is LeadMapPin.SavedPotential -> Triple("POTENTIAL", PromiseAmber.copy(alpha = 0.2f), PromiseAmber)
        is LeadMapPin.AiDiscovered -> Triple("AI ${pin.profile.intentScore}%", CdfCobalt.copy(alpha = 0.2f), CdfCobalt)
        is LeadMapPin.ConvertedCustomer -> Triple("ACTIVE", UsdEmeraldContainer, UsdEmerald)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}
