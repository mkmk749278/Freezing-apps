package com.freezingapps.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.freezingapps.app.data.model.FrozenApp
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the managed frozen apps list.
 * Provides methods to add/remove apps from the Frozen tab.
 */
@Dao
interface FrozenAppDao {

    /**
     * Add an app to the frozen list.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(frozenApp: FrozenApp)

    /**
     * Add multiple apps to the frozen list.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(frozenApps: List<FrozenApp>)

    /**
     * Remove an app from the frozen list.
     */
    @Query("DELETE FROM frozen_apps WHERE packageName = :packageName")
    suspend fun delete(packageName: String)

    /**
     * Get all frozen list entries as a reactive Flow.
     */
    @Query("SELECT * FROM frozen_apps ORDER BY addedTimestamp DESC")
    fun getAll(): Flow<List<FrozenApp>>

    /**
     * Get all package names in the frozen list (non-reactive snapshot).
     */
    @Query("SELECT packageName FROM frozen_apps")
    suspend fun getAllPackageNames(): List<String>

    /**
     * Check if a package is in the frozen list.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM frozen_apps WHERE packageName = :packageName)")
    suspend fun exists(packageName: String): Boolean

    /**
     * Remove all apps from the frozen list.
     */
    @Query("DELETE FROM frozen_apps")
    suspend fun deleteAll()
}
