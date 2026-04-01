package com.freezingapps.app.root

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

            // Root is available if exit code is 0 and output contains "uid=0"
            exitCode == 0 && output.contains("uid=0")
        } catch (e: Exception) {
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
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)

            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText()
            val exitCode = process.waitFor()

            RootCommandResult(
                success = exitCode == 0,
                output = stdout.trim(),
                error = stderr.trim(),
                exitCode = exitCode
            )
        } catch (e: Exception) {
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
        if (!isValidPackageName(packageName)) {
            return RootCommandResult(
                success = false,
                error = "Invalid package name: $packageName"
            )
        }
        return executeCommand("pm disable-user --user 0 $packageName")
    }

    /**
     * Unfreeze (enable) an app using 'pm enable' command.
     * Validates the package name before execution.
     *
     * @param packageName The package name to unfreeze
     * @return RootCommandResult with the operation result
     */
    suspend fun unfreezeApp(packageName: String): RootCommandResult {
        if (!isValidPackageName(packageName)) {
            return RootCommandResult(
                success = false,
                error = "Invalid package name: $packageName"
            )
        }
        return executeCommand("pm enable --user 0 $packageName")
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
