package com.sneha.safeherapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object UserHome : Screen("user_home")
    object GuardianDashboard : Screen("guardian_dashboard")
    object FakeCall : Screen("fake_call")
    object Settings : Screen("settings")
    object FakeCallSettings : Screen("fake_call_settings")
    object VoiceRecorder : Screen("voice_recorder") {
        fun createRoute(profileId: String) = "voice_recorder/$profileId"
    }
}
