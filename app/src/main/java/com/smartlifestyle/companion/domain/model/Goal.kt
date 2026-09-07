package com.smartlifestyle.companion.domain.model

/** A one-off or daily target, e.g. "Drink 8 glasses of water" or "Walk 8000 steps".
 * Distinct from [RoutineTask]: goals track a numeric target/progress, not a
 * schedule. `targetValue`/`currentValue` are generic so the same model covers
 * steps, glasses of water, minutes of exercise, etc. */
data class Goal(
    val id: String,
    val title: String,
    val targetValue: Float,
    val currentValue: Float,
    val unit: String,
    val createdAtMillis: Long
) {
    val progressFraction: Float
        get() = if (targetValue <= 0f) 0f else (currentValue / targetValue).coerceIn(0f, 1f)
    val isComplete: Boolean get() = currentValue >= targetValue
}

/** A recurring daily habit the user checks off (e.g. "Meditate", "No phone after
 * 10pm"). `completedDates` stores "yyyy-MM-dd" strings for each day it was marked
 * done - simple, human-readable, and trivial to diff for streak calculation
 * without needing a separate calendar library. */
data class Habit(
    val id: String,
    val title: String,
    val completedDates: List<String>,
    val createdAtMillis: Long
) {
    /** Current streak: consecutive days up to and including today (or yesterday,
     * if today isn't marked yet but yesterday was - so the streak doesn't reset
     * to zero the moment the clock passes midnight before the user has checked in). */
    fun currentStreak(today: String, yesterday: String): Int {
        val marked = completedDates.toSet()
        var streak = 0
        var cursor = when {
            marked.contains(today) -> today
            marked.contains(yesterday) -> yesterday
            else -> return 0
        }
        val cal = java.util.Calendar.getInstance()
        val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        cal.time = fmt.parse(cursor) ?: return 0
        while (marked.contains(fmt.format(cal.time))) {
            streak++
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
        }
        return streak
    }
}
