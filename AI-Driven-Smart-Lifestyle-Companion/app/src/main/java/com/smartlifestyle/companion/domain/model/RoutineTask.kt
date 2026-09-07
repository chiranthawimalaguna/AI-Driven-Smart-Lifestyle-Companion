package com.smartlifestyle.companion.domain.model

data class RoutineTask(
    val id: String,
    val title: String,
    val scheduledTimeMillis: Long,
    val isCompleted: Boolean,
    val priorityScore: Float
)

/** Context signals the recommendation engine scores tasks against. */
data class UserContext(
    val currentTimeMillis: Long,
    val stepsToday: Int,
    val isRaining: Boolean,
    val minutesSinceLastMovement: Int,
    val ambientLux: Float? = null
)
