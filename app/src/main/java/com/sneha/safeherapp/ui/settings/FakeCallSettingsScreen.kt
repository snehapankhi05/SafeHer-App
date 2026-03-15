package com.sneha.safeherapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneha.safeherapp.model.FakeCallerProfile
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import com.sneha.safeherapp.util.FakeCallPrefs
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FakeCallSettingsScreen(
    onBack: () -> Unit,
    onNavigateToVoiceRecorder: (String) -> Unit
) {
    val context = LocalContext.current
    var profiles by remember { mutableStateOf(FakeCallPrefs.getProfiles(context)) }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<FakeCallerProfile?>(null) }
    var tempName by remember { mutableStateOf("") }

    val gradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Fake Call Profiles", 
                        fontWeight = FontWeight.Bold, 
                        color = Color.Black
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (profiles.size < 3) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editingProfile = null
                        tempName = ""
                        showAddEditDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Profile") },
                    containerColor = Color(0xFF6A3CC3),
                    contentColor = Color.White
                )
            }
        }
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Create up to 3 profiles. Only the active one will be used.",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(profiles) { profile ->
                    ProfileCard(
                        profile = profile,
                        onSelect = {
                            val updated = profiles.map { it.copy(isActive = it.id == profile.id) }
                            profiles = updated
                            FakeCallPrefs.saveProfiles(context, updated)
                        },
                        onEdit = {
                            editingProfile = profile
                            tempName = profile.name
                            showAddEditDialog = true
                        },
                        onDelete = {
                            val updated = profiles.filter { it.id != profile.id }
                            if (profile.isActive && updated.isNotEmpty()) {
                                updated[0].isActive = true
                            }
                            profiles = updated
                            FakeCallPrefs.saveProfiles(context, updated)
                        }
                    )
                }
            }
        }
    }

    if (showAddEditDialog) {
        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = { 
                Text(
                    text = if (editingProfile == null) "Add Profile" else "Edit Profile", 
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ) 
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { 
                            Text(
                                "Caller Name",
                                color = Color.LightGray
                            ) 
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 16.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedBorderColor = Color(0xFF6A3CC3),
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = Color(0xFF6A3CC3),
                            unfocusedLabelColor = Color.LightGray
                        )
                    )
                    
                    if (editingProfile != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                // Save name before navigating
                                val currentEditing = editingProfile!!
                                currentEditing.name = tempName
                                val updated = profiles.map { if (it.id == currentEditing.id) currentEditing else it }
                                profiles = updated
                                FakeCallPrefs.saveProfiles(context, updated)
                                onNavigateToVoiceRecorder(currentEditing.id) 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6A3CC3),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record Voice", fontWeight = FontWeight.Medium)
                        }
                        if (editingProfile?.audioPath != null) {
                            Text(
                                text = "Voice recorded ✓", 
                                color = Color(0xFF2E7D32), 
                                fontSize = 12.sp, 
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "Save profile first to record voice.", 
                            color = Color.LightGray, 
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            if (editingProfile == null) {
                                val newProfile = FakeCallerProfile(
                                    id = UUID.randomUUID().toString(),
                                    name = tempName,
                                    isActive = profiles.isEmpty()
                                )
                                profiles = profiles + newProfile
                            } else {
                                val updated = profiles.map {
                                    if (it.id == editingProfile!!.id) it.copy(name = tempName) else it
                                }
                                profiles = updated
                            }
                            FakeCallPrefs.saveProfiles(context, profiles)
                            showAddEditDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6A3CC3),
                        contentColor = Color.White
                    )
                ) {
                    Text("Save", fontWeight = FontWeight.Medium)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Cancel", color = Color(0xFF6A3CC3), fontWeight = FontWeight.Medium)
                }
            }
        )
    }
}

@Composable
fun ProfileCard(
    profile: FakeCallerProfile,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val containerColor = if (profile.isActive) Color(0xFFEDE7F6) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = if (profile.isActive) 
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF6A3CC3)) 
        else null
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = profile.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                if (profile.isActive) {
                    Text(
                        "Active Profile", 
                        color = Color(0xFF6A3CC3), 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = if (profile.audioPath != null) "Voice recording set" else "No voice recording",
                    color = Color.DarkGray,
                    fontSize = 12.sp
                )
            }
            
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit, 
                    contentDescription = "Edit", 
                    tint = Color.Gray
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = "Delete", 
                    tint = Color(0xFFE57373)
                )
            }
        }
    }
}
