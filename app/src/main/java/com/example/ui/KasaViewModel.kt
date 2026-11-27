package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiQuickAction
import com.example.ai.KasaAiManager
import com.example.data.InstallmentPaymentOutcome
import com.example.data.KasaOfflineRepository
import com.example.data.local.CurrencyCode
import com.example.data.local.CustomerEntity
import com.example.data.local.CustomerSource
import com.example.data.local.ExpenseEntity
import com.example.data.local.KasaDatabase
import com.example.data.local.PaymentEntity
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.data.local.SaleType
import com.example.data.sync.CashDropRecord
import com.example.data.sync.ChatMessageRecord
import com.example.data.sync.FirebaseSyncManager
import com.example.utils.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AiChatBubble(
    val id: String,
    val fromUser: Boolean,
    val text: String
)

data class KasaUiState(
    val language: AppLanguage = AppLanguage.FR,
    val isDarkTheme: Boolean = true,
    val customers: List<CustomerEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val sales: List<SaleEntity> = emptyList(),
    val activeInstallments: List<SaleEntity> = emptyList(),
    val payments: List<PaymentEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val cashDrops: List<CashDropRecord> = emptyList(),
    val chatMessages: List<ChatMessageRecord> = emptyList(),
    val todaySalesUsd: Double = 0.0,
    val todaySalesCdf: Double = 0.0,
    val todayCollectionsUsd: Double = 0.0,
    val todayCollectionsCdf: Double = 0.0,
    val overdueUsd: Double = 0.0,
    val overdueCdf: Double = 0.0,
    val netProfitUsd: Double = 0.0,
    val netProfitCdf: Double = 0.0,
    val salesBySource: Map<String, Int> = CustomerSource.ALL.associateWith { 0 },
    val lowStockCount: Int = 0,
    val unsyncedCount: Int = 0,
    val isSyncing: Boolean = false,
    val isOnline: Boolean = false,
    val bannerMessage: String? = null,
    val greenToastMessage: String? = null,
    val latestCashDropOtp: CashDropRecord? = null,
    val lastCreatedSale: SaleEntity? = null,
    val aiMessages: List<AiChatBubble> = emptyList(),
    val isAiGenerating: Boolean = false,
    val aiError: String? = null
)

class KasaViewModel(application: Application) : AndroidViewModel(application) {

    private val localDb = KasaDatabase.getInstance(application)
    private val syncManager = FirebaseSyncManager(application, localDb)
    private val repository = KasaOfflineRepository(localDb, syncManager)
    private val aiManager = KasaAiManager()

    private val _uiState = MutableStateFlow(KasaUiState(isOnline = repository.isOnline()))
    val uiState: StateFlow<KasaUiState> = _uiState.asStateFlow()

    init {
        observeLocalLedgers()
        observeCloudStreams()
    }

    private fun observeLocalLedgers() {
        viewModelScope.launch {
            val baseStreams = combine(
                repository.observeCustomers(),
                repository.observeProducts(),
                repository.observeSales(),
                repository.observeActiveInstallments()
            ) { customers, products, sales, installments ->
                Quad(customers, products, sales, installments)
            }

            combine(
                baseStreams,
                repository.observePayments(),
                repository.observeExpenses()
            ) { quad, payments, expenses ->
                val (customers, products, sales, installments) = quad
                val today = TimeUtils.todayKey()

                val todaySalesUsd = sales
                    .filter { it.dayKey == today && it.currency == CurrencyCode.USD }
                    .sumOf { it.totalPrice }
                val todaySalesCdf = sales
                    .filter { it.dayKey == today && it.currency == CurrencyCode.CDF }
                    .sumOf { it.totalPrice }

                val todayCollectionsUsd = payments
                    .filter { it.dayKey == today && it.currency == CurrencyCode.USD }
                    .sumOf { it.amount }
                val todayCollectionsCdf = payments
                    .filter { it.dayKey == today && it.currency == CurrencyCode.CDF }
                    .sumOf { it.amount }

                val overdueUsd = installments
                    .filter { it.currency == CurrencyCode.USD }
                    .sumOf { it.remainingAmount }
                val overdueCdf = installments
                    .filter { it.currency == CurrencyCode.CDF }
                    .sumOf { it.remainingAmount }

                val todayProfitUsd = sales
                    .filter { it.dayKey == today && it.currency == CurrencyCode.USD }
                    .sumOf { it.profitAmount }
                val todayExpensesUsd = expenses
                    .filter { it.dayKey == today && it.currency == CurrencyCode.USD }
                    .sumOf { it.amount }

                val todayProfitCdf = sales
                    .filter { it.dayKey == today && it.currency == CurrencyCode.CDF }
                    .sumOf { it.profitAmount }
                val todayExpensesCdf = expenses
                    .filter { it.dayKey == today && it.currency == CurrencyCode.CDF }
                    .sumOf { it.amount }

                val sourceMap = CustomerSource.ALL.associateWith { src ->
                    sales.count { it.customerSource == src }
                }

                val unsynced = customers.count { !it.isSynced } +
                    products.count { !it.isSynced } +
                    sales.count { !it.isSynced } +
                    payments.count { !it.isSynced } +
                    expenses.count { !it.isSynced }

                _uiState.value.copy(
                    customers = customers,
                    products = products,
                    sales = sales,
                    activeInstallments = installments,
                    payments = payments,
                    expenses = expenses,
                    todaySalesUsd = todaySalesUsd,
                    todaySalesCdf = todaySalesCdf,
                    todayCollectionsUsd = todayCollectionsUsd,
                    todayCollectionsCdf = todayCollectionsCdf,
                    overdueUsd = overdueUsd,
                    overdueCdf = overdueCdf,
                    netProfitUsd = todayProfitUsd - todayExpensesUsd,
                    netProfitCdf = todayProfitCdf - todayExpensesCdf,
                    salesBySource = sourceMap,
                    lowStockCount = products.count { it.isLowStock },
                    unsyncedCount = unsynced,
                    isOnline = repository.isOnline()
                )
            }.collect { updated ->
                _uiState.value = updated
            }
        }
    }

    private fun observeCloudStreams() {
        viewModelScope.launch {
            repository.observeCashDrops()
                .catch { /* handled via FirestoreErrorHandler */ }
                .collect { drops ->
                    _uiState.update { it.copy(cashDrops = drops) }
                }
        }
        viewModelScope.launch {
            combine(
                repository.observeChatMessages(),
                repository.observeLocalChatMessages()
            ) { remote, local ->
                if (remote.isNotEmpty()) {
                    remote
                } else {
                    local.map {
                        ChatMessageRecord(
                            id = it.remoteId,
                            senderRole = it.senderRole,
                            senderName = it.senderName,
                            text = it.text,
                            createdAtMillis = it.createdAt
                        )
                    }
                }
            }
                .catch { /* handled via FirestoreErrorHandler */ }
                .collect { messages ->
                    _uiState.update { it.copy(chatMessages = messages) }
                }
        }
    }

    fun refreshConnectivity() {
        _uiState.update { it.copy(isOnline = repository.isOnline()) }
    }

    fun toggleLanguage() {
        _uiState.update {
            val next = if (it.language == AppLanguage.FR) AppLanguage.AR else AppLanguage.FR
            it.copy(language = next)
        }
    }

    fun toggleTheme() {
        _uiState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun clearBanner() {
        _uiState.update { it.copy(bannerMessage = null) }
    }

    fun clearGreenToast() {
        _uiState.update { it.copy(greenToastMessage = null) }
    }

    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedKolweziSampleData()
        }
    }

    fun syncToCloud() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, bannerMessage = null, isOnline = repository.isOnline()) }
            val result = repository.syncNow()
            _uiState.update { state ->
                state.copy(
                    isSyncing = false,
                    bannerMessage = result.fold(
                        onSuccess = { report ->
                            val strings = if (state.language == AppLanguage.FR) FrenchStrings else ArabicStrings
                            "${strings.syncComplete} (${report.totalSynced})"
                        },
                        onFailure = { err -> err.localizedMessage ?: "Sync error" }
                    )
                )
            }
        }
    }

    fun saveCustomer(
        id: Long = 0L,
        name: String,
        phone: String,
        address: String,
        gpsLat: Double,
        gpsLng: Double,
        source: String,
        loyaltyPoints: Int = 0
    ) {
        viewModelScope.launch {
            repository.saveCustomer(
                CustomerEntity(
                    id = id,
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    gpsLat = gpsLat,
                    gpsLng = gpsLng,
                    source = source,
                    loyaltyPoints = loyaltyPoints
                )
            )
        }
    }

    fun saveProduct(
        id: Long = 0L,
        name: String,
        category: String,
        costUsd: Double,
        costCdf: Double,
        priceUsd: Double,
        priceCdf: Double,
        stockQuantity: Int,
        videoUrl: String
    ) {
        viewModelScope.launch {
            repository.saveProduct(
                ProductEntity(
                    id = id,
                    name = name.trim(),
                    category = category,
                    costUsd = costUsd,
                    costCdf = costCdf,
                    priceUsd = priceUsd,
                    priceCdf = priceCdf,
                    stockQuantity = stockQuantity.coerceAtLeast(0),
                    videoUrl = videoUrl.trim()
                )
            )
        }
    }

    fun adjustStock(product: ProductEntity, delta: Int) {
        val nextQty = (product.stockQuantity + delta).coerceAtLeast(0)
        viewModelScope.launch {
            repository.saveProduct(product.copy(stockQuantity = nextQty))
        }
    }

    fun createSale(
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
    ) {
        viewModelScope.launch {
            val dueMillis = if (saleType == SaleType.INSTALLMENT) {
                TimeUtils.defaultDueDateMillis(daysAhead = dueDaysAhead.coerceAtLeast(1))
            } else {
                0L
            }
            val result = repository.createSale(
                customer = customer,
                product = product,
                quantity = quantity,
                saleType = saleType,
                currency = currency,
                paidAmountInput = paidAmountInput,
                dueDateMillis = dueMillis,
                signaturePath = signaturePath,
                isGiveawayEntry = isGiveawayEntry
            )
            result.fold(
                onSuccess = { sale ->
                    _uiState.update { it.copy(lastCreatedSale = sale) }
                    onSuccess(sale)
                },
                onFailure = { err ->
                    onError(err.localizedMessage ?: "Sale blocked")
                }
            )
        }
    }

    fun receiveInstallmentPayment(
        sale: SaleEntity,
        amount: Double,
        onSuccess: (InstallmentPaymentOutcome) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.receiveInstallmentPayment(sale, amount)
            result.fold(
                onSuccess = { outcome ->
                    if (outcome.earlyBirdApplied) {
                        val strings = if (_uiState.value.language == AppLanguage.FR) {
                            FrenchStrings
                        } else {
                            ArabicStrings
                        }
                        _uiState.update { it.copy(greenToastMessage = strings.earlyBirdToast) }
                    }
                    onSuccess(outcome)
                },
                onFailure = { err ->
                    onError(err.localizedMessage ?: "Payment failed")
                }
            )
        }
    }

    fun addExpense(title: String, amount: Double, currency: String) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            repository.addExpense(title, amount, currency)
        }
    }

    fun createCashDrop(amount: Double, currency: String, agentName: String, note: String) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val result = repository.createCashDrop(amount, currency, agentName, note)
            result.fold(
                onSuccess = { drop ->
                    _uiState.update { it.copy(latestCashDropOtp = drop, bannerMessage = null) }
                },
                onFailure = { err ->
                    _uiState.update { it.copy(bannerMessage = err.localizedMessage ?: "Cash drop error") }
                }
            )
        }
    }

    fun confirmCashDropOtp(drop: CashDropRecord, enteredOtp: String) {
        viewModelScope.launch {
            val strings = if (_uiState.value.language == AppLanguage.FR) FrenchStrings else ArabicStrings
            val result = repository.confirmCashDropWithOtp(drop, enteredOtp)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(greenToastMessage = strings.otpConfirmedSuccess) }
                },
                onFailure = { err ->
                    val msg = if (err.message == "INVALID_OTP") {
                        strings.otpInvalidError
                    } else {
                        err.localizedMessage ?: "Error"
                    }
                    _uiState.update { it.copy(bannerMessage = msg) }
                }
            )
        }
    }

    fun sendChatMessage(senderRole: String, senderName: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            val res = repository.sendChatMessage(senderRole, senderName, text)
            res.onFailure { err ->
                _uiState.update { it.copy(bannerMessage = err.localizedMessage ?: "Chat send error") }
            }
        }
    }

    fun analyzeTodayWithAi() {
        runAiQuickAction(AiQuickAction.DAILY_TARGET_LIQUIDITY)
    }

    fun runAiQuickAction(action: AiQuickAction, targetProduct: ProductEntity? = null) {
        val isFrench = _uiState.value.language == AppLanguage.FR
        val prompt = when (action) {
            AiQuickAction.DAILY_TARGET_LIQUIDITY -> if (isFrench) {
                "Analyse le bilan du jour séparément en USD et CDF, évalue l'atteinte de l'objectif net de 50 USD/jour et donne 3 actions de liquidité."
            } else {
                "حلّل بيانات اليوم مع فصل تام بين USD و CDF، وقيّم مدى اقترابنا من هدف 50 USD صافي ربح يومي مع 3 توصيات عملية للسيولة."
            }
            AiQuickAction.COLLECTIONS_PRIORITY -> if (isFrench) {
                "Classe les clients ayant des crédits actifs par priorité de recouvrement pour demain, et rédige un court message WhatsApp professionnel (FR + AR) pour le premier client."
            } else {
                "رتّب العملاء أصحاب الأقساط النشطة حسب أولوية التحصيل لغداً، وصغ رسالة واتساب مهذبة بالفرنسية والعربية لأول عميل."
            }
            AiQuickAction.SALES_OFFER_COMPOSER -> {
                val productLabel = targetProduct?.let {
                    "${it.name} (${it.category}) — ${it.priceUsd} USD / ${it.priceCdf.toInt()} CDF"
                } ?: "nos meilleurs produits Cuisine, Beauté & Maison"
                if (isFrench) {
                    "Rédige une offre commerciale percutante en Français et en Arabe (prête à copier sur WhatsApp et TikTok) pour : $productLabel, en mentionnant la disponibilité à Kolwezi et la tombola client."
                } else {
                    "صغ عرض بيع جذاب وجاهز للنشر على واتساب وتيك توك باللغتين العربية والفرنسية للمنتج: $productLabel، مع ذكر التوفر في كولويزي وفرصة سحب الجوائز."
                }
            }
            AiQuickAction.RESTOCK_ADVISOR -> if (isFrench) {
                "Analyse les articles en alerte stock bas (< 5 unités) dans Cuisine, Beauté et Maison, et recommande l'ordre de réapprovisionnement sans mélanger USD et CDF."
            } else {
                "حلّل المنتجات منخفضة المخزون (< 5 وحدات) في أقسام المطبخ والجمال والمنزل، واقترح خطة إعادة التزويد ذات الأولوية مع فصل USD و CDF."
            }
        }
        askAi(prompt)
    }

    fun askAi(prompt: String) {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty() || _uiState.value.isAiGenerating) return

        val strings = if (_uiState.value.language == AppLanguage.FR) FrenchStrings else ArabicStrings
        if (!repository.isOnline()) {
            _uiState.update { it.copy(isOnline = false, aiError = strings.aiOfflineError) }
            return
        }

        val userBubble = AiChatBubble(
            id = "u_${System.currentTimeMillis()}",
            fromUser = true,
            text = trimmed
        )
        val assistantId = "ai_${System.currentTimeMillis() + 1}"
        _uiState.update {
            it.copy(
                isOnline = true,
                aiError = null,
                isAiGenerating = true,
                aiMessages = it.aiMessages + userBubble + AiChatBubble(assistantId, false, "")
            )
        }

        viewModelScope.launch {
            try {
                val snapshot = repository.getTodayContextForAi()
                val stateSnapshot = _uiState.value
                val builder = StringBuilder()
                aiManager.streamAnalysis(
                    userPrompt = trimmed,
                    snapshot = snapshot,
                    activeInstallmentsUsd = stateSnapshot.overdueUsd,
                    activeInstallmentsCdf = stateSnapshot.overdueCdf,
                    lowStockProducts = stateSnapshot.products.filter { it.isLowStock },
                    activeInstallments = stateSnapshot.activeInstallments,
                    catalogProducts = stateSnapshot.products
                ).collect { chunk ->
                    builder.append(chunk)
                    val textSoFar = builder.toString()
                    _uiState.update { state ->
                        state.copy(
                            aiMessages = state.aiMessages.map { bubble ->
                                if (bubble.id == assistantId) bubble.copy(text = textSoFar) else bubble
                            }
                        )
                    }
                }
                _uiState.update { it.copy(isAiGenerating = false) }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isAiGenerating = false,
                        aiError = e.localizedMessage ?: strings.aiOfflineError,
                        aiMessages = state.aiMessages.filterNot { it.id == assistantId && it.text.isBlank() }
                    )
                }
            }
        }
    }

    suspend fun getCustomerForSale(customerId: Long): CustomerEntity? = repository.getCustomerById(customerId)

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
