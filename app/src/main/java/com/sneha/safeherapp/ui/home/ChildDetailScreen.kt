package com.sneha.safeherapp.ui.home

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.BatteryUnknown
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import com.sneha.safeherapp.data.model.User
import com.sneha.safeherapp.model.SafetyZone
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import kotlinx.coroutines.launch

@Composable
fun ChildDetailScreen(
    childId: String,
    onBack: () -> Unit,
    onNavigateToAddPlace: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val guardianUid = auth.currentUser?.uid
    val TAG = "BATTERY_DEBUG"

    // State for child data (guardian side record)
    var childRecord by remember { mutableStateOf<Child?>(null) }
    // State for live child data (from users collection)
    var liveChildData by remember { mutableStateOf<User?>(null) }
    
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Dashboard-consistent light gradient background
    val gradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    // Fetch Child Record and then listen to live data
    LaunchedEffect(childId, guardianUid) {
        if (guardianUid == null) {
            errorMessage = "User not authenticated"
            isLoading = false
            return@LaunchedEffect
        }

        // 1. Get the child record from guardian's collection
        db.collection("guardian_users").document(guardianUid)
            .collection("children").document(childId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    errorMessage = "Error loading child data"
                    isLoading = false
                    return@addSnapshotListener
                }
                
                if (snapshot != null && snapshot.exists()) {
                    val record = snapshot.toObject(Child::class.java)
                    childRecord = record
                    
                    // 2. If we have a childUid, listen to their live User profile
                    val childUid = record?.childUid
                    if (!childUid.isNullOrEmpty()) {
                        // STEP 4: VERIFY CONNECTION
                        Log.d(TAG, "Connected Child UID = $childUid")

                        // STEP 5: VERIFY FIREBASE READ
                        Log.d(TAG, "Reading Path: users/$childUid")

                        db.collection("users").document(childUid)
                            .addSnapshotListener { userSnapshot, userError ->
                                if (userError != null) {
                                    Log.e(TAG, "Error reading battery: ${userError.message}")
                                }

                                if (userSnapshot != null) {
                                    Log.d(TAG, "Document Exists: ${userSnapshot.exists()}")
                                    
                                    if (userSnapshot.exists()) {
                                        val userData = userSnapshot.toObject(User::class.java)
                                        liveChildData = userData
                                        Log.d(TAG, "Battery Value: ${userData?.batteryLevel}")
                                        Log.d(TAG, "Timestamp: ${userData?.lastUpdated}")
                                    } else {
                                        Log.d(TAG, "Battery Value: null")
                                    }
                                }
                                isLoading = false
                            }
                    } else {
                        Log.d(TAG, "Connected Child UID = null")
                        isLoading = false
                    }
                } else {
                    errorMessage = "Unable to load child information"
                    isLoading = false
                }
            }
    }

    // Derived values combining static record and live data
    val childName = liveChildData?.fullName?.takeIf { it.isNotBlank() } 
                    ?: childRecord?.fullName?.takeIf { it.isNotBlank() } 
                    ?: childRecord?.name ?: "Unknown"
    
    val childPhone = childRecord?.phone ?: ""
    val childEmail = liveChildData?.email?.takeIf { it.isNotBlank() } ?: childRecord?.email ?: ""
    
    // Live Stats
    val batteryLevel = liveChildData?.batteryLevel
    val batteryText = batteryLevel?.let { "$it%" } ?: "Battery Unavailable"
    val batteryColor = when {
        batteryLevel == null -> Color.Gray
        batteryLevel > 50 -> Color(0xFF4CAF50) // Green
        batteryLevel >= 20 -> Color(0xFFFF9800) // Orange
        else -> Color(0xFFF44336) // Red
    }
    
    val childLocationName = liveChildData?.locationName ?: "Location Hidden"
    val lastUpdated = liveChildData?.lastUpdated ?: childRecord?.lastUpdated
    val lastUpdatedText = formatLastUpdated(lastUpdated)
    
    val childLatLng = LatLng(
        liveChildData?.latitude ?: childRecord?.latitude ?: 23.0225, 
        liveChildData?.longitude ?: childRecord?.longitude ?: 72.5714
    )
    
    val childStatus = when {
        childRecord == null -> ChildStatus.UNKNOWN
        liveChildData == null -> ChildStatus.UNKNOWN
        // logic for unsafe zone could be added here if needed
        else -> ChildStatus.SAFE 
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(childLatLng, 15f)
    }

    // Safety Zones State
    var safetyZones by remember { mutableStateOf(emptyList<SafetyZone>()) }
    var selectedZoneId by remember { mutableStateOf<String?>(null) }
    var zoneToDelete by remember { mutableStateOf<SafetyZone?>(null) }

    // Fetch Safety Zones from Firebase
    LaunchedEffect(childId) {
        db.collection("safety_zones")
            .whereEqualTo("childId", childId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    safetyZones = snapshot.toObjects(SafetyZone::class.java)
                }
            }
    }

    // Sync camera if child location changes
    LaunchedEffect(childLatLng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(childLatLng, 15f)
    }

    // Deletion Dialog
    if (zoneToDelete != null) {
        AlertDialog(
            onDismissRequest = { zoneToDelete = null },
            title = { Text("Delete Safety Zone?") },
            text = { Text("Are you sure you want to remove \"${zoneToDelete?.placeName}\"?") },
            confirmButton = {
                TextButton(onClick = {
                    val id = zoneToDelete?.id
                    if (id != null) {
                        db.collection("safety_zones").document(id).delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Zone deleted", Toast.LENGTH_SHORT).show()
                            }
                    }
                    zoneToDelete = null
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { zoneToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // HEADER
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack, 
                modifier = Modifier.size(32.dp).offset(x = (-4).dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF333333)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Child Details",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF333333)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6A3CC3))
            }
        } else if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = errorMessage!!, fontWeight = FontWeight.Bold, color = Color.DarkGray)
                }
            }
        } else if (childRecord != null) {
            // 1. CHILD INFO CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = childName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF111111)
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when {
                                    batteryLevel == null -> Icons.AutoMirrored.Filled.BatteryUnknown
                                    batteryLevel < 20 -> Icons.Default.BatteryAlert
                                    batteryLevel < 50 -> Icons.Default.Battery3Bar
                                    else -> Icons.Default.Battery5Bar
                                },
                                contentDescription = null,
                                tint = batteryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = batteryText,
                                fontWeight = FontWeight.Bold,
                                color = batteryColor,
                                fontSize = 13.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(childStatus.color, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = childStatus.label.replace("🟢 ", "").replace("🔴 ", "").replace("🚶 ", ""),
                            fontWeight = FontWeight.Bold,
                            color = childStatus.color,
                            fontSize = 15.sp
                        )
                    }
                    
                    Text(
                        text = "Updated $lastUpdatedText",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = childPhone, fontSize = 13.sp, color = Color.DarkGray)
                    }

                    if (childEmail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = childEmail, fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. REAL GOOGLE MAP SECTION
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    uiSettings = MapUiSettings(
                        zoomControlsEnabled = true,
                        scrollGesturesEnabled = true,
                        zoomGesturesEnabled = true,
                        tiltGesturesEnabled = true,
                        rotationGesturesEnabled = true
                    ),
                    properties = MapProperties(
                        mapType = MapType.NORMAL
                    )
                ) {
                    // Actual Child Location Marker
                    Marker(
                        state = MarkerState(position = childLatLng),
                        title = "$childName's Current Location",
                        snippet = childLocationName
                    )

                    // DRAW SAVED CIRCLES PERMANENTLY
                    safetyZones.forEach { zone ->
                        val zoneLatLng = LatLng(zone.latitude, zone.longitude)
                        val color = when(zone.zoneType) {
                            "Safe" -> Color(0xFF4CAF50)
                            "Risky" -> Color(0xFFFF9800)
                            "Unsafe" -> Color(0xFFF44336)
                            else -> Color(0xFFFF9800)
                        }
                        
                        Circle(
                            center = zoneLatLng,
                            radius = zone.radius,
                            fillColor = color.copy(alpha = if (selectedZoneId == zone.id) 0.4f else 0.2f),
                            strokeColor = color,
                            strokeWidth = if (selectedZoneId == zone.id) 5f else 2f
                        )

                        Marker(
                            state = MarkerState(position = zoneLatLng),
                            title = zone.placeName,
                            snippet = "${zone.zoneType} Zone • ${zone.radius.toInt()}m",
                            icon = BitmapDescriptorFactory.defaultMarker(
                                when(zone.zoneType) {
                                    "Safe" -> BitmapDescriptorFactory.HUE_GREEN
                                    "Risky" -> BitmapDescriptorFactory.HUE_ORANGE
                                    "Unsafe" -> BitmapDescriptorFactory.HUE_RED
                                    else -> BitmapDescriptorFactory.HUE_YELLOW
                                }
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. ACTION BUTTONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionIconButtonCompact(
                    icon = Icons.Default.Call,
                    label = "Call",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$childPhone"))
                        context.startActivity(intent)
                    }
                )
                ActionIconButtonCompact(
                    icon = Icons.Default.Directions,
                    label = "Directions",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val uri = Uri.parse("google.navigation:q=${childLatLng.latitude},${childLatLng.longitude}")
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.google.android.apps.maps")
                        context.startActivity(intent)
                    }
                )
                ActionIconButtonCompact(
                    icon = Icons.Default.Refresh,
                    label = "Refresh",
                    modifier = Modifier.weight(1f),
                    onClick = { 
                        Toast.makeText(context, "Refreshing location...", Toast.LENGTH_SHORT).show()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. CURRENT LOCATION SECTION
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Current Location",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = childLocationName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF111111)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = childStatus.color.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (childStatus == ChildStatus.SAFE) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = childStatus.color,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (childStatus == ChildStatus.SAFE) "Child is currently in a safe area" else "Child is in an unsafe zone!",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = childStatus.color
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SAVED ZONES LIST
            Text(
                text = "Saved Safety Zones",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF333333),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (safetyZones.isEmpty()) {
                Text(
                    text = "No safety zones added yet. Tap the button below to add one.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    safetyZones.forEach { zone ->
                        SafetyZoneItem(
                            zone = zone,
                            isSelected = selectedZoneId == zone.id,
                            onDelete = { zoneToDelete = zone },
                            onClick = {
                                selectedZoneId = zone.id
                                scope.launch {
                                    cameraPositionState.animate(
                                        CameraUpdateFactory.newLatLngZoom(
                                            LatLng(zone.latitude, zone.longitude), 
                                            16f
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 5. ADD / MANAGE PLACES BUTTON
            Button(
                onClick = { onNavigateToAddPlace(childId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6A3CC3),
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.AddLocation, 
                    contentDescription = null, 
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Add / Manage Places", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

fun formatLastUpdated(timestamp: Timestamp?): String {
    if (timestamp == null) return "Unknown"
    val now = System.currentTimeMillis()
    val diff = now - timestamp.toDate().time
    val seconds = diff / 1000
    val minutes = seconds / 60
    
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} hours ago"
        else -> "${minutes / 1440} days ago"
    }
}

@Composable
fun SafetyZoneItem(
    zone: SafetyZone,
    isSelected: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val zoneColor = when(zone.zoneType) {
        "Safe" -> Color(0xFF4CAF50)
        "Risky" -> Color(0xFFFF9800)
        "Unsafe" -> Color(0xFFF44336)
        else -> Color(0xFFFF9800)
    }

    val icon = when(zone.placeName.lowercase()) {
        "home" -> Icons.Default.Home
        "school" -> Icons.Default.School
        "hostel" -> Icons.Default.Business
        else -> Icons.Default.Place
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) zoneColor.copy(alpha = 0.1f) else Color.White
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, zoneColor) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = zoneColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = zoneColor,
                    modifier = Modifier.padding(10.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = zone.placeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = "${zone.zoneType} Zone • ${zone.radius.toInt()}m",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun ActionIconButtonCompact(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White,
            contentColor = Color(0xFF6A3CC3)
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 6.dp
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = Color(0xFF6A3CC3), 
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label, 
                fontSize = 12.sp, 
                fontWeight = FontWeight.Bold, 
                color = Color(0xFF6A3CC3)
            )
        }
    }
}
