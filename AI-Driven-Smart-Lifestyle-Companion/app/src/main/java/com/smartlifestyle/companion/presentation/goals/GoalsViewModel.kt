package com.smartlifestyle.companion.presentation.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlifestyle.companion.data.repository.GoalsRepository
import com.smartlifestyle.companion.domain.model.Goal
import com.smartlifestyle.companion.domain.model.Habit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class HabitWithStreak(val habit: Habit, val streak: Int, val doneToday: Boolean)

data class GoalsUiState(
    val goals: List<Goal> = emptyList(),
    val habits: List<HabitWithStreak> = emptyList(),
    val isLoading: Boolean = true
)

class GoalsViewModel(private val goalsRepository: GoalsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())
    val uiState: StateFlow<GoalsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                goalsRepository.observeGoals(),
                goalsRepository.observeHabits()
            ) { goals, habits ->
                val today = goalsRepository.todayKey()
                val yesterday = goalsRepository.yesterdayKey()
                GoalsUiState(
                    goals = goals,
                    habits = habits.map { habit ->
                        HabitWithStreak(
                            habit = habit,
                            streak = habit.currentStreak(today, yesterday),
                            doneToday = habit.completedDates.contains(today)
                        )
                    },
                    isLoading = false
                )
            }.collect { _uiState.value = it }
        }
    }

    fun addGoal(title: String, target: Float, unit: String) {
        viewModelScope.launch { goalsRepository.addGoal(title, target, unit) }
    }

    fun updateGoalProgress(goal: Goal, newValue: Float) {
        viewModelScope.launch { goalsRepository.updateGoalProgress(goal, newValue) }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch { goalsRepository.deleteGoal(goal) }
    }

    fun addHabit(title: String) {
        viewModelScope.launch { goalsRepository.addHabit(title) }
    }

    fun toggleHabitToday(habit: Habit) {
        viewModelScope.launch { goalsRepository.toggleHabitToday(habit) }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch { goalsRepository.deleteHabit(habit) }
    }
}
