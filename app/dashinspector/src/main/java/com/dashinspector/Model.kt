package com.dashinspector

data class SharedPrefFile(
    val name: String,
    val entries: List<PrefEntry>)

data class PrefEntry(
    val key: String,
    val value: Any?,
    val type: String
)

data class ApiResponse<T>(
    val status: String,
    val data: T?,
    val message: String? = null
)

enum class PrefAction {
    ADD_ENTRY,
    UPDATE_ENTRY,
    ADD_PREF,
    UNKNOWN
}

data class PrefRequest(
    val name: String?,
    val key: String?,
    val value: String?,
    val type: String?
)

data class RemoveEntryRequest(
    val name: String?,
    val key: String?
)

data class RemovePrefRequest(
    val name: String?
)