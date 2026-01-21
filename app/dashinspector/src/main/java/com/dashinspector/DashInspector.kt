package com.dashinspector

import android.content.Context
import android.util.Log
import com.google.gson.Gson

/**
 * DashInspector - A runtime debugging tool for Android apps.
 *
 * Provides a web-based interface to inspect and modify SharedPreferences
 * and databases during development.
 *
 * Usage:
 * ```kotlin
 * // In your Application class
 * DashInspector.init(this)
 *
 * // Or with custom port
 * DashInspector.init(this, port = 9090)
 * ```
 */
object DashInspector {

    private const val TAG = "DashInspector"
    private const val DEFAULT_PORT = 8080

    private var server: DashInspectorServer? = null
    private var isRunning = false
    private var currentPort = DEFAULT_PORT

    /**
     * Initialize and start the DashInspector server.
     *
     * @param context Application context
     * @param port Port number for the server (default: 8080)
     */
    @JvmStatic
    @JvmOverloads
    fun init(context: Context, port: Int = DEFAULT_PORT) {
        if (isRunning) {
            Log.w(TAG, "DashInspector is already running on port $currentPort")
            return
        }

        val appContext = context.applicationContext
        val gson = Gson()
        val prefsInspector = SharedPreferencesInspector(appContext)
        val dbInspector = DatabaseInspector(appContext)

        server = DashInspectorServer(
            context = appContext,
            port = port,
            gson = gson,
            prefsInspector = prefsInspector,
            dbInspector = dbInspector
        )

        try {
            server?.start()
            isRunning = true
            currentPort = port
            logServerStarted(port)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start DashInspector server: ${e.message}")
            server = null
        }
    }

    /**
     * Stop the DashInspector server.
     */
    @JvmStatic
    fun stop() {
        server?.stop()
        server = null
        isRunning = false
        Log.i(TAG, "DashInspector server stopped")
    }

    /**
     * Check if the server is currently running.
     */
    @JvmStatic
    fun isRunning(): Boolean = isRunning

    /**
     * Get the current port number.
     */
    @JvmStatic
    fun getPort(): Int = currentPort

    private fun logServerStarted(port: Int) {
        val border = "╔══════════════════════════════════════════════════════════"
        val divider = "╟──────────────────────────────────────────────────────────"
        val footer = "╚══════════════════════════════════════════════════════════"

        Log.i(TAG, border)
        Log.i(TAG, "║ 🚀 DashInspector: Server is now ONLINE")
        Log.i(TAG, "║ 🌐 Access via: http://localhost:$port")
        Log.i(TAG, divider)
        Log.i(TAG, "║ 💡 REMINDER: Run the following command to access:")
        Log.i(TAG, "║    adb forward tcp:$port tcp:$port")
        Log.i(TAG, footer)
    }
}