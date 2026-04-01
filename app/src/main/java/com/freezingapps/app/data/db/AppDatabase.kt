package com.freezingapps.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
