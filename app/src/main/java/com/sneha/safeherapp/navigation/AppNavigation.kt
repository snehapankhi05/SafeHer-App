package com.sneha.safeherapp.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sneha.safeherapp.ui.auth.LoginScreen
import com.sneha.safeherapp.ui.auth.SignupScreen
import com.sneha.safeherapp.ui.chatbot.ChatbotScreen
import com.sneha.safeherapp.ui.contacts.EmergencyContactsScreen
import com.sneha.safeherapp.ui.fakecall.FakeCallScreen
import com.sneha.safeherapp.ui.fakecall.VoiceRecorderScreen
import com.sneha.safeherapp.ui.home.GuardianDashboard
import com.sneha.safeherapp.ui.home.UserHomeScreen
import com.sneha.safeherapp.ui.map.MapLandingScreen
import com.sneha.safeherapp.ui.map.MapScreen
import com.sneha.safeherapp.ui.map.ReportsScreen
import com.sneha.safeherapp.ui.settings.*
import com.sneha.safeherapp.util.FakeCallPrefs
import com.sneha.safeherapp.viewmodel.AuthState
import com.sneha.safeherapp.viewmodel.AuthViewModel

@Composable
fun AppNavigation(context: Context) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState

    LaunchedEffect(authState) {
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        
        when (authState) {
            is AuthState.Success -> {
                if (currentRoute == Screen.Login.route || currentRoute == Screen.Signup.route || currentRoute == null) {
                    val role = (authState as AuthState.Success).role
                    val targetRoute = if (role.equals("Guardian", ignoreCase = true)) {
                        Screen.GuardianDashboard.route
                    } else {
                        Screen.UserHome.route
                    }
                    
                    navController.navigate(targetRoute) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            is AuthState.Unauthenticated -> {
                if (currentRoute != Screen.Login.route && currentRoute != Screen.Signup.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    if (authState is AuthState.Idle || authState is AuthState.Loading && authState !is AuthState.Success) {
        if (authState !is AuthState.Success) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                onLoginClick = { email, password ->
                    authViewModel.login(email, password)
                },
                isLoading = authState is AuthState.Loading
            )
        }
        composable(Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignupClick = { name, email, password, role ->
                    authViewModel.signUp(name, email, password, role)
                },
                isLoading = authState is AuthState.Loading
            )
        }
        composable(Screen.UserHome.route) {
            UserHomeScreen(
                onLogout = { authViewModel.logout() },
                onNavigateToFakeCall = { navController.navigate(Screen.FakeCall.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToMap = { navController.navigate(Screen.MapLanding.route) },
                onNavigateToEmergencyContacts = { navController.navigate(Screen.EmergencyContacts.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToChatbot = { navController.navigate(Screen.Chatbot.route) }
            )
        }
        composable(Screen.GuardianDashboard.route) {
            GuardianDashboard()
        }
        composable(Screen.FakeCall.route) {
            FakeCallScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                navController = navController,
                onLogout = { authViewModel.logout() }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.HelpCenter.route) {
            HelpCenterScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.About.route) {
            AboutAppScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.PrivacyPolicy.route) {
            PrivacyPolicyScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Chatbot.route) {
            ChatbotScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.EmergencyContacts.route) {
            EmergencyContactsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.MapLanding.route) {
            MapLandingScreen(
                onOpenMap = {
                    navController.navigate(Screen.Map.createRoute(null))
                },
                onViewReports = {
                    navController.navigate(Screen.Reports.route)
                }
            )
        }
        composable(
            route = Screen.Map.route,
            arguments = listOf(
                navArgument("category") { type = NavType.StringType },
                navArgument("lat") { type = NavType.StringType; nullable = true },
                navArgument("lng") { type = NavType.StringType; nullable = true },
                navArgument("reason") { type = NavType.StringType; nullable = true },
                navArgument("level") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")?.let { 
                if (it == "none") null else it 
            }
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull()
            val lng = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull()
            val reason = backStackEntry.arguments?.getString("reason")
            val level = backStackEntry.arguments?.getString("level")
            
            MapScreen(
                category = category,
                initialLat = lat,
                initialLng = lng,
                initialReason = reason,
                initialLevel = level,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reports.route) {
            ReportsScreen(
                onBack = { navController.popBackStack() },
                onReportClick = { report ->
                    navController.navigate(
                        Screen.Map.createRoute(
                            category = null,
                            lat = report.latitude,
                            lng = report.longitude,
                            reason = report.reason,
                            level = report.alertLevel
                        )
                    )
                }
            )
        }
        composable(Screen.FakeCallSettings.route) {
            FakeCallSettingsScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVoiceRecorder = { profileId -> 
                    navController.navigate(Screen.VoiceRecorder.createRoute(profileId)) 
                }
            )
        }
        composable(
            route = "voice_recorder/{profileId}",
            arguments = listOf(navArgument("profileId") { type = NavType.StringType })
        ) { backStackEntry ->
            val profileId = backStackEntry.arguments?.getString("profileId") ?: ""
            VoiceRecorderScreen(
                profileId = profileId,
                onBack = { navController.popBackStack() },
                onSaved = { path ->
                    val profiles = FakeCallPrefs.getProfiles(context)
                    val updated = profiles.map { 
                        if (it.id == profileId) it.copy(audioPath = path) else it 
                    }
                    FakeCallPrefs.saveProfiles(context, updated)
                    navController.popBackStack()
                }
            )
        }
    }
}
