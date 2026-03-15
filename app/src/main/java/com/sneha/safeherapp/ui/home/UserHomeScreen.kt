package com.sneha.safeherapp.ui.home

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.PhoneCallback
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import com.sneha.safeherapp.viewmodel.SosState
import com.sneha.safeherapp.viewmodel.SosViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class EmergencyContact(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    onLogout: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sosViewModel: SosViewModel = viewModel()
    val sosState by sosViewModel.sosState
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    // Prevent navigation back to login
    BackHandler {
        (context as? Activity)?.finish()
    }

    // State for emergency contacts
    var contacts by remember { mutableStateOf(listOf<EmergencyContact>()) }
    var isLoadingContacts by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingContactId by remember { mutableStateOf<String?>(null) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    
    var isFakeCallTriggered by remember { mutableStateOf(false) }

    val isSosEnabled = contacts.isNotEmpty()

    // Real-time Firestore Snapshot Listener
    DisposableEffect(userId) {
        if (userId.isEmpty()) return@DisposableEffect onDispose {}
        
        isLoadingContacts = true
        val listenerRegistration = db.collection("users").document(userId)
            .collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoadingContacts = false
                    Toast.makeText(context, "Error syncing contacts", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                contacts = snapshot?.documents?.map { doc ->
                    EmergencyContact(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: ""
                    )
                } ?: emptyList()
                
                isLoadingContacts = false
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    // Permissions handling
    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            sosViewModel.triggerSos(context)
        } else {
            Toast.makeText(context, "Permissions required for SOS", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(sosState) {
        if (sosState is SosState.Success) {
            Toast.makeText(context, "SOS Alerts Sent Successfully!", Toast.LENGTH_LONG).show()
            sosViewModel.resetState()
        } else if (sosState is SosState.Error) {
            Toast.makeText(context, (sosState as SosState.Error).message, Toast.LENGTH_LONG).show()
            sosViewModel.resetState()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "SafeHer Menu",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF6A3CC3)
                )
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Map") },
                    selected = false,
                    onClick = { /* Navigate to Map */ },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Emergency Contacts") },
                    selected = false,
                    onClick = { /* Navigate to Contacts */ },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Chatbot") },
                    selected = false,
                    onClick = { /* Navigate to Chatbot */ },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) }
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
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF6A3CC3))
                        }
                    },
                    actions = {
                        IconButton(onClick = { /* Navigate to Profile */ }) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    // SOS Button
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(200.dp),
                            shape = CircleShape,
                            color = if (isSosEnabled) Color.Red else Color.Gray.copy(alpha = 0.5f),
                            shadowElevation = 12.dp,
                            onClick = {
                                if (isSosEnabled) {
                                    val permissions = arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.SEND_SMS
                                    )
                                    if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                                        sosViewModel.triggerSos(context)
                                    } else {
                                        permissionsLauncher.launch(permissions)
                                    }
                                }
                            },
                            enabled = isSosEnabled && sosState !is SosState.Sending
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (sosState is SosState.Sending) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp))
                                        Spacer(Modifier.height(8.dp))
                                        Text("Sending...", color = Color.White, fontSize = 14.sp)
                                    }
                                } else {
                                    Text(
                                        text = "SOS",
                                        color = Color.White,
                                        fontSize = 48.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isSosEnabled && !isLoadingContacts) {
                        Text(
                            text = "Add an emergency contact to activate SOS",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Emergency Contacts Card (FIX 1)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Emergency Contacts",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Button(
                                    onClick = { 
                                        editingContactId = null
                                        contactName = ""
                                        contactPhone = ""
                                        showAddDialog = true 
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isLoadingContacts,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6A3CC3),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add Contact")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add")
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isLoadingContacts) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                }
                            } else if (contacts.isEmpty()) {
                                Text(
                                    "No contacts added yet.",
                                    color = Color.DarkGray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                contacts.forEach { contact ->
                                    ContactItem(
                                        contact = contact,
                                        onDelete = {
                                            db.collection("users").document(userId)
                                                .collection("contacts").document(contact.id)
                                                .delete()
                                        },
                                        onEdit = {
                                            editingContactId = contact.id
                                            contactName = contact.name
                                            contactPhone = contact.phoneNumber
                                            showAddDialog = true
                                        }
                                    )
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Fake Call Feature
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (!isFakeCallTriggered) {
                                    isFakeCallTriggered = true
                                    onNavigateToFakeCall()
                                    scope.launch {
                                        delay(2000)
                                        isFakeCallTriggered = false
                                    }
                                }
                            },
                            enabled = !isFakeCallTriggered,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = ButtonDefaults.outlinedButtonBorder(enabled = !isFakeCallTriggered).copy(width = 2.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF6A3CC3)
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Fake Call",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // (FIX 2) Tips Text
                        Text(
                            text = "Tip:",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "• Record fake caller voices in Settings",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "• Go to Settings → Fake Call Settings to customize callers",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }

    // Add/Edit Contact Dialog
    if (showAddDialog) {
        Dialog(onDismissRequest = { 
            showAddDialog = false
            editingContactId = null
            contactName = ""
            contactPhone = ""
        }) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (editingContactId == null) "Add Emergency Contact" else "Edit Emergency Contact", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A3CC3)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val dialogTextFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color(0xFF6A3CC3),
                        focusedBorderColor = Color(0xFF6A3CC3),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF6A3CC3),
                        unfocusedLabelColor = Color.DarkGray
                    )

                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { input ->
                            contactName = input.filter { it.isLetter() || it.isWhitespace() }
                        },
                        label = { Text("Contact Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = dialogTextFieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { input ->
                            contactPhone = input.filter { it.isDigit() }
                        },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = dialogTextFieldColors,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { 
                            showAddDialog = false
                            editingContactId = null
                            contactName = ""
                            contactPhone = ""
                        }) {
                            Text("Cancel", color = Color(0xFF6A3CC3))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val contactData = mapOf(
                                    "name" to contactName,
                                    "phoneNumber" to contactPhone
                                )
                                
                                val contactsRef = db.collection("users").document(userId).collection("contacts")
                                
                                if (editingContactId == null) {
                                    contactsRef.add(contactData)
                                } else {
                                    contactsRef.document(editingContactId!!).set(contactData)
                                }

                                showAddDialog = false
                                editingContactId = null
                                contactName = ""
                                contactPhone = ""
                            },
                            shape = RoundedCornerShape(12.dp),
                            enabled = contactName.isNotBlank() && contactPhone.length >= 10,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A3CC3))
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: EmergencyContact, onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Text(contact.phoneNumber, color = Color.DarkGray, fontSize = 14.sp)
        }
        Row {
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373))
            }
        }
    }
}
