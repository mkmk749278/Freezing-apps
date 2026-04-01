package com.freezingapps.app.root

/**
 * Result of a root command execution.
 *
 * @property success Whether the command executed successfully
 * @property output The command's stdout output
 * @property error The command's stderr output (if any)
 * @property exitCode The process exit code
 */
data class RootCommandResult(
    val success: Boolean,
    val output: String = "",
    val error: String = "",
    val exitCode: Int = -1
)
