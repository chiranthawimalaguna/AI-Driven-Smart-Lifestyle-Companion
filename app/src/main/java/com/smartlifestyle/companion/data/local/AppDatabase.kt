package com.smartlifestyle.companion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartlifestyle.companion.data.local.dao.ChatDao
import com.smartlifestyle.companion.data.local.dao.HealthDao
import com.smartlifestyle.companion.data.local.dao.NotificationDao
import com.smartlifestyle.companion.data.local.entity.ChatMessageEntity
import com.smartlifestyle.companion.data.local.entity.HealthMetricEntity
import com.smartlifestyle.companion.data.local.entity.NotificationEntity

@Database(
    entities = [HealthMetricEntity::class, ChatMessageEntity::class, NotificationEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun healthDao(): HealthDao
    abstract fun chatDao(): ChatDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "smart_lifestyle.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}