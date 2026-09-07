package com.smartlifestyle.companion.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartlifestyle.companion.data.remote.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val userEmail: String? = null,
    val errorMessage: String? = null
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Fires immediately with the current state, then again on every sign-in/sign-out -
    // including sign-outs triggered outside this ViewModel (e.g. from MainActivity).
    // Carrying the email here too (not just isSignedIn) is what fixes ProfileScreen
    // showing "Unknown" - the old wiring read a one-time snapshot from MainActivity
    // that never updated after the initial composition.
    private val authStateListener: FirebaseAuth.AuthStateListener =
        authRepository.observeAuthState { user ->
            _uiState.value = _uiState.value.copy(isSignedIn = user != null, userEmail = user?.email, isLoading = false)
        }

    fun signIn(email: String, password: String) {
        if (!validate(email, password)) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Sign-in failed")
                }
            // On success, authStateListener updates isSignedIn - no need to set it here too.
        }
    }

    fun signUp(email: String, password: String) {
        if (!validate(email, password)) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            authRepository.signUp(email, password)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message ?: "Sign-up failed")
                }
        }
    }

    private fun validate(email: String, password: String): Boolean {
        val error = when {
            email.isBlank() || !email.contains("@") -> "Enter a valid email address"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        _uiState.value = _uiState.value.copy(errorMessage = error)
        return error == null
    }

    override fun onCleared() {
        authRepository.removeAuthStateListener(authStateListener)
    }
}
