package com.freezingapps.app.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import com.freezingapps.app.data.db.AppDatabase
import com.freezingapps.app.data.model.ActionLog
import com.freezingapps.app.data.model.AppInfo
import com.freezingapps.app.data.model.FrozenApp
import com.freezingapps.app.root.RootCommandExecutor
import com.freezingapps.app.root.RootCommandResult
import com.freezingapps.app.util.PackageUtils
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
 * - Validate packages before executing commands
 * - Log actions to the local database
 * - Export/import freeze states for backup/restore
 * - Manage the user's frozen app list (Frozen tab persistence)
 */
class AppRepository(private val context: Context) {

    companion object {
        private const val TAG = "AppRepository"

        /**
         * Critical system packages that should never be frozen as they
         * can cause the device to become unusable.
         */
        private val PROTECTED_SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.providers.contacts",
            "com.android.providers.telephony",
            "com.android.providers.settings",
            "com.android.inputdevices",
            "com.android.shell",
            "com.android.launcher3",
            "com.android.packageinstaller",
            "com.android.permissioncontroller"
        )
    }

    private val packageManager: PackageManager = context.packageManager
    private val database = AppDatabase.getInstance(context)
    private val actionLogDao = database.actionLogDao()
    private val frozenAppDao = database.frozenAppDao()

    /**
     * Load all installed apps with their current freeze status and frozen list membership.
     * Excludes this app itself from the list.
     *
     * @param includeSystem Whether to include system apps
     * @return List of AppInfo objects
     */
    suspend fun getInstalledApps(includeSystem: Boolean = true): List<AppInfo> =
        withContext(Dispatchers.IO) {
            val frozenPackages = RootCommandExecutor.getFrozenPackages().toSet()
            val frozenListPackages = frozenAppDao.getAllPackageNames().toSet()
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
                        isFrozen = frozenPackages.contains(appInfo.packageName),
                        isInFrozenList = frozenListPackages.contains(appInfo.packageName)
                    )
                }
                .sortedBy { it.appName.lowercase() }
        }

    /**
     * Load apps that are in the managed frozen list with their current system freeze status.
     * Returns only apps that are still installed on the device.
     *
     * @return List of AppInfo objects for apps in the frozen list
     */
    suspend fun getManagedFrozenApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val frozenListPackages = frozenAppDao.getAllPackageNames()
        val frozenPackages = RootCommandExecutor.getFrozenPackages().toSet()

        frozenListPackages.mapNotNull { packageName ->
            try {
                val appInfoSystem = packageManager.getApplicationInfo(
                    packageName, PackageManager.GET_META_DATA
                )
                AppInfo(
                    packageName = appInfoSystem.packageName,
                    appName = packageManager.getApplicationLabel(appInfoSystem).toString(),
                    icon = try {
                        packageManager.getApplicationIcon(packageName)
                    } catch (e: Exception) {
                        null
                    },
                    isSystemApp = (appInfoSystem.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isFrozen = frozenPackages.contains(packageName),
                    isInFrozenList = true,
                    isSelected = true // Default: all selected for Freeze All
                )
            } catch (e: Exception) {
                // Package no longer installed, clean up
                Log.w(TAG, "Package no longer installed, removing from frozen list: $packageName")
                frozenAppDao.delete(packageName)
                null
            }
        }.sortedBy { it.appName.lowercase() }
    }

    /**
     * Add an app to the managed frozen list.
     */
    suspend fun addToFrozenList(packageName: String) {
        Log.i(TAG, "Adding to frozen list: $packageName")
        frozenAppDao.insert(FrozenApp(packageName))
    }

    /**
     * Add multiple apps to the managed frozen list.
     */
    suspend fun addAllToFrozenList(packageNames: List<String>) {
        Log.i(TAG, "Adding ${packageNames.size} apps to frozen list")
        frozenAppDao.insertAll(packageNames.map { FrozenApp(it) })
    }

    /**
     * Remove an app from the managed frozen list.
     */
    suspend fun removeFromFrozenList(packageName: String) {
        Log.i(TAG, "Removing from frozen list: $packageName")
        frozenAppDao.delete(packageName)
    }

    /**
     * Uninstall an app via root command and remove it from the frozen list.
     * Validates the package exists and is not a protected system package.
     *
     * @param appInfo The app to uninstall
     * @return RootCommandResult with the operation result
     */
    suspend fun uninstallApp(appInfo: AppInfo): RootCommandResult {
        Log.i(TAG, "Uninstall requested: packageName=${appInfo.packageName}, appName=${appInfo.appName}")

        if (!isPackageInstalled(appInfo.packageName)) {
            val error = "Package not installed: ${appInfo.packageName}"
            Log.w(TAG, "Uninstall aborted - $error")
            return RootCommandResult(success = false, error = error)
        }

        if (isProtectedSystemApp(appInfo.packageName)) {
            val error = "Cannot uninstall protected system package: ${appInfo.packageName}"
            Log.w(TAG, "Uninstall aborted - $error")
            return RootCommandResult(success = false, error = error)
        }

        val result = RootCommandExecutor.uninstallApp(appInfo.packageName)
        Log.i(TAG, "Uninstall completed: packageName=${appInfo.packageName}, success=${result.success}")
        logAction(appInfo, "uninstall", result)

        // Remove from frozen list if present
        if (result.success) {
            frozenAppDao.delete(appInfo.packageName)
        }

        return result
    }

    /**
     * Get the frozen list as a reactive Flow.
     */
    fun getFrozenListFlow(): Flow<List<FrozenApp>> = frozenAppDao.getAll()

    /**
     * Freeze (disable) an app and log the action.
     * Validates the package exists and is not a protected system package before freezing.
     *
     * @param appInfo The app to freeze
     * @return RootCommandResult with the operation result
     */
    suspend fun freezeApp(appInfo: AppInfo): RootCommandResult {
        Log.i(TAG, "Freeze requested: packageName=${appInfo.packageName}, appName=${appInfo.appName}")

        if (!isPackageInstalled(appInfo.packageName)) {
            val error = "Package not installed: ${appInfo.packageName}"
            Log.w(TAG, "Freeze aborted - $error")
            val result = RootCommandResult(success = false, error = error)
            logAction(appInfo, "freeze", result)
            return result
        }

        if (isProtectedSystemApp(appInfo.packageName)) {
            val error = "Cannot freeze protected system package: ${appInfo.packageName}"
            Log.w(TAG, "Freeze aborted - $error")
            val result = RootCommandResult(success = false, error = error)
            logAction(appInfo, "freeze", result)
            return result
        }

        val result = RootCommandExecutor.freezeApp(appInfo.packageName)
        Log.i(TAG, "Freeze completed: packageName=${appInfo.packageName}, success=${result.success}")
        logAction(appInfo, "freeze", result)
        return result
    }

    /**
     * Unfreeze (enable) an app and log the action.
     * Validates the package exists before unfreezing.
     *
     * @param appInfo The app to unfreeze
     * @return RootCommandResult with the operation result
     */
    suspend fun unfreezeApp(appInfo: AppInfo): RootCommandResult {
        Log.i(TAG, "Unfreeze requested: packageName=${appInfo.packageName}, appName=${appInfo.appName}")

        if (!isPackageInstalled(appInfo.packageName)) {
            val error = "Package not installed: ${appInfo.packageName}"
            Log.w(TAG, "Unfreeze aborted - $error")
            val result = RootCommandResult(success = false, error = error)
            logAction(appInfo, "unfreeze", result)
            return result
        }

        val result = RootCommandExecutor.unfreezeApp(appInfo.packageName)
        Log.i(TAG, "Unfreeze completed: packageName=${appInfo.packageName}, success=${result.success}")
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
     * Validates each package exists before attempting to freeze.
     *
     * @param packageNames List of package names to freeze
     * @return Map of package name to success status
     */
    suspend fun importFreezeStates(packageNames: List<String>): Map<String, Boolean> {
        val results = mutableMapOf<String, Boolean>()
        for (packageName in packageNames) {
            if (!isPackageInstalled(packageName)) {
                Log.w(TAG, "Import skipped - package not installed: $packageName")
                results[packageName] = false
                continue
            }
            if (isProtectedSystemApp(packageName)) {
                Log.w(TAG, "Import skipped - protected system package: $packageName")
                results[packageName] = false
                continue
            }
            val result = RootCommandExecutor.freezeApp(packageName)
            Log.i(TAG, "Import freeze: packageName=$packageName, success=${result.success}")
            results[packageName] = result.success
        }
        return results
    }

    /**
     * Check if a package is installed on the device using PackageManager.
     *
     * @param packageName The package name to check
     * @return true if the package is installed
     */
    fun isPackageInstalled(packageName: String): Boolean {
        return PackageUtils.isPackageInstalled(context, packageName)
    }

    /**
     * Check if a package is a protected system package that should not be frozen.
     *
     * @param packageName The package name to check
     * @return true if the package is a protected system package
     */
    private fun isProtectedSystemApp(packageName: String): Boolean {
        return PROTECTED_SYSTEM_PACKAGES.contains(packageName)
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
