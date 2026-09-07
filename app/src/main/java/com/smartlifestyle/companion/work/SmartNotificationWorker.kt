package com.smartlifestyle.companion.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.smartlifestyle.companion.data.remote.AuthRepository
import com.smartlifestyle.companion.data.repository.RoutineRepository
import com.smartlifestyle.companion.data.repository.WeatherRepository
import com.smartlifestyle.companion.notification.NotificationHelper

/**
 * Runs on a periodic schedule (see WorkScheduler) but only actually notifies the user
 * when context genuinely warrants it - this is what makes the notifications "smart"
 * rather than fixed-time alarms: an overdue task combined with bad weather produces a
 * different message than an overdue task on a clear day, and a day with no overdue
 * tasks produces no notification at all.
 */
class SmartNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val hasNotificationPermission = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotificationPermission) return Result.success() // nothing to do, not an error

        val hasLocationPermission = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val routineRepository = RoutineRepository(FirebaseFirestore.getInstance(), AuthRepository())
        val overdueCount = routineRepository.getOverdueCount()

        val weather = WeatherRepository(applicationContext).getCurrentWeather(hasLocationPermission)

        val message = when {
            overdueCount > 0 && weather.isRaining ->
                "You have $overdueCount overdue task(s) and it's raining - good time to catch up indoors."
            overdueCount > 0 ->
                "You have $overdueCount overdue task(s) waiting."
            else -> null
        }

        message?.let {
            NotificationHelper.show(
                applicationContext,
                id = 1001,
                title = "Smart Lifestyle Companion",
                message = it
            )
        }

        return Result.success()
    }
}
