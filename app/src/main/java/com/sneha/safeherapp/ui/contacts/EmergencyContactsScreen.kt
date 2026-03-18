package com.sneha.safeherapp.ui.contacts

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink

data class PredefinedContact(
    val name: String,
    val number: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val contacts = listOf(
        PredefinedContact("Police", "100", Icons.Default.LocalPolice),
        PredefinedContact("Ambulance", "108", Icons.Default.LocalHospital),
        PredefinedContact("Fire Brigade", "101", Icons.Default.FireTruck),
        PredefinedContact("National Emergency", "112", Icons.Default.Emergency),
        PredefinedContact("Women Helpline", "1091", Icons.Default.SupportAgent),
        PredefinedContact("Domestic Violence", "181", Icons.Default.GppBad),
        PredefinedContact("Women Distress", "1090", Icons.Default.RecordVoiceOver),
        PredefinedContact("Child Helpline", "1098", Icons.Default.ChildCare),
        PredefinedContact("Mental Health Helpline", "9152987821", Icons.Default.Psychology),
        PredefinedContact("Traffic Police", "103", Icons.Default.Traffic),
        PredefinedContact("Animal Helpline", "9820122602", Icons.Default.Pets)
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Emergency Contacts",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6A3CC3)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF6A3CC3)
                        )
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
                    .padding(24.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        items(contacts) { contact ->
                            EmergencyContactRow(contact) {
                                makeCall(context, contact.number)
                            }
                            if (contact != contacts.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color.LightGray.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    "Note: Tapping a contact will open your dialer. Stay safe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
fun EmergencyContactRow(
    contact: PredefinedContact,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = LightPurple.copy(alpha = 0.5f)
            ) {
                Icon(
                    imageVector = contact.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = Color(0xFF6A3CC3)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = contact.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = contact.number,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Call",
            tint = Color(0xFF6A3CC3),
            modifier = Modifier.size(24.dp)
        )
    }
}

fun makeCall(context: Context, number: String) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$number")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
    }
}
