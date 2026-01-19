package com.dashinspector

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

/**
 * HTTP server for DashInspector that handles API requests and serves the web frontend.
 */
internal class DashInspectorServer(
    private val context: Context,
    private val gson: Gson,
    private val prefsInspector: SharedPreferencesInspector,
    port: Int,
) : NanoHTTPD(port) {

    private val assetManager = context.assets

    companion object {
        private const val TAG = "DashInspectorServer"
        private const val CONTENT_TYPE_JSON = "application/json"

        private const val PREFS_PATH = "/api/preferences"

        private val MIME_TYPES = mapOf(
            "html" to "text/html",
            "css" to "text/css",
            "js" to "application/javascript",
            "json" to CONTENT_TYPE_JSON,
            "png" to "image/png",
            "svg" to "image/svg+xml",
            "ico" to "image/x-icon",
            "woff" to "font/woff",
            "woff2" to "font/woff2"
        )
    }

    override fun serve(session: IHTTPSession?): Response {
        if (session == null) {
            return badRequest("Invalid request")
        }

        val uri = session.uri
        val method = session.method

        return when {
            uri.startsWith("/api/preferences") -> handlePreferencesApi(uri, method, session)
            uri.startsWith("/api/database") -> handleDatabaseApi(uri, method, session)
            else -> serveStaticFile(uri)
        }
    }

    // region Preferences API

    private fun handlePreferencesApi(uri: String, method: Method, session: IHTTPSession): Response {
        return when (uri) {
            PREFS_PATH -> getPreferences()
            "$PREFS_PATH/create" -> handleCreatePreference(method, session)
            "$PREFS_PATH/remove" -> handleRemovePreference(method, session)
            "$PREFS_PATH/entry/add" -> handleAddEntry(method, session)
            "$PREFS_PATH/entry/update" -> handleUpdateEntry(method, session)
            "$PREFS_PATH/entry/remove" -> handleRemoveEntry(method, session)
            else -> notImplemented("API endpoint not found")
        }
    }

    private fun getPreferences(): Response {
        val prefs = prefsInspector.getAllPreferences()
        return jsonResponse(prefs)
    }

    private fun handleCreatePreference(method: Method, session: IHTTPSession): Response {
        return requirePost(method) {
            val request = parseBody<PrefRequest>(session) ?: return@requirePost badRequest("Invalid request body")

            if (!request.isValidForCreate()) {
                return@requirePost badRequest("Missing required fields: name, key, type")
            }

            if (!request.isValueValidForType()) {
                return@requirePost badRequest("Value is required for type ${request.type}")
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            handleInspectorResult(result, "SharedPreference '${request.name}' created successfully")
        }
    }

    private fun handleAddEntry(method: Method, session: IHTTPSession): Response {
        return requirePost(method) {
            val request = parseBody<PrefRequest>(session) ?: return@requirePost badRequest("Invalid request body")

            if (!request.isValidForCreate()) {
                return@requirePost badRequest("Missing required fields: name, key, type")
            }

            if (!request.isValueValidForType()) {
                return@requirePost badRequest("Value is required for type ${request.type}")
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            handleInspectorResult(result, "Entry added successfully")
        }
    }

    private fun handleUpdateEntry(method: Method, session: IHTTPSession): Response {
        return requirePost(method) {
            val request = parseBody<PrefRequest>(session) ?: return@requirePost badRequest("Invalid request body")

            if (!request.isValidForCreate()) {
                return@requirePost badRequest("Missing required fields: name, key, type")
            }

            if (!request.isValueValidForType()) {
                return@requirePost badRequest("Value is required for type ${request.type}")
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            handleInspectorResult(result, "Entry updated successfully")
        }
    }

    private fun handleRemoveEntry(method: Method, session: IHTTPSession): Response {
        return requirePost(method) {
            val request = parseBody<RemoveEntryRequest>(session)
                ?: return@requirePost badRequest("Invalid request body")

            if (request.name.isNullOrEmpty() || request.key.isNullOrEmpty()) {
                return@requirePost badRequest("Missing required fields: name, key")
            }

            val result = prefsInspector.removeEntry(
                prefName = request.name,
                key = request.key
            )

            handleInspectorResult(result)
        }
    }

    private fun handleRemovePreference(method: Method, session: IHTTPSession): Response {
        return requirePost(method) {
            val request = parseBody<RemovePrefRequest>(session)
                ?: return@requirePost badRequest("Invalid request body")

            if (request.name.isNullOrEmpty()) {
                return@requirePost badRequest("Missing required field: name")
            }

            val result = prefsInspector.removePreference(prefName = request.name)
            handleInspectorResult(result)
        }
    }

    // endregion

    // region Database API

    private fun handleDatabaseApi(uri: String, method: Method, session: IHTTPSession): Response {
        return notImplemented("Database API not implemented yet")
    }

    // endregion

    // region Static File Serving

    private fun serveStaticFile(uri: String): Response {
        val path = when {
            uri == "/" -> "index.html"
            uri.startsWith("/") -> uri.substring(1)
            else -> uri
        }

        return try {
            val inputStream = assetManager.open(path)
            val bytes = inputStream.readBytes()
            inputStream.close()

            newFixedLengthResponse(
                Response.Status.OK,
                getMimeType(path),
                ByteArrayInputStream(bytes),
                bytes.size.toLong()
            )
        } catch (e: FileNotFoundException) {
            // SPA fallback: serve index.html for routes without file extensions
            if (!path.contains(".")) {
                return serveStaticFile("/index.html")
            }
            notFound("File not found: $path")
        } catch (e: Exception) {
            internalError("Error serving file: ${e.message}")
        }
    }

    private fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "")
        return MIME_TYPES[extension] ?: "application/octet-stream"
    }

    // endregion

    // region Helper Functions

    private inline fun requirePost(method: Method, block: () -> Response): Response {
        return if (method == Method.POST) {
            block()
        } else {
            methodNotAllowed()
        }
    }

    private inline fun <reified T> parseBody(session: IHTTPSession): T? {
        return try {
            val postData = HashMap<String, String>()
            session.parseBody(postData)
            Log.d(TAG, "Request body: $postData")
            gson.fromJson(postData["postData"], T::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing request body: ${e.message}")
            null
        }
    }

    private fun handleInspectorResult(
        result: Map<String, String>,
        successMessage: String? = null
    ): Response {
        return if (result["status"] == "error") {
            badRequest(result["message"] ?: "Unknown error")
        } else {
            successResponse(successMessage ?: result["message"] ?: "Success")
        }
    }

    // endregion

    // region Response Builders

    private fun jsonResponse(data: Any): Response {
        return newFixedLengthResponse(
            Response.Status.OK,
            CONTENT_TYPE_JSON,
            gson.toJson(data)
        )
    }

    private fun successResponse(message: String): Response {
        return newFixedLengthResponse(
            Response.Status.OK,
            CONTENT_TYPE_JSON,
            """{"message":"$message"}"""
        )
    }

    private fun badRequest(message: String): Response {
        return newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            CONTENT_TYPE_JSON,
            """{"error":"$message"}"""
        )
    }

    private fun notFound(message: String): Response {
        return newFixedLengthResponse(
            Response.Status.NOT_FOUND,
            "text/plain",
            message
        )
    }

    private fun methodNotAllowed(): Response {
        return newFixedLengthResponse(
            Response.Status.METHOD_NOT_ALLOWED,
            CONTENT_TYPE_JSON,
            """{"error":"Method not allowed"}"""
        )
    }

    private fun notImplemented(message: String): Response {
        return newFixedLengthResponse(
            Response.Status.NOT_IMPLEMENTED,
            CONTENT_TYPE_JSON,
            """{"error":"$message"}"""
        )
    }

    private fun internalError(message: String): Response {
        return newFixedLengthResponse(
            Response.Status.INTERNAL_ERROR,
            "text/plain",
            message
        )
    }

    // endregion
}