package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import com.example.data.local.KasaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class KasaSqliteDbTarget(val dbName: String, val labelFr: String, val labelAr: String) {
    MAIN_OFFLINE(
        dbName = "kasa_nova_offline.db",
        labelFr = "Base Principale (Ventes, Stock, CRM)",
        labelAr = "القاعدة الرئيسية (المبيعات والمخزون والعملاء)"
    ),
    CASH_FLOW(
        dbName = "kasa_nova_db",
        labelFr = "Base Trésorerie (Transactions Cash-Flow)",
        labelAr = "قاعدة التدفقات النقدية (Transactions)"
    )
}

object SqliteExporter {

    private fun checkpointDatabase(context: Context, target: KasaSqliteDbTarget) {
        when (target) {
            KasaSqliteDbTarget.MAIN_OFFLINE -> {
                val db = KasaDatabase.getInstance(context).openHelper.writableDatabase
                db.query("PRAGMA wal_checkpoint(FULL)").use { /* flush WAL */ }
            }
            KasaSqliteDbTarget.CASH_FLOW -> {
                val db = AppDatabase.getDatabase(context).openHelper.writableDatabase
                db.query("PRAGMA wal_checkpoint(FULL)").use { /* flush WAL */ }
            }
        }
    }

    fun suggestedFileName(target: KasaSqliteDbTarget): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val prefix = if (target == KasaSqliteDbTarget.MAIN_OFFLINE) "kasa_nova_offline" else "kasa_nova_cashflow"
        return "${prefix}_$stamp.db"
    }

    suspend fun exportToUri(
        context: Context,
        target: KasaSqliteDbTarget,
        destinationUri: Uri
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            checkpointDatabase(context, target)
            val sourceDb = context.getDatabasePath(target.dbName)
            require(sourceDb.exists()) { "Database file not found yet. Add a record first." }
            var bytesCopied = 0L
            context.contentResolver.openOutputStream(destinationUri)?.use { out ->
                sourceDb.inputStream().use { input ->
                    bytesCopied = input.copyTo(out)
                }
                out.flush()
            } ?: error("Unable to open destination file.")
            bytesCopied
        }
    }

    suspend fun prepareShareUri(
        context: Context,
        target: KasaSqliteDbTarget
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            checkpointDatabase(context, target)
            val sourceDb = context.getDatabasePath(target.dbName)
            require(sourceDb.exists()) { "Database file not found yet. Add a record first." }
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val destFile = File(exportDir, suggestedFileName(target))
            sourceDb.copyTo(destFile, overwrite = true)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", destFile)
        }
    }

    fun buildShareDatabaseIntent(uri: Uri, target: KasaSqliteDbTarget): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "KASA NOVA SQLite Backup (${target.dbName})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
