package com.example

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.auth.BiometricAuthGate
import com.example.core.configureAppCheck
import com.example.ui.screens.AppNavigation
import com.example.ui.theme.KasaNovaTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Setup global crash handler to log any unhandled startup crashes to Logcat
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("KasaCrashFatal", "Uncaught exception on thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        super.onCreate(savedInstanceState)
        Log.i("MainActivity", "MainActivity onCreate started.")

        try {
            // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
            configureAppCheck(this, intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error configuring App Check / Firebase on launch", e)
        }

        try {
            // STEP 2: Enable Edge-to-Edge and gate financial UI behind BiometricPrompt on launch
            enableEdgeToEdge()
            setContent {
                KasaNovaTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        BiometricAuthGate {
                            AppNavigation()
                        }
                    }
                }
            }
            Log.i("MainActivity", "MainActivity setContent completed successfully.")
        } catch (e: Exception) {
            Log.e("MainActivity", "Fatal error during setContent or UI inflation", e)
            throw e
        }
    }
}
