package com.dashinspector.handlers

import android.util.Log
import com.dashinspector.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handler for database-related API requests.
 */
class DatabaseHandler(
    private val dbInspector: DatabaseInspector
) {
    companion object {
        private const val TAG = "DatabaseHandler"
    }

    suspend fun getAllDatabases(call: ApplicationCall) {
        try {
            val databases = withContext(Dispatchers.IO) {
                dbInspector.getAllDatabases()
            }
            call.respond(HttpStatusCode.OK, databases)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting databases: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Failed to get databases: ${e.message}")
        }
    }

    suspend fun getSchema(call: ApplicationCall) {
        try {
            val request = call.receive<DatabaseRequest>()

            if (request.database.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }

            val schema = withContext(Dispatchers.IO) {
                dbInspector.getDatabaseSchema(request.database)
            }
            call.respond(HttpStatusCode.OK, schema)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting schema: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Failed to get schema: ${e.message}")
        }
    }

    suspend fun getERD(call: ApplicationCall) {
        try {
            val request = call.receive<DatabaseRequest>()

            if (request.database.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }

            val erd = withContext(Dispatchers.IO) {
                dbInspector.getERD(request.database)
            }
            call.respond(HttpStatusCode.OK, erd)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting ERD: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Failed to get ERD: ${e.message}")
        }
    }

    suspend fun getTableData(call: ApplicationCall) {
        try {
            val request = call.receive<TableDataRequest>()

            if (request.database.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.table.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: table")
                return
            }

            val data = withContext(Dispatchers.IO) {
                dbInspector.getTableData(
                    databaseName = request.database,
                    tableName = request.table,
                    page = request.page,
                    pageSize = request.pageSize
                )
            }
            call.respond(HttpStatusCode.OK, data)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting table data: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Failed to get table data: ${e.message}")
        }
    }

    suspend fun executeQuery(call: ApplicationCall) {
        try {
            val request = call.receive<QueryRequest>()

            if (request.database.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.query.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: query")
                return
            }

            val result = withContext(Dispatchers.IO) {
                dbInspector.executeQuery(request.database, request.query)
            }
            call.respond(HttpStatusCode.OK, result)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security error executing query: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, e.message ?: "Query not allowed")
        } catch (e: Exception) {
            Log.e(TAG, "Error executing query: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Failed to execute query: ${e.message}")
        }
    }

    suspend fun updateRow(call: ApplicationCall) {
        try {
            val request = call.receive<UpdateRowRequest>()

            if (request.database.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.table.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: table")
                return
            }
            if (request.primaryKey.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: primaryKey")
                return
            }
            if (request.values.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: values")
                return
            }

            val result = withContext(Dispatchers.IO) {
                dbInspector.updateRow(
                    databaseName = request.database,
                    tableName = request.table,
                    primaryKey = request.primaryKey,
                    values = request.values
                )
            }

            call.handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating row: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    suspend fun deleteRow(call: ApplicationCall) {
        try {
            val request = call.receive<DeleteRowRequest>()

            if (request.database.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.table.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: table")
                return
            }
            if (request.primaryKey.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: primaryKey")
                return
            }

            val result = withContext(Dispatchers.IO) {
                dbInspector.deleteRow(
                    databaseName = request.database,
                    tableName = request.table,
                    primaryKey = request.primaryKey
                )
            }

            call.handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting row: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    suspend fun insertRow(call: ApplicationCall) {
        try {
            val request = call.receive<InsertRowRequest>()

            if (request.database.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: database")
                return
            }
            if (request.table.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: table")
                return
            }
            if (request.values.isNullOrEmpty()) {
                call.respondError(HttpStatusCode.BadRequest, "Missing required field: values")
                return
            }

            val result = withContext(Dispatchers.IO) {
                dbInspector.insertRow(
                    databaseName = request.database,
                    tableName = request.table,
                    values = request.values
                )
            }

            call.handleInspectorResult(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting row: ${e.message}")
            call.respondError(HttpStatusCode.BadRequest, "Invalid request body")
        }
    }

    private suspend fun ApplicationCall.handleInspectorResult(
        result: Map<String, String>
    ) {
        if (result["status"] == "error") {
            respondError(HttpStatusCode.BadRequest, result["message"] ?: "Unknown error")
        } else {
            respondSuccess(result["message"] ?: "Success")
        }
    }

    private suspend fun ApplicationCall.respondSuccess(message: String) {
        respond(HttpStatusCode.OK, mapOf("message" to message))
    }

    private suspend fun ApplicationCall.respondError(status: HttpStatusCode, message: String) {
        respond(status, mapOf("error" to message))
    }
}
