package com.dashinspector

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException

class DashInspectorServer(
    private val context: Context,
    private val mGson: Gson,
    private val mPrefsInspector: SharedPreferencesInspector,
    port: Int,
) : NanoHTTPD(port) {

    private val assetManager = context.assets

    private val mimeTypes = mapOf(
        "html" to "text/html",
        "css" to "text/css",
        "js" to "application/javascript",
        "json" to "application/json",
        "png" to "image/png",
        "svg" to "image/svg+xml",
        "ico" to "image/x-icon",
        "woff" to "font/woff",
        "woff2" to "font/woff2"
    )

    private fun getMimeType(path: String): String {
        val extension = path.substringAfterLast('.', "")
        return mimeTypes[extension] ?: "application/octet-stream"
    }

    override fun serve(session: IHTTPSession?): Response {
        return session?.let {
            val msg = "<html><body><h1>Dash Inspector HTTP Server</h1>\n" +
                    "<p>This is a simple HTTP server running inside the Dash Inspector app.</p>" +
                    "</body></html>\n"

            val uri = session.uri
            val method = session.method

            when {
                // API endpoints
                uri.startsWith("/api/preferences") -> handlePrefsRequest(uri, method, session)
                uri.startsWith("/api/database") -> handleDatabaseRequest(uri, method, session)
                // Static files (Web Frontend)
                else -> serveStaticFile(uri)
            }
        } ?: newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            "text/plain",
            "Bad Request"
        )
    }

    private fun handlePrefsRequest(uri: String, method: Method, session: IHTTPSession): Response {
        // Placeholder for API request handling logic
        when (uri) {
            "/api/preferences" -> {
                val prefs = mPrefsInspector.getAllPreferences()
                val response = mGson.toJson(prefs)
                return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    response
                )
            }

            "/api/preferences/entry/add",
            "/api/preferences/entry/update",
            "/api/preferences/create" -> {
                if (method != Method.POST) {
                    return newFixedLengthResponse(
                        Response.Status.METHOD_NOT_ALLOWED,
                        "application/json",
                        "{\"error\":\"Method not allowed\"}"
                    )
                }
                val postData = HashMap<String, String>()
                session.parseBody(postData)
                Log.d("DashInspectorServer", "Post data: $postData")

                val prefRequest = try {
                    mGson.fromJson(postData["postData"], PrefRequest::class.java)
                } catch (ex: Exception) {
                    Log.e("DashInspectorServer", "Error parsing JSON")
                    null
                }

                if (prefRequest == null) {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"Invalid request body\"}"
                    )
                }

                Log.d("DashInspectorServer", "AddPrefRequest: $prefRequest")
                if (prefRequest.name.isNullOrEmpty() ||
                    prefRequest.type.isNullOrEmpty() ||
                    prefRequest.key.isNullOrEmpty()
                    ) {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"Invalid request body\"}"
                    )
                }

                if(prefRequest.type != "String" && prefRequest.type != "Set") {
                    if(prefRequest.value.isNullOrEmpty()) {
                        return newFixedLengthResponse(
                            Response.Status.BAD_REQUEST,
                            "application/json",
                            "{\"error\":\"Invalid request body\"}"
                        )
                    }
                }

                val response = mPrefsInspector.upsertPreference(
                    prefName = prefRequest.name,
                    key = prefRequest.key,
                    value = prefRequest.value,
                    type = prefRequest.type
                )

                if(response["status"] == "error") {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"${response["message"]}\"}"
                    )
                }

                val prefAction = when(uri) {
                    "/api/preferences/entry/add" -> PrefAction.ADD_ENTRY
                    "/api/preferences/entry/update" -> PrefAction.UPDATE_ENTRY
                    "/api/preferences/create" -> PrefAction.ADD_PREF
                    else -> PrefAction.UNKNOWN
                }

                val message = when(prefAction) {
                    PrefAction.ADD_ENTRY -> "Entry added successfully"
                    PrefAction.UPDATE_ENTRY -> "Entry updated successfully"
                    PrefAction.ADD_PREF -> "SharedPreferences '${prefRequest.name}' created successfully"
                    else -> "Operation successful"
                }

                return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"message\":\"$message\"}"
                )
            }
            "/api/preferences/entry/remove" -> {
                if (method != Method.POST) {
                    return newFixedLengthResponse(
                        Response.Status.METHOD_NOT_ALLOWED,
                        "application/json",
                        "{\"error\":\"Method not allowed\"}"
                    )
                }
                val postData = HashMap<String, String>()
                session.parseBody(postData)
                Log.d("DashInspectorServer", "Post data: $postData")

                val removeEntryRequest = try {
                    mGson.fromJson(postData["postData"], RemoveEntryRequest::class.java)
                } catch (ex: Exception) {
                    Log.e("DashInspectorServer", "Error parsing JSON")
                    null
                }

                if (removeEntryRequest == null ||
                    removeEntryRequest.name.isNullOrEmpty() ||
                    removeEntryRequest.key.isNullOrEmpty()
                ) {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"Invalid request body\"}"
                    )
                }

                Log.d("DashInspectorServer", "RemoveEntryRequest: $removeEntryRequest")

                val response = mPrefsInspector.removeEntry(
                    prefName = removeEntryRequest.name,
                    key = removeEntryRequest.key
                )

                if(response["status"] == "error") {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"${response["message"]}\"}"
                    )
                }

                return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"message\":\"${response["message"]}\"}"
                )
            }
            "/api/preferences/remove" -> {
                if (method != Method.POST) {
                    return newFixedLengthResponse(
                        Response.Status.METHOD_NOT_ALLOWED,
                        "application/json",
                        "{\"error\":\"Method not allowed\"}"
                    )
                }
                val postData = HashMap<String, String>()
                session.parseBody(postData)
                Log.d("DashInspectorServer", "Post data: $postData")
                val removePrefRequest = try {
                    mGson.fromJson(postData["postData"], RemovePrefRequest::class.java)
                } catch (ex: Exception) {
                    Log.e("DashInspectorServer", "Error parsing JSON")
                    null
                }
                if (removePrefRequest == null ||
                    removePrefRequest.name.isNullOrEmpty()
                ) {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"Invalid request body\"}"
                    )
                }
                Log.d("DashInspectorServer", "RemovePrefRequest: $removePrefRequest")
                val response = mPrefsInspector.removePreference(
                    prefName = removePrefRequest.name
                )
                if(response["status"] == "error") {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"${response["message"]}\"}"
                    )
                }
                return newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    "{\"message\":\"${response["message"]}\"}"
                )
            }
        }

        return newFixedLengthResponse(
            Response.Status.NOT_IMPLEMENTED,
            "application/json",
            "{\"error\":\"API not implemented yet\"}"
        )
    }

    private fun serveStaticFile(uri: String): Response {
        val path = when {
            uri == "/" -> "index.html"
            uri.startsWith("/") -> uri.substring(1)
            else -> uri
        }

        val assetPath = path

        return try {
            val inputStream = assetManager.open(assetPath)
            val mimeType = getMimeType(path)

            val bytes = inputStream.readBytes()
            inputStream.close()

            newFixedLengthResponse(
                Response.Status.OK,
                mimeType,
                ByteArrayInputStream(bytes),
                bytes.size.toLong()
            )
        } catch (e: FileNotFoundException) {
            if (!path.contains(".")) {
                return serveStaticFile("/index.html")
            }
            newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "text/plain",
                "File not found: $path"
            )
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                "text/plain",
                "Error: ${e.message}"
            )
        }
    }

    private fun handleDatabaseRequest(uri: String, method: Method, session: IHTTPSession): Response {
        // Placeholder for Database request handling logic
        return newFixedLengthResponse(
            Response.Status.NOT_IMPLEMENTED,
            "application/json",
            "{\"error\":\"Database API not implemented yet\"}"
        )

    }
}