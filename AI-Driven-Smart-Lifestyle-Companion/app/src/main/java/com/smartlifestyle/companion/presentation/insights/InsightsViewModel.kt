package com.smartlifestyle.companion.presentation.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlifestyle.companion.data.local.entity.HealthMetricEntity
import com.smartlifestyle.companion.data.repository.HealthRepository
import com.smartlifestyle.companion.domain.usecase.GenerateInsightsUseCase
import com.smartlifestyle.companion.domain.usecase.MetricInsight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class InsightsUiState(
    val steps: List<HealthMetricEntity> = emptyList(),
    val heartRate: List<HealthMetricEntity> = emptyList(),
    val sleep: List<HealthMetricEntity> = emptyList(),
    val water: List<HealthMetricEntity> = emptyList(),
    val mood: List<HealthMetricEntity> = emptyList(),
    val stepsInsight: MetricInsight? = null,
    val heartRateInsight: MetricInsight? = null,
    val sleepInsight: MetricInsight? = null,
    val waterInsight: MetricInsight? = null,
    val moodInsight: MetricInsight? = null,
    val isLoading: Boolean = true
) {
    /** Plain-language weekly summary built from the same MetricInsight objects
     * already computed for each metric card - not a separate model call, just a
     * concatenation of what's already been derived, consistent with the app's
     * transparent-by-design approach elsewhere. */
    val weeklyAiReport: String
        get() {
            val parts = listOfNotNull(
                stepsInsight?.takeIf { it.message.isNotBlank() }?.message,
                heartRateInsight?.takeIf { it.message.isNotBlank() }?.message,
                sleepInsight?.takeIf { it.message.isNotBlank() }?.message,
                moodInsight?.takeIf { it.message.isNotBlank() }?.message
            )
            return if (parts.isEmpty()) "Keep logging your activity, sleep, and mood - your weekly report fills in as data comes in."
            else parts.joinToString(" ")
        }
}

class InsightsViewModel(
    private val healthRepository: HealthRepository,
    private val generateInsights: GenerateInsightsUseCase = GenerateInsightsUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                healthRepository.observeMetricHistory("steps"),
                healthRepository.observeMetricHistory("heart_rate"),
                healthRepository.observeMetricHistory("sleep_minutes"),
                healthRepository.observeMetricHistory("water_glass"),
                healthRepository.observeMetricHistory("mood")
            ) { steps, heartRate, sleep, water, mood ->
                InsightsUiState(
                    steps = steps,
                    heartRate = heartRate,
                    sleep = sleep,
                    water = water,
                    mood = mood,
                    stepsInsight = generateInsights("steps", steps),
                    heartRateInsight = generateInsights("heart_rate", heartRate),
                    sleepInsight = generateInsights("sleep_minutes", sleep),
                    waterInsight = generateInsights("water_glass", water),
                    moodInsight = generateInsights("mood", mood),
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun logMood(score: Int) {
        viewModelScope.launch { healthRepository.logMood(score) }
    }
}
