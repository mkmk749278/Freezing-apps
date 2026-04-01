package com.freezingapps.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an action log entry stored in the local database.
 * Tracks freeze/unfreeze operations for user history.
 *
 * @property id Auto-generated primary key
 * @property packageName The target app's package name
 * @property appName The target app's display name
 * @property action The action performed ("freeze" or "unfreeze")
 * @property success Whether the action succeeded
 * @property timestamp When the action was performed (epoch millis)
 * @property errorMessage Optional error message if the action failed
 */
@Entity(tableName = "action_log")
data class ActionLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val packageName: String,
    val appName: String,
    val action: String,
    val success: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
