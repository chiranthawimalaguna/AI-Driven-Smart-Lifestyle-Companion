package com.smartlifestyle.companion

import android.app.Application
import com.smartlifestyle.companion.data.local.AppDatabase
import com.smartlifestyle.companion.notification.NotificationHelper
import com.smartlifestyle.companion.work.WorkScheduler

class SmartLifestyleCompanionApp : Application() {

    // Single Room instance shared across the app (data layer entry point)
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        // Firebase auto-initialises via the google-services plugin.
        NotificationHelper.createChannel(this)
        WorkScheduler.scheduleSmartNotifications(this)
        WorkScheduler.scheduleWaterReminders(this)
    }
}
