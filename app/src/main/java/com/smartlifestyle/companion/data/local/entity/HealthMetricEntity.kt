package com.smartlifestyle.companion.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class MetricSource { HEALTH_CONNECT, DEVICE_SENSOR, MANUAL_ENTRY }

@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,          // "steps" | "heart_rate" | "sleep_minutes"
    val value: Float,
    val recordedAtMillis: Long,
    val source: MetricSource   // lets the UI show "synced from watch" vs "entered manually"
)
