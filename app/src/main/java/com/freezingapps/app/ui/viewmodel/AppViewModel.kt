package com.freezingapps.app.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.freezingapps.app.data.model.ActionLog
import com.freezingapps.app.data.model.AppInfo
import com.freezingapps.app.data.repository.AppRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the main app list screen.
 * Manages app data, search/filter state, freeze/unfreeze operations,
 * and the managed frozen apps list.
 *
 * Architecture: MVVM pattern with LiveData for UI observation
 * and coroutines for background operations.
 */
class AppViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "AppViewModel"
    }

    private val repository = AppRepository(application)

    // Full list of installed apps
    private val _allApps = MutableLiveData<List<AppInfo>>()

    // Filtered list displayed in the All Apps tab UI
    private val _filteredApps = MutableLiveData<List<AppInfo>>()
    val filteredApps: LiveData<List<AppInfo>> = _filteredApps

    // Frozen apps only (system-level frozen, for backward compatibility)
    private val _frozenApps = MutableLiveData<List<AppInfo>>()
    val frozenApps: LiveData<List<AppInfo>> = _frozenApps

    // Filtered frozen apps (search within frozen tab - legacy)
    private val _filteredFrozenApps = MutableLiveData<List<AppInfo>>()
    val filteredFrozenApps: LiveData<List<AppInfo>> = _filteredFrozenApps

    // Managed frozen apps (user-curated list in the Frozen tab)
    private val _managedFrozenApps = MutableLiveData<List<AppInfo>>()

    // Filtered managed frozen apps (search within Frozen tab)
    private val _filteredManagedFrozenApps = MutableLiveData<List<AppInfo>>()
    val filteredManagedFrozenApps: LiveData<List<AppInfo>> = _filteredManagedFrozenApps

    // Loading state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    // Root availability
    private val _isRootAvailable = MutableLiveData<Boolean>()
    val isRootAvailable: LiveData<Boolean> = _isRootAvailable

    // Error/success messages for the UI
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    // Multi-select mode (All Apps tab)
    private val _isMultiSelectMode = MutableLiveData(false)
    val isMultiSelectMode: LiveData<Boolean> = _isMultiSelectMode

    // Action logs
    private val _actionLogs = MutableLiveData<List<ActionLog>>()
    val actionLogs: LiveData<List<ActionLog>> = _actionLogs

    // Current search query (All Apps tab)
    private var currentQuery: String = ""

    // Current frozen tab search query
    private var frozenSearchQuery: String = ""

    // Filter: show system apps
    private var showSystemApps: Boolean = true

    // Filter: show only frozen apps
    private var showOnlyFrozen: Boolean = false

    init {
        checkRootAccess()
        loadActionLogs()
    }

    /**
     * Check if root access is available (KSU root verification).
     */
    fun checkRootAccess() {
        viewModelScope.launch {
            val available = repository.isRootAvailable()
            Log.d(TAG, "Root access available: $available")
            _isRootAvailable.postValue(available)
            if (available) {
                loadApps()
            }
        }
    }

    /**
     * Load all installed apps from the system and the managed frozen list.
     */
    fun loadApps() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val apps = repository.getInstalledApps(showSystemApps)
                Log.d(TAG, "Loaded ${apps.size} apps (showSystemApps=$showSystemApps)")
                _allApps.postValue(apps)
                applyFilters(apps)

                // Also load the managed frozen apps
                loadManagedFrozenApps()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading apps", e)
                _message.postValue("Error loading apps: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    /**
     * Load apps in the managed frozen list from the database.
     */
    fun loadManagedFrozenApps() {
        viewModelScope.launch {
            try {
                val managedApps = repository.getManagedFrozenApps()
                Log.d(TAG, "Loaded ${managedApps.size} managed frozen apps")
                _managedFrozenApps.postValue(managedApps)
                applyManagedFrozenFilter(managedApps)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading managed frozen apps", e)
            }
        }
    }

    // ================== Frozen List Management ==================

    /**
     * Add an app to the managed frozen list (Frozen tab).
     */
    fun addToFrozenList(appInfo: AppInfo) {
        viewModelScope.launch {
            try {
                repository.addToFrozenList(appInfo.packageName)
                _message.postValue("Added to Frozen: ${appInfo.appName}")
                loadApps() // Refresh both tabs
            } catch (e: Exception) {
                Log.e(TAG, "Error adding to frozen list: ${appInfo.packageName}", e)
                _message.postValue("Error: ${e.message}")
            }
        }
    }

    /**
     * Remove an app from the managed frozen list (Frozen tab).
     */
    fun removeFromFrozenList(appInfo: AppInfo) {
        viewModelScope.launch {
            try {
                repository.removeFromFrozenList(appInfo.packageName)
                _message.postValue("Removed from Frozen: ${appInfo.appName}")
                loadApps() // Refresh both tabs
            } catch (e: Exception) {
                Log.e(TAG, "Error removing from frozen list: ${appInfo.packageName}", e)
                _message.postValue("Error: ${e.message}")
            }
        }
    }

    /**
     * Toggle an app's membership in the managed frozen list.
     */
    fun toggleFrozenList(appInfo: AppInfo) {
        if (appInfo.isInFrozenList) {
            removeFromFrozenList(appInfo)
        } else {
            addToFrozenList(appInfo)
        }
    }

    /**
     * Add all selected apps (in multi-select mode) to the frozen list.
     */
    fun addSelectedToFrozenList() {
        viewModelScope.launch {
            val selected = getSelectedApps()
            if (selected.isEmpty()) {
                _message.postValue("No apps selected")
                return@launch
            }

            val toAdd = selected.filter { !it.isInFrozenList }
            if (toAdd.isEmpty()) {
                _message.postValue("All selected apps are already in the Frozen list")
                exitMultiSelectMode()
                return@launch
            }

            Log.i(TAG, "Adding ${toAdd.size} apps to frozen list")
            repository.addAllToFrozenList(toAdd.map { it.packageName })
            _message.postValue("Added ${toAdd.size} apps to Frozen list")
            exitMultiSelectMode()
            loadApps()
        }
    }

    // ================== Freeze/Unfreeze Operations ==================

    /**
     * Toggle the freeze state of a single app.
     * Validates the package before proceeding.
     */
    fun toggleFreezeState(appInfo: AppInfo) {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Toggle freeze state: packageName=${appInfo.packageName}, currentlyFrozen=${appInfo.isFrozen}")

                if (!repository.isPackageInstalled(appInfo.packageName)) {
                    val errorMsg = "Package not found: ${appInfo.packageName}"
                    Log.w(TAG, "Toggle aborted - $errorMsg")
                    _message.postValue(errorMsg)
                    return@launch
                }

                val result = repository.toggleFreezeState(appInfo)
                if (result.success) {
                    val action = if (appInfo.isFrozen) "Unfrozen" else "Frozen"
                    _message.postValue("$action: ${appInfo.appName}")
                    loadApps() // Refresh the list
                } else {
                    Log.w(TAG, "Toggle failed for ${appInfo.packageName}: ${result.error}")
                    _message.postValue("Failed: ${result.error}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error toggling freeze state for ${appInfo.packageName}", e)
                _message.postValue("Error: ${e.message}")
            }
        }
    }

    /**
     * Freeze a specific app.
     */
    fun freezeApp(appInfo: AppInfo) {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Freeze app: packageName=${appInfo.packageName}")

                if (!repository.isPackageInstalled(appInfo.packageName)) {
                    val errorMsg = "Package not found: ${appInfo.packageName}"
                    Log.w(TAG, "Freeze aborted - $errorMsg")
                    _message.postValue(errorMsg)
                    return@launch
                }

                val result = repository.freezeApp(appInfo)
                if (result.success) {
                    _message.postValue("Frozen: ${appInfo.appName}")
                    loadApps()
                } else {
                    Log.w(TAG, "Freeze failed for ${appInfo.packageName}: ${result.error}")
                    _message.postValue("Failed to freeze: ${result.error}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error freezing ${appInfo.packageName}", e)
                _message.postValue("Error: ${e.message}")
            }
        }
    }

    /**
     * Unfreeze a specific app.
     */
    fun unfreezeApp(appInfo: AppInfo) {
        viewModelScope.launch {
            try {
                Log.i(TAG, "Unfreeze app: packageName=${appInfo.packageName}")

                if (!repository.isPackageInstalled(appInfo.packageName)) {
                    val errorMsg = "Package not found: ${appInfo.packageName}"
                    Log.w(TAG, "Unfreeze aborted - $errorMsg")
                    _message.postValue(errorMsg)
                    return@launch
                }

                val result = repository.unfreezeApp(appInfo)
                if (result.success) {
                    _message.postValue("Unfrozen: ${appInfo.appName}")
                    loadApps()
                } else {
                    Log.w(TAG, "Unfreeze failed for ${appInfo.packageName}: ${result.error}")
                    _message.postValue("Failed to unfreeze: ${result.error}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error unfreezing ${appInfo.packageName}", e)
                _message.postValue("Error: ${e.message}")
            }
        }
    }

    /**
     * Freeze all selected apps in the managed Frozen tab.
     * Only freezes apps that are currently selected (checked) and not already frozen.
     * Constraint: Does not affect apps outside the Frozen tab or unselected apps.
     */
    fun freezeAllInFrozenTab() {
        viewModelScope.launch {
            val apps = _managedFrozenApps.value ?: return@launch
            val selectedApps = apps.filter { it.isSelected }
            val toFreeze = selectedApps.filter { !it.isFrozen }

            if (selectedApps.isEmpty()) {
                _message.postValue("No apps selected")
                return@launch
            }

            if (toFreeze.isEmpty()) {
                _message.postValue("All selected apps are already frozen")
                return@launch
            }

            Log.i(TAG, "Freeze all selected in frozen tab: ${toFreeze.size} apps")
            _isLoading.postValue(true)
            var successCount = 0
            var failCount = 0

            for (app in toFreeze) {
                if (!repository.isPackageInstalled(app.packageName)) {
                    failCount++
                    continue
                }
                val result = repository.freezeApp(app)
                if (result.success) successCount++ else failCount++
            }

            val message = buildBulkResultMessage("Frozen", successCount, failCount, 0)
            Log.i(TAG, "Freeze all completed: $message")
            _message.postValue(message)
            loadApps()
        }
    }

    /**
     * Toggle selection state of a managed frozen app (for Freeze All).
     */
    fun toggleManagedFrozenSelection(appInfo: AppInfo) {
        val apps = _managedFrozenApps.value?.toMutableList() ?: return
        val index = apps.indexOfFirst { it.packageName == appInfo.packageName }
        if (index >= 0) {
            apps[index] = apps[index].copy(isSelected = !apps[index].isSelected)
            _managedFrozenApps.value = apps
            applyManagedFrozenFilter(apps)
        }
    }

    /**
     * Select all managed frozen apps.
     */
    fun selectAllManagedFrozen() {
        val apps = _managedFrozenApps.value?.toMutableList() ?: return
        for (i in apps.indices) {
            apps[i] = apps[i].copy(isSelected = true)
        }
        _managedFrozenApps.value = apps
        applyManagedFrozenFilter(apps)
    }

    /**
     * Deselect all managed frozen apps.
     */
    fun deselectAllManagedFrozen() {
        val apps = _managedFrozenApps.value?.toMutableList() ?: return
        for (i in apps.indices) {
            apps[i] = apps[i].copy(isSelected = false)
        }
        _managedFrozenApps.value = apps
        applyManagedFrozenFilter(apps)
    }

    // ================== Multi-Select (All Apps Tab) ==================

    /**
     * Freeze all selected apps in multi-select mode.
     * Validates each package before attempting to freeze.
     */
    fun freezeSelected() {
        viewModelScope.launch {
            val selected = getSelectedApps()
            if (selected.isEmpty()) {
                _message.postValue("No apps selected")
                return@launch
            }

            Log.i(TAG, "Bulk freeze: ${selected.size} apps selected")
            _isLoading.postValue(true)
            var successCount = 0
            var failCount = 0
            var skippedCount = 0

            for (app in selected) {
                if (!app.isFrozen) {
                    if (!repository.isPackageInstalled(app.packageName)) {
                        Log.w(TAG, "Bulk freeze skipped - package not installed: ${app.packageName}")
                        skippedCount++
                        continue
                    }
                    val result = repository.freezeApp(app)
                    if (result.success) successCount++ else failCount++
                }
            }

            val message = buildBulkResultMessage("Frozen", successCount, failCount, skippedCount)
            Log.i(TAG, "Bulk freeze completed: $message")
            _message.postValue(message)
            exitMultiSelectMode()
            loadApps()
        }
    }

    /**
     * Unfreeze all selected apps in multi-select mode.
     * Validates each package before attempting to unfreeze.
     */
    fun unfreezeSelected() {
        viewModelScope.launch {
            val selected = getSelectedApps()
            if (selected.isEmpty()) {
                _message.postValue("No apps selected")
                return@launch
            }

            Log.i(TAG, "Bulk unfreeze: ${selected.size} apps selected")
            _isLoading.postValue(true)
            var successCount = 0
            var failCount = 0
            var skippedCount = 0

            for (app in selected) {
                if (app.isFrozen) {
                    if (!repository.isPackageInstalled(app.packageName)) {
                        Log.w(TAG, "Bulk unfreeze skipped - package not installed: ${app.packageName}")
                        skippedCount++
                        continue
                    }
                    val result = repository.unfreezeApp(app)
                    if (result.success) successCount++ else failCount++
                }
            }

            val message = buildBulkResultMessage("Unfrozen", successCount, failCount, skippedCount)
            Log.i(TAG, "Bulk unfreeze completed: $message")
            _message.postValue(message)
            exitMultiSelectMode()
            loadApps()
        }
    }

    /**
     * Toggle selection state of an app in multi-select mode.
     */
    fun toggleSelection(appInfo: AppInfo) {
        val apps = _allApps.value?.toMutableList() ?: return
        val index = apps.indexOfFirst { it.packageName == appInfo.packageName }
        if (index >= 0) {
            apps[index] = apps[index].copy(isSelected = !apps[index].isSelected)
            _allApps.value = apps
            applyFilters(apps)
        }
    }

    /**
     * Select all visible apps.
     */
    fun selectAll() {
        val apps = _allApps.value?.toMutableList() ?: return
        val visiblePackages = _filteredApps.value?.map { it.packageName }?.toSet() ?: return
        for (i in apps.indices) {
            if (visiblePackages.contains(apps[i].packageName)) {
                apps[i] = apps[i].copy(isSelected = true)
            }
        }
        _allApps.value = apps
        applyFilters(apps)
    }

    /**
     * Deselect all apps.
     */
    fun deselectAll() {
        val apps = _allApps.value?.toMutableList() ?: return
        for (i in apps.indices) {
            apps[i] = apps[i].copy(isSelected = false)
        }
        _allApps.value = apps
        applyFilters(apps)
    }

    /**
     * Enter multi-select mode.
     */
    fun enterMultiSelectMode() {
        _isMultiSelectMode.value = true
    }

    /**
     * Exit multi-select mode and deselect all apps.
     */
    fun exitMultiSelectMode() {
        deselectAll()
        _isMultiSelectMode.value = false
    }

    // ================== Search & Filter ==================

    /**
     * Search/filter apps by name or package name.
     */
    fun searchApps(query: String) {
        currentQuery = query
        applyFilters(_allApps.value ?: emptyList())
    }

    /**
     * Search/filter frozen apps by name (for the Frozen Apps tab).
     * Updates filteredManagedFrozenApps LiveData with matching results.
     */
    fun searchFrozenApps(query: String) {
        frozenSearchQuery = query
        applyManagedFrozenFilter(_managedFrozenApps.value ?: emptyList())
    }

    /**
     * Toggle system apps visibility.
     */
    fun setShowSystemApps(show: Boolean) {
        showSystemApps = show
        loadApps()
    }

    /**
     * Toggle showing only frozen apps.
     */
    fun setShowOnlyFrozen(show: Boolean) {
        showOnlyFrozen = show
        applyFilters(_allApps.value ?: emptyList())
    }

    // ================== Bulk Operations (Settings) ==================

    /**
     * Freeze all non-frozen, non-system apps.
     */
    fun freezeAllApps() {
        viewModelScope.launch {
            val apps = _allApps.value ?: return@launch
            val toFreeze = apps.filter { !it.isFrozen && !it.isSystemApp }
            if (toFreeze.isEmpty()) {
                _message.postValue("No apps to freeze")
                return@launch
            }

            Log.i(TAG, "Freeze all: ${toFreeze.size} apps")
            _isLoading.postValue(true)
            var successCount = 0
            var failCount = 0

            for (app in toFreeze) {
                if (!repository.isPackageInstalled(app.packageName)) {
                    failCount++
                    continue
                }
                val result = repository.freezeApp(app)
                if (result.success) successCount++ else failCount++
            }

            val message = buildBulkResultMessage("Frozen", successCount, failCount, 0)
            Log.i(TAG, "Freeze all completed: $message")
            _message.postValue(message)
            loadApps()
        }
    }

    /**
     * Unfreeze all frozen apps.
     */
    fun unfreezeAllApps() {
        viewModelScope.launch {
            val apps = _allApps.value ?: return@launch
            val toUnfreeze = apps.filter { it.isFrozen }
            if (toUnfreeze.isEmpty()) {
                _message.postValue("No apps to unfreeze")
                return@launch
            }

            Log.i(TAG, "Unfreeze all: ${toUnfreeze.size} apps")
            _isLoading.postValue(true)
            var successCount = 0
            var failCount = 0

            for (app in toUnfreeze) {
                if (!repository.isPackageInstalled(app.packageName)) {
                    failCount++
                    continue
                }
                val result = repository.unfreezeApp(app)
                if (result.success) successCount++ else failCount++
            }

            val message = buildBulkResultMessage("Unfrozen", successCount, failCount, 0)
            Log.i(TAG, "Unfreeze all completed: $message")
            _message.postValue(message)
            loadApps()
        }
    }

    // ================== Backup & Logs ==================

    /**
     * Export freeze states for backup.
     *
     * @return List of currently frozen package names
     */
    suspend fun exportFreezeStates(): List<String> = repository.exportFreezeStates()

    /**
     * Import freeze states from backup.
     *
     * @param packageNames List of package names to freeze
     */
    fun importFreezeStates(packageNames: List<String>) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            Log.i(TAG, "Importing freeze states for ${packageNames.size} packages")
            val results = repository.importFreezeStates(packageNames)
            val successCount = results.values.count { it }
            val failCount = results.values.count { !it }
            val message = "Restored: $successCount, Failed: $failCount"
            Log.i(TAG, "Import completed: $message")
            _message.postValue(message)
            loadApps()
        }
    }

    /**
     * Backup freeze states and post a message.
     */
    fun backupFreezeStates() {
        viewModelScope.launch {
            try {
                val frozenPackages = exportFreezeStates()
                Log.i(TAG, "Backup completed: ${frozenPackages.size} frozen packages")
                _message.postValue("Backed up ${frozenPackages.size} frozen apps")
            } catch (e: Exception) {
                Log.e(TAG, "Backup failed", e)
                _message.postValue("Backup failed: ${e.message}")
            }
        }
    }

    /**
     * Clear action logs.
     */
    fun clearActionLogs() {
        viewModelScope.launch {
            repository.clearActionLogs()
            _message.postValue("Action history cleared")
        }
    }

    // ================== Private Helpers ==================

    /**
     * Build a summary message for bulk operations.
     */
    private fun buildBulkResultMessage(
        action: String,
        successCount: Int,
        failCount: Int,
        skippedCount: Int
    ): String = buildString {
        append("$action: $successCount")
        if (failCount > 0) append(", Failed: $failCount")
        if (skippedCount > 0) append(", Skipped: $skippedCount")
    }

    /**
     * Get currently selected apps from All Apps list.
     */
    private fun getSelectedApps(): List<AppInfo> {
        return _allApps.value?.filter { it.isSelected } ?: emptyList()
    }

    /**
     * Apply search and filter criteria to the All Apps list.
     * Also updates the legacy frozen apps list.
     */
    private fun applyFilters(apps: List<AppInfo>) {
        var filtered = apps

        // Apply search query
        if (currentQuery.isNotBlank()) {
            val query = currentQuery.lowercase()
            filtered = filtered.filter {
                it.appName.lowercase().contains(query) ||
                        it.packageName.lowercase().contains(query)
            }
        }

        // Apply frozen-only filter
        if (showOnlyFrozen) {
            filtered = filtered.filter { it.isFrozen }
        }

        _filteredApps.postValue(filtered)

        // Update legacy frozen apps list
        val frozen = apps.filter { it.isFrozen }
        _frozenApps.postValue(frozen)
        applyFrozenFilter(frozen)
    }

    /**
     * Apply search filter to the legacy frozen apps list.
     */
    private fun applyFrozenFilter(frozenApps: List<AppInfo>) {
        if (frozenSearchQuery.isBlank()) {
            _filteredFrozenApps.postValue(frozenApps)
        } else {
            val query = frozenSearchQuery.lowercase()
            val filtered = frozenApps.filter {
                it.appName.lowercase().contains(query) ||
                        it.packageName.lowercase().contains(query)
            }
            _filteredFrozenApps.postValue(filtered)
        }
    }

    /**
     * Apply search filter to the managed frozen apps list.
     * Filters by app name or package name, case-insensitive.
     */
    private fun applyManagedFrozenFilter(managedApps: List<AppInfo>) {
        if (frozenSearchQuery.isBlank()) {
            _filteredManagedFrozenApps.postValue(managedApps)
        } else {
            val query = frozenSearchQuery.lowercase()
            val filtered = managedApps.filter {
                it.appName.lowercase().contains(query) ||
                        it.packageName.lowercase().contains(query)
            }
            _filteredManagedFrozenApps.postValue(filtered)
        }
    }

    /**
     * Load action logs from the database.
     */
    private fun loadActionLogs() {
        viewModelScope.launch {
            repository.getActionLogs().collectLatest { logs ->
                _actionLogs.postValue(logs)
            }
        }
    }
}
