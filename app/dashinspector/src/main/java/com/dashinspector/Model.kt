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
