package com.example.ai

import com.example.data.TodayBusinessSnapshot
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import java.util.Locale

class KasaAiManager {

    private val systemInstructionText = """
        أنت KASA NOVA AI، وحدة التحليل والتوصيات الذكية.
        مهامك: تحليل المبيعات، التحصيل، السيولة.
        قواعد حاسمة: تحصيل دين قديم ليس ربحاً، لا تخلط USD و CDF، دورك التحليل والتوصية فقط.
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
        lowStockCount: Int
    ): String {
        return """
            تقرير قاعدة البيانات المحلية (Room) - تاريخ ${snapshot.dayKey} (توقيت كولويزي Africa/Lubumbashi):

            [دفتر الدولار USD - منفصل تماماً]
            - إجمالي المبيعات اليوم: ${String.format(Locale.US, "%.2f", snapshot.salesUsdTotal)} USD
            - ربح المبيعات اليوم: ${String.format(Locale.US, "%.2f", snapshot.salesUsdProfit)} USD
            - إجمالي التحصيلات اليوم: ${String.format(Locale.US, "%.2f", snapshot.collectionsUsdTotal)} USD
            - المصروفات اليومية: ${String.format(Locale.US, "%.2f", snapshot.expensesUsdTotal)} USD
            - الديون النشطة قيد التحصيل: ${String.format(Locale.US, "%.2f", activeInstallmentsUsd)} USD

            [دفتر الفرنك الكونغولي CDF - منفصل تماماً]
            - إجمالي المبيعات اليوم: ${String.format(Locale.US, "%,.0f", snapshot.salesCdfTotal)} CDF
            - ربح المبيعات اليوم: ${String.format(Locale.US, "%,.0f", snapshot.salesCdfProfit)} CDF
            - إجمالي التحصيلات اليوم: ${String.format(Locale.US, "%,.0f", snapshot.collectionsCdfTotal)} CDF
            - المصروفات اليومية: ${String.format(Locale.US, "%,.0f", snapshot.expensesCdfTotal)} CDF
            - الديون النشطة قيد التحصيل: ${String.format(Locale.US, "%,.0f", activeInstallmentsCdf)} CDF

            [المخزون]
            - أصناف منخفضة المخزون (< 5 وحدات): $lowStockCount
        """.trimIndent()
    }

    fun streamAnalysis(
        userPrompt: String,
        snapshot: TodayBusinessSnapshot,
        activeInstallmentsUsd: Double,
        activeInstallmentsCdf: Double,
        lowStockCount: Int
    ): Flow<String> {
        val formattedContext = formatDailyRoomContext(
            snapshot = snapshot,
            activeInstallmentsUsd = activeInstallmentsUsd,
            activeInstallmentsCdf = activeInstallmentsCdf,
            lowStockCount = lowStockCount
        )
        val prompt = "$formattedContext\n\nطلب المستخدم:\n$userPrompt"
        return model.generateContentStream(prompt)
            .mapNotNull { chunk -> chunk.text }
    }
}
