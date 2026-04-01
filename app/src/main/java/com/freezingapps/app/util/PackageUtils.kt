package com.freezingapps.app.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Utility functions for package validation and checks.
 */
object PackageUtils {

    private const val TAG = "PackageUtils"

    /**
     * Check if a package is installed on the device using PackageManager.
     *
     * @param context Application or activity context
     * @param packageName The package name to check
     * @return true if the package is installed
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Package not found: $packageName")
            false
        }
    }
}
