package com.freezingapps.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an app that the user has added to the Frozen tab for management.
 * Persisted in Room database to survive app restarts.
 *
 * @property packageName The app's unique package name (primary key)
 * @property addedTimestamp When the app was added to the frozen list (epoch millis)
 */
@Entity(tableName = "frozen_apps")
data class FrozenApp(
    @PrimaryKey
    val packageName: String,
    val addedTimestamp: Long = System.currentTimeMillis()
)
