package com.example.core

import android.util.Log
import kotlin.system.exitProcess

class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    private val defaultHandler: Thread.UncaughtExceptionHandler? = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e("KasaNovaCrash", "FATAL EXCEPTION on thread ${t.name} (${t.id})", e)
        try {
            e.printStackTrace()
        } catch (_: Exception) {
        }
        
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(t, e)
        } else {
            exitProcess(1)
        }
    }

    companion object {
        fun init() {
            if (Thread.getDefaultUncaughtExceptionHandler() !is CrashHandler) {
                Thread.setDefaultUncaughtExceptionHandler(CrashHandler())
                Log.i("CrashHandler", "Global CrashHandler registered successfully.")
            }
        }
    }
}
