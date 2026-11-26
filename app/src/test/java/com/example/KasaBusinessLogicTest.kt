package com.example

import com.example.data.local.CurrencyCode
import com.example.data.local.ProductCategory
import com.example.data.local.ProductEntity
import com.example.utils.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KasaBusinessLogicTest {

    @Test
    fun productCategoriesAreStrictlyCuisineBeauteMaison() {
        assertEquals(listOf("Cuisine", "Beauté", "Maison"), ProductCategory.ALL)
        assertFalse(ProductCategory.ALL.any { it.contains("Solar", ignoreCase = true) })
    }

    @Test
    fun earlyBirdDiscountTriggersOnlyAtLeastFiveDaysBeforeDueDate() {
        val now = 1_764_000_000_000L
        val dueIn6Days = now + 6L * TimeUtils.MILLIS_PER_DAY
        val dueIn5Days = now + 5L * TimeUtils.MILLIS_PER_DAY
        val dueIn4Days = now + 4L * TimeUtils.MILLIS_PER_DAY

        assertTrue(TimeUtils.isEarlyBirdEligible(now, dueIn6Days))
        assertTrue(TimeUtils.isEarlyBirdEligible(now, dueIn5Days))
        assertFalse(TimeUtils.isEarlyBirdEligible(now, dueIn4Days))
        assertFalse(TimeUtils.isEarlyBirdEligible(now, 0L))
    }

    @Test
    fun radarDistanceHighlightsCustomersBeyond15KmFromGolfIstm() {
        val nearKolweziKm = TimeUtils.distanceKmFromGolfIstm(-10.7235, 25.4920)
        val distantRadarKm = TimeUtils.distanceKmFromGolfIstm(-10.5850, 25.5650)

        assertTrue(nearKolweziKm < TimeUtils.RADAR_HIGHLIGHT_KM)
        assertTrue(distantRadarKm > TimeUtils.RADAR_HIGHLIGHT_KM)
    }

    @Test
    fun usdAndCdfLedgersNeverAutoConvertAndLowStockThresholdIsFive() {
        val lowStockItem = ProductEntity(
            name = "Mixeur Pro",
            category = ProductCategory.CUISINE,
            costUsd = 20.0,
            costCdf = 56000.0,
            priceUsd = 32.0,
            priceCdf = 90000.0,
            stockQuantity = 4
        )
        assertTrue(lowStockItem.isLowStock)
        assertEquals(32.0, lowStockItem.unitPrice(CurrencyCode.USD), 0.001)
        assertEquals(12.0, lowStockItem.unitProfit(CurrencyCode.USD), 0.001)
        assertEquals(90000.0, lowStockItem.unitPrice(CurrencyCode.CDF), 0.001)
        assertEquals(34000.0, lowStockItem.unitProfit(CurrencyCode.CDF), 0.001)
    }
}
