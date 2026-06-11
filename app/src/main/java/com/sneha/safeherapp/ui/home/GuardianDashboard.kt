package com.sneha.safeherapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink

enum class ChildStatus(val label: String, val color: Color) {
    SAFE("🟢 Safe", Color(0xFF4CAF50)),
    UNSAFE("🔴 In Unsafe Area", Color(0xFFF44336)),
    MOVING("🚶 On the way", Color(0xFFFFC107)),
    UNKNOWN("❓ Unknown", Color(0xFF9E9E9E))
}

data class ChildData(
    val id: String,
    val name: String,
    val location: String,
    val status: ChildStatus,
    val battery: Int,
    val lastUpdated: String
)

data class ActiveAlert(
    val message: String,
    val time: String,
    val isCritical: Boolean
)

@Composable
fun GuardianDashboard(
    onChildClick: (String) -> Unit
) {
    val gradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    // Dummy Data
    val children = listOf(
        ChildData("1", "Sneha", "At Home", ChildStatus.SAFE, 62, "2 min ago"),
        ChildData("2", "Khush", "Satellite Road", ChildStatus.UNSAFE, 15, "just now")
    )

    val activeAlerts = listOf(
        ActiveAlert("Khush left School", "2 min ago", true)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // Alert Priority Section
            item {
                AlertSection(alerts = activeAlerts)
            }

            item {
                Text(
                    "Your Children",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF333333),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Child List Section
            items(children) { child ->
                ChildCard(child = child, onClick = { onChildClick(child.id) })
            }
        }
    }
}

@Composable
fun AlertSection(alerts: List<ActiveAlert>) {
    if (alerts.isEmpty()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.5f)
        ) {
            Text(
                "No alerts — everything is safe",
                modifier = Modifier.padding(12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    } else {
        val latestAlert = alerts.first()
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (latestAlert.isCritical) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (latestAlert.isCritical) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (latestAlert.isCritical) Color(0xFFD32F2F) else Color(0xFFF57F17),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${latestAlert.message} • ${latestAlert.time}",
                        fontWeight = FontWeight.Bold,
                        color = if (latestAlert.isCritical) Color(0xFFD32F2F) else Color(0xFFE65100),
                        fontSize = 14.sp
                    )
                }
                TextButton(
                    onClick = { /* View All Alerts */ },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp).padding(top = 2.dp)
                ) {
                    Text(
                        text = "View all alerts →", 
                        color = Color(0xFF6A3CC3), 
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ChildCard(child: ChildData, onClick: () -> Unit) {
    val isUnsafe = child.status == ChildStatus.UNSAFE
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isUnsafe) {
                    Modifier.drawBehind {
                        drawLine(
                            color = Color(0xFFF44336),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 10f
                        )
                    }
                } else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnsafe) Color(0xFFFFF5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = child.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF111111)
                )
                
                val batteryColor = when {
                    child.battery >= 50 -> Color(0xFF4CAF50)
                    child.battery >= 20 -> Color(0xFFFFC107)
                    else -> Color(0xFFF44336)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getBatteryIcon(child.battery),
                        contentDescription = null,
                        tint = batteryColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Battery: ${child.battery}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = child.location,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF444444)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = child.status.label,
                    fontWeight = FontWeight.Bold,
                    color = child.status.color,
                    fontSize = 14.sp
                )
                
                Text(
                    text = "Updated ${child.lastUpdated}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onClick,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A3CC3)),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("View Location", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledTonalIconButton(
                        onClick = { /* Call Action */ },
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFEDE7F6))
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", tint = Color(0xFF6A3CC3), modifier = Modifier.size(20.dp))
                    }
                    Text("📞 Call", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A3CC3))
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledTonalIconButton(
                        onClick = { /* Track Action */ },
                        modifier = Modifier.size(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color(0xFFEDE7F6))
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Track Now", tint = Color(0xFF6A3CC3), modifier = Modifier.size(20.dp))
                    }
                    Text("📍 Track", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6A3CC3))
                }
            }
        }
    }
}

private fun getBatteryIcon(level: Int): ImageVector {
    return when {
        level >= 80 -> Icons.Default.BatteryFull
        level >= 50 -> Icons.Default.Battery5Bar
        level >= 20 -> Icons.Default.Battery2Bar
        else -> Icons.Default.BatteryAlert
    }
}
