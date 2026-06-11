package com.sneha.safeherapp.ui.home

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sneha.safeherapp.data.model.GuardianConnection
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import com.sneha.safeherapp.viewmodel.SosState
import com.sneha.safeherapp.viewmodel.SosViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserHomeScreen(
    onLogout: () -> Unit,
    onNavigateToFakeCall: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToEmergencyContacts: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChatbot: () -> Unit,
    onNavigateToConnectGuardian: () -> Unit
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val sosViewModel: SosViewModel = viewModel()
    val sosState by sosViewModel.sosState
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val userId = auth.currentUser?.uid ?: ""

    // We still keep a listener for primary guardian to sync battery, 
    // though the UI now points to a management screen.
    var primaryGuardianUid by remember { mutableStateOf<String?>(null) }

    BackHandler {
        (context as? Activity)?.finish()
    }

    var contacts by remember { mutableStateOf(listOf<com.sneha.safeherapp.data.model.EmergencyContact>()) }
    var isLoadingContacts by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingContactId by remember { mutableStateOf<String?>(null) }
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    
    var isFakeCallTriggered by remember { mutableStateOf(false) }
    val isSosEnabled = contacts.isNotEmpty()

    val updateBatteryStatus = {
        if (userId.isNotEmpty()) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            val updates = hashMapOf(
                "battery" to batteryLevel,
                "lastUpdated" to FieldValue.serverTimestamp()
            )
            
            db.collection("users").document(userId).update(updates)
            
            // Sync with all active guardians
            db.collection("user_connections")
                .whereEqualTo("childUid", userId)
                .whereEqualTo("status", "accepted")
                .get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { connDoc ->
                        val gUid = connDoc.getString("guardianUid")
                        if (gUid != null) {
                            db.collection("guardian_users").document(gUid)
                                .collection("children").whereEqualTo("childUid", userId).get()
                                .addOnSuccessListener { childSnapshot ->
                                    for (doc in childSnapshot.documents) {
                                        doc.reference.update(updates)
                                    }
                                }
                        }
                    }
                }
        }
    }

    DisposableEffect(userId) {
        if (userId.isEmpty()) return@DisposableEffect onDispose {}
        val connectionListener = db.collection("user_connections")
            .whereEqualTo("childUid", userId)
            .whereEqualTo("status", "accepted")
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                primaryGuardianUid = snapshot?.documents?.firstOrNull()?.getString("guardianUid")
                updateBatteryStatus()
            }
        onDispose { connectionListener.remove() }
    }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            while(true) {
                updateBatteryStatus()
                delay(300000) 
            }
        }
    }

    DisposableEffect(context, userId) {
        if (userId.isEmpty()) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                updateBatteryStatus()
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    DisposableEffect(userId) {
        if (userId.isEmpty()) return@DisposableEffect onDispose {}
        isLoadingContacts = true
        val listenerRegistration = db.collection("users").document(userId)
            .collection("contacts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    isLoadingContacts = false
                    return@addSnapshotListener
                }
                contacts = snapshot?.documents?.map { doc ->
                    com.sneha.safeherapp.data.model.EmergencyContact(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        phoneNumber = doc.getString("phoneNumber") ?: ""
                    )
                } ?: emptyList()
                isLoadingContacts = false
            }
        onDispose { listenerRegistration.remove() }
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
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
                    color = Color(0xFF6A3CC3),
                    fontWeight = FontWeight.Bold
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
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            onNavigateToMap() 
                        }
                    },
                    icon = { Icon(Icons.Default.LocationOn, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Emergency Contacts") },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            onNavigateToEmergencyContacts()
                        }
                    },
                    icon = { Icon(Icons.Default.Contacts, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Chatbot") },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            onNavigateToChatbot()
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("Manage Guardians") },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close()
                            onNavigateToConnectGuardian()
                        }
                    },
                    icon = { Icon(Icons.Default.Security, contentDescription = null) }
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
                        IconButton(onClick = onNavigateToProfile) {
                            Icon(Icons.Default.AccountCircle, contentDescription = "Profile", tint = Color(0xFF6A3CC3))
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
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
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val sosButtonSize = if (screenWidth < 360.dp) 160.dp else 200.dp
                    
                    // SOS Button
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier.size(sosButtonSize),
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
                                        fontSize = if (screenWidth < 360.dp) 36.sp else 48.sp,
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
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // 1. Emergency Contacts Card (FIXED BUTTON RESPONSIVENESS)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Trusted Contacts",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
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
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF6A3CC3),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Add", fontSize = 14.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (isLoadingContacts) {
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                                }
                            } else if (contacts.isEmpty()) {
                                Text(
                                    "No contacts added yet.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            } else {
                                contacts.take(3).forEach { contact ->
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
                                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                }
                                if (contacts.size > 3) {
                                    TextButton(
                                        onClick = onNavigateToEmergencyContacts,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text("View All (${contacts.size})", color = Color(0xFF6A3CC3), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 2. Your Guardians Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Your Guardians",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Manage trusted guardians who monitor your safety and live location.",
                                fontSize = 13.sp,
                                color = Color.DarkGray,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToConnectGuardian,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF6A3CC3),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Guardian Management", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 3. Public Help Numbers
                    OutlinedButton(
                        onClick = onNavigateToEmergencyContacts,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(width = 1.5.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6A3CC3))
                    ) {
                        Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFF6A3CC3), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Public Help Numbers", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // 4. Fake Call Feature
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                        border = BorderStroke(1.dp, Color(0xFF6A3CC3).copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = ButtonDefaults.outlinedButtonBorder(enabled = !isFakeCallTriggered).copy(width = 1.5.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6A3CC3))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.PhoneCallback, contentDescription = null, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Trigger Fake Call", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tip: Record custom caller voices in Settings",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(40.dp))
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
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (editingContactId == null) "New Contact" else "Edit Contact", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6A3CC3)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    val dialogTextFieldColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        cursorColor = Color(0xFF6A3CC3),
                        focusedBorderColor = Color(0xFF6A3CC3),
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                        focusedLabelColor = Color(0xFF6A3CC3)
                    )

                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { input ->
                            contactName = input.filter { it.isLetter() || it.isWhitespace() }
                        },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = dialogTextFieldColors
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { input ->
                            contactPhone = input.filter { it.isDigit() }.take(10)
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
                            Text("Cancel", color = Color.Gray)
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
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ContactItem(contact: com.sneha.safeherapp.data.model.EmergencyContact, onDelete: () -> Unit, onEdit: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name, 
                fontWeight = FontWeight.Bold, 
                fontSize = 15.sp, 
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = contact.phoneNumber, 
                color = Color.Gray, 
                fontSize = 13.sp
            )
        }
        Row {
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
            }
        }
    }
}
