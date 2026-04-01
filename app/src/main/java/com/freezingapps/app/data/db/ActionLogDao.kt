package com.freezingapps.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.freezingapps.app.data.model.ActionLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for action log entries.
 * Provides methods to insert and query freeze/unfreeze history.
 */
@Dao
interface ActionLogDao {

    /**
     * Insert a new action log entry.
     */
    @Insert
    suspend fun insert(actionLog: ActionLog)

    /**
     * Get all action logs ordered by most recent first.
     * Returns a Flow for reactive UI updates.
     */
    @Query("SELECT * FROM action_log ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<ActionLog>>

    /**
     * Get action logs for a specific package.
     */
    @Query("SELECT * FROM action_log WHERE packageName = :packageName ORDER BY timestamp DESC")
    fun getLogsForPackage(packageName: String): Flow<List<ActionLog>>

    /**
     * Delete all action logs.
     */
    @Query("DELETE FROM action_log")
    suspend fun deleteAll()

    /**
     * Get the count of all log entries.
     */
    @Query("SELECT COUNT(*) FROM action_log")
    suspend fun getCount(): Int
}
