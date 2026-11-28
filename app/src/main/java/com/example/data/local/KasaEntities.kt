package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

object SaleType {
    const val CASH = "CASH"
    const val INSTALLMENT = "INSTALLMENT"
}

object CurrencyCode {
    const val USD = "USD"
    const val CDF = "CDF"
}

object CustomerSource {
    const val FACEBOOK = "Facebook"
    const val TIKTOK = "TikTok"
    const val WHATSAPP = "WhatsApp"
    const val REFERRAL = "Referral"

    val ALL = listOf(FACEBOOK, TIKTOK, WHATSAPP, REFERRAL)
}

object CustomerStatus {
    const val ACTIVE = "ACTIVE"
    const val POTENTIAL = "POTENTIAL"
}

object ProductCategory {
    const val CUISINE = "Cuisine"
    const val BEAUTE = "Beauté"
    const val MAISON = "Maison"

    val ALL = listOf(CUISINE, BEAUTE, MAISON)
}

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String,
    val address: String,
    @ColumnInfo(name = "gps_lat") val gpsLat: Double = -10.7167,
    @ColumnInfo(name = "gps_lng") val gpsLng: Double = 25.4667,
    val source: String = CustomerSource.WHATSAPP,
    val status: String = CustomerStatus.ACTIVE,
    @ColumnInfo(name = "loyalty_points") val loyaltyPoints: Int = 0,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val category: String = ProductCategory.CUISINE,
    @ColumnInfo(name = "cost_usd") val costUsd: Double,
    @ColumnInfo(name = "cost_cdf") val costCdf: Double,
    @ColumnInfo(name = "price_usd") val priceUsd: Double,
    @ColumnInfo(name = "price_cdf") val priceCdf: Double,
    @ColumnInfo(name = "stock_quantity") val stockQuantity: Int,
    @ColumnInfo(name = "video_url") val videoUrl: String = "",
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    fun unitPrice(currency: String): Double = if (currency == CurrencyCode.USD) priceUsd else priceCdf
    fun unitCost(currency: String): Double = if (currency == CurrencyCode.USD) costUsd else costCdf
    fun unitProfit(currency: String): Double = unitPrice(currency) - unitCost(currency)
    val isLowStock: Boolean get() = stockQuantity < 5
}

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "customer_id") val customerId: Long,
    @ColumnInfo(name = "product_id") val productId: Long,
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    @ColumnInfo(name = "product_name") val productName: String = "",
    @ColumnInfo(name = "customer_source") val customerSource: String = CustomerSource.WHATSAPP,
    val type: String, // CASH or INSTALLMENT
    val quantity: Int = 1,
    @ColumnInfo(name = "total_price") val totalPrice: Double,
    @ColumnInfo(name = "profit_amount") val profitAmount: Double = 0.0,
    val currency: String, // USD or CDF (strictly separated)
    @ColumnInfo(name = "paid_amount") val paidAmount: Double,
    @ColumnInfo(name = "remaining_amount") val remainingAmount: Double,
    @ColumnInfo(name = "due_date") val dueDate: Long = 0L,
    @ColumnInfo(name = "signature_path") val signaturePath: String = "",
    @ColumnInfo(name = "is_giveaway_entry") val isGiveawayEntry: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "day_key") val dayKey: String,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "sale_id") val saleId: Long,
    @ColumnInfo(name = "customer_name") val customerName: String = "",
    val amount: Double,
    @ColumnInfo(name = "discount_applied") val discountApplied: Double = 0.0,
    val currency: String, // USD or CDF
    @ColumnInfo(name = "payment_date") val paymentDate: Long,
    @ColumnInfo(name = "day_key") val dayKey: String,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val amount: Double,
    val currency: String, // USD or CDF
    @ColumnInfo(name = "expense_date") val expenseDate: Long,
    @ColumnInfo(name = "day_key") val dayKey: String,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "remote_id") val remoteId: String,
    @ColumnInfo(name = "sender_role") val senderRole: String, // OWNER or AGENT
    @ColumnInfo(name = "sender_name") val senderName: String,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)
