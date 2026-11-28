package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import com.example.utils.KasaSqliteDbTarget
import com.example.utils.SqliteExporter
import kotlinx.coroutines.launch

@Composable
fun SqliteExportDialog(
    initialTarget: KasaSqliteDbTarget,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onExported: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTarget by remember { mutableStateOf(initialTarget) }
    var isWorking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorking = true
        errorMessage = null
        scope.launch {
            val result = SqliteExporter.exportToUri(context, selectedTarget, uri)
            isWorking = false
            result.fold(
                onSuccess = { bytes ->
                    val kb = (bytes / 1024L).coerceAtLeast(1L)
                    val msg = if (isArabic) {
                        "تم تصدير قاعدة البيانات SQLite بنجاح ($kb KB)"
                    } else {
                        "Base SQLite exportée avec succès ($kb KB)"
                    }
                    onExported(msg)
                    onDismiss()
                },
                onFailure = { err ->
                    errorMessage = err.localizedMessage ?: "Export error"
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
                Icon(Icons.Filled.Storage, contentDescription = null, tint = SovereignGold)
                Text(
                    text = if (isArabic) "تصدير قاعدة البيانات (SQLite)" else "Exporter SQLite (.db)",
                    color = SovereignGold,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) {
                        "اختر قاعدة البيانات المراد تصديرها كملف SQLite (.db):"
                    } else {
                        "Sélectionnez la base Room à sauvegarder au format SQLite (.db) :"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                KasaSqliteDbTarget.entries.forEach { target ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTarget = target
                                errorMessage = null
                            }
                            .testTag("sqlite_target_${target.name}")
                    ) {
                        RadioButton(
                            selected = selectedTarget == target,
                            onClick = {
                                selectedTarget = target
                                errorMessage = null
                            }
                        )
                        Column {
                            Text(
                                text = if (isArabic) target.labelAr else target.labelFr,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = target.dbName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                        Text(if (isArabic) "جاري التحضير..." else "Préparation SQLite...")
                    }
                }

                Button(
                    onClick = {
                        saveFileLauncher.launch(SqliteExporter.suggestedFileName(selectedTarget))
                    },
                    enabled = !isWorking,
                    colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sqlite_save_device_button")
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "حفظ ملف .db على الهاتف" else "Enregistrer (.db) sur l'appareil")
                }

                OutlinedButton(
                    onClick = {
                        isWorking = true
                        errorMessage = null
                        scope.launch {
                            val res = SqliteExporter.prepareShareUri(context, selectedTarget)
                            isWorking = false
                            res.fold(
                                onSuccess = { uri ->
                                    val shareIntent = Intent.createChooser(
                                        SqliteExporter.buildShareDatabaseIntent(uri, selectedTarget),
                                        "SQLite Export"
                                    )
                                    runCatching { context.startActivity(shareIntent) }
                                    onDismiss()
                                },
                                onFailure = { err ->
                                    errorMessage = err.localizedMessage ?: "Share error"
                                }
                            )
                        }
                    },
                    enabled = !isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sqlite_share_button")
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, tint = UsdEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "مشاركة ملف .db (واتساب / Drive)" else "Partager (.db) via WhatsApp / Drive",
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
