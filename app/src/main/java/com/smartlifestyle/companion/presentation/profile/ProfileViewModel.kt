package com.smartlifestyle.companion.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartlifestyle.companion.data.local.dao.NotificationDao
import com.smartlifestyle.companion.data.local.entity.NotificationEntity
import com.smartlifestyle.companion.data.remote.AuthRepository
import com.smartlifestyle.companion.data.repository.ProfileRepository
import com.smartlifestyle.companion.data.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val isLoading: Boolean = true,
    val justSaved: Boolean = false,
    val saveError: Boolean = false,
    val notifications: List<NotificationEntity> = emptyList()
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val notificationDao: NotificationDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        authListener = authRepository.observeAuthState { user ->
            if (user != null) loadProfile() else _uiState.value = ProfileUiState(isLoading = false)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profile = profileRepository.getProfile()
            _uiState.value = _uiState.value.copy(profile = profile, isLoading = false)
        }
    }

    fun save(profile: UserProfile) {
        _uiState.value = _uiState.value.copy(justSaved = false, saveError = false)
        viewModelScope.launch {
            val success = profileRepository.saveProfile(profile)
            _uiState.value = if (success)
                _uiState.value.copy(profile = profile, justSaved = true, saveError = false)
            else
                _uiState.value.copy(justSaved = false, saveError = true)
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(notifications = notificationDao.getLatest(20))
        }
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { authRepository.removeAuthStateListener(it) }
    }
}