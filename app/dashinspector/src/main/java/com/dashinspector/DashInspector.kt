package com.dashinspector

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class DashInspector {
    private lateinit var mServer : DashInspectorServer
    private lateinit var mGson : Gson
    private lateinit var mSharedPreferenceInspector: SharedPreferencesInspector

    fun initialize(context: Context, port: Int = DEFAULT_PORT) {
        mSharedPreferenceInspector = SharedPreferencesInspector(context)
        mGson = Gson()

        mServer = DashInspectorServer(
            context,
            port = port,
            mGson = mGson,
            mPrefsInspector = mSharedPreferenceInspector
        ).also {
            try {
                it.start()
                logDashInspectorStarted(port)
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Failed to start Dash Inspector server: ${e.message}")
            }
        }

    }

    private fun logDashInspectorStarted(port: Int) {
        val border = "╔══════════════════════════════════════════════════════════"
        val divider = "╟──────────────────────────────────────────────────────────"
        val footer = "╚══════════════════════════════════════════════════════════"

        Log.i(TAG, border)
        Log.i(TAG, "║ 🚀 Dash Inspector: Server is now ONLINE")
        Log.i(TAG, "║ 🌐 Access via: http://localhost:$port")
        Log.i(TAG, divider)
        Log.i(TAG, "║ 💡 REMINDER: Run the following commands before access:")
        Log.i(TAG, "║    kill -9 \$(lsof -ti :$port)")
        Log.i(TAG, "║    adb forward tcp:$port tcp:$port")
        Log.i(TAG, footer)
    }

    companion object {
        private const val TAG = "DashInspector"
        private const val DEFAULT_PORT = 8080
    }
}