package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.CustomerEntity
import com.example.data.local.KasaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LeadsCsvExporter {

    fun suggestedFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        return "kasa_nova_potential_leads_$stamp.csv"
    }

    fun buildCsvContent(leads: List<CustomerEntity>): String {
        val sb = StringBuilder()
        // UTF-8 BOM for clean French/Arabic/Swahili rendering in Microsoft Excel & Google Sheets
        sb.append('\uFEFF')
        sb.append("ID,Name,Phone,Address / Neighborhood,Marketing Source,Status,GPS Latitude,GPS Longitude,Updated At\r\n")
        for (lead in leads) {
            sb.append(lead.id).append(',')
            sb.append(escapeCsv(lead.name)).append(',')
            sb.append(escapeCsv(lead.phone)).append(',')
            sb.append(escapeCsv(lead.address)).append(',')
            sb.append(escapeCsv(lead.source)).append(',')
            sb.append(escapeCsv(lead.status)).append(',')
            sb.append(String.format(Locale.US, "%.6f", lead.gpsLat)).append(',')
            sb.append(String.format(Locale.US, "%.6f", lead.gpsLng)).append(',')
            sb.append(escapeCsv(TimeUtils.formatDateTime(lead.updatedAt)))
            sb.append("\r\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n') || value.contains('\r')
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    suspend fun exportPotentialLeadsToUri(
        context: Context,
        destinationUri: Uri
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val leads = KasaDatabase.getInstance(context).customerDao().getPotentialLeads()
            require(leads.isNotEmpty()) { "No POTENTIAL leads found in database yet." }
            val csv = buildCsvContent(leads)
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                out.write(csv.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Unable to open destination file.")
            leads.size
        }
    }

    suspend fun prepareShareCsvUri(
        context: Context
    ): Result<Pair<Uri, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val leads = KasaDatabase.getInstance(context).customerDao().getPotentialLeads()
            require(leads.isNotEmpty()) { "No POTENTIAL leads found in database yet." }
            val csv = buildCsvContent(leads)
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val destFile = File(exportDir, suggestedFileName())
            destFile.writeText(csv, Charsets.UTF_8)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
            uri to leads.size
        }
    }

    fun buildShareCsvIntent(uri: Uri): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "KASA NOVA · Potential Leads (CSV)")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
