package com.sneha.safeherapp.navigation

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sneha.safeherapp.ui.auth.LoginScreen
import com.sneha.safeherapp.ui.auth.SignupScreen

@Composable
fun AppNavigation(context: Context) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                onLoginClick = { email, password ->
                    Toast.makeText(context, "Login clicked for $email", Toast.LENGTH_SHORT).show()
                }
            )
        }
        composable(Screen.Signup.route) {
            SignupScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onSignupClick = { name, email, password, role ->
                    Toast.makeText(context, "Signup clicked for $name as $role", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}
