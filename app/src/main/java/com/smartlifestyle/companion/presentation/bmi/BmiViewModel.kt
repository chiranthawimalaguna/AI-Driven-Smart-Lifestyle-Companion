package com.smartlifestyle.companion.presentation.bmi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlifestyle.companion.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class BmiCategory(val label: String) {
    UNDERWEIGHT("Underweight"),
    NORMAL("Normal weight"),
    OVERWEIGHT("Overweight"),
    OBESE("Obesity")
}

data class BmiUiState(
    val weightKg: String = "",
    val heightCm: String = "",
    val result: Float? = null,
    val category: BmiCategory? = null,
    val saved: Boolean = false
)

/**
 * Standard WHO BMI bands. Deliberately descriptive only - a category name and the
 * number, nothing prescriptive about diet or exercise, plus a clear disclaimer in
 * the UI. BMI is a simple screening calculation, not a diagnosis; this screen
 * doesn't try to be more than that.
 */
class BmiViewModel(private val healthRepository: HealthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BmiUiState())
    val uiState: StateFlow<BmiUiState> = _uiState.asStateFlow()

    fun onWeightChange(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _uiState.value = _uiState.value.copy(weightKg = value, saved = false)
        }
    }

    fun onHeightChange(value: String) {
        if (value.all { it.isDigit() || it == '.' }) {
            _uiState.value = _uiState.value.copy(heightCm = value, saved = false)
        }
    }

    fun calculate() {
        val weight = _uiState.value.weightKg.toFloatOrNull() ?: return
        val heightCm = _uiState.value.heightCm.toFloatOrNull() ?: return
        if (weight <= 0f || heightCm <= 0f) return

        val heightM = heightCm / 100f
        val bmi = weight / (heightM * heightM)
        val category = when {
            bmi < 18.5f -> BmiCategory.UNDERWEIGHT
            bmi < 25f -> BmiCategory.NORMAL
            bmi < 30f -> BmiCategory.OVERWEIGHT
            else -> BmiCategory.OBESE
        }

        _uiState.value = _uiState.value.copy(result = bmi, category = category, saved = false)

        viewModelScope.launch {
            healthRepository.recordBmi(bmi)
            _uiState.value = _uiState.value.copy(saved = true)
        }
    }
}
