package com.smartlifestyle.companion.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartlifestyle.companion.data.local.dao.ChatDao
import com.smartlifestyle.companion.data.local.dao.HealthDao
import com.smartlifestyle.companion.data.local.entity.ChatMessageEntity
import com.smartlifestyle.companion.data.local.entity.HealthMetricEntity

@Database(
    entities = [HealthMetricEntity::class, ChatMessageEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun healthDao(): HealthDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_lifestyle.db"
                )
                    .fallbackToDestructiveMigration() // fine for a project skeleton; use real
                    // migrations before this touches production data
                    .build().also { INSTANCE = it }
            }
    }
}
