package com.smartlifestyle.companion.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlifestyle.companion.data.local.entity.MetricSource
import com.smartlifestyle.companion.data.repository.HealthRepository
import com.smartlifestyle.companion.data.repository.RoutineRepository
import com.smartlifestyle.companion.data.repository.WeatherRepository
import com.smartlifestyle.companion.domain.model.LifestyleSnapshot
import com.smartlifestyle.companion.domain.model.RoutineTask
import com.smartlifestyle.companion.domain.model.UserContext
import com.smartlifestyle.companion.domain.usecase.ComputeLifeScoreUseCase
import com.smartlifestyle.companion.domain.usecase.GetSmartSuggestionsUseCase
import com.smartlifestyle.companion.domain.usecase.LifeScoreInput
import com.smartlifestyle.companion.domain.usecase.LifeScoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val tasks: List<RoutineTask> = emptyList(),
    val stepsToday: Int = 0,
    val stepsSource: MetricSource? = null,
    val temperatureCelsius: Float? = null,
    val isRaining: Boolean = false,
    val ambientLux: Float? = null,
    val heartRateBpm: Float? = null,
    val heartRateSource: MetricSource? = null,
    val waterGlassesToday: Int = 0,
    val sleepMinutesLastNight: Int? = null,
    val averageMoodLast7Days: Float? = null,
    val lifeScore: LifeScoreResult? = null,
    val topRecommendation: String? = null,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val routineRepository: RoutineRepository,
    private val healthRepository: HealthRepository,
    private val weatherRepository: WeatherRepository,
    private val getSmartSuggestions: GetSmartSuggestionsUseCase,
    private val hasLocationPermission: () -> Boolean,
    private val computeLifeScore: ComputeLifeScoreUseCase = ComputeLifeScoreUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            routineRepository.observeTasks().collect { tasks ->
                refresh(tasks)
            }
        }
    }

    private suspend fun refresh(tasks: List<RoutineTask>) {
        val stepsReading = healthRepository.refreshSteps()
        val weather = weatherRepository.getCurrentWeather(hasLocationPermission())
        val ambientLux = healthRepository.refreshAmbientLight()
        val minutesSinceMovement = healthRepository.minutesSinceLastMovement()
        val heartRate = healthRepository.refreshHeartRate()
        val waterGlasses = healthRepository.todayWaterGlassCount()
        val sleepMinutes = healthRepository.refreshSleep()
        val averageMood = healthRepository.averageMoodLast7Days()

        val context = UserContext(
            currentTimeMillis = System.currentTimeMillis(),
            stepsToday = stepsReading.value.toInt(),
            isRaining = weather.isRaining,
            minutesSinceLastMovement = minutesSinceMovement,
            ambientLux = ambientLux
        )

        val ranked = getSmartSuggestions(tasks, context)

        val lifeScore = computeLifeScore(
            LifeScoreInput(
                stepsToday = stepsReading.value.toInt(),
                stepGoal = DEFAULT_STEP_GOAL,
                waterGlassesToday = waterGlasses,
                waterGoal = DEFAULT_WATER_GOAL,
                tasksTotal = ranked.size,
                tasksCompleted = ranked.count { it.isCompleted },
                sleepMinutesLastNight = sleepMinutes,
                sleepGoalMinutes = DEFAULT_SLEEP_GOAL_MINUTES
            )
        )

        val topRecommendation = ranked.firstOrNull { !it.isCompleted }?.let { "Next up: ${it.title}" }

        _uiState.value = DashboardUiState(
            tasks = ranked,
            stepsToday = stepsReading.value.toInt(),
            stepsSource = stepsReading.source,
            temperatureCelsius = weather.temperatureCelsius,
            isRaining = weather.isRaining,
            ambientLux = ambientLux,
            heartRateBpm = heartRate.value,
            heartRateSource = heartRate.source,
            waterGlassesToday = waterGlasses,
            sleepMinutesLastNight = sleepMinutes,
            averageMoodLast7Days = averageMood,
            lifeScore = lifeScore,
            topRecommendation = topRecommendation,
            isLoading = false
        )
    }

    /** Snapshot handed to the AI Coach so its answers are grounded in the same
     * data the dashboard is showing right now. */
    fun currentSnapshot(): LifestyleSnapshot {
        val s = _uiState.value
        return LifestyleSnapshot(
            stepsToday = s.stepsToday,
            waterGlassesToday = s.waterGlassesToday,
            heartRateBpm = s.heartRateBpm,
            sleepMinutesLastNight = s.sleepMinutesLastNight,
            temperatureCelsius = s.temperatureCelsius,
            isRaining = s.isRaining,
            tasksRemaining = s.tasks.count { !it.isCompleted },
            tasksOverdue = s.tasks.count { !it.isCompleted && it.scheduledTimeMillis < System.currentTimeMillis() },
            averageMoodLast7Days = s.averageMoodLast7Days
        )
    }

    fun addTask(title: String, scheduledTimeMillis: Long) {
        viewModelScope.launch { routineRepository.addTask(title, scheduledTimeMillis) }
    }

    fun editTask(task: RoutineTask, newTitle: String, newScheduledTimeMillis: Long) {
        viewModelScope.launch { routineRepository.editTask(task, newTitle, newScheduledTimeMillis) }
    }

    fun deleteTask(task: RoutineTask) {
        viewModelScope.launch { routineRepository.deleteTask(task) }
    }

    fun toggleTaskCompletion(task: RoutineTask) {
        viewModelScope.launch { routineRepository.setCompleted(task, !task.isCompleted) }
    }

    /** Manual-entry fallback when no sensor (watch or phone) has reported steps. */
    fun recordManualSteps(steps: Int) {
        viewModelScope.launch { healthRepository.recordManualSteps(steps.toFloat()) }
    }

    /** Optimistically bumps the local count immediately so the UI feels instant,
     * rather than waiting for the next full refresh cycle (which is only triggered
     * by task changes, not by water logging). */
    fun logWaterGlass() {
        _uiState.value = _uiState.value.copy(waterGlassesToday = _uiState.value.waterGlassesToday + 1)
        viewModelScope.launch { healthRepository.logWaterGlass() }
    }

    fun logMood(score: Int) {
        viewModelScope.launch { healthRepository.logMood(score) }
    }

    companion object {
        // Hardcoded defaults for now. Profile > Preferences lets the user *view*
        // step/water targets, but feeding those live into this Life Score
        // calculation is a documented next step, not done in this pass - see the
        // setup guide's "known limitations" section.
        private const val DEFAULT_STEP_GOAL = 8000
        private const val DEFAULT_WATER_GOAL = 8
        private const val DEFAULT_SLEEP_GOAL_MINUTES = 480
    }
}
