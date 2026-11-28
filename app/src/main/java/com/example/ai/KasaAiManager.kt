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

    private val leadSchema by lazy {
        com.google.firebase.ai.type.Schema.obj(
            properties = mapOf(
                "leads" to com.google.firebase.ai.type.Schema.array(
                    items = com.google.firebase.ai.type.Schema.obj(
                        properties = mapOf(
                            "name" to com.google.firebase.ai.type.Schema.string("Full Congolese/Kolwezi customer name"),
                            "phone" to com.google.firebase.ai.type.Schema.string("DRC mobile number starting with +243 99... or +243 97..."),
                            "neighborhood" to com.google.firebase.ai.type.Schema.string("Specific Kolwezi neighborhood inside target area"),
                            "signal" to com.google.firebase.ai.type.Schema.string("High-intent social or local buying signal"),
                            "productInterest" to com.google.firebase.ai.type.Schema.string("Specific home appliance needed"),
                            "installmentBudget" to com.google.firebase.ai.type.Schema.string("Estimated installment capacity in USD or CDF"),
                            "source" to com.google.firebase.ai.type.Schema.enumeration(
                                listOf("Facebook", "TikTok", "WhatsApp", "Referral"),
                                "Discovery channel"
                            ),
                            "intentScore" to com.google.firebase.ai.type.Schema.integer("Purchase intent score from 75 to 98"),
                            "gpsLat" to com.google.firebase.ai.type.Schema.double("Latitude near Kolwezi around -10.71"),
                            "gpsLng" to com.google.firebase.ai.type.Schema.double("Longitude near Kolwezi around 25.46")
                        )
                    ),
                    description = "4 to 5 realistic high-intent Kolwezi appliance leads"
                )
            )
        )
    }

    private val leadModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = com.google.firebase.ai.type.generationConfig {
                responseMimeType = "application/json"
                responseSchema = leadSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are KASA NOVA Lead Intelligence for HAMADA GROUP in Kolwezi (Lualaba, DRC).
                    We sell quality home appliances (blenders, vacuum cleaners, cookware, irons) on installments (Cuisine, Beauté, Maison — NO solar).
                    Generate realistic, culturally authentic Kolwezi prospective customer leads matching the search criteria.
                    Keep USD and CDF separate. Phone numbers must look realistic for DRC (+243 99X XXX XXX or +243 97X XXX XXX).
                    """.trimIndent()
                )
            }
        )
    }

    private val pitchModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    You are an expert Swahili sales advisor for KASA NOVA (HAMADA GROUP) in Kolwezi, DRC.
                    Write warm, respectful, trustworthy WhatsApp outreach messages in natural Katanga/Congolese Swahili.
                    Focus on product durability, genuine local trust in Kolwezi, and convenient installment terms without ever converting USD and CDF automatically.
                    Output only the ready-to-send WhatsApp message text.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun findHighIntentLeads(
        targetArea: String,
        productInterest: String,
        budgetCapability: String
    ): List<AiLeadProfile> {
        val prompt = """
            Find 4 realistic high-intent prospective installment customers in Kolwezi, DRC:
            - Target Area / Neighborhood: $targetArea
            - Product Interest: $productInterest
            - Budget / Installment Capability: $budgetCapability
            Focus strictly on home & kitchen/cleaning/beauty appliances (no solar products).
        """.trimIndent()

        val response = kotlinx.coroutines.withTimeout(60_000L) {
            leadModel.generateContent(prompt)
        }
        val jsonText = response.text
            ?: throw IllegalStateException("Empty response from Gemini Lead Finder.")

        val root = org.json.JSONObject(jsonText)
        val array = root.getJSONArray("leads")
        val results = mutableListOf<AiLeadProfile>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            results.add(
                AiLeadProfile(
                    id = "lead_${System.currentTimeMillis()}_$i",
                    name = item.getString("name"),
                    phone = item.getString("phone"),
                    neighborhood = item.getString("neighborhood"),
                    signal = item.getString("signal"),
                    productInterest = item.getString("productInterest"),
                    installmentBudget = item.getString("installmentBudget"),
                    source = item.optString("source", "Facebook"),
                    intentScore = item.optInt("intentScore", 88).coerceIn(70, 99),
                    gpsLat = item.optDouble("gpsLat", -10.7167),
                    gpsLng = item.optDouble("gpsLng", 25.4667)
                )
            )
        }
        return results
    }

    suspend fun generateSwahiliPitch(lead: AiLeadProfile): String {
        val prompt = """
            Write a personalized WhatsApp message in Swahili for this Kolwezi customer:
            - Customer Name: ${lead.name}
            - Neighborhood: ${lead.neighborhood}, Kolwezi
            - Buying Signal: ${lead.signal}
            - Interested Product: ${lead.productInterest}
            - Installment Budget: ${lead.installmentBudget}
            Mention KASA NOVA · HAMADA GROUP Kolwezi, emphasize product quality/warranty, flexible installment plan (malipo kwa awamu), and invite them to reply or call +243 992 120 772.
        """.trimIndent()

        val response = kotlinx.coroutines.withTimeout(60_000L) {
            pitchModel.generateContent(prompt)
        }
        return response.text?.trim()?.takeIf { it.isNotEmpty() }
            ?: throw IllegalStateException("Empty Swahili pitch generated.")
    }
}

data class AiLeadProfile(
    val id: String,
    val name: String,
    val phone: String,
    val neighborhood: String,
    val signal: String,
    val productInterest: String,
    val installmentBudget: String,
    val source: String,
    val intentScore: Int,
    val gpsLat: Double,
    val gpsLng: Double,
    val swahiliPitch: String? = null
)
