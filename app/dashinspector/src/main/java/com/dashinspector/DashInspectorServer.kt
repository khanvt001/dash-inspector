package com.dashinspector

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.FileNotFoundException

/**
 * HTTP server for DashInspector that handles API requests and serves the web frontend.
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
        private const val CONTENT_TYPE_JSON = "application/json"

        private val MIME_TYPES = mapOf(
            "html" to ContentType.Text.Html,
            "css" to ContentType.Text.CSS,
            "js" to ContentType.Application.JavaScript,
            "json" to ContentType.Application.Json,
            "png" to ContentType.Image.PNG,
            "svg" to ContentType.Image.SVG,
            "ico" to ContentType.parse("image/x-icon"),
            "woff" to ContentType.parse("font/woff"),
            "woff2" to ContentType.parse("font/woff2")
        )
    }

    fun start() {
        server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
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

            routing {
                // Preferences API routes
                get("/api/preferences") {
                    handleGetPreferences()
                }

                post("/api/preferences/create") {
                    handleCreatePreference()
                }

                post("/api/preferences/remove") {
                    handleRemovePreference()
                }

                post("/api/preferences/entry/add") {
                    handleAddEntry()
                }

                post("/api/preferences/entry/update") {
                    handleUpdateEntry()
                }

                post("/api/preferences/entry/remove") {
                    handleRemoveEntry()
                }

                // Database API routes
                get("/api/database") {
                    handleGetDatabases()
                }

                post("/api/database/schema") {
                    handleGetSchema()
                }

                post("/api/database/erd") {
                    handleGetERD()
                }

                post("/api/database/table/data") {
                    handleGetTableData()
                }

                post("/api/database/query") {
                    handleExecuteQuery()
                }

                post("/api/database/table/update") {
                    handleUpdateRow()
                }

                post("/api/database/table/delete") {
                    handleDeleteRow()
                }

                post("/api/database/table/insert") {
                    handleInsertRow()
                }

                // Static file serving
                get("/{...}") {
                    handleStaticFile(call.request.path())
                }
            }
        }

        server?.start(wait = false)
        Log.d(TAG, "DashInspector server started on port $port")
    }

    fun stop() {
        server?.stop(1000, 2000)
        Log.d(TAG, "DashInspector server stopped")
    }

    // region Preferences API Handlers

    private suspend fun ApplicationCall.handleGetPreferences() {
        val prefs = prefsInspector.getAllPreferences()
        respond(HttpStatusCode.OK, prefs)
    }

    private suspend fun ApplicationCall.handleCreatePreference() {
        try {
            val request = receive<PrefRequest>()

            if (!request.isValidForCreate()) {
                respondError(HttpStatusCode.BadRequest, "Missing required fields: name, key, type")
                return
            }

            if (!request.isValueValidForType()) {
                respondError(HttpStatusCode.BadRequest, "Value is required for type ${request.type}")
                return
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            handleInspectorResult(result, "SharedPreference '${request.name}' created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating preference: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    private suspend fun ApplicationCall.handleAddEntry() {
        try {
            val request = receive<PrefRequest>()

            if (!request.isValidForCreate()) {
                respondError(HttpStatusCode.BadRequest, "Missing required fields: name, key, type")
                return
            }

            if (!request.isValueValidForType()) {
                respondError(HttpStatusCode.BadRequest, "Value is required for type ${request.type}")
                return
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            handleInspectorResult(result, "Entry added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding entry: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    private suspend fun ApplicationCall.handleUpdateEntry() {
        try {
            val request = receive<PrefRequest>()

            if (!request.isValidForCreate()) {
                respondError(HttpStatusCode.BadRequest, "Missing required fields: name, key, type")
                return
            }

            if (!request.isValueValidForType()) {
                respondError(HttpStatusCode.BadRequest, "Value is required for type ${request.type}")
                return
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            handleInspectorResult(result, "Entry updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating entry: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    private suspend fun ApplicationCall.handleRemoveEntry() {
        try {
            val request = receive<RemoveEntryRequest>()

            if (request.name.isNullOrEmpty() || request.key.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required fields: name, key")
                return
            }

            val result = prefsInspector.removeEntry(
                prefName = request.name,
                key = request.key
            )

            handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing entry: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    private suspend fun ApplicationCall.handleRemovePreference() {
        try {
            val request = receive<RemovePrefRequest>()

            if (request.name.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: name")
                return
            }

            val result = prefsInspector.removePreference(prefName = request.name)
            handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing preference: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    // endregion

    // region Database API Handlers

    private suspend fun ApplicationCall.handleGetDatabases() {
        try {
            val databases = runBlocking(Dispatchers.IO) {
                dbInspector.getAllDatabases()
            }
            respond(HttpStatusCode.OK, databases)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting databases: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Failed to get databases: ${e.message}")
        }
    }

    private suspend fun ApplicationCall.handleGetSchema() {
        try {
            val request = receive<DatabaseRequest>()

            if (request.database.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }

            val schema = runBlocking(Dispatchers.IO) {
                dbInspector.getDatabaseSchema(request.database)
            }
            respond(HttpStatusCode.OK, schema)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting schema: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Failed to get schema: ${e.message}")
        }
    }

    private suspend fun ApplicationCall.handleGetERD() {
        try {
            val request = receive<DatabaseRequest>()

            if (request.database.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }

            val erd = runBlocking(Dispatchers.IO) {
                dbInspector.getERD(request.database)
            }
            respond(HttpStatusCode.OK, erd)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ERD: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Failed to get ERD: ${e.message}")
        }
    }

    private suspend fun ApplicationCall.handleGetTableData() {
        try {
            val request = receive<TableDataRequest>()

            if (request.database.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.table.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: table")
                return
            }

            val data = runBlocking(Dispatchers.IO) {
                dbInspector.getTableData(
                    databaseName = request.database,
                    tableName = request.table,
                    page = request.page,
                    pageSize = request.pageSize
                )
            }
            respond(HttpStatusCode.OK, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting table data: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Failed to get table data: ${e.message}")
        }
    }

    private suspend fun ApplicationCall.handleExecuteQuery() {
        try {
            val request = receive<QueryRequest>()

            if (request.database.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.query.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: query")
                return
            }

            val result = runBlocking(Dispatchers.IO) {
                dbInspector.executeQuery(request.database, request.query)
            }
            respond(HttpStatusCode.OK, result)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error executing query: ${e.message}")
            respondError(HttpStatusCode.BadRequest, e.message ?: "Query not allowed")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing query: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Failed to execute query: ${e.message}")
        }
    }

    private suspend fun ApplicationCall.handleUpdateRow() {
        try {
            val request = receive<UpdateRowRequest>()

            if (request.database.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.table.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: table")
                return
            }
            if (request.primaryKey.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: primaryKey")
                return
            }
            if (request.values.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: values")
                return
            }

            val result = runBlocking(Dispatchers.IO) {
                dbInspector.updateRow(
                    databaseName = request.database,
                    tableName = request.table,
                    primaryKey = request.primaryKey,
                    values = request.values
                )
            }

            handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating row: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    private suspend fun ApplicationCall.handleDeleteRow() {
        try {
            val request = receive<DeleteRowRequest>()

            if (request.database.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.table.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: table")
                return
            }
            if (request.primaryKey.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: primaryKey")
                return
            }

            val result = runBlocking(Dispatchers.IO) {
                dbInspector.deleteRow(
                    databaseName = request.database,
                    tableName = request.table,
                    primaryKey = request.primaryKey
                )
            }

            handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting row: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    private suspend fun ApplicationCall.handleInsertRow() {
        try {
            val request = receive<InsertRowRequest>()

            if (request.database.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.table.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: table")
                return
            }
            if (request.values.isNullOrEmpty()) {
                respondError(HttpStatusCode.BadRequest, "Missing required field: values")
                return
            }

            val result = runBlocking(Dispatchers.IO) {
                dbInspector.insertRow(
                    databaseName = request.database,
                    tableName = request.table,
                    values = request.values
                )
            }

            handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting row: ${e.message}")
            respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    // endregion

    // region Static File Serving

    private suspend fun ApplicationCall.handleStaticFile(uri: String) {
        val path = when {
            uri == "/" -> "index.html"
            uri.startsWith("/") -> uri.substring(1)
            else -> uri
        }

        try {
            val inputStream = assetManager.open(path)
            val bytes = inputStream.readBytes()
            inputStream.close()

            val contentType = getMimeType(path)
            response.header(HttpHeaders.ContentType, contentType.toString())
            respond(HttpStatusCode.OK, bytes)
        } catch (e: FileNotFoundException) {
            // SPA fallback: serve index.html for routes without file extensions
            if (!path.contains(".")) {
                handleStaticFile("/")
                return
            }
            respondText("File not found: $path", ContentType.Text.Plain, HttpStatusCode.NotFound)
        } catch (e: Exception) {
            Log.e(TAG, "Error serving file: ${e.message}")
            respondText("Error serving file: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
        }
    }

    private fun getMimeType(path: String): ContentType {
        val extension = path.substringAfterLast('.', "")
        return MIME_TYPES[extension] ?: ContentType.Application.OctetStream
    }

    // endregion

    // region Helper Functions

    private suspend fun ApplicationCall.handleInspectorResult(
        result: Map<String, String>,
        successMessage: String? = null
    ) {
        if (result["status"] == "error") {
            respondError(HttpStatusCode.BadRequest, result["message"] ?: "Unknown error")
        } else {
            respondSuccess(successMessage ?: result["message"] ?: "Success")
        }
    }

    private suspend fun ApplicationCall.respondSuccess(message: String) {
        respond(HttpStatusCode.OK, mapOf("message" to message))
    }

    private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) {
        respond(status, mapOf("error" to message))
    }

    // endregion
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
