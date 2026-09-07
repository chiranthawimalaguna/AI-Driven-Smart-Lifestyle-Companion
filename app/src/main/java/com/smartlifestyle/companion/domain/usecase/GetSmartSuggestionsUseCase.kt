package com.smartlifestyle.companion.domain.usecase

import com.smartlifestyle.companion.domain.model.RoutineTask
import com.smartlifestyle.companion.domain.model.UserContext
import java.util.Calendar

/**
 * Lightweight, explainable recommendation engine: re-scores and reorders the user's
 * routine tasks based on context (time of day, activity level, weather, overdue status).
 *
 * This is deliberately a weighted rule-based model rather than a black-box ML model -
 * it's fast, needs no training data, and every score is explainable in the report's
 * methodology section. A TensorFlow Lite model (e.g. predicting the best reminder time
 * from historical completion patterns) is a natural "future enhancement" to layer on
 * top of this once enough usage data has been collected.
 */
class GetSmartSuggestionsUseCase {

    operator fun invoke(tasks: List<RoutineTask>, context: UserContext): List<RoutineTask> {
        return tasks.map { task -> task.copy(priorityScore = score(task, context)) }
            .sortedByDescending { it.priorityScore }
    }

    private fun score(task: RoutineTask, context: UserContext): Float {
        if (task.isCompleted) return -1f

        var score = 0f

        // Overdue tasks get pushed to the top.
        val isOverdue = task.scheduledTimeMillis < context.currentTimeMillis
        if (isOverdue) score += 40f

        // Tasks scheduled within the next hour are urgent.
        val minutesUntilDue = (task.scheduledTimeMillis - context.currentTimeMillis) / 60000
        if (minutesUntilDue in 0..60) score += 25f

        // Sedentary nudge: if it's a movement-related task and the user hasn't moved
        // in a while, boost it - this is the "adaptive, not just scheduled" behaviour.
        val isMovementTask = task.title.contains("walk", ignoreCase = true) ||
            task.title.contains("exercise", ignoreCase = true)
        if (isMovementTask && context.minutesSinceLastMovement > 90) score += 20f

        // Weather-aware deprioritisation for outdoor tasks.
        val isOutdoorTask = task.title.contains("run", ignoreCase = true) ||
            task.title.contains("walk", ignoreCase = true)
        if (isOutdoorTask && context.isRaining) score -= 30f

        // Time-of-day fit (e.g. don't push "morning stretch" suggestions at night).
        val hour = Calendar.getInstance().apply { timeInMillis = context.currentTimeMillis }
            .get(Calendar.HOUR_OF_DAY)
        if (hour in 6..9 && task.title.contains("morning", ignoreCase = true)) score += 10f

        // Ambient-light-driven wind-down nudge: dark room + evening hours + a
        // relaxation/sleep task = boost it. This is the ambient light sensor's
        // contribution to the recommendation engine, alongside the step counter.
        val isWindDownTask = listOf("wind down", "sleep", "relax", "bed").any {
            task.title.contains(it, ignoreCase = true)
        }
        if (isWindDownTask && hour in 20..23 && context.ambientLux != null && context.ambientLux < 20f) {
            score += 20f
        }

        return score
    }
}
