package com.freezingapps.app.worker

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.freezingapps.app.FreezingApp
import com.freezingapps.app.R
import com.freezingapps.app.data.db.AppDatabase
import com.freezingapps.app.data.model.ActionLog
import com.freezingapps.app.root.RootCommandExecutor

/**
 * WorkManager worker for scheduled freeze/unfreeze operations.
 * Executes the scheduled task and sends a notification with the result.
 */
class FreezeWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val KEY_PACKAGE_NAME = "package_name"
        const val KEY_ACTION = "action"
        private const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        val packageName = inputData.getString(KEY_PACKAGE_NAME) ?: return Result.failure()
        val action = inputData.getString(KEY_ACTION) ?: return Result.failure()

        // Execute the freeze/unfreeze command
        val result = if (action == "freeze") {
            RootCommandExecutor.freezeApp(packageName)
        } else {
            RootCommandExecutor.unfreezeApp(packageName)
        }

        // Log the action
        val database = AppDatabase.getInstance(applicationContext)
        database.actionLogDao().insert(
            ActionLog(
                packageName = packageName,
                appName = packageName, // Use package name since we don't have display name
                action = action,
                success = result.success,
                errorMessage = if (!result.success) result.error else null
            )
        )

        // Send notification
        sendNotification(packageName, action, result.success)

        return if (result.success) Result.success() else Result.retry()
    }

    /**
     * Send a notification with the operation result.
     */
    private fun sendNotification(packageName: String, action: String, success: Boolean) {
        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val title = if (success) {
            applicationContext.getString(R.string.notification_success_title)
        } else {
            applicationContext.getString(R.string.notification_failure_title)
        }

        val message = if (success) {
            applicationContext.getString(R.string.notification_success_message, action, packageName)
        } else {
            applicationContext.getString(R.string.notification_failure_message, action, packageName)
        }

        val notification = NotificationCompat.Builder(
            applicationContext, FreezingApp.NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
