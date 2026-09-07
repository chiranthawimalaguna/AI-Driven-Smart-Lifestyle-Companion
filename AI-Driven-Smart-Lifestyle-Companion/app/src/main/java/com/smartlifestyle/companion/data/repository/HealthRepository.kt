package com.smartlifestyle.companion.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.smartlifestyle.companion.data.health.DeviceSensorManager
import com.smartlifestyle.companion.data.health.HealthConnectManager
import com.smartlifestyle.companion.data.local.dao.HealthDao
import com.smartlifestyle.companion.data.local.entity.HealthMetricEntity
import com.smartlifestyle.companion.data.local.entity.MetricSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

sealed class HealthReading(val value: Float, val source: MetricSource)
class SensorReading(value: Float) : HealthReading(value, MetricSource.DEVICE_SENSOR)
class WatchReading(value: Float) : HealthReading(value, MetricSource.HEALTH_CONNECT)
class ManualReading(value: Float) : HealthReading(value, MetricSource.MANUAL_ENTRY)

/**
 * Sensor data is buffered in Room first (high-frequency, cheap local writes), then
 * pushed to Firestore as a durable cloud copy - the opposite order to
 * RoutineRepository, which writes straight to Firestore. Buffering locally first
 * avoids a cloud write on every single sensor event; only the values the app
 * actually surfaces to the user get synced.
 *
 * Cloud pushes are best-effort: a failed push never blocks the UI, since Room
 * already has the value the user needs to see right now.
 *
 * Data source priority (highest to lowest, each falling back to the next):
 *   Heart rate:  Health Connect (real watch data) -> simulated
 *   Steps:       Health Connect (real watch data) -> phone's own step-counter
 *                sensor -> manual entry
 *   Ambient light: phone's own light sensor only (no watch equivalent)
 */
class HealthRepository(
    private val sensorManager: DeviceSensorManager,
    private val healthConnectManager: HealthConnectManager,
    private val healthDao: HealthDao,
    private val firestore: FirebaseFirestore,
    private val userIdProvider: () -> String?
) {

    fun observeSteps(): Flow<List<HealthMetricEntity>> = healthDao.observeMetric("steps")

    /** Generic history reader used by the trends/history screen - any metric type
     * already buffered in Room (steps, heart_rate, ambient_light). */
    fun observeMetricHistory(type: String): Flow<List<HealthMetricEntity>> = healthDao.observeMetric(type)

    /**
     * Tries real watch step data via Health Connect first (Redmi Watch 3 Active,
     * synced through Mi Fitness -> Google Fit -> Health Connect). Falls back to the
     * phone's own step-counter sensor, then to whatever the user last entered
     * manually. Each fallback is guarded with a timeout so an unresponsive source
     * never freezes the refresh cycle - see refreshHeartRate() for the same pattern.
     */
    suspend fun refreshSteps(): HealthReading {
        val fromWatch = withTimeoutOrNull(SENSOR_READ_TIMEOUT_MS) { healthConnectManager.readTodaySteps() }
        val reading = if (fromWatch != null) {
            WatchReading(fromWatch.toFloat())
        } else {
            val sensed = if (sensorManager.hasStepSensor) {
                withTimeoutOrNull(SENSOR_READ_TIMEOUT_MS) { sensorManager.observeStepCount().firstOrNull() }
            } else null
            if (sensed != null) SensorReading(sensed.toFloat()) else ManualReading(0f)
        }
        saveAndSync(
            HealthMetricEntity(
                type = "steps",
                value = reading.value,
                recordedAtMillis = System.currentTimeMillis(),
                source = reading.source
            )
        )
        return reading
    }

    /** Called from the manual-entry fallback UI when the device has no step sensor. */
    suspend fun recordManualSteps(steps: Float) {
        saveAndSync(
            HealthMetricEntity(
                type = "steps",
                value = steps,
                recordedAtMillis = System.currentTimeMillis(),
                source = MetricSource.MANUAL_ENTRY
            )
        )
    }

    /**
     * Tries real watch data via Health Connect first (Redmi Watch 3 Active, synced
     * through Mi Fitness -> Google Fit -> Health Connect). Falls back to the
     * SIMULATED generator - see DeviceSensorManager's doc comment - if Health
     * Connect isn't set up, has no permission, or simply has no recent reading yet.
     * Either way the dashboard always has something to show; the source is recorded
     * so the UI can honestly label which one it got.
     */
    suspend fun refreshHeartRate(): HealthReading {
        val fromWatch = withTimeoutOrNull(SENSOR_READ_TIMEOUT_MS) {
            healthConnectManager.readLatestHeartRate()
        }
        val reading = if (fromWatch != null) {
            WatchReading(fromWatch.toFloat())
        } else {
            ManualReading(sensorManager.simulatedHeartRate().toFloat())
        }
        saveAndSync(
            HealthMetricEntity(
                type = "heart_rate",
                value = reading.value,
                recordedAtMillis = System.currentTimeMillis(),
                source = reading.source
            )
        )
        return reading
    }

    /**
     * Reads last night's sleep duration from Health Connect and buffers it as a
     * "sleep_minutes" metric, same pattern as steps/heart rate. This closes a real
     * gap: HealthConnectManager already had readLastNightSleepMinutes() implemented,
     * but nothing in the app was calling it yet - sleep never actually reached the
     * dashboard or Insights before this. Returns null (and records nothing) if no
     * sleep session has synced, rather than fabricating a number.
     */
    suspend fun refreshSleep(): Int? {
        val minutes = withTimeoutOrNull(SENSOR_READ_TIMEOUT_MS) {
            healthConnectManager.readLastNightSleepMinutes()
        } ?: return null
        saveAndSync(
            HealthMetricEntity(
                type = "sleep_minutes",
                value = minutes.toFloat(),
                recordedAtMillis = System.currentTimeMillis(),
                source = MetricSource.HEALTH_CONNECT
            )
        )
        return minutes.toInt()
    }

    /** Mood is always manually entered (1-5 scale) - there's no sensor for it. */
    suspend fun logMood(score: Int) = recordMetric("mood", score.toFloat())

    /** Average of all mood entries recorded in the last 7 days, or null if none. */
    suspend fun averageMoodLast7Days(): Float? {
        val all = healthDao.observeMetric("mood").firstOrNull() ?: return null
        val weekAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
        val recent = all.filter { it.recordedAtMillis >= weekAgo }
        return if (recent.isEmpty()) null else recent.map { it.value }.average().toFloat()
    }

    /** Real ambient light reading in lux from the phone's light sensor - the app's
     * second IoT/environment sensor alongside the step counter. Null if the device
     * has no light sensor. */
    suspend fun refreshAmbientLight(): Float? {
        if (!sensorManager.hasLightSensor) return null
        val lux = withTimeoutOrNull(SENSOR_READ_TIMEOUT_MS) { sensorManager.observeAmbientLight().firstOrNull() } ?: return null
        saveAndSync(
            HealthMetricEntity(
                type = "ambient_light",
                value = lux,
                recordedAtMillis = System.currentTimeMillis(),
                source = MetricSource.DEVICE_SENSOR
            )
        )
        return lux
    }

    /**
     * Looks back through recent step-count readings (already stored in Room by
     * refreshSteps) for the most recent point where the count actually increased,
     * and returns how many minutes ago that was. This is what feeds
     * UserContext.minutesSinceLastMovement, replacing what was a hardcoded 0 -
     * the sedentary nudge in GetSmartSuggestionsUseCase now reflects real history
     * instead of always assuming the user just moved.
     */
    suspend fun minutesSinceLastMovement(): Int {
        val readings = healthDao.observeMetric("steps").firstOrNull() ?: return DEFAULT_SEDENTARY_MINUTES
        if (readings.size < 2) return 0 // not enough history yet - assume recently active

        for (i in 0 until readings.size - 1) {
            if (readings[i].value > readings[i + 1].value) {
                val elapsedMillis = System.currentTimeMillis() - readings[i].recordedAtMillis
                return TimeUnit.MILLISECONDS.toMinutes(elapsedMillis).toInt()
            }
        }
        // No increase found anywhere in the stored window - treat as "a while."
        return DEFAULT_SEDENTARY_MINUTES
    }

    /** Generic manual-entry recorder, reused by BMI and water-glass logging - both
     * are simple user-entered values with no sensor involved, so they don't need
     * their own bespoke save methods the way steps/heart rate do. */
    suspend fun recordMetric(type: String, value: Float) {
        saveAndSync(
            HealthMetricEntity(
                type = type,
                value = value,
                recordedAtMillis = System.currentTimeMillis(),
                source = MetricSource.MANUAL_ENTRY
            )
        )
    }

    suspend fun recordBmi(bmi: Float) = recordMetric("bmi", bmi)

    /** Each call logs exactly one glass (value is always 1f - the count of entries
     * today is what matters, not the value itself). */
    suspend fun logWaterGlass() = recordMetric("water_glass", 1f)

    /** Counts water_glass entries recorded since local midnight - i.e. "today". */
    suspend fun todayWaterGlassCount(): Int {
        val all = healthDao.observeMetric("water_glass").firstOrNull() ?: return 0
        val startOfDay = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        return all.count { it.recordedAtMillis >= startOfDay }
    }

    private suspend fun saveAndSync(metric: HealthMetricEntity) {
        healthDao.insert(metric)
        pushToCloud(metric)
    }

    private suspend fun pushToCloud(metric: HealthMetricEntity) {
        val uid = userIdProvider() ?: return
        try {
            firestore.collection("users").document(uid).collection("health_metrics")
                .add(
                    hashMapOf(
                        "type" to metric.type,
                        "value" to metric.value,
                        "recordedAtMillis" to metric.recordedAtMillis,
                        "source" to metric.source.name
                    )
                ).await()
        } catch (e: Exception) {
            // Best-effort: Room already has the local copy, so a failed cloud push
            // doesn't affect what the user sees right now. A periodic retry/resync
            // job is a natural next step, left as a TODO.
        }
    }

    companion object {
        private const val DEFAULT_SEDENTARY_MINUTES = 120
        private const val SENSOR_READ_TIMEOUT_MS = 3000L
    }
}
