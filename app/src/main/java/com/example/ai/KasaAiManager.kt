package com.example.ai

import com.example.data.TodayBusinessSnapshot
import com.example.data.local.ProductEntity
import com.example.data.local.SaleEntity
import com.example.utils.TimeUtils
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.util.Locale

enum class AiQuickAction {
    DAILY_TARGET_LIQUIDITY,
    COLLECTIONS_PRIORITY,
    SALES_OFFER_COMPOSER,
    RESTOCK_ADVISOR
}

class KasaAiManager {

    private val systemInstructionText = """
        أنت KASA NOVA AI، وحدة التحليل والتوصيات الذكية لنظام HAMADA GROUP في كولويزي.
        مهامك: تحليل المبيعات، ترتيب أولويات التحصيل، حساب الوصول لهدف 50 USD صافي ربح يومي، تحليل المخزون، وصياغة عروض البيع بالفرنسية والعربية.
        قواعد حاسمة: تحصيل دين قديم ليس ربحاً، لا تخلط USD و CDF، لا تعتبر وعود الدفع تحصيلاً فعلياً، دورك التحليل والتوصية فقط.
    """.trimIndent()

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content { text(systemInstructionText) }
        )
    }

    fun formatDailyRoomContext(
        snapshot: TodayBusinessSnapshot,
        activeInstallmentsUsd: Double,
        activeInstallmentsCdf: Double,
        lowStockProducts: List<ProductEntity>,
        activeInstallments: List<SaleEntity>,
        catalogProducts: List<ProductEntity>
    ): String {
        val netUsdToday = snapshot.salesUsdProfit - snapshot.expensesUsdTotal
        val remainingTo50Target = (50.0 - netUsdToday).coerceAtLeast(0.0)
        val topInstallments = activeInstallments.take(5).joinToString("\n") { sale ->
            val dueStr = if (sale.dueDate > 0L) TimeUtils.formatDateOnly(sale.dueDate) else "-"
            "  * ${sale.customerName} | ${sale.productName} | Reste: ${sale.remainingAmount} ${sale.currency} | Échéance: $dueStr"
        }.ifBlank { "  * لا توجد أقساط نشطة حالياً" }

        val lowStockLines = lowStockProducts.take(6).joinToString("\n") { p ->
            "  * ${p.name} [${p.category}]: متبقي ${p.stockQuantity} وحدة"
        }.ifBlank { "  * المخزون فوق الحد الآمن (>= 5)" }

        val topProducts = catalogProducts.take(6).joinToString("\n") { p ->
            "  * ${p.name} [${p.category}] — ${String.format(Locale.US, "%.2f", p.priceUsd)} USD / ${String.format(Locale.US, "%,.0f", p.priceCdf)} CDF"
        }.ifBlank { "  * لا توجد منتجات مسجلة" }

        return """
            تقرير قاعدة البيانات المحلية (Room) - تاريخ ${snapshot.dayKey} (توقيت كولويزي Africa/Lubumbashi):

            [دفتر الدولار USD - منفصل تماماً]
            - إجمالي المبيعات اليوم: ${String.format(Locale.US, "%.2f", snapshot.salesUsdTotal)} USD
            - ربح المبيعات اليوم: ${String.format(Locale.US, "%.2f", snapshot.salesUsdProfit)} USD
            - المصروفات اليومية: ${String.format(Locale.US, "%.2f", snapshot.expensesUsdTotal)} USD
            - صافي ربح USD اليوم مقارنة بهدف 50 USD: ${String.format(Locale.US, "%.2f", netUsdToday)} USD (المتبقي للهدف: ${String.format(Locale.US, "%.2f", remainingTo50Target)} USD)
            - إجمالي التحصيلات اليوم (دين قديم + مقدمات): ${String.format(Locale.US, "%.2f", snapshot.collectionsUsdTotal)} USD
            - الديون النشطة قيد التحصيل: ${String.format(Locale.US, "%.2f", activeInstallmentsUsd)} USD

            [دفتر الفرنك الكونغولي CDF - منفصل تماماً]
            - إجمالي المبيعات اليوم: ${String.format(Locale.US, "%,.0f", snapshot.salesCdfTotal)} CDF
            - ربح المبيعات اليوم: ${String.format(Locale.US, "%,.0f", snapshot.salesCdfProfit)} CDF
            - المصروفات اليومية: ${String.format(Locale.US, "%,.0f", snapshot.expensesCdfTotal)} CDF
            - إجمالي التحصيلات اليوم: ${String.format(Locale.US, "%,.0f", snapshot.collectionsCdfTotal)} CDF
            - الديون النشطة قيد التحصيل: ${String.format(Locale.US, "%,.0f", activeInstallmentsCdf)} CDF

            [أولويات التحصيل النشطة]
$topInstallments

            [أصناف منخفضة المخزون (< 5 وحدات)]
$lowStockLines

            [كتالوج المنتجات المتاح (Cuisine / Beauté / Maison)]
$topProducts
        """.trimIndent()
    }

    fun streamAnalysis(
        userPrompt: String,
        snapshot: TodayBusinessSnapshot,
        activeInstallmentsUsd: Double,
        activeInstallmentsCdf: Double,
        lowStockProducts: List<ProductEntity>,
        activeInstallments: List<SaleEntity>,
        catalogProducts: List<ProductEntity>
    ): Flow<String> {
        val formattedContext = formatDailyRoomContext(
            snapshot = snapshot,
            activeInstallmentsUsd = activeInstallmentsUsd,
            activeInstallmentsCdf = activeInstallmentsCdf,
            lowStockProducts = lowStockProducts,
            activeInstallments = activeInstallments,
            catalogProducts = catalogProducts
        )
        val prompt = "$formattedContext\n\nطلب المستخدم:\n$userPrompt"
        return model.generateContentStream(prompt)
            .mapNotNull { chunk -> chunk.text }
    }
}
