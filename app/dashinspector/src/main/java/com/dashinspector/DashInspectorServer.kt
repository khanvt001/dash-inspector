package com.dashinspector

import android.content.Context
import android.util.Log
import com.dashinspector.handlers.DatabaseHandler
import com.dashinspector.handlers.PreferencesHandler
import com.dashinspector.routes.databaseRoutes
import com.dashinspector.routes.preferencesRoutes
import com.dashinspector.routes.staticRoutes
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*

/**
 * HTTP server orchestrator for DashInspector.
 *
 * Coordinates the setup of Ktor server, plugins, handlers, and routes.
 */
internal class DashInspectorServer(
    private val context: Context,
    private val gson: Gson,
    private val prefsInspector: SharedPreferencesInspector,
    private val dbInspector: DatabaseInspector,
    private val port: Int,
) {

    private val assetManager = context.assets
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null

    companion object {
        private const val TAG = "DashInspectorServer"
    }

    /**
     * Starts the Ktor server with all configured routes and plugins.
     */
    fun start() {
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
            configurePlugins()
            configureRoutes()
        }

        server?.start(wait = false)
        Log.d(TAG, "DashInspector server started on port $port")
    }

    /**
     * Stops the Ktor server.
     */
    fun stop() {
        server?.stop(1000, 2000)
        Log.d(TAG, "DashInspector server stopped")
    }

    /**
     * Configures Ktor plugins for content negotiation and CORS.
     */
    private fun Application.configurePlugins() {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }

        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
            allowMethod(HttpMethod.Delete)
            allowMethod(HttpMethod.Options)
        }
    }

    /**
     * Configures all application routes by delegating to feature-specific route modules.
     */
    private fun Application.configureRoutes() {
        // Initialize handlers
        val preferencesHandler = PreferencesHandler(prefsInspector)
        val databaseHandler = DatabaseHandler(dbInspector)

        routing {
            // Register feature routes
            preferencesRoutes(preferencesHandler)
            databaseRoutes(databaseHandler)
            staticRoutes(assetManager)
        }
    }
}

// region Extension functions for validation
internal fun PrefRequest.isValidForCreate(): Boolean {
    return !name.isNullOrEmpty() && !key.isNullOrEmpty() && !type.isNullOrEmpty()
}

internal fun PrefRequest.isValueValidForType(): Boolean {
    return when (type) {
        "Boolean", "Int", "Long", "Float", "String" -> !value.isNullOrEmpty()
        else -> false
    }
}
// endregion
