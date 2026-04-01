package com.freezingapps.app.root

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * Handles execution of root (su) commands for freeze/unfreeze operations.
 * Designed to work with KernelSU (KSU) root authentication.
 *
 * Security notes:
 * - All commands are executed through the 'su' binary
 * - Package names are validated before use in commands
 * - Only 'pm disable' and 'pm enable' commands are used for freeze/unfreeze
 */
object RootCommandExecutor {

    private const val TAG = "RootCommandExecutor"

    // Regex pattern for valid Android package names
    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)*$")

    /**
     * Check if root access (su) is available.
     * Executes a simple 'id' command via su to verify root.
     *
     * @return true if root access is available
     */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("id\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            val exitCode = process.waitFor()

            val available = exitCode == 0 && output.contains("uid=0")
            Log.d(TAG, "Root availability check: $available")
            available
        } catch (e: Exception) {
            Log.e(TAG, "Root availability check failed", e)
            false
        }
    }

    /**
     * Execute a command with root (su) privileges.
     *
     * @param command The command to execute
     * @return RootCommandResult with the execution result
     */
    suspend fun executeCommand(command: String): RootCommandResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Executing root command: $command")
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)

            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()

            val result = RootCommandResult(
                success = exitCode == 0,
                output = stdout.trim(),
                error = stderr.trim(),
                exitCode = exitCode
            )
            Log.d(TAG, "Command result: success=${result.success}, exitCode=$exitCode, output=${result.output}, error=${result.error}")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Root command execution failed: $command", e)
            RootCommandResult(
                success = false,
                error = e.message ?: "Unknown error executing root command"
            )
        }
    }

    /**
     * Freeze (disable) an app using 'pm disable-user' command.
     * Validates the package name before execution.
     *
     * @param packageName The package name to freeze
     * @return RootCommandResult with the operation result
     */
    suspend fun freezeApp(packageName: String): RootCommandResult {
        Log.i(TAG, "Freeze requested for package: $packageName")
        if (!isValidPackageName(packageName)) {
            Log.w(TAG, "Freeze rejected - invalid package name format: $packageName")
            return RootCommandResult(
                success = false,
                error = "Invalid package name: $packageName"
            )
        }
        val result = executeCommand("pm disable-user --user 0 $packageName")
        Log.i(TAG, "Freeze result for $packageName: success=${result.success}")
        return result
    }

    /**
     * Unfreeze (enable) an app using 'pm enable' command.
     * Validates the package name before execution.
     *
     * @param packageName The package name to unfreeze
     * @return RootCommandResult with the operation result
     */
    suspend fun unfreezeApp(packageName: String): RootCommandResult {
        Log.i(TAG, "Unfreeze requested for package: $packageName")
        if (!isValidPackageName(packageName)) {
            Log.w(TAG, "Unfreeze rejected - invalid package name format: $packageName")
            return RootCommandResult(
                success = false,
                error = "Invalid package name: $packageName"
            )
        }
        val result = executeCommand("pm enable --user 0 $packageName")
        Log.i(TAG, "Unfreeze result for $packageName: success=${result.success}")
        return result
    }

    /**
     * Check if a specific package is currently frozen (disabled).
     *
     * @param packageName The package name to check
     * @return true if the package is disabled/frozen
     */
    suspend fun isAppFrozen(packageName: String): Boolean {
        if (!isValidPackageName(packageName)) return false
        val result = executeCommand("pm list packages -d")
        return result.success && result.output.contains("package:$packageName")
    }

    /**
     * Get a list of all disabled (frozen) packages.
     *
     * @return List of frozen package names
     */
    suspend fun getFrozenPackages(): List<String> {
        val result = executeCommand("pm list packages -d")
        if (!result.success) return emptyList()

        return result.output
            .lines()
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:").trim() }
    }

    /**
     * Validates that a package name follows Android package naming conventions.
     * Prevents command injection by ensuring only valid characters are used.
     *
     * @param packageName The package name to validate
     * @return true if the package name is valid
     */
    private fun isValidPackageName(packageName: String): Boolean {
        return packageName.isNotBlank() &&
                packageName.length <= 256 &&
                PACKAGE_NAME_REGEX.matches(packageName)
    }
}
