package com.sneha.safeherapp.ui.map

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink

@Composable
fun MapLandingScreen(
    onOpenMap: () -> Unit,
    onViewReports: () -> Unit
) {
    val context = LocalContext.current
    val gradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    val darkHeading = Color(0xFF111111)
    val darkText = Color(0xFF333333)
    val primaryPurple = Color(0xFF6A3CC3)

    val nearbyServices = listOf(
        NearbyServiceItem("Hospital", Icons.Default.LocalHospital, "hospital"),
        NearbyServiceItem("Police", Icons.Default.LocalPolice, "police station"),
        NearbyServiceItem("Fire", Icons.Default.FireTruck, "fire station"),
        NearbyServiceItem("Bus", Icons.Default.DirectionsBus, "bus stop"),
        NearbyServiceItem("Metro", Icons.Default.Subway, "metro station"),
        NearbyServiceItem("Pharmacy", Icons.Default.LocalPharmacy, "pharmacy")
    )

    val openGoogleMaps = { query: String ->
        val gmmIntentUri = Uri.parse("geo:0,0?q=$query near me")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/$query+near+me"))
            context.startActivity(browserIntent)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding()
        ) {
            // Top Section
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(
                    text = "Safety Map",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = darkHeading
                )
                Text(
                    text = "Explore safe and unsafe areas around you",
                    fontSize = 16.sp,
                    color = darkText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 1: Open Live Map
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable { onOpenMap() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEDE7F6)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.padding(14.dp).size(28.dp),
                            tint = primaryPurple
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(20.dp))
                    
                    Column {
                        Text(
                            text = "Open Safety Map",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkHeading
                        )
                        Text(
                            text = "Real-time safety zones",
                            fontSize = 14.sp,
                            color = darkText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section 2: View Community Reports
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clickable { onViewReports() },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ListAlt,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = primaryPurple
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "Community Reports",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = darkHeading
                        )
                        Text(
                            text = "See alerts from others",
                            fontSize = 13.sp,
                            color = darkText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Section 3: Nearby Services Grid
            Text(
                text = "Nearby Services",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = darkHeading,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(nearbyServices) { service ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable { openGoogleMaps(service.query) },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = service.icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = primaryPurple
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = service.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = darkText
                            )
                        }
                    }
                }
            }
        }
    }
}

data class NearbyServiceItem(
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val query: String
)
