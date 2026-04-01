package com.freezingapps.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.freezingapps.app.data.model.ActionLog
import com.freezingapps.app.data.model.FrozenApp

/**
 * Room database for storing action history and managed frozen apps list.
 * Uses a singleton pattern to ensure only one instance exists.
 */
@Database(
    entities = [ActionLog::class, FrozenApp::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun actionLogDao(): ActionLogDao
    abstract fun frozenAppDao(): FrozenAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from version 1 to 2: adds the frozen_apps table.
         * Preserves existing action_log data.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `frozen_apps` (" +
                            "`packageName` TEXT NOT NULL, " +
                            "`addedTimestamp` INTEGER NOT NULL, " +
                            "PRIMARY KEY(`packageName`))"
                )
            }
        }

        /**
         * Get the singleton database instance.
         * Creates the database on first call.
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freezing_apps_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
