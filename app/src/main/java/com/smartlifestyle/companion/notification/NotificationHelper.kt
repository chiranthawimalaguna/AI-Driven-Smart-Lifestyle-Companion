package com.smartlifestyle.companion.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartlifestyle.companion.R
import com.smartlifestyle.companion.data.local.AppDatabase
import com.smartlifestyle.companion.data.local.entity.NotificationEntity

object NotificationHelper {
    private const val CHANNEL_ID = "smart_lifestyle_nudges"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID, "Smart lifestyle nudges", NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Context-aware reminders (movement, overdue tasks, weather changes)" }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    suspend fun show(context: Context, id: Int, title: String, message: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
        AppDatabase.getInstance(context).notificationDao().insert(
            NotificationEntity(title = title, message = message, timestampMillis = System.currentTimeMillis())
        )
    }
}