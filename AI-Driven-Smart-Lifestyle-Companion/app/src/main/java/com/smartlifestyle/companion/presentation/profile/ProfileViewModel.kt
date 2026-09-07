package com.smartlifestyle.companion.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartlifestyle.companion.data.repository.ProfileRepository
import com.smartlifestyle.companion.data.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isLoading: Boolean = true,
    val justSaved: Boolean = false
)

class ProfileViewModel(private val profileRepository: ProfileRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profileRepository.getProfile()
            _uiState.value = ProfileUiState(profile = profile, isLoading = false)
        }
    }

    fun save(profile: UserProfile) {
        _uiState.value = _uiState.value.copy(profile = profile, justSaved = false)
        viewModelScope.launch {
            profileRepository.saveProfile(profile)
            _uiState.value = _uiState.value.copy(justSaved = true)
        }
    }
}
