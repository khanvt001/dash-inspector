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

    fun updatePreference(prefName: String, key: String, value: Any, type: String) : Map<String, String> {
        try {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            if(prefs == null) {
                return mapOf(
                    "status" to "error",
                    "message" to "SharedPreferences '$prefName' not found"
                )
            }
            if(!prefs.contains(key)){
                return mapOf(
                    "status" to "error",
                    "message" to "Key '$key' not found in SharedPreferences '$prefName'"
                )
            }
            prefs.edit().apply {
                when (type) {
                    "String" -> putString(key, value as String)
                    "Int", "Integer" -> putInt(key, (value as Number).toInt())
                    "Long" -> putLong(key, (value as Number).toLong())
                    "Float" -> putFloat(key, (value as Number).toFloat())
                    "Boolean" -> putBoolean(key, value as Boolean)
                    "Set" -> putStringSet(key, (value as List<String>).toSet())
                    else -> return mapOf(
                        "status" to "error",
                        "message" to "Unsupported type '$type'"
                    )
                }
                apply()
                return mapOf(
                    "status" to "success",
                    "message" to "Preference updated successfully"
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return mapOf(
                "status" to "error",
                "message" to "Exception: ${e.message}"
            )
        }
    }

    fun deletePreference(prefName: String, key: String) {
        try {
            val prefs = context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            prefs.edit { remove(key) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}