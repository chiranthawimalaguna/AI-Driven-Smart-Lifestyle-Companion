package com.smartlifestyle.companion.work

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import com.smartlifestyle.companion.data.health.DeviceSensorManager
import com.smartlifestyle.companion.data.health.HealthConnectManager
import com.smartlifestyle.companion.data.local.AppDatabase
import com.smartlifestyle.companion.data.remote.AuthRepository
import com.smartlifestyle.companion.data.repository.HealthRepository
import com.smartlifestyle.companion.notification.NotificationHelper

/**
 * Periodic hydration reminder. Like SmartNotificationWorker, this only actually
 * notifies when it's genuinely useful - if the user has already hit today's glass
 * goal, it stays quiet rather than nagging.
 */
class WaterReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val hasNotificationPermission = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasNotificationPermission) return Result.success()

        val authRepository = AuthRepository()
        val firestore = FirebaseFirestore.getInstance()
        val db = AppDatabase.getInstance(applicationContext)
        val healthRepository = HealthRepository(
            DeviceSensorManager(applicationContext),
            HealthConnectManager(applicationContext),
            db.healthDao(),
            firestore
        ) { authRepository.currentUser?.uid }

        val glasses = healthRepository.todayWaterGlassCount()
        if (glasses < DAILY_GLASS_GOAL) {
            NotificationHelper.show(
                applicationContext,
                id = 1002,
                title = "Stay hydrated",
                message = "You've had $glasses of $DAILY_GLASS_GOAL glasses today \u2014 time for some water?"
            )
        }

        return Result.success()
    }

    companion object {
        private const val DAILY_GLASS_GOAL = 8
    }
}
