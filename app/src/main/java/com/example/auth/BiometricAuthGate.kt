package com.example.auth

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.ui.theme.OverdueCrimson
import com.example.ui.theme.PromiseAmber
import com.example.ui.theme.SovereignGold
import com.example.ui.theme.UsdEmerald
import com.example.ui.theme.kasaAmbientBackgroundBrush

@Composable
fun BiometricAuthGate(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findFragmentActivity() }
    val biometricManager = remember(context) { BiometricManager.from(context) }

    val canAuthenticateStatus = remember(biometricManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            biometricManager.canAuthenticate()
        }
    }

    val isBiometricAvailable = canAuthenticateStatus == BiometricManager.BIOMETRIC_SUCCESS
    var isUnlocked by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    fun launchPrompt() {
        val host = activity
        if (host == null) {
            isUnlocked = true
            return
        }
        statusMessage = null
        val executor = ContextCompat.getMainExecutor(host)
        val prompt = BiometricPrompt(
            host,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                    statusMessage = null
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS ||
                        errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                        errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
                    ) {
                        statusMessage = errString.toString()
                    } else {
                        statusMessage = errString.toString()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    statusMessage = "Empreinte ou visage non reconnu / البصمة غير مطابقة"
                }
            }
        )

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle("KASA NOVA · Verrouillage Propriétaire")
            .setSubtitle("Authentification requise · التحقق بالبصمة أو الوجه لحماية البيانات المالية")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            infoBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            infoBuilder.setDeviceCredentialAllowed(true)
        }

        runCatching {
            prompt.authenticate(infoBuilder.build())
        }.onFailure { err ->
            statusMessage = err.localizedMessage ?: "Biometric prompt unavailable"
        }
    }

    LaunchedEffect(isBiometricAvailable) {
        if (isBiometricAvailable && !isUnlocked) {
            launchPrompt()
        }
    }

    if (isUnlocked) {
        content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(kasaAmbientBackgroundBrush(darkTheme = true))
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, SovereignGold.copy(alpha = 0.5f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_kasa_nova_icon),
                        contentDescription = "KASA NOVA",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, SovereignGold, RoundedCornerShape(16.dp))
                    )

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SovereignGold.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Fingerprint,
                            contentDescription = "Biometric Lock",
                            tint = SovereignGold,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "KASA NOVA · Accès Sécurisé",
                        style = MaterialTheme.typography.titleLarge,
                        color = SovereignGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Déverrouillez par empreinte digitale, visage ou code PIN pour accéder aux données financières.\nافتح بالبصمة أو الوجه لحماية البيانات المالية للمالك.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    if (statusMessage != null) {
                        Text(
                            text = statusMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OverdueCrimson,
                            textAlign = TextAlign.Center
                        )
                    }

                    if (isBiometricAvailable) {
                        Button(
                            onClick = { launchPrompt() },
                            colors = ButtonDefaults.buttonColors(containerColor = SovereignGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("biometric_unlock_button")
                        ) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Déverrouiller · فتح بالبصمة / الوجه",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = "Aucune biométrie ou code écran configuré sur cet appareil.\nلا توجد بصمة أو قفل شاشة مفعّل على هذا الجهاز.",
                            style = MaterialTheme.typography.labelLarge,
                            color = PromiseAmber,
                            textAlign = TextAlign.Center
                        )
                        OutlinedButton(
                            onClick = { isUnlocked = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("biometric_bypass_no_hardware")
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = UsdEmerald)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Continuer · متابعة الدخول",
                                color = UsdEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
