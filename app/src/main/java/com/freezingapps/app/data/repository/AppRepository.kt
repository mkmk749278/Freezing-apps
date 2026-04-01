package com.freezingapps.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.freezingapps.app.data.db.AppDatabase
import com.freezingapps.app.data.model.ActionLog
import com.freezingapps.app.data.model.AppInfo
import com.freezingapps.app.root.RootCommandExecutor
import com.freezingapps.app.root.RootCommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing app data and freeze/unfreeze operations.
 * Acts as a single source of truth for the ViewModel layer.
 *
 * Responsibilities:
 * - Load installed apps from the system package manager
 * - Execute freeze/unfreeze commands via root
 * - Log actions to the local database
 * - Export/import freeze states for backup/restore
 */
class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val database = AppDatabase.getInstance(context)
    private val actionLogDao = database.actionLogDao()

    /**
     * Load all installed apps with their current freeze status.
     * Excludes this app itself from the list.
     *
     * @param includeSystem Whether to include system apps
     * @return List of AppInfo objects
     */
    suspend fun getInstalledApps(includeSystem: Boolean = true): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val frozenPackages = RootCommandExecutor.getFrozenPackages().toSet()
            val packages = packageManager.getInstalledApplications(
                PackageManager.GET_META_DATA
            )

            packages
                .filter { it.packageName != context.packageName }
                .filter { includeSystem || (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                .map { appInfo ->
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = packageManager.getApplicationLabel(appInfo).toString(),
                        icon = try {
                            packageManager.getApplicationIcon(appInfo.packageName)
                        } catch (e: Exception) {
                            null
                        },
                        isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                        isFrozen = frozenPackages.contains(appInfo.packageName)
                    )
                }
                .sortedBy { it.appName.lowercase() }
        }

    /**
     * Freeze (disable) an app and log the action.
     *
     * @param appInfo The app to freeze
     * @return RootCommandResult with the operation result
     */
    suspend fun freezeApp(appInfo: AppInfo): RootCommandResult {
        val result = RootCommandExecutor.freezeApp(appInfo.packageName)
        logAction(appInfo, "freeze", result)
        return result
    }

    /**
     * Unfreeze (enable) an app and log the action.
     *
     * @param appInfo The app to unfreeze
     * @return RootCommandResult with the operation result
     */
    suspend fun unfreezeApp(appInfo: AppInfo): RootCommandResult {
        val result = RootCommandExecutor.unfreezeApp(appInfo.packageName)
        logAction(appInfo, "unfreeze", result)
        return result
    }

    /**
     * Toggle the freeze state of an app.
     *
     * @param appInfo The app to toggle
     * @return RootCommandResult with the operation result
     */
    suspend fun toggleFreezeState(appInfo: AppInfo): RootCommandResult {
        return if (appInfo.isFrozen) {
            unfreezeApp(appInfo)
        } else {
            freezeApp(appInfo)
        }
    }

    /**
     * Check if root access is available.
     */
    suspend fun isRootAvailable(): Boolean = RootCommandExecutor.isRootAvailable()

    /**
     * Get all action logs as a Flow for reactive UI.
     */
    fun getActionLogs(): Flow<List<ActionLog>> = actionLogDao.getAllLogs()

    /**
     * Clear all action logs.
     */
    suspend fun clearActionLogs() = actionLogDao.deleteAll()

    /**
     * Export current freeze states as a list of frozen package names.
     * Useful for backup/restore functionality.
     *
     * @return List of currently frozen package names
     */
    suspend fun exportFreezeStates(): List<String> = RootCommandExecutor.getFrozenPackages()

    /**
     * Import freeze states by freezing specified packages.
     *
     * @param packageNames List of package names to freeze
     * @return Map of package name to success status
     */
    suspend fun importFreezeStates(packageNames: List<String>): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        for (packageName in packageNames) {
            val result = RootCommandExecutor.freezeApp(packageName)
            results[packageName] = result.success
        }
        return results
    }

    /**
     * Log an action to the database.
     */
    private suspend fun logAction(
        appInfo: AppInfo,
        action: String,
        result: RootCommandResult
    ) {
        actionLogDao.insert(
            ActionLog(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                action = action,
                success = result.success,
                errorMessage = if (!result.success) result.error else null
            )
        )
    }
}
