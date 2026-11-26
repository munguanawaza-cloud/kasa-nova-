package com.example.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object TimeUtils {
    private val lubumbashiTz: TimeZone = TimeZone.getTimeZone("Africa/Lubumbashi")
    const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    const val EARLY_BIRD_DAYS_THRESHOLD = 5L

    // Golf ISTM, Kolwezi coordinates
    const val GOLF_ISTM_LAT = -10.7167
    const val GOLF_ISTM_LNG = 25.4667
    const val RADAR_ZONE_RADIUS_METERS = 25_000.0
    const val RADAR_HIGHLIGHT_KM = 15.0

    fun nowMillis(): Long = System.currentTimeMillis()

    fun todayKey(millis: Long = nowMillis()): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = lubumbashiTz
        return sdf.format(Date(millis))
    }

    fun formatDateTime(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)
        sdf.timeZone = lubumbashiTz
        return sdf.format(Date(millis))
    }

    fun formatDateOnly(millis: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        sdf.timeZone = lubumbashiTz
        return sdf.format(Date(millis))
    }

    fun defaultDueDateMillis(fromMillis: Long = nowMillis(), daysAhead: Int = 30): Long {
        return fromMillis + daysAhead.toLong() * MILLIS_PER_DAY
    }

    fun isEarlyBirdEligible(paymentMillis: Long, dueDateMillis: Long): Boolean {
        if (dueDateMillis <= 0L) return false
        return (dueDateMillis - paymentMillis) >= EARLY_BIRD_DAYS_THRESHOLD * MILLIS_PER_DAY
    }

    fun isOverdue(dueDateMillis: Long, nowMillis: Long = nowMillis()): Boolean {
        return dueDateMillis > 0L && nowMillis > dueDateMillis
    }

    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusKm * c
    }

    fun distanceKmFromGolfIstm(lat: Double, lng: Double): Double {
        return distanceKm(GOLF_ISTM_LAT, GOLF_ISTM_LNG, lat, lng)
    }
}
