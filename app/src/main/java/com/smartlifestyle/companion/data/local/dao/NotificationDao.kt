package com.smartlifestyle.companion.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.smartlifestyle.companion.data.local.entity.NotificationEntity

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: NotificationEntity)

    @Query("SELECT * FROM notifications ORDER BY timestampMillis DESC LIMIT :limit")
    suspend fun getLatest(limit: Int = 20): List<NotificationEntity>
}