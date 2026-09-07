package com.smartlifestyle.companion.domain.usecase

import com.smartlifestyle.companion.domain.model.LifestyleSnapshot

/**
 * On-device, rule-based "AI Coach" - same explainable-by-design philosophy as
 * GetSmartSuggestionsUseCase and GenerateInsightsUseCase elsewhere in this app.
 * It answers a handful of common question categories by keyword match against the
 * user's real current [LifestyleSnapshot], and generates personalised advice / a
 * daily plan from the same data. Every reply is traceable to a concrete rule -
 * there's no hidden model and nothing here should be described as a trained AI.
 *
 * This is intentionally swappable: AiCoachRepository tries a real hosted LLM first
 * (if AI_API_KEY is configured, see BuildConfig) and falls back to this engine if
 * no key is set or the network call fails - so the coach always answers something,
 * online or offline, key-configured or not.
 */
class AiCoachEngine {

    fun reply(userMessage: String, snapshot: LifestyleSnapshot): String {
        val text = userMessage.lowercase()
        return when {
            "water" in text || "hydrat" in text ->
                "You've had ${snapshot.waterGlassesToday} glasses of water today. " +
                    if (snapshot.waterGlassesToday < 6) "Try to get a couple more in over the next few hours."
                    else "That's a solid pace - keep it up."

            "sleep" in text || "tired" in text ->
                snapshot.sleepMinutesLastNight?.let { minutes ->
                    "You slept about ${minutes / 60}h ${minutes % 60}m last night. " +
                        if (minutes < 360) "That's on the low side - an earlier wind-down tonight could help."
                        else "That's a reasonable night's sleep."
                } ?: "I don't have last night's sleep data yet - it syncs automatically once your watch/Health Connect reports it."

            "step" in text || "walk" in text || "exercise" in text || "activity" in text ->
                "You're at ${snapshot.stepsToday} steps today." +
                    if (snapshot.isRaining) " It's raining right now, so an indoor stretch break might be more appealing than a walk."
                    else " A short walk now would be a good next move."

            "task" in text || "todo" in text || "routine" in text ->
                when {
                    snapshot.tasksOverdue > 0 -> "You have ${snapshot.tasksOverdue} overdue task(s) - want to tackle the top one first?"
                    snapshot.tasksRemaining > 0 -> "${snapshot.tasksRemaining} task(s) left today. Nothing's overdue yet, so you're in good shape."
                    else -> "You're all caught up on today's tasks."
                }

            "mood" in text || "feel" in text ->
                snapshot.averageMoodLast7Days?.let {
                    "Your average mood over the last week has been around ${"%.1f".format(it)}/5."
                } ?: "Log a mood entry on the Insights tab and I can start tracking trends for you."

            "weather" in text ->
                snapshot.temperatureCelsius?.let { temp ->
                    "It's about ${temp.toInt()}\u00b0C right now" + if (snapshot.isRaining) " and raining." else "."
                } ?: "I don't have current weather - check that location permission is granted."

            "hello" in text || "hi" in text || text.trim() == "hey" ->
                "Hi! I can help with your water, sleep, steps, tasks, mood, or the weather - what's on your mind?"

            else ->
                "I can help with questions about your water, sleep, steps, tasks, mood, or the weather today. Try asking about one of those."
        }
    }

    fun personalizedAdvice(snapshot: LifestyleSnapshot): List<String> {
        val advice = mutableListOf<String>()
        if (snapshot.waterGlassesToday < 4) advice.add("You're behind on water today - aim for a glass in the next hour.")
        if (snapshot.stepsToday < 3000) advice.add("Activity is low so far - even a 10-minute walk makes a difference.")
        if ((snapshot.sleepMinutesLastNight ?: 480) < 360) advice.add("Last night's sleep was short - consider winding down earlier tonight.")
        if (snapshot.tasksOverdue > 0) advice.add("You have overdue tasks piling up - clearing even one tends to reduce the rest.")
        if (snapshot.averageMoodLast7Days != null && snapshot.averageMoodLast7Days < 2.5f) {
            advice.add("Your mood trend has been lower this week - worth being a bit gentler with your goals today.")
        }
        if (advice.isEmpty()) advice.add("Everything's tracking well today - no specific nudges needed.")
        return advice
    }

    /** Produces an ordered daily plan: overdue/urgent items from real task data,
     * plus context-driven suggestions (hydration, movement, wind-down) slotted
     * into the parts of the day they're most relevant. */
    fun dailyPlan(snapshot: LifestyleSnapshot): List<String> {
        val plan = mutableListOf<String>()
        if (snapshot.tasksOverdue > 0) plan.add("Clear your ${snapshot.tasksOverdue} overdue task(s) first.")
        if (snapshot.stepsToday < 5000) plan.add("Fit in a walk - you're at ${snapshot.stepsToday} steps so far.")
        if (snapshot.waterGlassesToday < 6) plan.add("Drink ${6 - snapshot.waterGlassesToday} more glass(es) of water through the day.")
        if (snapshot.tasksRemaining > 0) plan.add("Work through your remaining ${snapshot.tasksRemaining} task(s) on the Today tab.")
        plan.add("Wind down screen-free for 20-30 minutes before bed tonight.")
        return plan
    }
}
