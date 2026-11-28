package com.example

import android.app.Application
import android.util.Log
import com.example.core.CrashHandler

class KasaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init()
        Log.i("KasaApplication", "KasaApplication initialized successfully at startup.")
    }
}
