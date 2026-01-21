package com.dashinspector

/**
 * Data models for DashInspector API.
 */

// region SharedPreferences Models

/**
 * Represents a SharedPreference file with its entries.
 */
data class SharedPrefFile(
    val name: String,
    val entries: List<PrefEntry>
)

/**
 * Represents a single entry in a SharedPreference file.
 */
data class PrefEntry(
    val key: String,
    val value: Any?,
    val type: String
)

// endregion

// region API Request Models

/**
 * Request model for creating or updating preferences.
 */
data class PrefRequest(
    val name: String?,
    val key: String?,
    val value: String?,
    val type: String?
)

/**
 * Request model for removing a preference entry.
 */
internal data class RemoveEntryRequest(
    val name: String?,
    val key: String?
)

/**
 * Request model for removing an entire preference file.
 */
internal data class RemovePrefRequest(
    val name: String?
)

// endregion

// region Database Models

/**
 * Represents a database file with basic metadata.
 */
data class DatabaseFile(
    val name: String,
    val path: String,
    val size: Long,
    val isRoomDatabase: Boolean
)

/**
 * Represents column information in a database table.
 */
data class ColumnInfo(
    val cid: Int,
    val name: String,
    val type: String,
    val notnull: Boolean,
    val defaultValue: String?,
    val pk: Int
)

/**
 * Represents a table with its columns and row count.
 */
data class TableInfo(
    val name: String,
    val columns: List<ColumnInfo>,
    val rowCount: Int
)

/**
 * Represents a foreign key relationship.
 */
data class ForeignKeyInfo(
    val id: Int,
    val seq: Int,
    val table: String,
    val from: String,
    val to: String,
    val onUpdate: String,
    val onDelete: String
)

/**
 * Represents a table in the ERD with columns and foreign keys.
 */
data class ERDTableInfo(
    val name: String,
    val columns: List<ColumnInfo>,
    val foreignKeys: List<ForeignKeyInfo>
)

/**
 * Represents a relationship between two tables for ERD visualization.
 */
data class ERDRelationship(
    val fromTable: String,
    val fromColumn: String,
    val toTable: String,
    val toColumn: String,
    val relationshipType: String // "one-to-one", "one-to-many", "many-to-one"
)

/**
 * Response model for ERD data.
 */
data class ERDResponse(
    val database: String,
    val tables: List<ERDTableInfo>,
    val relationships: List<ERDRelationship>,
    val isRoomDatabase: Boolean
)

// endregion

// region Database Response Models

/**
 * Response model for listing all databases.
 */
data class DatabaseListResponse(
    val databases: List<DatabaseFile>,
    val total: Int
)

/**
 * Response model for database schema (tables and columns).
 */
data class DatabaseSchemaResponse(
    val database: String,
    val tables: List<TableInfo>,
    val isRoomDatabase: Boolean
)

/**
 * Response model for paginated table data.
 */
data class TableDataResponse(
    val database: String,
    val table: String,
    val columns: List<String>,
    val rows: List<List<Any?>>,
    val totalRows: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * Response model for SQL query results.
 */
data class QueryResponse(
    val database: String,
    val query: String,
    val columns: List<String>,
    val rows: List<List<Any?>>,
    val rowCount: Int,
    val executionTimeMs: Long
)

// endregion

// region Database Request Models

/**
 * Request model for database operations requiring database name.
 */
data class DatabaseRequest(
    val database: String?
)

/**
 * Request model for paginated table data retrieval.
 */
data class TableDataRequest(
    val database: String?,
    val table: String?,
    val page: Int = 1,
    val pageSize: Int = 50
)

/**
 * Request model for SQL query execution.
 */
data class QueryRequest(
    val database: String?,
    val query: String?
)

/**
 * Request model for updating a row.
 */
data class UpdateRowRequest(
    val database: String?,
    val table: String?,
    val primaryKey: Map<String, Any?>?,
    val values: Map<String, Any?>?
)

/**
 * Request model for deleting a row.
 */
data class DeleteRowRequest(
    val database: String?,
    val table: String?,
    val primaryKey: Map<String, Any?>?
)

/**
 * Request model for inserting a row.
 */
data class InsertRowRequest(
    val database: String?,
    val table: String?,
    val values: Map<String, Any?>?
)

// endregion
