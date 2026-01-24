package com.dashinspector

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import java.io.File

/**
 * Inspector class for reading and modifying SharedPreferences at runtime.
 */
class SharedPreferencesInspector(private val context: Context) {

    /**
     * Retrieves all SharedPreference files and their entries.
     *
     * @return Map containing list of preferences and total count
     */
    fun getAllPreferences(): Map<String, Any> {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val prefsList = prefsDir.listFiles()
            ?.filter { it.extension == "xml" }
            ?.map { file -> readPreferenceFile(file.nameWithoutExtension) }
            ?: emptyList()

        return mapOf(
            "preferences" to prefsList,
            "total" to prefsList.size
        )
    }

    /**
     * Removes a specific entry from a SharedPreference file.
     *
     * @param prefName Name of the SharedPreference file
     * @param key Key to remove
     * @return Result map with status and message
     */
    fun removeEntry(prefName: String, key: String): Map<String, String> {
        return runCatching {
            val prefs = getSharedPreferences(prefName)
            prefs.edit().remove(key).apply()
            successResult("Entry '$key' removed from '$prefName'")
        }.getOrElse { e ->
            errorResult("Failed to remove entry: ${e.message}")
        }
    }

    /**
     * Deletes an entire SharedPreference file.
     *
     * @param prefName Name of the SharedPreference file to delete
     * @return Result map with status and message
     */
    fun removePreference(prefName: String): Map<String, String> {
        return runCatching {
            val deleted = context.deleteSharedPreferences(prefName)
            if (deleted) {
                successResult("Preference '$prefName' deleted successfully")
            } else {
                errorResult("Failed to delete preference '$prefName'")
            }
        }.getOrElse { e ->
            errorResult("Failed to delete preference: ${e.message}")
        }
    }

    /**
     * Creates or updates a preference entry.
     *
     * @param prefName Name of the SharedPreference file
     * @param key Key for the entry
     * @param value Value to store (will be converted based on type)
     * @param type Data type (String, Int, Integer, Long, Float, Boolean, StringSet)
     * @return Result map with status and message
     */
    @SuppressLint("ApplySharedPref")
    fun upsertPreference(
        prefName: String,
        key: String,
        value: Any?,
        type: String
    ): Map<String, String> {
        return runCatching {
            val prefs = getSharedPreferences(prefName)
            val editor = prefs.edit()

            when (type) {
                "String" -> editor.putString(key, value?.toString())
                "Int", "Integer" -> editor.putInt(key, parseNumber(value).toInt())
                "Long" -> editor.putLong(key, parseNumber(value).toLong())
                "Float" -> editor.putFloat(key, parseNumber(value).toFloat())
                "Boolean" -> editor.putBoolean(key, parseBoolean(value))
                "StringSet" -> editor.putStringSet(key, parseStringSet(value))
                else -> return errorResult("Unsupported type: $type")
            }

            // Use commit() to ensure the change is applied before reading
            editor.commit()
            successResult("Preference saved successfully")
        }.getOrElse { e ->
            errorResult("Failed to save preference: ${e.message}")
        }
    }

    // region Private Helpers

    private fun getSharedPreferences(name: String): SharedPreferences {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    private fun readPreferenceFile(prefName: String): SharedPrefFile {
        val prefs = getSharedPreferences(prefName)
        val entries = prefs.all.map { (key, value) ->
            PrefEntry(
                key = key,
                value = value,
                type = getTypeName(value)
            )
        }
        return SharedPrefFile(name = prefName, entries = entries)
    }

    private fun getTypeName(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> "String"
            is Int -> "Int"
            is Long -> "Long"
            is Float -> "Float"
            is Boolean -> "Boolean"
            is Set<*> -> "StringSet"
            else -> value.javaClass.simpleName
        }
    }

    private fun parseNumber(value: Any?): Number {
        return when (value) {
            is Number -> value
            is String -> value.toDoubleOrNull() ?: 0.0
            else -> 0
        }
    }

    private fun parseBoolean(value: Any?): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            else -> false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseStringSet(value: Any?): Set<String> {
        return when (value) {
            is Set<*> -> value.filterIsInstance<String>().toSet()
            is List<*> -> value.filterIsInstance<String>().toSet()
            is String -> value.split(",").map { it.trim() }.toSet()
            else -> emptySet()
        }
    }

    private fun successResult(message: String): Map<String, String> {
        return mapOf("status" to "success", "message" to message)
    }

    private fun errorResult(message: String): Map<String, String> {
        return mapOf("status" to "error", "message" to message)
    }

    // endregion
}
