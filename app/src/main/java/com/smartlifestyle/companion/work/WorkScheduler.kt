package com.smartlifestyle.companion.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WorkScheduler {

    private const val TASK_WORK_NAME = "smart_notification_check"
    private const val WATER_WORK_NAME = "water_reminder_check"

    /** WorkManager's minimum periodic interval is 15 minutes; 2 hours is plenty for nudges. */
    fun scheduleSmartNotifications(context: Context) {
        val request = PeriodicWorkRequestBuilder<SmartNotificationWorker>(2, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            TASK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Separate cadence (3 hours) from the task reminders, so the two don't always
     * fire in the same window and start to feel like notification spam. */
    fun scheduleWaterReminders(context: Context) {
        val request = PeriodicWorkRequestBuilder<WaterReminderWorker>(3, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WATER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
