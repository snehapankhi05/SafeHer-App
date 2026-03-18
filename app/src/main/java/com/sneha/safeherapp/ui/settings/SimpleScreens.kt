package com.sneha.safeherapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSettingsScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
fun HelpCenterScreen(onBack: () -> Unit) {
    SimpleSettingsScreen(title = "Help Center", onBack = onBack) {
        HelpSection(
            title = "How SOS Works",
            description = "Press the large red SOS button on the home screen to instantly send your location and an emergency message to your trusted contacts."
        )
        HelpSection(
            title = "How Fake Call Works",
            description = "Trigger a realistic incoming call to excuse yourself from uncomfortable situations. You can record custom caller voices in Settings."
        )
        HelpSection(
            title = "Using Emergency Contacts",
            description = "Add your family and friends to 'Trusted Contacts' for SOS alerts. Use 'Public Help Numbers' for immediate police, medical, or fire assistance."
        )
    }
}

@Composable
fun AboutAppScreen(onBack: () -> Unit) {
    SimpleSettingsScreen(title = "About App", onBack = onBack) {
        Text(
            text = "SafeHer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6A3CC3)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SafeHer is a comprehensive women's safety application designed to provide security and peace of mind. Our mission is to leverage technology to create a safer environment for everyone.",
            fontSize = 16.sp,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Version 1.0.0", color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    SimpleSettingsScreen(title = "Privacy Policy", onBack = onBack) {
        Text(
            text = "Your privacy is our priority.",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "1. Data Usage: SafeHer does not sell or misuse your personal data. All information is stored securely.\n\n" +
                   "2. Location Tracking: Your location is accessed only when you trigger an SOS alert or use the Safety Map features to ensure your safety.\n\n" +
                   "3. Contacts: We access your contacts only to allow you to select emergency responders.\n\n" +
                   "4. Audio: Microphone access is used exclusively for recording fake caller voices if you choose to use that feature.",
            fontSize = 16.sp,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    SimpleSettingsScreen(title = "Profile", onBack = onBack) {
        // Simple profile placeholder
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("User Profile Info", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Account settings and personal details will appear here.", color = Color.DarkGray)
            }
        }
    }
}

@Composable
fun HelpSection(title: String, description: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF6A3CC3))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = description, fontSize = 16.sp, lineHeight = 22.sp, color = Color.DarkGray)
    }
}
