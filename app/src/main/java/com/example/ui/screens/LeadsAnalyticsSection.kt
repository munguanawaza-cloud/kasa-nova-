package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerSource
import com.example.data.local.CustomerStatus
import com.example.data.local.SaleEntity
import com.example.ui.KasaUiState
import com.example.ui.theme.CdfCobalt
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald
import com.example.ui.theme.UsdEmeraldContainer
import com.example.utils.TimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

private data class DailyLeadConversionPoint(
    val dayIndex: Int,
    val dayKey: String,
    val shortLabel: String,
    val dailyPotential: Int,
    val dailyConverted: Int,
    val cumulativeCohort: Int,
    val cumulativeConverted: Int,
    val conversionRatePct: Float
)

private data class SourceConversionStat(
    val source: String,
    val totalCount: Int,
    val convertedCount: Int,
    val ratePct: Float
)

private data class LeadsAnalyticsSummary(
    val points: List<DailyLeadConversionPoint>,
    val totalCohort30d: Int,
    val potentialLeads30d: Int,
    val convertedCustomers30d: Int,
    val overallConversionPct: Float,
    val sourceStats: List<SourceConversionStat>
)

@Composable
fun LeadsAnalyticsSection(
    state: KasaUiState,
    isArabic: Boolean
) {
    val summary = remember(state.customers, state.sales) {
        build30DayLeadsAnalytics(state.customers, state.sales)
    }
    var selectedDayIndex by remember(summary.points) {
        mutableIntStateOf((summary.points.size - 1).coerceAtLeast(0))
    }
    val selectedPoint = summary.points.getOrNull(selectedDayIndex)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("leads_analytics_dashboard"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overview Header + KPI Strip
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.4f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(UsdEmeraldContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QueryStats,
                                    contentDescription = "Leads Analytics",
                                    tint = UsdEmerald
                                )
                            }
                            Column {
                                Text(
                                    text = if (isArabic) "تحليلات تحويل الزبائن (30 يوماً)" else "Leads Analytics · 30 Jours",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic) {
                                        "نسبة تحوّل العملاء المحتملين إلى مشترين فعليين"
                                    } else {
                                        "Taux de conversion POTENTIAL → Clients actifs (Room)"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(UsdEmeraldContainer)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f%%", summary.overallConversionPct),
                                style = MaterialTheme.typography.titleMedium,
                                color = UsdEmerald,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("leads_conversion_rate_badge")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnalyticsKpiTile(
                            label = if (isArabic) "معدل التحويل" else "Conversion",
                            value = String.format(Locale.US, "%.1f%%", summary.overallConversionPct),
                            accent = UsdEmerald,
                            icon = Icons.Filled.TrendingUp,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsKpiTile(
                            label = if (isArabic) "محتملون" else "POTENTIAL",
                            value = summary.potentialLeads30d.toString(),
                            accent = PromiseAmber,
                            icon = Icons.Filled.Groups,
                            modifier = Modifier.weight(1f)
                        )
                        AnalyticsKpiTile(
                            label = if (isArabic) "تم تحويلهم" else "Convertis",
                            value = summary.convertedCustomers30d.toString(),
                            accent = SovereignGold,
                            icon = Icons.Filled.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 30-Day Conversion Rate Area + Volume Chart
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "منحنى التحويل اليومي (آخر 30 يوماً)" else "Courbe de Conversion · 30 derniers jours",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "المس للمعاينة" else "Toucher pour inspecter",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Interactive Tooltip Box
                    if (selectedPoint != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = selectedPoint.dayKey,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SovereignGold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isArabic) {
                                        "محتملون جدد: ${selectedPoint.dailyPotential} · تحوّل: ${selectedPoint.dailyConverted}"
                                    } else {
                                        "Prospects: +${selectedPoint.dailyPotential} · Convertis: +${selectedPoint.dailyConverted}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = String.format(Locale.US, "%.1f%%", selectedPoint.conversionRatePct),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = UsdEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${selectedPoint.cumulativeConverted}/${selectedPoint.cumulativeCohort}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    ConversionAreaChart(
                        points = summary.points,
                        selectedIndex = selectedDayIndex,
                        onSelectIndex = { idx -> selectedDayIndex = idx }
                    )

                    // X-Axis Labels
                    if (summary.points.size >= 30) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 34.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(0, 9, 19, 29).forEach { idx ->
                                Text(
                                    text = summary.points[idx].shortLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Chart Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendItem(
                            color = UsdEmerald,
                            label = if (isArabic) "نسبة التحويل %" else "Taux de conversion (%)"
                        )
                        LegendItem(
                            color = SovereignGold,
                            label = if (isArabic) "حجم العملاء اليومي" else "Activité prospects / jour"
                        )
                    }
                }
            }
        }

        // Channel Conversion Breakdown
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
                    Text(
                        text = if (isArabic) "الأداء حسب القناة (آخر 30 يوماً)" else "Conversion par Canal (30 jours)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = SovereignGold
                    )
                    summary.sourceStats.forEach { stat ->
                        val barColor = when (stat.source) {
                            CustomerSource.WHATSAPP -> UsdEmerald
                            CustomerSource.FACEBOOK -> CdfCobalt
                            CustomerSource.TIKTOK -> PromiseAmber
                            else -> SovereignGold
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stat.source,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${stat.convertedCount}/${stat.totalCount} (${String.format(Locale.US, "%.0f%%", stat.ratePct)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = barColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            LinearProgressIndicator(
                                progress = { (stat.ratePct / 100f).coerceIn(0f, 1f) },
                                color = barColor,
                                trackColor = barColor.copy(alpha = 0.16f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }
                    }
                }
            }
        }

        if (summary.totalCohort30d == 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = if (isArabic) {
                            "لا توجد بيانات عملاء خلال آخر 30 يوماً بعد. ابحث واحفظ عملاء محتملين ثم سجّل مبيعاتهم لمتابعة معدل التحويل هنا."
                        } else {
                            "Aucune donnée sur les 30 derniers jours. Enregistrez des prospects depuis Prospection puis créez des ventes pour suivre le taux de conversion."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun AnalyticsKpiTile(
    label: String,
    value: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accent,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConversionAreaChart(
    points: List<DailyLeadConversionPoint>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Y-axis percentage labels
        Column(
            modifier = Modifier
                .width(34.dp)
                .height(176.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            listOf("100%", "75%", "50%", "25%", "0%").forEach { tick ->
                Text(
                    text = tick,
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }
        }
        Spacer(modifier = Modifier.width(6.dp))

        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(176.dp)
                .testTag("leads_conversion_chart_canvas")
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        if (points.isNotEmpty() && size.width > 0) {
                            val ratio = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val idx = (ratio * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
                }
                .pointerInput(points.size) {
                    detectHorizontalDragGestures { change, _ ->
                        change.consume()
                        if (points.isNotEmpty() && size.width > 0) {
                            val ratio = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            val idx = (ratio * (points.size - 1)).roundToInt().coerceIn(0, points.lastIndex)
                            onSelectIndex(idx)
                        }
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)

            // Horizontal percentage gridlines
            for (step in 0..4) {
                val y = h * (step / 4f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = if (step == 4) null else dashEffect
                )
            }

            if (points.isEmpty()) return@Canvas

            val count = points.size
            val stepX = if (count > 1) w / (count - 1).toFloat() else w
            val maxDailyActivity = points.maxOf { it.dailyPotential + it.dailyConverted }.coerceAtLeast(1)

            // Secondary Bar Series: Daily Lead / Conversion Volume
            val barWidth = (stepX * 0.48f).coerceIn(3.dp.toPx(), 10.dp.toPx())
            points.forEachIndexed { idx, pt ->
                val activity = pt.dailyPotential + pt.dailyConverted
                if (activity > 0) {
                    val barHeight = (activity.toFloat() / maxDailyActivity.toFloat()) * (h * 0.38f)
                    val cx = idx * stepX
                    drawRoundRect(
                        color = SovereignGold.copy(alpha = 0.34f),
                        topLeft = Offset((cx - barWidth / 2f).coerceIn(0f, w - barWidth), h - barHeight),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }
            }

            // Primary Area + Smooth Line Series: Conversion Rate (%)
            val coordinates = points.mapIndexed { idx, pt ->
                val x = idx * stepX
                val y = h - (pt.conversionRatePct.coerceIn(0f, 100f) / 100f) * h
                Offset(x, y)
            }

            val linePath = Path().apply {
                moveTo(coordinates.first().x, coordinates.first().y)
                for (i in 1 until coordinates.size) {
                    val prev = coordinates[i - 1]
                    val curr = coordinates[i]
                    val midX = (prev.x + curr.x) / 2f
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }

            val fillPath = Path().apply {
                addPath(linePath)
                lineTo(coordinates.last().x, h)
                lineTo(coordinates.first().x, h)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        UsdEmerald.copy(alpha = 0.34f),
                        UsdEmerald.copy(alpha = 0.02f)
                    ),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = linePath,
                color = UsdEmerald,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Selected Day Crosshair & Active Point
            val selIdx = selectedIndex.coerceIn(0, coordinates.lastIndex)
            val selPt = coordinates[selIdx]
            drawLine(
                color = SovereignGold.copy(alpha = 0.65f),
                start = Offset(selPt.x, 0f),
                end = Offset(selPt.x, h),
                strokeWidth = 1.2.dp.toPx(),
                pathEffect = dashEffect
            )
            drawCircle(
                color = SovereignGold.copy(alpha = 0.28f),
                radius = 9.dp.toPx(),
                center = selPt
            )
            drawCircle(
                color = UsdEmerald,
                radius = 5.dp.toPx(),
                center = selPt
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = selPt
            )
        }
    }
}

private fun build30DayLeadsAnalytics(
    customers: List<CustomerEntity>,
    sales: List<SaleEntity>
): LeadsAnalyticsSummary {
    val now = TimeUtils.nowMillis()
    val tz = TimeZone.getTimeZone("Africa/Lubumbashi")
    val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = tz }
    val shortFmt = SimpleDateFormat("dd/MM", Locale.US).apply { timeZone = tz }

    val windowDays = 30
    val startMillis = now - (windowDays - 1) * TimeUtils.MILLIS_PER_DAY
    val dayKeys = (0 until windowDays).map { offset ->
        val ts = startMillis + offset * TimeUtils.MILLIS_PER_DAY
        dayFmt.format(Date(ts)) to shortFmt.format(Date(ts))
    }
    val validDayKeySet = dayKeys.map { it.first }.toSet()

    // Earliest sale per customer tells us when conversion occurred if sales exist
    val earliestSaleByCustomer = sales.groupBy { it.customerId }
        .mapValues { (_, list) -> list.minByOrNull { it.createdAt }?.createdAt ?: 0L }

    data class CustomerCohortEvent(
        val customer: CustomerEntity,
        val isConverted: Boolean,
        val eventDayKey: String
    )

    val cohortEvents = customers.mapNotNull { cust ->
        val earliestSaleMillis = earliestSaleByCustomer[cust.id] ?: 0L
        val isConverted = cust.status == CustomerStatus.ACTIVE || earliestSaleMillis > 0L
        val eventTs = when {
            earliestSaleMillis > 0L -> earliestSaleMillis
            else -> cust.updatedAt
        }
        val key = dayFmt.format(Date(eventTs))
        if (key in validDayKeySet || eventTs >= startMillis - TimeUtils.MILLIS_PER_DAY) {
            val clampedKey = if (key in validDayKeySet) key else dayKeys.first().first
            CustomerCohortEvent(cust, isConverted, clampedKey)
        } else {
            null
        }
    }

    var cumulativePotential = 0
    var cumulativeConverted = 0

    val points = dayKeys.mapIndexed { idx, (dayKey, shortLabel) ->
        val dayEvents = cohortEvents.filter { it.eventDayKey == dayKey }
        val dailyPotential = dayEvents.count { !it.isConverted }
        val dailyConverted = dayEvents.count { it.isConverted }
        cumulativePotential += dailyPotential
        cumulativeConverted += dailyConverted
        val cohort = cumulativePotential + cumulativeConverted
        val rate = if (cohort == 0) 0f else (cumulativeConverted.toFloat() / cohort.toFloat()) * 100f

        DailyLeadConversionPoint(
            dayIndex = idx,
            dayKey = dayKey,
            shortLabel = shortLabel,
            dailyPotential = dailyPotential,
            dailyConverted = dailyConverted,
            cumulativeCohort = cohort,
            cumulativeConverted = cumulativeConverted,
            conversionRatePct = rate
        )
    }

    val totalCohort = cohortEvents.size
    val potentialLeads30d = cohortEvents.count { !it.isConverted }
    val converted30d = cohortEvents.count { it.isConverted }
    val overallPct = if (totalCohort == 0) 0f else (converted30d.toFloat() / totalCohort.toFloat()) * 100f

    val sourceStats = CustomerSource.ALL.map { src ->
        val srcEvents = cohortEvents.filter { it.customer.source.equals(src, ignoreCase = true) }
        val total = srcEvents.size
        val converted = srcEvents.count { it.isConverted }
        val pct = if (total == 0) 0f else (converted.toFloat() / total.toFloat()) * 100f
        SourceConversionStat(
            source = src,
            totalCount = total,
            convertedCount = converted,
            ratePct = pct
        )
    }

    return LeadsAnalyticsSummary(
        points = points,
        totalCohort30d = totalCohort,
        potentialLeads30d = potentialLeads30d,
        convertedCustomers30d = converted30d,
        overallConversionPct = overallPct,
        sourceStats = sourceStats
    )
}
