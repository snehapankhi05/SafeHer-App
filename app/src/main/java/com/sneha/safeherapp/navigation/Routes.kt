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
    object Map : Screen("map/{category}?lat={lat}&lng={lng}&reason={reason}&level={level}") {
        fun createRoute(category: String?, lat: Double? = null, lng: Double? = null, reason: String? = null, level: String? = null): String {
            val cat = category ?: "none"
            var path = "map/$cat"
            val params = mutableListOf<String>()
            if (lat != null) params.add("lat=$lat")
            if (lng != null) params.add("lng=$lng")
            if (reason != null) params.add("reason=$reason")
            if (level != null) params.add("level=$level")
            
            if (params.isNotEmpty()) {
                path += "?" + params.joinToString("&")
            }
            return path
        }
    }
    object MapLanding : Screen("map_landing")
    object EmergencyContacts : Screen("emergency_contacts")
    object Reports : Screen("reports")
}
