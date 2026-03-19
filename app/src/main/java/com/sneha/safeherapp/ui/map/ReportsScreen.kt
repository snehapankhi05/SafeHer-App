package com.sneha.safeherapp.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    onReportClick: (SafetyReport) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    var reports by remember { mutableStateOf(listOf<SafetyReport>()) }
    var isLoading by remember { mutableStateOf(true) }

    val gradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    LaunchedEffect(Unit) {
        db.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                reports = snapshot?.documents?.map { doc ->
                    SafetyReport(
                        id = doc.id,
                        latitude = doc.getDouble("latitude") ?: 0.0,
                        longitude = doc.getDouble("longitude") ?: 0.0,
                        reason = doc.getString("reason") ?: "",
                        alertLevel = doc.getString("alertLevel") ?: "LOW",
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        userId = doc.getString("userId") ?: ""
                    )
                } ?: emptyList()
                isLoading = false
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Safety Reports", fontWeight = FontWeight.Bold, color = Color(0xFF111111)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF111111))
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF4A2E8E))
            } else if (reports.isEmpty()) {
                Text(
                    text = "No reports found nearby.",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF444444)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reports) { report ->
                        ReportItem(report = report, onClick = { onReportClick(report) })
                    }
                }
            }
        }
    }
}

@Composable
fun ReportItem(report: SafetyReport, onClick: () -> Unit) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateString = sdf.format(Date(report.timestamp))

    val alertColor = when(report.alertLevel) {
        "HIGH" -> Color(0xFFD32F2F)
        "MEDIUM" -> Color(0xFFFF9100)
        else -> Color(0xFFFFD600)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = alertColor.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = alertColor,
                    modifier = Modifier.padding(12.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = report.reason,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF111111),
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = alertColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = report.alertLevel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = alertColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = dateString,
                    fontSize = 12.sp,
                    color = Color(0xFF444444)
                )
                Text(
                    text = String.format(Locale.getDefault(), "Location: %.4f, %.4f", report.latitude, report.longitude),
                    fontSize = 12.sp,
                    color = Color(0xFF4A2E8E)
                )
            }
        }
    }
}
