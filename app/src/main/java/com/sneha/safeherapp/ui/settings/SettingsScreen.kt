package com.sneha.safeherapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.sneha.safeherapp.navigation.Screen
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onLogout: () -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Settings", 
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.Black
                        )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    SettingsItem(
                        title = "Profile",
                        icon = Icons.Default.Person
                    ) {
                        navController.navigate(Screen.Profile.route)
                    }
                }
                item {
                    SettingsItem(
                        title = "Fake Call Settings",
                        icon = Icons.Default.Phone
                    ) {
                        navController.navigate(Screen.FakeCallSettings.route)
                    }
                }
                item {
                    SettingsItem(
                        title = "Help Center",
                        icon = Icons.AutoMirrored.Filled.Help
                    ) {
                        navController.navigate(Screen.HelpCenter.route)
                    }
                }
                item {
                    SettingsItem(
                        title = "About App",
                        icon = Icons.Default.Info
                    ) {
                        navController.navigate(Screen.About.route)
                    }
                }
                item {
                    SettingsItem(
                        title = "Privacy Policy",
                        icon = Icons.Default.PrivacyTip
                    ) {
                        navController.navigate(Screen.PrivacyPolicy.route)
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    SettingsItem(
                        title = "Logout",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        textColor = Color.Red
                    ) {
                        onLogout()
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String,
    icon: ImageVector,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (textColor == Color.Red) Color.Red else Color(0xFF6A3CC3),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.DarkGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
