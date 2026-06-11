package com.sneha.safeherapp.ui.home

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import com.sneha.safeherapp.viewmodel.ConnectGuardianViewModel
import com.sneha.safeherapp.viewmodel.ConnectState
import com.sneha.safeherapp.viewmodel.ConnectedGuardianUI

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectGuardianScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val viewModel: ConnectGuardianViewModel = viewModel()
    val connectState by viewModel.connectState.collectAsState()
    val guardians by viewModel.guardians.collectAsState()
    
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    
    var inviteCode by remember { mutableStateOf("") }
    var showAddForm by remember { mutableStateOf(false) }
    
    val primaryPurple = Color(0xFF6A3CC3)
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    LaunchedEffect(connectState) {
        if (connectState is ConnectState.Success) {
            inviteCode = ""
            showAddForm = false
            viewModel.resetState()
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Guardian Management",
                        fontWeight = FontWeight.Bold,
                        color = primaryPurple
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = primaryPurple)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Info
                Text(
                    text = "Manage Your Safety Circle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Your guardians can track your location and receive SOS alerts in real-time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                )

                // List of Guardians
                if (guardians.isNotEmpty()) {
                    guardians.forEach { guardian ->
                        GuardianCard(
                            guardian = guardian,
                            onRemove = { viewModel.removeGuardian(guardian.guardianUid) }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // Empty State for Guardians
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = primaryPurple.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No guardians connected", fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Always Visible "Add Guardian" Button or Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = primaryPurple.copy(alpha = 0.1f)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = primaryPurple, modifier = Modifier.padding(8.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Add New Guardian",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showAddForm = !showAddForm }) {
                                Icon(
                                    if (showAddForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = primaryPurple
                                )
                            }
                        }

                        AnimatedVisibility(visible = showAddForm) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    "Enter the invite code shared by your guardian.",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                OutlinedTextField(
                                    value = inviteCode,
                                    onValueChange = { inviteCode = it.uppercase() },
                                    label = { Text("Invite Code") },
                                    placeholder = { Text("SH-12345") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryPurple,
                                        focusedLabelColor = primaryPurple,
                                        focusedTextColor = Color.Black,
                                        unfocusedTextColor = Color.Black
                                    )
                                )
                                
                                if (connectState is ConnectState.Error) {
                                    Text(
                                        text = (connectState as ConnectState.Error).message,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 8.dp, start = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                                
                                // Preview Section
                                if (connectState is ConnectState.Preview) {
                                    val invite = (connectState as ConnectState.Preview).invite
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                        colors = CardDefaults.cardColors(containerColor = primaryPurple.copy(alpha = 0.05f)),
                                        border = BorderStroke(1.dp, primaryPurple.copy(alpha = 0.2f))
                                    ) {
                                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Color(0xFF4CAF50))
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(invite.guardianName, fontWeight = FontWeight.Bold, color = Color.Black)
                                                Text(invite.relationship, fontSize = 12.sp, color = Color.Gray)
                                            }
                                        }
                                    }
                                }

                                Button(
                                    onClick = { 
                                        if (connectState is ConnectState.Preview) {
                                            viewModel.acceptConnection((connectState as ConnectState.Preview).invite)
                                        } else {
                                            viewModel.verifyInviteCode(inviteCode)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryPurple),
                                    enabled = inviteCode.isNotBlank() && connectState !is ConnectState.Loading
                                ) {
                                    if (connectState is ConnectState.Loading) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                    } else {
                                        Text(
                                            if (connectState is ConnectState.Preview) "Confirm Connection" else "Verify Code",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun GuardianCard(
    guardian: ConnectedGuardianUI,
    onRemove: () -> Unit
) {
    val primaryPurple = Color(0xFF6A3CC3)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove Guardian?", fontWeight = FontWeight.Bold) },
            text = { Text("This person will no longer be able to monitor your location or receive alerts.") },
            confirmButton = {
                TextButton(onClick = { 
                    onRemove()
                    showDeleteConfirm = false
                }) {
                    Text("Remove", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = primaryPurple.copy(alpha = 0.1f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = primaryPurple, modifier = Modifier.size(28.dp))
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                            Surface(modifier = Modifier.size(18.dp), shape = CircleShape, color = Color(0xFF4CAF50), border = BorderStroke(2.dp, Color.White)) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.padding(3.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = guardian.guardianName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = Color.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = guardian.relationship,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = Color.LightGray)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Status", color = Color.Gray, fontSize = 11.sp)
                    Text("Connected", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Since", color = Color.Gray, fontSize = 11.sp)
                    Text(guardian.connectedSince, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}
