package com.dashinspector.routes

import android.content.res.AssetManager
import android.util.Log
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.FileNotFoundException

/**
 * Defines routes for serving static files (web frontend).
 */
fun Route.staticRoutes(assetManager: AssetManager) {
    get("/{...}") {
        serveStaticFile(call, assetManager)
    }
}

private suspend fun serveStaticFile(call: ApplicationCall, assetManager: AssetManager) {
    val uri = call.request.path()
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
        call.response.header(HttpHeaders.ContentType, contentType.toString())
        call.respond(HttpStatusCode.OK, bytes)
    } catch (e: FileNotFoundException) {
        // SPA fallback: serve index.html for routes without file extensions
        if (!path.contains(".")) {
            serveIndexHtml(call, assetManager)
            return
        }
        call.respondText("File not found: $path", ContentType.Text.Plain, HttpStatusCode.NotFound)
    } catch (e: Exception) {
        Log.e("StaticRoutes", "Error serving file: ${e.message}")
        call.respondText("Error serving file: ${e.message}", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
    }
}

private suspend fun serveIndexHtml(call: ApplicationCall, assetManager: AssetManager) {
    try {
        val inputStream = assetManager.open("index.html")
        val bytes = inputStream.readBytes()
        inputStream.close()

        call.response.header(HttpHeaders.ContentType, ContentType.Text.Html.toString())
        call.respond(HttpStatusCode.OK, bytes)
    } catch (e: Exception) {
        Log.e("StaticRoutes", "Error serving index.html: ${e.message}")
        call.respondText("Error serving index.html", ContentType.Text.Plain, HttpStatusCode.InternalServerError)
    }
}

private fun getMimeType(path: String): ContentType {
    val mimeTypes = mapOf(
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

    val extension = path.substringAfterLast('.', "")
    return mimeTypes[extension] ?: ContentType.Application.OctetStream
}
