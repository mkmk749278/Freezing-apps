package com.freezingapps.app.data.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed app with its freeze status.
 *
 * @property packageName The app's package name (e.g., "com.example.app")
 * @property appName The user-visible app name
 * @property icon The app's launcher icon
 * @property isSystemApp Whether this is a system app
 * @property isFrozen Whether the app is currently frozen (disabled)
 * @property isSelected Whether the app is selected for bulk operations
 * @property isInFrozenList Whether the app has been added to the managed Frozen tab
 */
data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    var isFrozen: Boolean = false,
    var isSelected: Boolean = false,
    var isInFrozenList: Boolean = false
)
