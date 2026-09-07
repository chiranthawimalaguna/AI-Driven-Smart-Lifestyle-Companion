package com.smartlifestyle.companion.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.smartlifestyle.companion.presentation.auth.AuthViewModel
import com.smartlifestyle.companion.presentation.auth.LoginScreen
import com.smartlifestyle.companion.presentation.bmi.BmiScreen
import com.smartlifestyle.companion.presentation.bmi.BmiViewModel
import com.smartlifestyle.companion.presentation.coach.CoachScreen
import com.smartlifestyle.companion.presentation.coach.CoachViewModel
import com.smartlifestyle.companion.presentation.dashboard.DashboardScreen
import com.smartlifestyle.companion.presentation.dashboard.DashboardViewModel
import com.smartlifestyle.companion.presentation.goals.GoalsScreen
import com.smartlifestyle.companion.presentation.goals.GoalsViewModel
import com.smartlifestyle.companion.presentation.insights.InsightsScreen
import com.smartlifestyle.companion.presentation.insights.InsightsViewModel
import com.smartlifestyle.companion.presentation.profile.ProfileScreen
import com.smartlifestyle.companion.presentation.profile.ProfileViewModel

/** Five bottom-nav destinations (Today / AI Coach / Goals / Insights / Profile),
 * matching the assignment's required navigation structure. BMI is intentionally
 * NOT a bottom tab - it's a secondary screen reachable from Profile > Personal
 * Information, since it's a one-off calculator rather than something checked daily. */
object Routes {
    const val LOGIN = "login"
    const val TODAY = "today"
    const val COACH = "coach"
    const val GOALS = "goals"
    const val INSIGHTS = "insights"
    const val PROFILE = "profile"
    const val BMI = "bmi"
}

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.TODAY, "Today", Icons.Filled.CheckCircle),
    BottomTab(Routes.COACH, "AI Coach", Icons.Filled.SmartToy),
    BottomTab(Routes.GOALS, "Goals", Icons.Filled.Flag),
    BottomTab(Routes.INSIGHTS, "Insights", Icons.Filled.QueryStats),
    BottomTab(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

@Composable
fun AppNavHost(
    authViewModel: AuthViewModel,
    dashboardViewModel: DashboardViewModel,
    coachViewModel: CoachViewModel,
    goalsViewModel: GoalsViewModel,
    insightsViewModel: InsightsViewModel,
    profileViewModel: ProfileViewModel,
    bmiViewModel: BmiViewModel,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val authState by authViewModel.uiState.collectAsState()
    val startDestination = if (authState.isSignedIn) Routes.TODAY else Routes.LOGIN
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomTabs.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(bottom = if (showBottomBar) padding.calculateBottomPadding() else 0.dp)
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = authViewModel,
                    onSignedIn = {
                        navController.navigate(Routes.TODAY) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.TODAY) {
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(Routes.COACH) {
                CoachScreen(viewModel = coachViewModel)
            }
            composable(Routes.GOALS) {
                GoalsScreen(viewModel = goalsViewModel)
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen(viewModel = insightsViewModel)
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    email = authState.userEmail,
                    viewModel = profileViewModel,
                    onNavigateToBmi = { navController.navigate(Routes.BMI) },
                    onSignOut = {
                        onSignOut()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.BMI) {
                BmiScreen(viewModel = bmiViewModel, onBack = { navController.popBackStack() })
            }
        }
    }
}
