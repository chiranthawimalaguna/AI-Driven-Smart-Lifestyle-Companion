package com.smartlifestyle.companion.domain.usecase

import com.smartlifestyle.companion.data.local.entity.HealthMetricEntity
import kotlin.math.abs

enum class Trend { UP, DOWN, STABLE, INSUFFICIENT_DATA }

data class MetricInsight(
    val average: Float,
    val min: Float,
    val max: Float,
    val trend: Trend,
    val percentChange: Float,
    val message: String
)

/**
 * Turns a raw list of stored health readings into descriptive statistics (average,
 * min, max) and a short natural-language insight based on comparing recent readings
 * against older ones.
 *
 * This is deliberately basic statistical trend detection - not a trained machine
 * learning model - consistent with the project's wider design choice (see
 * GetSmartSuggestionsUseCase) to favour transparent, explainable logic over a
 * black-box model with no training data to learn from yet. It's a natural
 * foundation for a future on-device ML enhancement once enough usage history
 * exists, rather than something dressed up to look like ML today.
 */
class GenerateInsightsUseCase {

    operator fun invoke(metricType: String, entries: List<HealthMetricEntity>): MetricInsight {
        if (entries.size < MIN_ENTRIES_FOR_TREND) {
            val avg = if (entries.isEmpty()) 0f else entries.map { it.value }.average().toFloat()
            return MetricInsight(
                average = avg,
                min = entries.minOfOrNull { it.value } ?: 0f,
                max = entries.maxOfOrNull { it.value } ?: 0f,
                trend = Trend.INSUFFICIENT_DATA,
                percentChange = 0f,
                message = "Not enough data yet to spot a trend - keep using the app and check back soon."
            )
        }

        // Entries are newest-first (see HealthDao.observeMetric's ORDER BY), so the
        // first half is the more recent readings and the second half is older ones.
        val mid = entries.size / 2
        val recent = entries.subList(0, mid)
        val older = entries.subList(mid, entries.size)

        val avgRecent = recent.map { it.value }.average().toFloat()
        val avgOlder = older.map { it.value }.average().toFloat()
        val avgAll = entries.map { it.value }.average().toFloat()
        val min = entries.minOf { it.value }
        val max = entries.maxOf { it.value }

        val percentChange = if (avgOlder != 0f) ((avgRecent - avgOlder) / avgOlder) * 100f else 0f
        val trend = when {
            percentChange > TREND_THRESHOLD_PERCENT -> Trend.UP
            percentChange < -TREND_THRESHOLD_PERCENT -> Trend.DOWN
            else -> Trend.STABLE
        }

        return MetricInsight(avgAll, min, max, trend, percentChange, buildMessage(metricType, trend, avgAll, percentChange))
    }

    private fun buildMessage(metricType: String, trend: Trend, average: Float, percentChange: Float): String {
        val pct = abs(percentChange).toInt()
        return when (metricType) {
            "steps" -> when (trend) {
                Trend.UP -> "You're moving more than before - up $pct%. Keep it going."
                Trend.DOWN -> "Activity has dropped $pct% recently - a short walk could help."
                Trend.STABLE -> "Your activity level has been fairly steady."
                Trend.INSUFFICIENT_DATA -> ""
            }
            "ambient_light" -> when (trend) {
                Trend.UP -> "Your surroundings have been brighter than usual recently."
                Trend.DOWN -> "Your evenings have been on the darker side lately - good for winding down."
                Trend.STABLE -> "Light levels around you have been fairly consistent."
                Trend.INSUFFICIENT_DATA -> ""
            }
            // Deliberately descriptive only, no health/medical framing - reports the
            // trend and number without implying "good," "bad," "elevated," etc.
            "heart_rate" -> when (trend) {
                Trend.UP -> "Heart rate has trended up $pct% across recent readings, averaging ${average.toInt()} bpm."
                Trend.DOWN -> "Heart rate has trended down $pct% across recent readings, averaging ${average.toInt()} bpm."
                Trend.STABLE -> "Heart rate has been steady, averaging ${average.toInt()} bpm."
                Trend.INSUFFICIENT_DATA -> ""
            }
            "sleep_minutes" -> {
                val hours = (average / 60).toInt()
                val mins = (average % 60).toInt()
                when (trend) {
                    Trend.UP -> "You've been sleeping more lately - averaging ${hours}h ${mins}m, up $pct%."
                    Trend.DOWN -> "Sleep has dropped $pct% recently - averaging ${hours}h ${mins}m."
                    Trend.STABLE -> "Sleep has been consistent, averaging ${hours}h ${mins}m."
                    Trend.INSUFFICIENT_DATA -> ""
                }
            }
            "water_glass" -> when (trend) {
                Trend.UP -> "Hydration is up $pct% recently - nice work."
                Trend.DOWN -> "Water intake has dropped $pct% recently."
                Trend.STABLE -> "Water intake has been fairly consistent."
                Trend.INSUFFICIENT_DATA -> ""
            }
            "mood" -> when (trend) {
                Trend.UP -> "Mood has trended up $pct% recently, averaging ${"%.1f".format(average)}/5."
                Trend.DOWN -> "Mood has trended down $pct% recently, averaging ${"%.1f".format(average)}/5."
                Trend.STABLE -> "Mood has been steady, averaging ${"%.1f".format(average)}/5."
                Trend.INSUFFICIENT_DATA -> ""
            }
            else -> "Average: ${average.toInt()}"
        }
    }

    companion object {
        private const val MIN_ENTRIES_FOR_TREND = 4
        private const val TREND_THRESHOLD_PERCENT = 10f
    }
}
