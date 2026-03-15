package com.sneha.safeherapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object UserHome : Screen("user_home")
    object GuardianDashboard : Screen("guardian_dashboard")
    object FakeCall : Screen("fake_call")
}
