package com.example.data

import com.example.data.local.ChatEntity
import com.example.data.local.CurrencyCode
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerSource
import com.example.data.local.ExpenseEntity
import com.example.data.local.KasaDatabase
import com.example.data.local.PaymentEntity
import com.example.data.local.ProductCategory
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleType
import com.example.data.sync.CashDropRecord
import com.example.data.sync.ChatMessageRecord
import com.example.data.sync.FirebaseSyncManager
import com.example.data.sync.SyncReport
import com.example.utils.TimeUtils
import kotlinx.coroutines.flow.Flow

data class InstallmentPaymentOutcome(
    val updatedSale: SaleEntity,
    val netCashPaid: Double,
    val discountApplied: Double,
    val earlyBirdApplied: Boolean,
    val loyaltyPointsAwarded: Int
)

class KasaOfflineRepository(
    private val localDb: KasaDatabase,
    private val syncManager: FirebaseSyncManager
) {
    fun observeCustomers(): Flow<List<CustomerEntity>> = localDb.customerDao().observeAll()
    suspend fun getPotentialLeads(): List<CustomerEntity> = localDb.customerDao().getPotentialLeads()
    fun observeProducts(): Flow<List<ProductEntity>> = localDb.productDao().observeAll()
    fun observeSales(): Flow<List<SaleEntity>> = localDb.saleDao().observeAll()
    fun observeActiveInstallments(): Flow<List<SaleEntity>> = localDb.saleDao().observeActiveInstallments()
    fun observePayments(): Flow<List<PaymentEntity>> = localDb.paymentDao().observeAll()
    fun observeExpenses(): Flow<List<ExpenseEntity>> = localDb.expenseDao().observeAll()

    fun observeCashDrops(): Flow<List<CashDropRecord>> = syncManager.observeCashDrops()
    fun observeChatMessages(): Flow<List<ChatMessageRecord>> = syncManager.observeChatMessages()
    fun observeLocalChatMessages(): Flow<List<ChatEntity>> = localDb.chatDao().observeAll()

    fun isOnline(): Boolean = syncManager.isOnline()

    suspend fun syncNow(): Result<SyncReport> = syncManager.syncPendingData()

    suspend fun createCashDrop(
        amount: Double,
        currency: String,
        agentName: String,
        note: String
    ): Result<CashDropRecord> = syncManager.createCashDrop(amount, currency, agentName, note)

    suspend fun confirmCashDropWithOtp(drop: CashDropRecord, enteredOtp: String): Result<Unit> =
        syncManager.confirmCashDropWithOtp(drop, enteredOtp)

    suspend fun sendChatMessage(senderRole: String, senderName: String, text: String): Result<Unit> {
        val cleanText = text.trim()
        val now = TimeUtils.nowMillis()
        val res = syncManager.sendChatMessage(senderRole, senderName, cleanText)
        localDb.chatDao().insert(
            ChatEntity(
                remoteId = "local_$now",
                senderRole = senderRole,
                senderName = senderName,
                text = cleanText,
                createdAt = now,
                isSynced = res.isSuccess
            )
        )
        return res
    }

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        val now = TimeUtils.nowMillis()
        val row = customer.copy(isSynced = false, updatedAt = now)
        return if (row.id == 0L) {
            localDb.customerDao().insert(row)
        } else {
            localDb.customerDao().update(row)
            row.id
        }
    }

    suspend fun savePotentialLead(
        name: String,
        phone: String,
        address: String,
        gpsLat: Double,
        gpsLng: Double,
        source: String
    ): Long {
        val now = TimeUtils.nowMillis()
        return localDb.customerDao().insert(
            CustomerEntity(
                name = name.trim(),
                phone = phone.trim(),
                address = address.trim(),
                gpsLat = gpsLat,
                gpsLng = gpsLng,
                source = source,
                status = com.example.data.local.CustomerStatus.POTENTIAL,
                loyaltyPoints = 0,
                isSynced = false,
                updatedAt = now
            )
        )
    }

    suspend fun saveProduct(product: ProductEntity): Long {
        val now = TimeUtils.nowMillis()
        val row = product.copy(isSynced = false, updatedAt = now)
        return if (row.id == 0L) {
            localDb.productDao().insert(row)
        } else {
            localDb.productDao().update(row)
            row.id
        }
    }

    suspend fun createSale(
        customer: CustomerEntity,
        product: ProductEntity,
        quantity: Int,
        saleType: String,
        currency: String,
        paidAmountInput: Double,
        dueDateMillis: Long = TimeUtils.defaultDueDateMillis(),
        signaturePath: String = "",
        isGiveawayEntry: Boolean = false
    ): Result<SaleEntity> {
        return try {
            require(quantity > 0) { "Quantity must be positive" }
            require(product.stockQuantity >= quantity) { "Insufficient stock" }

            val unitPrice = product.unitPrice(currency)
            val unitCost = product.unitCost(currency)
            val totalPrice = unitPrice * quantity
            val profitAmount = (unitPrice - unitCost) * quantity
            require(profitAmount >= 0.0) { "Negative profit blocked" }

            val paidAmount = if (saleType == SaleType.CASH) {
                totalPrice
            } else {
                paidAmountInput.coerceIn(0.0, totalPrice)
            }
            val remainingAmount = (totalPrice - paidAmount).coerceAtLeast(0.0)
            val now = TimeUtils.nowMillis()
            val dayKey = TimeUtils.todayKey(now)
            val effectiveDueDate = if (saleType == SaleType.INSTALLMENT && remainingAmount > 0.0) {
                if (dueDateMillis > now) dueDateMillis else TimeUtils.defaultDueDateMillis(now)
            } else {
                0L
            }

            // V5.0: Reward Loyalty Points whenever customer buys CASH
            val loyaltyAward = if (saleType == SaleType.CASH) 10 else 0

            val sale = SaleEntity(
                customerId = customer.id,
                productId = product.id,
                customerName = customer.name,
                productName = product.name,
                customerSource = customer.source,
                type = saleType,
                quantity = quantity,
                totalPrice = totalPrice,
                profitAmount = profitAmount,
                currency = currency,
                paidAmount = paidAmount,
                remainingAmount = remainingAmount,
                dueDate = effectiveDueDate,
                signaturePath = signaturePath,
                isGiveawayEntry = isGiveawayEntry,
                createdAt = now,
                dayKey = dayKey,
                isSynced = false
            )

            val insertedId = localDb.saleDao().recordSaleAndDecrementStock(
                sale = sale,
                productDao = localDb.productDao(),
                paymentDao = localDb.paymentDao(),
                customerDao = localDb.customerDao(),
                loyaltyPointsAwarded = loyaltyAward,
                dayKey = dayKey
            )
            Result.success(sale.copy(id = insertedId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun receiveInstallmentPayment(
        sale: SaleEntity,
        amount: Double
    ): Result<InstallmentPaymentOutcome> {
        return try {
            require(amount > 0.0) { "Payment must be positive" }
            val now = TimeUtils.nowMillis()
            val dayKey = TimeUtils.todayKey(now)

            // Credit against remaining installment balance
            val creditedToBalance = amount.coerceAtMost(sale.remainingAmount)
            val earlyBird = TimeUtils.isEarlyBirdEligible(now, sale.dueDate)
            val discount = if (earlyBird) creditedToBalance * 0.05 else 0.0
            val netCashPaid = (creditedToBalance - discount).coerceAtLeast(0.0)

            val newRemaining = (sale.remainingAmount - creditedToBalance).coerceAtLeast(0.0)
            val newPaid = (sale.totalPrice - newRemaining).coerceAtLeast(sale.paidAmount)

            val updatedSale = sale.copy(
                paidAmount = newPaid,
                remainingAmount = newRemaining,
                isSynced = false
            )
            localDb.saleDao().updateSale(updatedSale)
            localDb.paymentDao().insert(
                PaymentEntity(
                    saleId = sale.id,
                    customerName = sale.customerName,
                    amount = netCashPaid,
                    discountApplied = discount,
                    currency = sale.currency,
                    paymentDate = now,
                    dayKey = dayKey,
                    isSynced = false
                )
            )

            // V5.0: Reward Loyalty Points when customer pays early
            val pointsAwarded = if (earlyBird) 15 else 0
            if (pointsAwarded > 0) {
                localDb.customerDao().addLoyaltyPoints(sale.customerId, pointsAwarded, now)
            }

            Result.success(
                InstallmentPaymentOutcome(
                    updatedSale = updatedSale,
                    netCashPaid = netCashPaid,
                    discountApplied = discount,
                    earlyBirdApplied = earlyBird,
                    loyaltyPointsAwarded = pointsAwarded
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addExpense(title: String, amount: Double, currency: String) {
        val now = TimeUtils.nowMillis()
        localDb.expenseDao().insert(
            ExpenseEntity(
                title = title.trim(),
                amount = amount,
                currency = currency,
                expenseDate = now,
                dayKey = TimeUtils.todayKey(now),
                isSynced = false
            )
        )
    }

    suspend fun getCustomerById(id: Long): CustomerEntity? = localDb.customerDao().getById(id)

    suspend fun getTodayContextForAi(): TodayBusinessSnapshot {
        val today = TimeUtils.todayKey()
        val sales = localDb.saleDao().getByDay(today)
        val payments = localDb.paymentDao().getByDay(today)
        val expenses = localDb.expenseDao().getByDay(today)

        val usdSales = sales.filter { it.currency == CurrencyCode.USD }
        val cdfSales = sales.filter { it.currency == CurrencyCode.CDF }
        val usdCollections = payments.filter { it.currency == CurrencyCode.USD }.sumOf { it.amount }
        val cdfCollections = payments.filter { it.currency == CurrencyCode.CDF }.sumOf { it.amount }
        val usdExpenses = expenses.filter { it.currency == CurrencyCode.USD }.sumOf { it.amount }
        val cdfExpenses = expenses.filter { it.currency == CurrencyCode.CDF }.sumOf { it.amount }

        return TodayBusinessSnapshot(
            dayKey = today,
            salesUsdTotal = usdSales.sumOf { it.totalPrice },
            salesUsdProfit = usdSales.sumOf { it.profitAmount },
            collectionsUsdTotal = usdCollections,
            expensesUsdTotal = usdExpenses,
            salesCdfTotal = cdfSales.sumOf { it.totalPrice },
            salesCdfProfit = cdfSales.sumOf { it.profitAmount },
            collectionsCdfTotal = cdfCollections,
            expensesCdfTotal = cdfExpenses
        )
    }

    suspend fun seedKolweziSampleData() {
        val now = TimeUtils.nowMillis()
        val today = TimeUtils.todayKey(now)
        val dueSoonEarlyBird = TimeUtils.defaultDueDateMillis(now, daysAhead = 14)

        val c1 = localDb.customerDao().insert(
            CustomerEntity(
                name = "Dépôt Mwamba",
                phone = "+243 992 120 772",
                address = "Manika, Kolwezi",
                gpsLat = -10.7235,
                gpsLng = 25.4920,
                source = CustomerSource.WHATSAPP,
                loyaltyPoints = 40,
                updatedAt = now
            )
        )
        val c2 = localDb.customerDao().insert(
            CustomerEntity(
                name = "Quincaillerie Dilala",
                phone = "+243 970 903 085",
                address = "Dilala, Kolwezi",
                gpsLat = -10.7112,
                gpsLng = 25.4615,
                source = CustomerSource.FACEBOOK,
                loyaltyPoints = 25,
                updatedAt = now
            )
        )
        localDb.customerDao().insert(
            CustomerEntity(
                name = "Marché Musompo",
                phone = "+243 994 128 810",
                address = "Musompo, Kolwezi",
                gpsLat = -10.7310,
                gpsLng = 25.4480,
                source = CustomerSource.TIKTOK,
                loyaltyPoints = 10,
                updatedAt = now
            )
        )
        localDb.customerDao().insert(
            CustomerEntity(
                name = "Entrepôt Tenke-Route (Radar)",
                phone = "+243 991 550 321",
                address = "Axe Lualaba (> 18 km)",
                gpsLat = -10.5850,
                gpsLng = 25.5650,
                source = CustomerSource.REFERRAL,
                loyaltyPoints = 55,
                updatedAt = now
            )
        )

        val p1 = localDb.productDao().insert(
            ProductEntity(
                name = "Mixeur Électrique Pro 2L",
                category = ProductCategory.CUISINE,
                costUsd = 28.0,
                costCdf = 79000.0,
                priceUsd = 45.0,
                priceCdf = 128000.0,
                stockQuantity = 3, // triggers < 5 red low-stock alert
                updatedAt = now
            )
        )
        val p2 = localDb.productDao().insert(
            ProductEntity(
                name = "Coffret Soin & Lissage Beauté",
                category = ProductCategory.BEAUTE,
                costUsd = 18.0,
                costCdf = 51000.0,
                priceUsd = 30.0,
                priceCdf = 85000.0,
                stockQuantity = 16,
                updatedAt = now
            )
        )
        localDb.productDao().insert(
            ProductEntity(
                name = "Batterie Casseroles Inox 12 Pcs",
                category = ProductCategory.CUISINE,
                costUsd = 42.0,
                costCdf = 119000.0,
                priceUsd = 65.0,
                priceCdf = 185000.0,
                stockQuantity = 11,
                updatedAt = now
            )
        )
        localDb.productDao().insert(
            ProductEntity(
                name = "Fer Vapeur & Table Maison",
                category = ProductCategory.MAISON,
                costUsd = 22.0,
                costCdf = 62000.0,
                priceUsd = 35.0,
                priceCdf = 99000.0,
                stockQuantity = 4, // triggers < 5 red low-stock alert
                updatedAt = now
            )
        )

        localDb.saleDao().insertSale(
            SaleEntity(
                customerId = c1,
                productId = p1,
                customerName = "Dépôt Mwamba",
                productName = "Mixeur Électrique Pro 2L",
                customerSource = CustomerSource.WHATSAPP,
                type = SaleType.INSTALLMENT,
                quantity = 2,
                totalPrice = 90.0,
                profitAmount = 34.0,
                currency = CurrencyCode.USD,
                paidAmount = 30.0,
                remainingAmount = 60.0,
                dueDate = dueSoonEarlyBird,
                isGiveawayEntry = true,
                createdAt = now,
                dayKey = today
            )
        )
        localDb.saleDao().insertSale(
            SaleEntity(
                customerId = c2,
                productId = p2,
                customerName = "Boutique Dilala Beauté",
                productName = "Coffret Soin & Lissage Beauté",
                customerSource = CustomerSource.FACEBOOK,
                type = SaleType.INSTALLMENT,
                quantity = 3,
                totalPrice = 255000.0,
                profitAmount = 102000.0,
                currency = CurrencyCode.CDF,
                paidAmount = 95000.0,
                remainingAmount = 160000.0,
                dueDate = dueSoonEarlyBird,
                isGiveawayEntry = false,
                createdAt = now,
                dayKey = today
            )
        )
        localDb.paymentDao().insert(
            PaymentEntity(
                saleId = 1L,
                customerName = "Dépôt Mwamba",
                amount = 85.0,
                discountApplied = 0.0,
                currency = CurrencyCode.USD,
                paymentDate = now,
                dayKey = today
            )
        )
        localDb.expenseDao().insert(
            ExpenseEntity(
                title = "Carburant livraison Kolwezi",
                amount = 10.0,
                currency = CurrencyCode.USD,
                expenseDate = now,
                dayKey = today
            )
        )
    }
}

data class TodayBusinessSnapshot(
    val dayKey: String,
    val salesUsdTotal: Double,
    val salesUsdProfit: Double,
    val collectionsUsdTotal: Double,
    val expensesUsdTotal: Double,
    val salesCdfTotal: Double,
    val salesCdfProfit: Double,
    val collectionsCdfTotal: Double,
    val expensesCdfTotal: Double
)
