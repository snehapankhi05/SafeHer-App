package com.sneha.safeherapp.ui.home

import android.content.pm.PackageManager
import android.location.Geocoder
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.*
import com.sneha.safeherapp.model.SafetyZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddPlaceScreen(
    childId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val db = remember { FirebaseFirestore.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    
    var placesClient by remember { mutableStateOf<PlacesClient?>(null) }

    // Initialize Places SDK
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            try {
                val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                val apiKey = appInfo.metaData.getString("com.google.android.geo.API_KEY")
                if (!apiKey.isNullOrBlank()) {
                    Places.initialize(context, apiKey)
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
        if (Places.isInitialized()) {
            placesClient = Places.createClient(context)
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }

    var placeName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("Safe") }
    var radius by remember { mutableFloatStateOf(500f) }
    var isSaving by remember { mutableStateOf(false) }
    
    val initialLocation = LatLng(23.0225, 72.5714) 
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialLocation, 15f)
    }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf(emptyList<AutocompletePrediction>()) }
    var isSearchingSuggestions by remember { mutableStateOf(false) }
    var isLoadingSuggestions by remember { mutableStateOf(false) }

    // REAL SEARCH FUNCTIONALITY
    fun searchLocation(query: String) {
        if (query.isBlank()) return
        isLoadingSuggestions = true
        focusManager.clearFocus()
        keyboardController?.hide()
        
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context)
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(query, 1)
                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        val latLng = LatLng(address.latitude, address.longitude)
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                        placeName = address.featureName ?: query
                        searchQuery = address.getAddressLine(0) ?: query
                        suggestions = emptyList()
                        isSearchingSuggestions = false
                    } else {
                        Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                    }
                    isLoadingSuggestions = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoadingSuggestions = false
                }
            }
        }
    }

    fun fetchSuggestions(query: String) {
        if (query.isBlank() || placesClient == null) {
            suggestions = emptyList()
            isLoadingSuggestions = false
            isSearchingSuggestions = false
            return
        }
        isLoadingSuggestions = true
        val request = FindAutocompletePredictionsRequest.builder().setQuery(query).build()
        placesClient?.findAutocompletePredictions(request)
            ?.addOnSuccessListener { response ->
                suggestions = response.autocompletePredictions
                isLoadingSuggestions = false
                isSearchingSuggestions = true
            }
            ?.addOnFailureListener {
                suggestions = emptyList()
                isLoadingSuggestions = false
                isSearchingSuggestions = false
            }
    }

    fun onSuggestionSelected(prediction: AutocompletePrediction) {
        val client = placesClient ?: return
        val placeId = prediction.placeId
        val placeFields = listOf(Place.Field.LOCATION, Place.Field.NAME)
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()
        
        isLoadingSuggestions = true
        isSearchingSuggestions = false
        focusManager.clearFocus()
        keyboardController?.hide()

        client.fetchPlace(request)
            .addOnSuccessListener { response ->
                val latLng = response.place.location
                if (latLng != null) {
                    scope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
                    }
                    searchQuery = prediction.getPrimaryText(null).toString()
                    placeName = response.place.name ?: searchQuery
                    suggestions = emptyList()
                } else {
                    Toast.makeText(context, "Could not load location details", Toast.LENGTH_SHORT).show()
                }
                isLoadingSuggestions = false
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error fetching details", Toast.LENGTH_SHORT).show()
                isLoadingSuggestions = false
            }
    }

    fun saveSafetyZone() {
        val guardianId = auth.currentUser?.uid ?: return
        if (placeName.isBlank()) {
            Toast.makeText(context, "Please enter a name for this place", Toast.LENGTH_SHORT).show()
            return
        }

        isSaving = true
        val target = cameraPositionState.position.target
        scope.launch {
            try {
                // Duplicate check
                val existing = db.collection("safety_zones")
                    .whereEqualTo("childId", childId)
                    .whereEqualTo("placeName", placeName).get().await()

                if (!existing.isEmpty) {
                    Toast.makeText(context, "A zone with name \"$placeName\" already exists", Toast.LENGTH_LONG).show()
                    isSaving = false
                    return@launch
                }

                val docRef = db.collection("safety_zones").document()
                val zone = SafetyZone(
                    id = docRef.id,
                    placeName = placeName,
                    latitude = target.latitude,
                    longitude = target.longitude,
                    radius = radius.toDouble(),
                    zoneType = selectedType,
                    guardianId = guardianId,
                    childId = childId,
                    timestamp = Timestamp.now()
                )
                docRef.set(zone).await()
                Toast.makeText(context, "Safety Zone \"$placeName\" Added Successfully!", Toast.LENGTH_LONG).show()
                showBottomSheet = false
                onBack()
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
            } finally { isSaving = false }
        }
    }

    // Global Back Handler
    val isImeVisible = WindowInsets.isImeVisible
    BackHandler(enabled = isImeVisible || showBottomSheet) {
        if (isImeVisible) {
            focusManager.clearFocus()
            keyboardController?.hide()
        } else if (showBottomSheet) {
            showBottomSheet = false
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .statusBarsPadding()
            ) {
                // PART 1 — CLEAN HEADER
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
                    }
                    
                    Text(
                        text = "SafeHer",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6A3CC3)
                        )
                    )
                }

                Text(
                    text = "Add Safety Zone",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1A1A1A),
                        fontSize = 24.sp
                    )
                )

                // PART 2 — PROFESSIONAL SEARCH BAR
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color(0xFFF1F3F4),
                    border = null
                ) {
                    Column {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Search, null, tint = Color(0xFF5F6368), modifier = Modifier.size(20.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { 
                                    searchQuery = it
                                    fetchSuggestions(it)
                                },
                                placeholder = { Text("Search places or addresses", color = Color(0xFF70757A), fontSize = 15.sp) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    disabledContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (suggestions.isNotEmpty()) {
                                            onSuggestionSelected(suggestions[0])
                                        } else {
                                            searchLocation(searchQuery)
                                        }
                                    }
                                )
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { 
                                    searchQuery = ""
                                    suggestions = emptyList()
                                    isSearchingSuggestions = false
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, "Clear", tint = Color(0xFF5F6368), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                        if (isLoadingSuggestions) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(1.5.dp).padding(horizontal = 16.dp), 
                                color = Color(0xFF6A3CC3),
                                trackColor = Color.Transparent
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // PART 3 — MAP LAYOUT
            GoogleMap(
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false, compassEnabled = false, mapToolbarEnabled = false),
                properties = MapProperties(isMyLocationEnabled = false),
                onMapClick = { latLng ->
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLng(latLng)) }
                }
            ) {
                Circle(
                    center = cameraPositionState.position.target,
                    radius = radius.toDouble(),
                    fillColor = when(selectedType) {
                        "Safe" -> Color(0x334CAF50)
                        "Risky" -> Color(0x33FF9800)
                        else -> Color(0x33F44336)
                    },
                    strokeColor = when(selectedType) {
                        "Safe" -> Color(0xFF4CAF50)
                        "Risky" -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    strokeWidth = 2f
                )
            }

            // Suggestions List
            AnimatedVisibility(
                visible = isSearchingSuggestions && suggestions.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
                modifier = Modifier.padding(horizontal = 16.dp).align(Alignment.TopCenter)
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    color = Color.White
                ) {
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(suggestions) { prediction ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSuggestionSelected(prediction) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(prediction.getPrimaryText(null).toString(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                Text(prediction.getSecondaryText(null).toString(), fontSize = 13.sp, color = Color(0xFF5F6368))
                            }
                            HorizontalDivider(color = Color(0xFFF1F3F4))
                        }
                    }
                }
            }

            // Marker in center
            Box(modifier = Modifier.align(Alignment.Center).padding(bottom = 36.dp)) {
                Icon(Icons.Default.LocationOn, null, tint = Color(0xFFEA4335), modifier = Modifier.size(48.dp))
            }

            // Recenter FAB
            FloatingActionButton(
                onClick = { 
                    scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(initialLocation, 15f)) }
                },
                modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 100.dp, end = 20.dp).size(48.dp),
                containerColor = Color.White,
                contentColor = Color(0xFF6A3CC3),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Recenter", modifier = Modifier.size(20.dp))
            }

            // PART 4 — PROFESSIONAL BUTTON POSITIONING
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { 
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        showBottomSheet = true 
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(56.dp)
                        .shadow(8.dp, RoundedCornerShape(28.dp)),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A3CC3)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text("Set Zone Details", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                }
            }
        }

        // DETAILS BOTTOM SHEET
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle() },
                contentWindowInsets = { WindowInsets.ime.union(WindowInsets.navigationBars) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Text("Safety Zone Details", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF202124))
                    
                    OutlinedTextField(
                        value = placeName,
                        onValueChange = { placeName = it },
                        label = { Text("Place Name (e.g. Home, School)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedBorderColor = Color(0xFF6A3CC3),
                            focusedLabelColor = Color(0xFF6A3CC3)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { 
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        })
                    )

                    Column {
                        Text("Mark this zone as:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF3C4043))
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ZoneTypeChipDetail("Safe", selectedType == "Safe", Color(0xFF4CAF50)) { selectedType = "Safe" }
                            ZoneTypeChipDetail("Risky", selectedType == "Risky", Color(0xFFFF9800)) { selectedType = "Risky" }
                            ZoneTypeChipDetail("Unsafe", selectedType == "Unsafe", Color(0xFFF44336)) { selectedType = "Unsafe" }
                        }
                    }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Alert Radius", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF3C4043))
                            Text("${radius.toInt()} meters", fontWeight = FontWeight.ExtraBold, color = Color(0xFF6A3CC3))
                        }
                        Slider(
                            value = radius, 
                            onValueChange = { radius = it }, 
                            valueRange = 100f..1000f, 
                            colors = SliderDefaults.colors(thumbColor = Color(0xFF6A3CC3), activeTrackColor = Color(0xFF6A3CC3))
                        )
                    }

                    Button(
                        onClick = { saveSafetyZone() }, 
                        enabled = !isSaving, 
                        modifier = Modifier.fillMaxWidth().height(56.dp), 
                        shape = RoundedCornerShape(14.dp), 
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A3CC3)),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Save Safety Zone", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun ZoneTypeChipDetail(label: String, isSelected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) color else color.copy(alpha = 0.08f),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        contentColor = if (isSelected) Color.White else color
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
