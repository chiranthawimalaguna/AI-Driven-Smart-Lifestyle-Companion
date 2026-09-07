package com.smartlifestyle.companion.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Reads real wearable data that reaches the phone via:
 *   Redmi Watch 3 Active -> Mi Fitness app -> Google Fit sync -> Health Connect.
 *
 * The watch has no public SDK to pair with directly, so Health Connect is the
 * correct, Google-documented integration point. Sync can lag or simply not be set
 * up yet, so every read here returns null on any failure or absence of data -
 * callers (HealthRepository) always have a fallback (the phone's own step sensor,
 * or simulated heart rate) so a flaky or unconfigured sync never breaks the app,
 * it just means the reading falls back to what already worked before this was
 * added.
 */
class HealthConnectManager(private val context: Context) {

    private val client: HealthConnectClient? by lazy {
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    val isAvailable: Boolean get() = client != null

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        val hc = client ?: return false
        val granted = hc.permissionController.getGrantedPermissions()
        return granted.containsAll(requiredPermissions)
    }

    /** Returns null if Health Connect is unavailable, not permitted, or has no
     * reading for today - caller should fall back to the phone's own sensor. */
    suspend fun readTodaySteps(): Long? {
        return try {
            val hc = client ?: return null
            val start = Instant.now().truncatedTo(ChronoUnit.DAYS)
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, Instant.now())
                )
            )
            if (response.records.isEmpty()) null else response.records.sumOf { it.count }
        } catch (e: Exception) {
            null
        }
    }

    /** Most recent heart-rate sample from the last 6 hours, or null if none synced. */
    suspend fun readLatestHeartRate(): Double? {
        return try {
            val hc = client ?: return null
            val start = Instant.now().minus(6, ChronoUnit.HOURS)
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, Instant.now())
                )
            )
            response.records
                .flatMap { it.samples }
                .maxByOrNull { it.time }
                ?.beatsPerMinute?.toDouble()
        } catch (e: Exception) {
            null
        }
    }

    /** Total sleep duration (minutes) from the last 24 hours, or null if none synced. */
    suspend fun readLastNightSleepMinutes(): Long? {
        return try {
            val hc = client ?: return null
            val start = Instant.now().minus(1, ChronoUnit.DAYS)
            val response = hc.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, Instant.now())
                )
            )
            if (response.records.isEmpty()) null
            else response.records.sumOf { ChronoUnit.MINUTES.between(it.startTime, it.endTime) }
        } catch (e: Exception) {
            null
        }
    }
}
