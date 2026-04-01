package com.freezingapps.app.data.model

/**
 * Represents a scheduled freeze/unfreeze task.
 *
 * @property id Unique identifier for the scheduled task
 * @property packageName The target app's package name
 * @property appName The target app's display name
 * @property action The action to perform ("freeze" or "unfreeze")
 * @property scheduledTimeMillis When the task should execute (epoch millis)
 * @property workRequestId The WorkManager work request UUID string
 */
data class ScheduledTask(
    val id: String,
    val packageName: String,
    val appName: String,
    val action: String,
    val scheduledTimeMillis: Long,
    val workRequestId: String
)
