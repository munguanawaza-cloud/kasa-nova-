package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.example.data.local.CustomerStatus
import com.example.ui.KasaUiState
import com.example.ui.LocalKasaStrings
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.SovereignGoldContainer
import com.example.ui.theme.UsdEmerald
import com.example.utils.TimeUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun MapScreen(state: KasaUiState) {
    val strings = LocalKasaStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val golfIstm = remember { LatLng(TimeUtils.GOLF_ISTM_LAT, TimeUtils.GOLF_ISTM_LNG) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var agentLatLng by remember { mutableStateOf(LatLng(-10.7185, 25.4712)) }
    var radarScanActive by remember { mutableStateOf(false) }
    var onlyPotentialLeads by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            readLastKnownLatLng(context)?.let { agentLatLng = it }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            readLastKnownLatLng(context)?.let { agentLatLng = it }
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(golfIstm, 11.4f)
    }

    val displayedCustomers = remember(state.customers, onlyPotentialLeads) {
        if (onlyPotentialLeads) {
            state.customers.filter { it.status == CustomerStatus.POTENTIAL }
        } else {
            state.customers
        }
    }

    val potentialCount = remember(state.customers) {
        state.customers.count { it.status == CustomerStatus.POTENTIAL }
    }

    val radarCustomers = remember(displayedCustomers) {
        displayedCustomers.filter {
            TimeUtils.distanceKmFromGolfIstm(it.gpsLat, it.gpsLng) > TimeUtils.RADAR_HIGHLIGHT_KM
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header + Radar Button + Location Permission Banner
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.mapTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = SovereignGold,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        radarScanActive = !radarScanActive
                        val target = if (radarScanActive && radarCustomers.isNotEmpty()) {
                            LatLng(radarCustomers.first().gpsLat, radarCustomers.first().gpsLng)
                        } else {
                            golfIstm
                        }
                        val zoom = if (radarScanActive) 10.6f else 11.4f
                        scope.launch {
                            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(target, zoom))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (radarScanActive) UsdEmerald else SovereignGold
                    ),
                    modifier = Modifier.testTag("map_radar_button")
                ) {
                    Icon(Icons.Filled.Radar, contentDescription = strings.radarButtonLabel)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.radarButtonLabel, fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !onlyPotentialLeads,
                    onClick = { onlyPotentialLeads = false },
                    label = { Text("All (${state.customers.size})") }
                )
                FilterChip(
                    selected = onlyPotentialLeads,
                    onClick = { onlyPotentialLeads = true },
                    label = { Text("Lead Map · POTENTIAL ($potentialCount)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PromiseAmber.copy(alpha = 0.22f),
                        selectedLabelColor = PromiseAmber
                    ),
                    modifier = Modifier.testTag("map_filter_potential_leads")
                )
            }

            if (!hasLocationPermission) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = strings.locationPermissionNeeded,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        },
                        modifier = Modifier.testTag("grant_location_button")
                    ) {
                        Text(strings.grantLocation)
                    }
                }
            }
        }

        // Google Map Compose with 25km Circle around Golf ISTM
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
            ) {
                // 25km Radius around Golf ISTM (-10.7167, 25.4667)
                Circle(
                    center = golfIstm,
                    radius = TimeUtils.RADAR_ZONE_RADIUS_METERS,
                    fillColor = Color(0x22D4AF37),
                    strokeColor = SovereignGold,
                    strokeWidth = 4f
                )

                // Golf ISTM Center Anchor
                Marker(
                    state = remember { MarkerState(position = golfIstm) },
                    title = "Golf ISTM, Kolwezi",
                    snippet = "Centre KASA NOVA (Rayon 25 km)",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )

                // Agent Live GPS Marker
                Marker(
                    state = MarkerState(position = agentLatLng),
                    title = "Agent KASA NOVA",
                    snippet = "GPS Live",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )

                // Customer & Lead Markers
                displayedCustomers.forEach { customer ->
                    val distKm = TimeUtils.distanceKmFromGolfIstm(customer.gpsLat, customer.gpsLng)
                    val isRadarCustomer = distKm > TimeUtils.RADAR_HIGHLIGHT_KM
                    val isPotentialLead = customer.status == CustomerStatus.POTENTIAL
                    Marker(
                        state = MarkerState(position = LatLng(customer.gpsLat, customer.gpsLng)),
                        title = when {
                            isPotentialLead -> "[LEAD] ${customer.name}"
                            isRadarCustomer -> "★ ${strings.radarCustomerTag}: ${customer.name}"
                            else -> customer.name
                        },
                        snippet = "${customer.address} · ${customer.source} · ${String.format(Locale.US, "%.1f km", distKm)}",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            when {
                                isPotentialLead -> BitmapDescriptorFactory.HUE_CYAN
                                isRadarCustomer -> BitmapDescriptorFactory.HUE_YELLOW
                                else -> BitmapDescriptorFactory.HUE_ORANGE
                            }
                        )
                    )
                }
            }
        }

        // Radar Customers Strip (> 15 km away highlighted with Star)
        if (radarCustomers.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(radarCustomers, key = { it.id }) { cust ->
                    val distKm = TimeUtils.distanceKmFromGolfIstm(cust.gpsLat, cust.gpsLng)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SovereignGoldContainer),
                        shape = MaterialTheme.shapes.small,
                        onClick = {
                            scope.launch {
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(LatLng(cust.gpsLat, cust.gpsLng), 12.5f)
                                )
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = strings.radarCustomerTag,
                                tint = SovereignGold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = strings.radarCustomerTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${cust.name} (${String.format(Locale.US, "%.1f km", distKm)})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
private fun readLastKnownLatLng(context: Context): LatLng? {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
    for (p in providers) {
        val loc = runCatching { lm.getLastKnownLocation(p) }.getOrNull()
        if (loc != null) return LatLng(loc.latitude, loc.longitude)
    }
    return null
}
