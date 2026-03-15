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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sneha.safeherapp.ui.auth.LoginScreen
import com.sneha.safeherapp.ui.auth.SignupScreen
import com.sneha.safeherapp.ui.home.GuardianDashboard
import com.sneha.safeherapp.ui.home.UserHomeScreen
import com.sneha.safeherapp.viewmodel.AuthState
import com.sneha.safeherapp.viewmodel.AuthViewModel

@Composable
fun AppNavigation(context: Context) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                val role = (authState as AuthState.Success).role
                val targetRoute = if (role.equals("Guardian", ignoreCase = true)) {
                    Screen.GuardianDashboard.route
                } else {
                    Screen.UserHome.route
                }
                
                navController.navigate(targetRoute) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                    popUpTo(Screen.Signup.route) { inclusive = true }
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
            is AuthState.Unauthenticated -> {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {}
        }
    }

    if (authState is AuthState.Idle || authState is AuthState.Loading && authState !is AuthState.Success) {
        // Show a full-screen loader during initial check or background loading
        // But only if we are not already navigating (Success)
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
            UserHomeScreen(onLogout = { authViewModel.logout() })
        }
        composable(Screen.GuardianDashboard.route) {
            GuardianDashboard()
        }
    }
}
