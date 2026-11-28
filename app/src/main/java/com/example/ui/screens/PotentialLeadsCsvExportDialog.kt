package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald
import com.example.utils.LeadsCsvExporter
import kotlinx.coroutines.launch

@Composable
fun PotentialLeadsCsvExportDialog(
    potentialCount: Int,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onExported: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isWorking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val saveCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        errorMessage = null
        scope.launch {
            val result = LeadsCsvExporter.exportPotentialLeadsToUri(context, uri)
            isWorking = false
            result.fold(
                onSuccess = { count ->
                    val msg = if (isArabic) {
                        "تم تصدير $count عميل محتمل إلى ملف CSV بنجاح"
                    } else {
                        "$count prospects (POTENTIAL) exportés en CSV"
                    }
                    onExported(msg)
                    onDismiss()
                },
                onFailure = { err ->
                    errorMessage = err.localizedMessage ?: "CSV export error"
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.TableChart, contentDescription = null, tint = SovereignGold)
                Text(
                    text = if (isArabic) "تصدير العملاء المحتملين (CSV)" else "Exporter Prospects (CSV)",
                    color = SovereignGold,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) {
                        "العملاء الجاهزون للتصدير إلى Excel أو Google Sheets: $potentialCount (حالة POTENTIAL)"
                    } else {
                        "Prospects (statut POTENTIAL) prêts pour Excel ou Google Sheets : $potentialCount"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                if (potentialCount == 0) {
                    Text(
                        text = if (isArabic) {
                            "احفظ عميلاً محتملاً من شاشة البحث الذكي أولاً."
                        } else {
                            "Enregistrez d'abord au moins un prospect depuis AI Lead Finder."
                        },
                        color = OverdueCrimson,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = OverdueCrimson,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (isWorking) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(if (isArabic) "جاري إنشاء ملف CSV..." else "Génération CSV...")
                    }
                }

                Button(
                    onClick = { saveCsvLauncher.launch(LeadsCsvExporter.suggestedFileName()) },
                    enabled = !isWorking && potentialCount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("csv_save_device_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (isArabic) "حفظ ملف .csv (Excel / Sheets)" else "Enregistrer (.csv) pour Excel / Sheets"
                    )
                }

                OutlinedButton(
                    onClick = {
                        isWorking = true
                        errorMessage = null
                        scope.launch {
                            val res = LeadsCsvExporter.prepareShareCsvUri(context)
                            isWorking = false
                            res.fold(
                                onSuccess = { (uri, count) ->
                                    val shareIntent = Intent.createChooser(
                                        LeadsCsvExporter.buildShareCsvIntent(uri),
                                        "Export Potential Leads CSV"
                                    )
                                    runCatching { context.startActivity(shareIntent) }
                                    val msg = if (isArabic) {
                                        "تم تجهيز ملف CSV ($count عميل)"
                                    } else {
                                        "CSV prêt ($count prospects)"
                                    }
                                    onExported(msg)
                                    onDismiss()
                                },
                                onFailure = { err ->
                                    errorMessage = err.localizedMessage ?: "Share error"
                                }
                            )
                        }
                    },
                    enabled = !isWorking && potentialCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("csv_share_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = UsdEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "مشاركة .csv (واتساب / Drive / بريد)" else "Partager (.csv) via WhatsApp / Drive / Sheets",
                        color = UsdEmerald
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إغلاق" else "Fermer")
            }
        }
    )
}
