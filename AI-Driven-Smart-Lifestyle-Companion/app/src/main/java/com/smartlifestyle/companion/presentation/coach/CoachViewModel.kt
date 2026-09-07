package com.smartlifestyle.companion.presentation.coach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlifestyle.companion.data.repository.AiCoachRepository
import com.smartlifestyle.companion.domain.model.ChatMessage
import com.smartlifestyle.companion.domain.model.LifestyleSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CoachUiState(
    val messages: List<ChatMessage> = emptyList(),
    val advice: List<String> = emptyList(),
    val dailyPlan: List<String> = emptyList(),
    val isSending: Boolean = false
)

/**
 * [snapshotProvider] is a function rather than a fixed value because the coach
 * needs the *current* dashboard state at the moment a message is sent or advice
 * is requested, not a stale snapshot captured when this ViewModel was created.
 */
class CoachViewModel(
    private val aiCoachRepository: AiCoachRepository,
    private val snapshotProvider: () -> LifestyleSnapshot
) : ViewModel() {

    private val _uiState = MutableStateFlow(CoachUiState())
    val uiState: StateFlow<CoachUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            aiCoachRepository.observeMessages().collect { messages ->
                _uiState.value = _uiState.value.copy(messages = messages)
            }
        }
        refreshAdviceAndPlan()
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        _uiState.value = _uiState.value.copy(isSending = true)
        viewModelScope.launch {
            aiCoachRepository.sendMessage(text, snapshotProvider())
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    fun refreshAdviceAndPlan() {
        val snapshot = snapshotProvider()
        _uiState.value = _uiState.value.copy(
            advice = aiCoachRepository.personalizedAdvice(snapshot),
            dailyPlan = aiCoachRepository.dailyPlan(snapshot)
        )
    }
}
