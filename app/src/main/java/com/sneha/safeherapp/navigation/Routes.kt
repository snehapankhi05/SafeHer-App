package com.sneha.safeherapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object UserHome : Screen("user_home")
    object GuardianDashboard : Screen("guardian_dashboard")
    object FakeCall : Screen("fake_call")
    object Settings : Screen("settings")
    object FakeCallSettings : Screen("fake_call_settings")
    object Profile : Screen("profile")
    object HelpCenter : Screen("help_center")
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy_policy")
    object Chatbot : Screen("chatbot")
    object VoiceRecorder : Screen("voice_recorder") {
        fun createRoute(profileId: String) = "voice_recorder/$profileId"
    }
    object Map : Screen("map/{category}") {
        fun createRoute(category: String?) = if (category != null) "map/$category" else "map/none"
    }
    object MapLanding : Screen("map_landing")
    object EmergencyContacts : Screen("emergency_contacts")
}
