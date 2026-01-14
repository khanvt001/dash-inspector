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

data class UpdatePrefRequest(
    val name: String,
    val key: String,
    val value: String,
    val type: String
)