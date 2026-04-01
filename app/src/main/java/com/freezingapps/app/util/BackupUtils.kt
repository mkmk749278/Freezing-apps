package com.freezingapps.app.util

import android.content.Context
import com.freezingapps.app.data.model.AppInfo
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Utility class for backup and restore of freeze states.
 * Exports/imports frozen app lists as JSON files.
 */
object BackupUtils {

    private const val BACKUP_FILENAME = "freeze_backup.json"
    private const val KEY_PACKAGES = "frozen_packages"
    private const val KEY_TIMESTAMP = "timestamp"
    private const val KEY_VERSION = "version"

    /**
     * Export current freeze states to a JSON file.
     *
     * @param context Application context
     * @param frozenPackages List of currently frozen package names
     * @return The backup file, or null if export failed
     */
    fun exportToFile(context: Context, frozenPackages: List<String>): File? {
        return try {
            val json = JSONObject().apply {
                put(KEY_VERSION, 1)
                put(KEY_TIMESTAMP, System.currentTimeMillis())
                put(KEY_PACKAGES, JSONArray(frozenPackages))
            }

            val file = File(context.getExternalFilesDir(null), BACKUP_FILENAME)
            file.writeText(json.toString(2))
            file
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Import freeze states from a JSON backup file.
     *
     * @param context Application context
     * @return List of package names to freeze, or null if import failed
     */
    fun importFromFile(context: Context): List<String>? {
        return try {
            val file = File(context.getExternalFilesDir(null), BACKUP_FILENAME)
            if (!file.exists()) return null

            val json = JSONObject(file.readText())
            val packagesArray = json.getJSONArray(KEY_PACKAGES)
            val packages = mutableListOf<String>()
            for (i in 0 until packagesArray.length()) {
                packages.add(packagesArray.getString(i))
            }
            packages
        } catch (e: Exception) {
            null
        }
    }
}
