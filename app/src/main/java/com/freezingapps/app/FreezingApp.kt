package com.freezingapps.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

/**
 * Application class for Freezing Apps.
 * Initializes notification channels and app-wide configuration.
 */
class FreezingApp : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "freezing_apps_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Freeze Operations"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Creates the notification channel for freeze/unfreeze operation results.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for freeze/unfreeze operations"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
