package com.sneha.safeherapp.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import com.sneha.safeherapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.*

enum class AlertLevel(val color: Color, val label: String) {
    LOW(Color(0xFFFFD600), "Low Risk"),
    MEDIUM(Color(0xFFFF9100), "Medium Risk"),
    HIGH(Color(0xFFD32F2F), "High Risk")
}

data class PlaceSuggestion(
    val mainText: String,
    val fullAddress: String,
    val latLng: LatLng,
    val category: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    category: String?,
    initialLat: Double? = null,
    initialLng: Double? = null,
    initialReason: String? = null,
    initialLevel: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var reports by remember { mutableStateOf(listOf<SafetyReport>()) }
    var showReportOptions by remember { mutableStateOf(false) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var selectedPlaceName by remember { mutableStateOf("") }
    
    // Search States
    var isSearching by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    var searchSuggestions by remember { mutableStateOf(listOf<PlaceSuggestion>()) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }

    // Nearby Services
    var nearbyPlaces by remember { mutableStateOf(listOf<PlaceSuggestion>()) }
    var isLoadingNearby by remember { mutableStateOf(false) }
    var selectedServiceLabel by remember { mutableStateOf<String?>(category) }

    // Report Dialog States
    var showReportDialog by remember { mutableStateOf(false) }
    var reasonText by remember { mutableStateOf("") }
    var selectedAlertLevel by remember { mutableStateOf(AlertLevel.LOW) }

    // Nearby Reports Sheet
    var showNearbySheet by remember { mutableStateOf(false) }

    // Safety Alert States
    var showSafetyDialog by remember { mutableStateOf(false) }
    var safetyAlertMessage by remember { mutableStateOf("") }
    var hasShownAlertForCurrentTarget by remember { mutableStateOf(false) }

    // Navigation States
    var showSafetyAlert by remember { mutableStateOf<String?>(null) }
    var lastNotifiedAreaId by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(20.5937, 78.9629), 5f)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Initial focus if passed from reports screen
    LaunchedEffect(initialLat, initialLng) {
        if (initialLat != null && initialLng != null) {
            val target = LatLng(initialLat, initialLng)
            selectedLocation = target
            selectedPlaceName = initialReason ?: "Reported Area"
            cameraPositionState.position = CameraPosition.fromLatLngZoom(target, 16f)
        }
    }

    LaunchedEffect(Unit) {
        db.collection("reports").addSnapshotListener { snapshot, _ ->
            snapshot?.let {
                reports = it.documents.map { doc ->
                    SafetyReport(
                        id = doc.id,
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        reason = doc.getString("reason") ?: "",
                        alertLevel = doc.getString("alertLevel") ?: "LOW",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        userId = doc.getString("userId") ?: ""
                    )
                }
            }
        }
    }

    LaunchedEffect(searchText) {
        if (searchText.length > 2) {
            isLoadingSuggestions = true
            delay(500)
            val results = withContext(Dispatchers.IO) { getSearchSuggestions(context, searchText) }
            searchSuggestions = results
            isLoadingSuggestions = false
        } else {
            searchSuggestions = emptyList()
            isLoadingSuggestions = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getCurrentLocation(context, fusedLocationClient) { latLng ->
                userLocation = latLng
                if (initialLat == null) cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getCurrentLocation(context, fusedLocationClient) { latLng ->
                userLocation = latLng
                if (initialLat == null) cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
                
                // Fetch nearby places if category was passed
                category?.let { cat ->
                    scope.launch {
                        isLoadingNearby = true
                        val results = withContext(Dispatchers.IO) {
                            fetchNearbyPlacesAPI(context, cat, latLng)
                        }
                        nearbyPlaces = results
                        isLoadingNearby = false
                        if (results.isEmpty()) {
                            Toast.makeText(context, "No nearby places found. Try increasing range.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    // Safety Alert Check when searching/selecting destination
    LaunchedEffect(selectedLocation, reports) {
        if (selectedLocation != null && !hasShownAlertForCurrentTarget) {
            val nearbyReports = reports.filter { report ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    selectedLocation!!.latitude, selectedLocation!!.longitude,
                    report.latitude, report.longitude,
                    results
                )
                results[0] < 1000 // 1 km
            }
            
            if (nearbyReports.isNotEmpty()) {
                safetyAlertMessage = "This route has ${nearbyReports.size} reported unsafe area(s) nearby. Please stay cautious."
                showSafetyDialog = true
                hasShownAlertForCurrentTarget = true
                Toast.makeText(context, "⚠ Unsafe area reported near your route. Stay alert.", Toast.LENGTH_LONG).show()
            }
        } else if (selectedLocation == null) {
            hasShownAlertForCurrentTarget = false
        }
    }

    LaunchedEffect(userLocation, reports) {
        while(true) {
            userLocation?.let { loc ->
                val nearbyUnsafe = reports.find { calculateDistance(loc, LatLng(it.latitude, it.longitude)) < 300 }
                if (nearbyUnsafe != null) {
                    val alertMsg = if (nearbyUnsafe.alertLevel == "HIGH") "🚨 High-risk area nearby" else "⚠ Unsafe area nearby. Stay alert."
                    showSafetyAlert = alertMsg
                    if (lastNotifiedAreaId != nearbyUnsafe.id) {
                        showNotification(context, "Safety Alert", alertMsg)
                        lastNotifiedAreaId = nearbyUnsafe.id
                    }
                } else {
                    showSafetyAlert = null
                    lastNotifiedAreaId = null
                }
            }
            delay(15000)
        }
    }

    BackHandler(enabled = isSearching || selectionMode || selectedLocation != null || selectedServiceLabel != null) {
        if (isSearching) isSearching = false
        else if (selectionMode) selectionMode = false
        else if (selectedServiceLabel != null) {
            selectedServiceLabel = null
            nearbyPlaces = emptyList()
        }
        else if (selectedLocation != null) {
            selectedLocation = null
            selectedPlaceName = ""
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = remember { MapProperties(isMyLocationEnabled = true) },
            uiSettings = remember { MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false) },
            onMapClick = { latLng ->
                if (selectionMode) {
                    selectedLocation = latLng
                    selectionMode = false
                    showReportDialog = true
                } else {
                    selectedLocation = null
                    selectedPlaceName = ""
                }
            }
        ) {
            reports.forEach { report ->
                val level = try { AlertLevel.valueOf(report.alertLevel) } catch(e: Exception) { AlertLevel.LOW }
                val isSelected = selectedLocation?.latitude == report.latitude && selectedLocation?.longitude == report.longitude
                
                Marker(
                    state = MarkerState(position = LatLng(report.latitude, report.longitude)),
                    title = if (isSelected) "Reported Unsafe Area" else report.reason,
                    snippet = if (isSelected) report.reason else "Risk: ${level.label}",
                    zIndex = if (isSelected) 1.0f else 0.0f,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when(level) {
                            AlertLevel.LOW -> BitmapDescriptorFactory.HUE_YELLOW
                            AlertLevel.MEDIUM -> BitmapDescriptorFactory.HUE_ORANGE
                            AlertLevel.HIGH -> BitmapDescriptorFactory.HUE_RED
                        }
                    )
                )
            }

            nearbyPlaces.forEach { place ->
                Marker(
                    state = MarkerState(position = place.latLng),
                    title = place.mainText,
                    snippet = place.fullAddress,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (selectedServiceLabel) {
                            "hospital" -> BitmapDescriptorFactory.HUE_ROSE
                            "police" -> BitmapDescriptorFactory.HUE_BLUE
                            "fire_station" -> BitmapDescriptorFactory.HUE_VIOLET
                            "pharmacy" -> BitmapDescriptorFactory.HUE_GREEN
                            "bus_station" -> BitmapDescriptorFactory.HUE_CYAN
                            "subway_station" -> BitmapDescriptorFactory.HUE_CYAN
                            else -> BitmapDescriptorFactory.HUE_AZURE
                        }
                    ),
                    onClick = {
                        selectedLocation = place.latLng
                        selectedPlaceName = place.mainText
                        false
                    }
                )
            }
            
            selectedLocation?.let {
                if (reports.none { r -> r.latitude == it.latitude && r.longitude == it.longitude } &&
                    nearbyPlaces.none { p -> p.latLng == it }) {
                    Marker(
                        state = MarkerState(position = it),
                        title = selectedPlaceName.ifBlank { "Selected Location" },
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }
        }

        // Top Search UI
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth().shadow(elevation = 8.dp, shape = RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                color = Color.White
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { if (isSearching) isSearching = false else onBack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF4A2E8E))
                        }
                        TextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Search location...", color = Color.Gray) },
                            modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) isSearching = true },
                            colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, disabledContainerColor = Color.Transparent, focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, focusedTextColor = Color(0xFF222222), unfocusedTextColor = Color(0xFF222222)),
                            singleLine = true
                        )
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { searchText = ""; searchSuggestions = emptyList() }) { Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.DarkGray) }
                        } else {
                            IconButton(onClick = { getCurrentLocation(context, fusedLocationClient) { latLng -> userLocation = latLng; scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) } } }) { Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = Color(0xFF4A2E8E)) }
                        }
                    }
                    if (isSearching) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Box(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                            if (isLoadingSuggestions) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF4A2E8E))
                            if (searchSuggestions.isEmpty() && searchText.length > 2 && !isLoadingSuggestions) {
                                Text("No results found", modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, color = Color.Gray)
                            } else {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(searchSuggestions) { suggestion ->
                                        Column(modifier = Modifier.fillMaxWidth().clickable { searchText = suggestion.mainText; isSearching = false; selectedLocation = suggestion.latLng; selectedPlaceName = suggestion.mainText; scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(suggestion.latLng, 15f)) } }.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(text = suggestion.mainText, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF111111), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                    Text(text = suggestion.fullAddress, fontSize = 12.sp, color = Color(0xFF666666), maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = showSafetyAlert != null, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
                Surface(modifier = Modifier.padding(top = 12.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = if (showSafetyAlert?.contains("🚨") == true) Color(0xFFFDECEA) else Color(0xFFFFF9C4), shadowElevation = 4.dp) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = showSafetyAlert ?: "", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (showSafetyAlert?.contains("🚨") == true) Color(0xFFD32F2F) else Color(0xFFF57F17), modifier = Modifier.weight(1f))
                        IconButton(onClick = { showSafetyAlert = null }, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray) }
                    }
                }
            }

            // Nearby Status Badge
            selectedServiceLabel?.let { label ->
                Surface(
                    modifier = Modifier.padding(top = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (isLoadingNearby) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp, color = Color(0xFF4A2E8E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finding nearby ${label.replace("_", " ")}...", fontSize = 12.sp, color = Color(0xFF4A2E8E))
                        } else {
                            Text("Nearby ${label.replace("_", " ").split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A2E8E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp).clickable { 
                                selectedServiceLabel = null
                                nearbyPlaces = emptyList()
                            }, tint = Color.Gray)
                        }
                    }
                }
            }
        }

        // FAB stack
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (selectedLocation != null) 200.dp else 24.dp, end = 16.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FloatingActionButton(
                onClick = { showNearbySheet = true }, 
                containerColor = Color.White, 
                contentColor = Color(0xFF4A2E8E), 
                shape = CircleShape
            ) { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Nearby Reports") }
            
            ExtendedFloatingActionButton(
                onClick = { showReportOptions = true }, 
                containerColor = Color(0xFFD32F2F), 
                contentColor = Color.White, 
                shape = RoundedCornerShape(16.dp), 
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White) }, 
                text = { Text("Report Danger", fontWeight = FontWeight.Bold, color = Color.White) }
            )
            
            FloatingActionButton(
                onClick = { 
                    getCurrentLocation(context, fusedLocationClient) { latLng -> 
                        userLocation = latLng; 
                        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 15f)) } 
                    } 
                }, 
                containerColor = Color.White, 
                contentColor = Color(0xFF4A2E8E), 
                shape = CircleShape
            ) { Icon(Icons.Default.MyLocation, contentDescription = "My Location") }
        }

        // Bottom Details Card
        AnimatedVisibility(visible = selectedLocation != null && !isSearching && !selectionMode, modifier = Modifier.align(Alignment.BottomCenter), enter = slideInVertically(initialOffsetY = { it }) + fadeIn(), exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()) {
            Card(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(48.dp), shape = CircleShape, color = Color(0xFFEDE7F6)) { Icon(Icons.Default.Place, contentDescription = null, tint = Color(0xFF4A2E8E), modifier = Modifier.padding(12.dp)) }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selectedPlaceName.ifBlank { "Selected Location" }, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF111111))
                            Text("${String.format("%.4f", selectedLocation?.latitude)}, ${String.format("%.4f", selectedLocation?.longitude)}", fontSize = 12.sp, color = Color(0xFF444444))
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = { val uri = Uri.parse("google.navigation:q=${selectedLocation?.latitude},${selectedLocation?.longitude}"); context.startActivity(Intent(Intent.ACTION_VIEW, uri).setPackage("com.google.android.apps.maps")) }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2E8E))) { Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White); Spacer(modifier = Modifier.width(8.dp)); Text("Get Directions", fontWeight = FontWeight.Bold, color = Color.White) }
                }
            }
        }

        if (selectionMode) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(48.dp))
                    Surface(color = Color.Black.copy(alpha = 0.7f), shape = RoundedCornerShape(8.dp), modifier = Modifier.padding(top = 8.dp)) { Text("Tap on Map to Pin Location", color = Color.White, modifier = Modifier.padding(8.dp), fontSize = 12.sp) }
                }
            }
        }
    }

    // Safety Alert Dialog
    if (showSafetyDialog) {
        AlertDialog(
            onDismissRequest = { showSafetyDialog = false },
            title = { Text("Safety Alert", fontWeight = FontWeight.Bold) },
            text = { Text(safetyAlertMessage) },
            confirmButton = {
                Button(onClick = { showSafetyDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showNearbySheet) {
        ModalBottomSheet(onDismissRequest = { showNearbySheet = false }, containerColor = Color.White) {
            val center = cameraPositionState.position.target
            val nearby = reports.filter { calculateDistance(center, LatLng(it.latitude, it.longitude)) < 2000 }.sortedBy { calculateDistance(center, LatLng(it.latitude, it.longitude)) }
            Column(modifier = Modifier.padding(bottom = 32.dp, start = 16.dp, end = 16.dp).fillMaxWidth()) {
                Text("Nearby Unsafe Reports", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF111111))
                Spacer(modifier = Modifier.height(16.dp))
                if (nearby.isEmpty()) {
                    Text("No reports within 2km of current view.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(nearby) { report ->
                            val dist = calculateDistance(center, LatLng(report.latitude, report.longitude)).toInt()
                            ListItem(
                                headlineContent = { Text(report.reason, fontWeight = FontWeight.Bold) },
                                supportingContent = { Text("${dist}m away") },
                                leadingContent = { 
                                    val level = try { AlertLevel.valueOf(report.alertLevel) } catch(e: Exception) { AlertLevel.LOW }
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = level.color) 
                                },
                                modifier = Modifier.clickable {
                                    showNearbySheet = false
                                    selectedLocation = LatLng(report.latitude, report.longitude)
                                    selectedPlaceName = report.reason
                                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(selectedLocation!!, 16f)) }
                                }
                            )
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }

    if (showReportOptions) {
        ModalBottomSheet(onDismissRequest = { showReportOptions = false }, containerColor = Color.White) {
            Column(modifier = Modifier.padding(bottom = 48.dp, start = 24.dp, end = 24.dp).fillMaxWidth()) {
                Text("Report Unsafe Location", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF111111))
                Spacer(modifier = Modifier.height(16.dp))
                ReportOptionItem(title = "Select on Map", icon = Icons.Default.AddLocationAlt, onClick = { showReportOptions = false; selectionMode = true })
                ReportOptionItem(title = "Search Location", icon = Icons.Default.Search, onClick = { showReportOptions = false; isSearching = true })
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = Color.White,
            title = { Text("Report Safety Concern", fontWeight = FontWeight.Bold, color = Color(0xFF111111)) },
            text = {
                Column {
                    Text("Risk Level:", fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color(0xFF333333))
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        AlertLevel.entries.forEach { level ->
                            FilterChip(selected = selectedAlertLevel == level, onClick = { selectedAlertLevel = level }, label = { Text(level.label, fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = level.color.copy(alpha = 0.2f), selectedLabelColor = level.color, labelColor = Color(0xFF444444)))
                        }
                    }
                    OutlinedTextField(value = reasonText, onValueChange = { reasonText = it }, label = { Text("Describe the issue") }, modifier = Modifier.fillMaxWidth(), minLines = 3, colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color(0xFF111111), unfocusedTextColor = Color(0xFF222222)))
                }
            },
            confirmButton = { Button(onClick = { if (reasonText.isNotBlank() && selectedLocation != null) { val report = mapOf("latitude" to selectedLocation!!.latitude, "longitude" to selectedLocation!!.longitude, "reason" to reasonText, "alertLevel" to selectedAlertLevel.name, "timestamp" to System.currentTimeMillis(), "userId" to (auth.currentUser?.uid ?: "anonymous")); db.collection("reports").add(report); showReportDialog = false; reasonText = ""; selectedLocation = null; searchText = ""; searchSuggestions = emptyList(); isSearching = false; Toast.makeText(context, "Report Submitted!", Toast.LENGTH_LONG).show() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A2E8E))) { Text("Submit Report", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("Cancel", color = Color.Gray) } }
        )
    }
}

@Composable
fun ReportOptionItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onClick() }, shape = RoundedCornerShape(12.dp), color = Color(0xFFF5F5F5)) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, contentDescription = null, tint = Color(0xFF4A2E8E)); Spacer(modifier = Modifier.width(16.dp)); Text(title, fontWeight = FontWeight.Medium, color = Color(0xFF222222)) } }
}

data class SafetyReport(
    val id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val reason: String = "",
    val alertLevel: String = "LOW",
    val timestamp: Long = 0L,
    val userId: String = ""
)

@SuppressLint("MissingPermission")
private fun getCurrentLocation(context: Context, fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient, onLocationFound: (LatLng) -> Unit) { fusedLocationClient.lastLocation.addOnSuccessListener { it?.let { onLocationFound(LatLng(it.latitude, it.longitude)) } } }

private fun getSearchSuggestions(context: Context, query: String): List<PlaceSuggestion> { return try { val geocoder = Geocoder(context); val addresses = geocoder.getFromLocationName(query, 5); addresses?.map { addr -> PlaceSuggestion(mainText = addr.featureName ?: addr.thoroughfare ?: "Unknown Place", fullAddress = addr.getAddressLine(0) ?: "", latLng = LatLng(addr.latitude, addr.longitude)) } ?: emptyList() } catch (e: Exception) { emptyList() } }

private suspend fun fetchNearbyPlacesAPI(context: Context, type: String, location: LatLng): List<PlaceSuggestion> {
    val apiKey = BuildConfig.MAPS_API_KEY
    val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=${location.latitude},${location.longitude}&radius=3000&type=$type&key=$apiKey"
    
    Log.d("MapScreen", "Fetching Nearby: $url")
    
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val jsonData = response.body?.string()
            
            val suggestions = mutableListOf<PlaceSuggestion>()
            if (jsonData != null) {
                val jsonObject = JSONObject(jsonData)
                val results = jsonObject.getJSONArray("results")
                Log.d("MapScreen", "Received ${results.length()} results")
                
                for (i in 0 until results.length()) {
                    val place = results.getJSONObject(i)
                    val name = place.getString("name")
                    val vicinity = if (place.has("vicinity")) place.getString("vicinity") else ""
                    val geometry = place.getJSONObject("geometry")
                    val loc = geometry.getJSONObject("location")
                    val lat = loc.getDouble("lat")
                    val lng = loc.getDouble("lng")
                    
                    suggestions.add(PlaceSuggestion(
                        mainText = name,
                        fullAddress = vicinity,
                        latLng = LatLng(lat, lng)
                    ))
                }
            }
            suggestions
        } catch (e: Exception) {
            Log.e("MapScreen", "API Error: ${e.message}", e)
            emptyList()
        }
    }
}

private fun calculateDistance(start: LatLng, end: LatLng): Float { val results = FloatArray(1); android.location.Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results); return results[0] }

private fun showNotification(context: Context, title: String, message: String) { val channelId = "safety_alerts"; val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { val channel = NotificationChannel(channelId, "Safety Alerts", NotificationManager.IMPORTANCE_HIGH); notificationManager.createNotificationChannel(channel) }; val notification = NotificationCompat.Builder(context, channelId).setSmallIcon(android.R.drawable.ic_dialog_alert).setContentTitle(title).setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build(); notificationManager.notify(System.currentTimeMillis().toInt(), notification) }
