package com.smartlifestyle.companion.domain.model

enum class ChatSender { USER, COACH }

data class ChatMessage(
    val id: Long = 0,
    val sender: ChatSender,
    val text: String,
    val timestampMillis: Long
)

/** Snapshot of the user's current state, handed to the AI Coach so its replies and
 * daily plan are grounded in real data instead of generic advice. */
data class LifestyleSnapshot(
    val stepsToday: Int,
    val waterGlassesToday: Int,
    val heartRateBpm: Float?,
    val sleepMinutesLastNight: Int?,
    val temperatureCelsius: Float?,
    val isRaining: Boolean,
    val tasksRemaining: Int,
    val tasksOverdue: Int,
    val averageMoodLast7Days: Float?
)
