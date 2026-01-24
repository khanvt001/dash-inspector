package com.dashinspector.handlers

import android.util.Log
import com.dashinspector.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

/**
 * Handler for SharedPreferences-related API requests.
 */
class PreferencesHandler(
    private val prefsInspector: SharedPreferencesInspector
) {
    companion object {
        private const val TAG = "PreferencesHandler"
    }

    suspend fun getAllPreferences(call: ApplicationCall) {
        val prefs = prefsInspector.getAllPreferences()
        call.respond(HttpStatusCode.OK, prefs)
    }

    suspend fun createPreference(call: ApplicationCall) {
        try {
            val request = call.receive<PrefRequest>()

            if (!request.isValidForCreate()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required fields: name, key, type")
                return
            }

            if (!request.isValueValidForType()) {
                call.respondError(HttpStatusCode.BadRequest, "Value is required for type ${request.type}")
                return
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            call.handleInspectorResult(result, "SharedPreference '${request.name}' created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating preference: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    suspend fun addEntry(call: ApplicationCall) {
        try {
            val request = call.receive<PrefRequest>()

            if (!request.isValidForCreate()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required fields: name, key, type")
                return
            }

            if (!request.isValueValidForType()) {
                call.respondError(HttpStatusCode.BadRequest, "Value is required for type ${request.type}")
                return
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            call.handleInspectorResult(result, "Entry added successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error adding entry: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    suspend fun updateEntry(call: ApplicationCall) {
        try {
            val request = call.receive<PrefRequest>()

            if (!request.isValidForCreate()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required fields: name, key, type")
                return
            }

            if (!request.isValueValidForType()) {
                call.respondError(HttpStatusCode.BadRequest, "Value is required for type ${request.type}")
                return
            }

            val result = prefsInspector.upsertPreference(
                prefName = request.name!!,
                key = request.key!!,
                value = request.value,
                type = request.type!!
            )

            call.handleInspectorResult(result, "Entry updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating entry: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    suspend fun removeEntry(call: ApplicationCall) {
        try {
            val request = call.receive<RemoveEntryRequest>()

            if (request.name.isNullOrEmpty() || request.key.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required fields: name, key")
                return
            }

            val result = prefsInspector.removeEntry(
                prefName = request.name,
                key = request.key
            )

            call.handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing entry: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    suspend fun removePreference(call: ApplicationCall) {
        try {
            val request = call.receive<RemovePrefRequest>()

            if (request.name.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: name")
                return
            }

            val result = prefsInspector.removePreference(prefName = request.name)
            call.handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing preference: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

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
}
