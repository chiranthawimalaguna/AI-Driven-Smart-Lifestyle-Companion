package com.smartlifestyle.companion.domain.usecase

data class LifeScoreInput(
    val stepsToday: Int,
    val stepGoal: Int,
    val waterGlassesToday: Int,
    val waterGoal: Int,
    val tasksTotal: Int,
    val tasksCompleted: Int,
    val sleepMinutesLastNight: Int?,
    val sleepGoalMinutes: Int
)

data class LifeScoreResult(
    val score: Int,
    val label: String,
    val breakdown: Map<String, Int>
)

/**
 * Combines today's activity, hydration, task completion, and sleep into a single
 * 0-100 "Life Score" for the dashboard. Deliberately a transparent weighted
 * average (each component capped at its own share of 100) rather than a trained
 * model - same explainable-by-design approach as GetSmartSuggestionsUseCase - so
 * a user can see exactly why the number is what it is via [breakdown].
 */
class ComputeLifeScoreUseCase {

    operator fun invoke(input: LifeScoreInput): LifeScoreResult {
        val stepsComponent = component(input.stepsToday.toFloat(), input.stepGoal.toFloat(), STEPS_WEIGHT)
        val waterComponent = component(input.waterGlassesToday.toFloat(), input.waterGoal.toFloat(), WATER_WEIGHT)
        val tasksComponent = if (input.tasksTotal == 0) {
            TASKS_WEIGHT // no tasks scheduled today shouldn't punish the score
        } else {
            component(input.tasksCompleted.toFloat(), input.tasksTotal.toFloat(), TASKS_WEIGHT)
        }
        val sleepComponent = if (input.sleepMinutesLastNight == null) {
            SLEEP_WEIGHT / 2 // unknown - neutral half-credit rather than zero
        } else {
            component(input.sleepMinutesLastNight.toFloat(), input.sleepGoalMinutes.toFloat(), SLEEP_WEIGHT)
        }

        val total = stepsComponent + waterComponent + tasksComponent + sleepComponent
        val label = when {
            total >= 85 -> "Thriving"
            total >= 65 -> "On track"
            total >= 40 -> "Getting there"
            else -> "Needs attention"
        }

        return LifeScoreResult(
            score = total,
            label = label,
            breakdown = mapOf(
                "Steps" to stepsComponent,
                "Water" to waterComponent,
                "Tasks" to tasksComponent,
                "Sleep" to sleepComponent
            )
        )
    }

    private fun component(actual: Float, goal: Float, weight: Int): Int {
        if (goal <= 0f) return weight
        val fraction = (actual / goal).coerceIn(0f, 1f)
        return (fraction * weight).toInt()
    }

    companion object {
        private const val STEPS_WEIGHT = 30
        private const val WATER_WEIGHT = 20
        private const val TASKS_WEIGHT = 30
        private const val SLEEP_WEIGHT = 20
    }
}
