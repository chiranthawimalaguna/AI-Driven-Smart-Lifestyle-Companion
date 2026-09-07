package com.smartlifestyle.companion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.smartlifestyle.companion.data.health.DeviceSensorManager
import com.smartlifestyle.companion.data.health.HealthConnectManager
import com.smartlifestyle.companion.data.remote.AuthRepository
import com.smartlifestyle.companion.data.repository.AiCoachRepository
import com.smartlifestyle.companion.data.repository.GoalsRepository
import com.smartlifestyle.companion.data.repository.HealthRepository
import com.smartlifestyle.companion.data.repository.ProfileRepository
import com.smartlifestyle.companion.data.repository.RoutineRepository
import com.smartlifestyle.companion.data.repository.WeatherRepository
import com.smartlifestyle.companion.domain.usecase.GetSmartSuggestionsUseCase
import com.smartlifestyle.companion.presentation.auth.AuthViewModel
import com.smartlifestyle.companion.presentation.bmi.BmiViewModel
import com.smartlifestyle.companion.presentation.coach.CoachViewModel
import com.smartlifestyle.companion.presentation.dashboard.DashboardViewModel
import com.smartlifestyle.companion.presentation.goals.GoalsViewModel
import com.smartlifestyle.companion.presentation.insights.InsightsViewModel
import com.smartlifestyle.companion.presentation.navigation.AppNavHost
import com.smartlifestyle.companion.presentation.profile.ProfileViewModel
import com.smartlifestyle.companion.presentation.theme.SmartLifestyleCompanionTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Batches the runtime permission requests this app needs: location (for weather
    // context) and notifications (Android 13+). The step counter sensor needs no
    // runtime permission on modern Android.
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* results observed via hasLocationPermission() below on next refresh */ }

    // Health Connect uses its own permission contract, separate from normal Android
    // runtime permissions - this is what shows the "Allow the app to access
    // Health Connect" system screen for steps/heart rate/sleep.
    private lateinit var healthConnectPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissions()

        val app = application as SmartLifestyleCompanionApp
        val authRepository = AuthRepository()
        val firestore = FirebaseFirestore.getInstance()
        val userIdProvider = { authRepository.currentUser?.uid }

        val sensorManager = DeviceSensorManager(this)
        val healthConnectManager = HealthConnectManager(this)
        val healthRepository = HealthRepository(
            sensorManager, healthConnectManager, app.database.healthDao(), firestore, userIdProvider
        )
        val routineRepository = RoutineRepository(firestore, authRepository)
        val weatherRepository = WeatherRepository(applicationContext)
        val goalsRepository = GoalsRepository(firestore, authRepository)
        val profileRepository = ProfileRepository(firestore, authRepository)
        val aiCoachRepository = AiCoachRepository(app.database.chatDao())

        requestHealthConnectPermissionsIfNeeded(healthConnectManager)

        val dashboardFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                DashboardViewModel(
                    routineRepository,
                    healthRepository,
                    weatherRepository,
                    GetSmartSuggestionsUseCase(),
                    hasLocationPermission = { hasLocationPermission() }
                ) as T
        }
        val authFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(authRepository) as T
        }
        val bmiFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                BmiViewModel(healthRepository) as T
        }
        val goalsFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                GoalsViewModel(goalsRepository) as T
        }
        val insightsFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                InsightsViewModel(healthRepository) as T
        }
        val profileFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ProfileViewModel(profileRepository, authRepository, app.database.notificationDao()) as T
        }

        setContent {
            SmartLifestyleCompanionTheme {
                val dashboardViewModel: DashboardViewModel = viewModel(factory = dashboardFactory)
                val authViewModel: AuthViewModel = viewModel(factory = authFactory)
                val bmiViewModel: BmiViewModel = viewModel(factory = bmiFactory)
                val goalsViewModel: GoalsViewModel = viewModel(factory = goalsFactory)
                val insightsViewModel: InsightsViewModel = viewModel(factory = insightsFactory)
                val profileViewModel: ProfileViewModel = viewModel(factory = profileFactory)

                // Built with a closure factory (rather than its own top-level class)
                // because it needs dashboardViewModel's live snapshot, which only
                // exists once the dashboard ViewModel above has been resolved.
                val coachFactory = remember(dashboardViewModel) {
                    object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T =
                            CoachViewModel(aiCoachRepository, snapshotProvider = { dashboardViewModel.currentSnapshot() }) as T
                    }
                }
                val coachViewModel: CoachViewModel = viewModel(factory = coachFactory)

                AppNavHost(
                    authViewModel = authViewModel,
                    dashboardViewModel = dashboardViewModel,
                    coachViewModel = coachViewModel,
                    goalsViewModel = goalsViewModel,
                    insightsViewModel = insightsViewModel,
                    profileViewModel = profileViewModel,
                    bmiViewModel = bmiViewModel,
                    onSignOut = { authRepository.signOut() }
                )
            }
        }
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    /**
     * Asks for Health Connect access (steps/heart rate/sleep) once, if Health
     * Connect is installed and available and we don't already have permission.
     * If Health Connect isn't available on this device at all (older Android
     * without the Health Connect app installed, or an emulator without it), this
     * silently does nothing - HealthRepository already falls back to simulated
     * heart rate and the phone's own step sensor in that case, so there's nothing
     * to prompt for.
     */
    private fun requestHealthConnectPermissionsIfNeeded(healthConnectManager: HealthConnectManager) {
        healthConnectPermissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { /* granted set observed indirectly - next dashboard refresh just tries
               a real Health Connect read and falls back if still not granted */ }

        if (!healthConnectManager.isAvailable) return

        lifecycleScope.launch {
            if (!healthConnectManager.hasAllPermissions()) {
                healthConnectPermissionLauncher.launch(healthConnectManager.requiredPermissions)
            }
        }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
