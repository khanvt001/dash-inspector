package com.dashinspector

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File

/**
 * Inspector class for reading and modifying SQLite/Room databases at runtime.
 */
internal class DatabaseInspector(private val context: Context) {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 50
        private const val MAX_PAGE_SIZE = 500

        // Only allow read-only queries for safety
        private val ALLOWED_QUERY_PREFIXES = listOf("SELECT", "PRAGMA", "EXPLAIN")

        // System tables to exclude from listing
        private val SYSTEM_TABLES = setOf(
            "android_metadata",
            "sqlite_sequence"
        )

        // Room-specific tables
        private val ROOM_TABLES = setOf(
            "room_master_table"
        )
    }

    /**
     * Retrieves all database files in the app's databases directory.
     */
    fun getAllDatabases(): DatabaseListResponse {
        val dbDir = context.getDatabasePath("_").parentFile
            ?: return DatabaseListResponse(emptyList(), 0)

        val databases = dbDir.listFiles()
            ?.filter { it.isFile && !it.name.endsWith("-journal") && !it.name.endsWith("-wal") && !it.name.endsWith("-shm") }
            ?.map { file ->
                DatabaseFile(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    isRoomDatabase = isRoomDatabase(file.absolutePath)
                )
            }
            ?: emptyList()

        return DatabaseListResponse(
            databases = databases,
            total = databases.size
        )
    }

    /**
     * Retrieves the schema (tables and columns) for a specific database.
     */
    fun getDatabaseSchema(databaseName: String): DatabaseSchemaResponse {
        val dbPath = getDatabasePath(databaseName)
        val db = openDatabase(dbPath) ?: throw IllegalArgumentException("Cannot open database: $databaseName")

        try {
            val tables = mutableListOf<TableInfo>()
            val tableNames = getTableNames(db)

            for (tableName in tableNames) {
                if (SYSTEM_TABLES.contains(tableName) || ROOM_TABLES.contains(tableName)) {
                    continue
                }

                val columns = getTableColumns(db, tableName)
                val rowCount = getTableRowCount(db, tableName)
                tables.add(TableInfo(name = tableName, columns = columns, rowCount = rowCount))
            }

            return DatabaseSchemaResponse(
                database = databaseName,
                tables = tables,
                isRoomDatabase = isRoomDatabase(dbPath)
            )
        } finally {
            db.close()
        }
    }

    /**
     * Retrieves paginated data from a specific table.
     */
    fun getTableData(
        databaseName: String,
        tableName: String,
        page: Int = 1,
        pageSize: Int = DEFAULT_PAGE_SIZE
    ): TableDataResponse {
        val dbPath = getDatabasePath(databaseName)
        val db = openDatabase(dbPath) ?: throw IllegalArgumentException("Cannot open database: $databaseName")

        try {
            val sanitizedPageSize = pageSize.coerceIn(1, MAX_PAGE_SIZE)
            val sanitizedPage = page.coerceAtLeast(1)
            val offset = (sanitizedPage - 1) * sanitizedPageSize

            val columns = getTableColumns(db, tableName).map { it.name }
            val totalRows = getTableRowCount(db, tableName)
            val totalPages = if (totalRows > 0) ((totalRows + sanitizedPageSize - 1) / sanitizedPageSize) else 1

            val rows = mutableListOf<List<Any?>>()
            val query = "SELECT * FROM \"${tableName.replace("\"", "\"\"")}\" LIMIT ? OFFSET ?"
            val cursor = db.rawQuery(query, arrayOf(sanitizedPageSize.toString(), offset.toString()))

            cursor.use {
                while (it.moveToNext()) {
                    rows.add(extractRow(it))
                }
            }

            return TableDataResponse(
                database = databaseName,
                table = tableName,
                columns = columns,
                rows = rows,
                totalRows = totalRows,
                page = sanitizedPage,
                pageSize = sanitizedPageSize,
                totalPages = totalPages
            )
        } finally {
            db.close()
        }
    }

    /**
     * Executes a read-only SQL query.
     */
    fun executeQuery(databaseName: String, query: String): QueryResponse {
        val trimmedQuery = query.trim()
        val upperQuery = trimmedQuery.uppercase()

        // Security check: only allow read-only queries
        val isAllowed = ALLOWED_QUERY_PREFIXES.any { upperQuery.startsWith(it) }
        if (!isAllowed) {
            throw SecurityException("Only SELECT, PRAGMA, and EXPLAIN queries are allowed")
        }

        val dbPath = getDatabasePath(databaseName)
        val db = openDatabase(dbPath) ?: throw IllegalArgumentException("Cannot open database: $databaseName")

        try {
            val startTime = System.currentTimeMillis()
            val cursor = db.rawQuery(trimmedQuery, null)
            val columns = cursor.columnNames.toList()
            val rows = mutableListOf<List<Any?>>()

            cursor.use {
                while (it.moveToNext()) {
                    rows.add(extractRow(it))
                }
            }

            val executionTime = System.currentTimeMillis() - startTime

            return QueryResponse(
                database = databaseName,
                query = trimmedQuery,
                columns = columns,
                rows = rows,
                rowCount = rows.size,
                executionTimeMs = executionTime
            )
        } finally {
            db.close()
        }
    }

    /**
     * Updates a row in a table.
     */
    fun updateRow(
        databaseName: String,
        tableName: String,
        primaryKey: Map<String, Any?>,
        values: Map<String, Any?>
    ): Map<String, String> {
        if (primaryKey.isEmpty()) {
            return errorResult("Primary key is required for update")
        }
        if (values.isEmpty()) {
            return errorResult("No values to update")
        }

        val dbPath = getDatabasePath(databaseName)
        val db = openDatabaseWritable(dbPath)
            ?: return errorResult("Cannot open database for writing: $databaseName")

        try {
            val setClause = values.keys.joinToString(", ") { "\"${it.replace("\"", "\"\"")}\" = ?" }
            val whereClause = primaryKey.keys.joinToString(" AND ") { "\"${it.replace("\"", "\"\"")}\" = ?" }

            val sql = "UPDATE \"${tableName.replace("\"", "\"\"")}\" SET $setClause WHERE $whereClause"
            val args = (values.values + primaryKey.values).map { it?.toString() ?: "" }.toTypedArray()

            db.execSQL(sql, args)
            return successResult("Row updated successfully")
        } catch (e: Exception) {
            return errorResult("Failed to update row: ${e.message}")
        } finally {
            db.close()
        }
    }

    /**
     * Deletes a row from a table.
     */
    fun deleteRow(
        databaseName: String,
        tableName: String,
        primaryKey: Map<String, Any?>
    ): Map<String, String> {
        if (primaryKey.isEmpty()) {
            return errorResult("Primary key is required for delete")
        }

        val dbPath = getDatabasePath(databaseName)
        val db = openDatabaseWritable(dbPath)
            ?: return errorResult("Cannot open database for writing: $databaseName")

        try {
            val whereClause = primaryKey.keys.joinToString(" AND ") { "\"${it.replace("\"", "\"\"")}\" = ?" }
            val sql = "DELETE FROM \"${tableName.replace("\"", "\"\"")}\" WHERE $whereClause"
            val args = primaryKey.values.map { it?.toString() ?: "" }.toTypedArray()

            db.execSQL(sql, args)
            return successResult("Row deleted successfully")
        } catch (e: Exception) {
            return errorResult("Failed to delete row: ${e.message}")
        } finally {
            db.close()
        }
    }

    /**
     * Inserts a new row into a table.
     */
    fun insertRow(
        databaseName: String,
        tableName: String,
        values: Map<String, Any?>
    ): Map<String, String> {
        if (values.isEmpty()) {
            return errorResult("No values to insert")
        }

        val dbPath = getDatabasePath(databaseName)
        val db = openDatabaseWritable(dbPath)
            ?: return errorResult("Cannot open database for writing: $databaseName")

        try {
            val columns = values.keys.joinToString(", ") { "\"${it.replace("\"", "\"\"")}\"" }
            val placeholders = values.keys.joinToString(", ") { "?" }
            val sql = "INSERT INTO \"${tableName.replace("\"", "\"\"")}\" ($columns) VALUES ($placeholders)"
            val args = values.values.map { it?.toString() ?: "" }.toTypedArray()

            db.execSQL(sql, args)
            return successResult("Row inserted successfully")
        } catch (e: Exception) {
            return errorResult("Failed to insert row: ${e.message}")
        } finally {
            db.close()
        }
    }

    /**
     * Retrieves ERD (Entity Relationship Diagram) data for a database.
     * Includes tables, columns, and foreign key relationships.
     */
    fun getERD(databaseName: String): ERDResponse {
        val dbPath = getDatabasePath(databaseName)
        val db = openDatabase(dbPath) ?: throw IllegalArgumentException("Cannot open database: $databaseName")

        try {
            val tables = mutableListOf<ERDTableInfo>()
            val relationships = mutableListOf<ERDRelationship>()
            val tableNames = getTableNames(db)

            // Build table info with foreign keys
            for (tableName in tableNames) {
                if (SYSTEM_TABLES.contains(tableName) || ROOM_TABLES.contains(tableName)) {
                    continue
                }

                val columns = getTableColumns(db, tableName)
                val foreignKeys = getTableForeignKeys(db, tableName)

                tables.add(ERDTableInfo(
                    name = tableName,
                    columns = columns,
                    foreignKeys = foreignKeys
                ))

                // Build relationships from foreign keys
                for (fk in foreignKeys) {
                    // Determine relationship type based on primary key
                    val fromColumn = columns.find { it.name == fk.from }
                    val isPrimaryKey = fromColumn?.pk ?: 0 > 0
                    val relationshipType = if (isPrimaryKey) "one-to-one" else "many-to-one"

                    relationships.add(ERDRelationship(
                        fromTable = tableName,
                        fromColumn = fk.from,
                        toTable = fk.table,
                        toColumn = fk.to,
                        relationshipType = relationshipType
                    ))
                }
            }

            Log.d("DashInspector","Table: $tables")
            Log.d("DashInspector","Relationships: $relationships")

            return ERDResponse(
                database = databaseName,
                tables = tables,
                relationships = relationships,
                isRoomDatabase = isRoomDatabase(dbPath)
            )
        } finally {
            db.close()
        }
    }

    // region Private Helpers

    private fun getDatabasePath(databaseName: String): String {
        return context.getDatabasePath(databaseName).absolutePath
    }

    private fun openDatabase(path: String): SQLiteDatabase? {
        return try {
            SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            null
        }
    }

    private fun openDatabaseWritable(path: String): SQLiteDatabase? {
        return try {
            SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE)
        } catch (e: Exception) {
            null
        }
    }

    private fun isRoomDatabase(path: String): Boolean {
        val db = openDatabase(path) ?: return false
        return try {
            val cursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='room_master_table'",
                null
            )
            val hasRoomTable = cursor.count > 0
            cursor.close()
            hasRoomTable
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    private fun getTableNames(db: SQLiteDatabase): List<String> {
        val tables = mutableListOf<String>()
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name",
            null
        )
        cursor.use {
            while (it.moveToNext()) {
                tables.add(it.getString(0))
            }
        }
        return tables
    }

    private fun getTableColumns(db: SQLiteDatabase, tableName: String): List<ColumnInfo> {
        val columns = mutableListOf<ColumnInfo>()
        val cursor = db.rawQuery("PRAGMA table_info(\"${tableName.replace("\"", "\"\"")}\")", null)
        cursor.use {
            while (it.moveToNext()) {
                columns.add(
                    ColumnInfo(
                        cid = it.getInt(0),
                        name = it.getString(1),
                        type = it.getString(2) ?: "TEXT",
                        notnull = it.getInt(3) == 1,
                        defaultValue = it.getString(4),
                        pk = it.getInt(5)
                    )
                )
            }
        }
        return columns
    }

    private fun getTableForeignKeys(db: SQLiteDatabase, tableName: String): List<ForeignKeyInfo> {
        val foreignKeys = mutableListOf<ForeignKeyInfo>()
        val cursor = db.rawQuery("PRAGMA foreign_key_list(\"${tableName.replace("\"", "\"\"")}\")", null)
        cursor.use {
            while (it.moveToNext()) {
                foreignKeys.add(
                    ForeignKeyInfo(
                        id = it.getInt(0),
                        seq = it.getInt(1),
                        table = it.getString(2),
                        from = it.getString(3),
                        to = it.getString(4) ?: it.getString(3), // Use 'from' if 'to' is null
                        onUpdate = it.getString(5) ?: "NO ACTION",
                        onDelete = it.getString(6) ?: "NO ACTION"
                    )
                )
            }
        }
        return foreignKeys
    }

    private fun getTableRowCount(db: SQLiteDatabase, tableName: String): Int {
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM \"${tableName.replace("\"", "\"\"")}\"",
            null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    private fun extractRow(cursor: Cursor): List<Any?> {
        val row = mutableListOf<Any?>()
        for (i in 0 until cursor.columnCount) {
            val value = when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> null
                Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                Cursor.FIELD_TYPE_STRING -> cursor.getString(i)
                Cursor.FIELD_TYPE_BLOB -> {
                    val blob = cursor.getBlob(i)
                    "[BLOB: ${blob.size} bytes]"
                }
                else -> cursor.getString(i)
            }
            row.add(value)
        }
        return row
    }

    private fun successResult(message: String): Map<String, String> {
        return mapOf("status" to "success", "message" to message)
    }

    private fun errorResult(message: String): Map<String, String> {
        return mapOf("status" to "error", "message" to message)
    }

    // endregion
}
