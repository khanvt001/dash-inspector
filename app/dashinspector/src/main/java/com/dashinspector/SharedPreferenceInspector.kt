package com.dashinspector

import android.content.Context
import java.io.File
import androidx.core.content.edit

class SharedPreferencesInspector(private val context: Context) {

    fun getAllPreferences(): Map<String, Any> {
        val prefsDir = File(context.applicationInfo.dataDir, "shared_prefs")
        val prefsList =  prefsDir.listFiles()
            ?.filter { it.extension == "xml" }
            ?.map { file ->
                val prefName = file.nameWithoutExtension
                val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
                SharedPrefFile(
                    name = prefName,
                    entries = prefs.all.map { (key, value) ->
                        PrefEntry(key, value, value?.javaClass?.simpleName ?: "null")
                    }
                )
            }
            ?: emptyList()

        return mapOf(
            "preferences" to prefsList,
            "total" to prefsList.size
        )
    }

    fun removeEntry(prefName: String, key: String) : Map<String, String> {
        try {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            prefs.apply {
                edit().apply{
                    remove(key)
                    apply()
                }
            }
            return mapOf(
                "status" to "success",
                "message" to "Entry '$key' removed successfully from preference '$prefName'"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return mapOf(
                "status" to "error",
                "message" to "Exception occurred: ${e.message}"
            )
        }
    }

    fun removePreference(prefName: String) : Map<String, String> {
        try {
            val result = context.deleteSharedPreferences(prefName)
            return if(result){
                mapOf(
                    "status" to "success",
                    "message" to "Preference '$prefName' deleted successfully"
                )
            } else {
                mapOf(
                    "status" to "error",
                    "message" to "Failed to delete preference '$prefName'"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return mapOf(
                "status" to "error",
                "message" to "Exception occurred: ${e.message}"
            )
        }
    }

    fun upsertPreference(prefName: String, key: String, value: Any?, type: String) : Map<String, String> {
        val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
        prefs.edit().apply {
            when (type) {
                "String" -> putString(key, value as String)
                "Int", "Integer" -> putInt(key, (value as Number).toInt())
                "Long" -> putLong(key, (value as Number).toLong())
                "Float" -> putFloat(key, (value as Number).toFloat())
                "Boolean" -> putBoolean(key, value as Boolean)
                "StringSet" -> putStringSet(key, (value as List<String>).toSet())
                else -> {
                    return mapOf(
                        "status" to "error",
                        "message" to "Unsupported type '$type'"
                    )
                }
            }
            apply()
            return mapOf(
                "status" to "success",
                "message" to "Preference upserted successfully"
            )
        }
    }
}