package com.sneha.safeherapp.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.sneha.safeherapp.viewmodel.MapViewModel
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    category: String?,
    onBack: () -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var showReportDialog by remember { mutableStateOf(false) }
    var isNearDanger by remember { mutableStateOf(false) }
    
    val AHMEDABAD = LatLng(23.0225, 72.5714)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(AHMEDABAD, 12f)
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    userLocation = userLatLng
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(userLatLng, 15f),
                        durationMs = 1000
                    )
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Failed to process location", e)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Proximity warning check
    LaunchedEffect(userLocation, viewModel.dangerReports.size) {
        userLocation?.let { user ->
            val nearby = viewModel.dangerReports.any { report ->
                val results = FloatArray(1)
                Location.distanceBetween(
                    user.latitude, user.longitude,
                    report.location.latitude, report.location.longitude,
                    results
                )
                results[0] < 200 // 200 meters
            }
            isNearDanger = nearby
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SafeHer Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showReportDialog = true },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) {
                Icon(Icons.Default.Warning, contentDescription = "Report")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                userLocation?.let {
                    Marker(state = MarkerState(position = it), title = "You are here")
                }

                viewModel.dangerReports.forEach { report ->
                    Marker(
                        state = MarkerState(position = LatLng(report.location.latitude, report.location.longitude)),
                        title = report.reason,
                        snippet = report.description,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }

            if (isNearDanger) {
                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "⚠️ This area has been reported unsafe",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (showReportDialog) {
                DangerReportDialog(
                    onDismiss = { showReportDialog = false },
                    onSubmit = { reason, desc ->
                        userLocation?.let {
                            viewModel.reportDanger(it.latitude, it.longitude, reason, desc)
                            Toast.makeText(context, "Report Submitted", Toast.LENGTH_SHORT).show()
                        }
                        showReportDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun DangerReportDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var selectedReason by remember { mutableStateOf("Harassment") }
    var description by remember { mutableStateOf("") }
    val reasons = listOf("Harassment", "Isolated Area", "Suspicious Activity", "Stalking")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Unsafe Area") },
        text = {
            Column {
                Text("Select Reason:", style = MaterialTheme.typography.labelLarge)
                Column(Modifier.selectableGroup()) {
                    reasons.forEach { text ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .selectable(
                                    selected = (text == selectedReason),
                                    onClick = { selectedReason = text },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (text == selectedReason), onClick = null)
                            Text(text = text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Optional Details") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, description) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
