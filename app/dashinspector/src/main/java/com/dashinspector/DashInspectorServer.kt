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
                uri.startsWith("/api/") -> handleApiRequest(uri, method, session)

                // Static files (Web Frontend)
                else -> serveStaticFile(uri)
            }
        } ?: newFixedLengthResponse(
            Response.Status.BAD_REQUEST,
            "text/plain",
            "Bad Request"
        )
    }

    private fun handleApiRequest(uri: String, method: Method, session: IHTTPSession): Response {
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

            "/api/preferences/update" -> {
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

                val updatePrefRequest = try {
                    mGson.fromJson(
                        postData["postData"],
                        UpdatePrefRequest::class.java
                    )
                } catch (e: Exception) {
                    Log.e("DashInspectorServer", "Error parsing JSON")
                    null
                }

                Log.d("DashInspectorServer", "UpdatePrefRequest: $updatePrefRequest")
                if (updatePrefRequest == null) {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"Invalid request body\"}"
                    )
                }

                if (updatePrefRequest.name.isEmpty() ||
                    updatePrefRequest.key.isEmpty() ||
                    updatePrefRequest.type.isEmpty() ||
                    updatePrefRequest.value.isEmpty()
                ) {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"error\":\"Invalid request body\"}"
                    )
                }

                val response = mPrefsInspector.updatePreference(
                    updatePrefRequest.name,
                    updatePrefRequest.key,
                    updatePrefRequest.value,
                    updatePrefRequest.type
                )

                // This is a placeholder implementation
                return newFixedLengthResponse(
                    if (response["status"] == "success") Response.Status.OK else Response.Status.BAD_REQUEST,
                    "application/json",
                    response["message"].let { "{\"message\":\"$it\"}" }
                )
            }
            // Add more API endpoints as needed
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
}