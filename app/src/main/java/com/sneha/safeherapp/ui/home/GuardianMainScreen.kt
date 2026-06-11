package com.sneha.safeherapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sneha.safeherapp.navigation.Screen
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuardianMainScreen(
    onLogout: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAddChild: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isTopLevel = currentRoute == Screen.GuardianHome.route || currentRoute == Screen.GuardianNotifications.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevel,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Guardian Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF6A3CC3),
                    fontWeight = FontWeight.Bold
                )
                
                NavigationDrawerItem(
                    label = { Text("My Children") },
                    selected = currentRoute == Screen.GuardianHome.route || currentRoute?.startsWith("child_detail") == true,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.GuardianHome.route) {
                                popUpTo(Screen.GuardianHome.route) { inclusive = true }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
                
                NavigationDrawerItem(
                    label = { Text("Notification History") },
                    selected = currentRoute == Screen.GuardianNotifications.route,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            navController.navigate(Screen.GuardianNotifications.route)
                        }
                    },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) }
                )
                
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToSettings()
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                )
                
                NavigationDrawerItem(
                    label = { Text("Profile") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onNavigateToProfile()
                        }
                    },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) }
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                NavigationDrawerItem(
                    label = { Text("Logout") },
                    selected = false,
                    onClick = {
                        scope.launch {
                            drawerState.close()
                            onLogout()
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "SafeHer",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6A3CC3)
                        )
                    },
                    navigationIcon = {
                        if (isTopLevel) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF6A3CC3))
                            }
                        } else {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF6A3CC3))
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color(0xFF6A3CC3))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        ) { innerPadding ->
            val gradient = Brush.verticalGradient(
                colors = listOf(SoftPink, LightPurple, Color.White)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.GuardianHome.route
                ) {
                    composable(Screen.GuardianHome.route) {
                        GuardianHomeScreen(
                            onChildClick = { child ->
                                navController.navigate(Screen.ChildDetail.createRoute(child.id))
                            },
                            onAddChildClick = {
                                onNavigateToAddChild()
                            }
                        )
                    }
                    composable(Screen.AddChild.route) {
                        AddChildScreen(onBack = { navController.popBackStack() })
                    }
                    composable(
                        route = Screen.ChildDetail.route,
                        arguments = listOf(navArgument("childId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getString("childId") ?: ""
                        ChildDetailScreen(
                            childId = childId,
                            onBack = { navController.popBackStack() },
                            onNavigateToAddPlace = { id ->
                                navController.navigate(Screen.AddPlace.createRoute(id))
                            }
                        )
                    }
                    composable(
                        route = Screen.AddPlace.route,
                        arguments = listOf(navArgument("childId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getString("childId") ?: ""
                        AddPlaceScreen(
                            childId = childId,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.GuardianNotifications.route) {
                        PlaceholderScreen("Notification History")
                    }
                }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "$title Screen",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Text(
                text = "Coming Soon",
                fontSize = 14.sp,
                color = Color.LightGray
            )
        }
    }
}
