package com.sneha.safeherapp.ui.home

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChildScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val primaryPurple = Color(0xFF6A3CC3)
    val softLavender = Color(0xFFFDF7FF)
    val selectionPurple = Color(0xFFF3E8FF)
    val pendingOrange = Color(0xFFFFA500)
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val TAG = "SafeHerFirestore"
    val scope = rememberCoroutineScope()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    var childName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }

    val relationships = listOf("Daughter", "Son", "Sister", "Brother", "Friend", "Other")
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = primaryPurple,
        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f),
        focusedLabelColor = primaryPurple,
        unfocusedLabelColor = primaryPurple.copy(alpha = 0.7f),
        cursorColor = primaryPurple,
        focusedTextColor = Color(0xFF1A1A1A),
        unfocusedTextColor = Color(0xFF1A1A1A),
        focusedPlaceholderColor = Color.Gray.copy(alpha = 0.6f),
        unfocusedPlaceholderColor = Color.Gray.copy(alpha = 0.6f)
    )

    val professionalInvite = """
        🛡️ Join my SafeHer Safety Circle

        You have been invited to connect on SafeHer for live safety tracking, emergency alerts, and location sharing.

        Invite Code: $generatedCode

        Steps to Join:
        1. Open SafeHer app
        2. Tap 'Join Family'
        3. Enter the invite code above

        Stay safe with SafeHer 💜
    """.trimIndent()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Add Child",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = primaryPurple
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryPurple
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.background(backgroundGradient)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 1. Intro Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Connect Your Child",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add your child to receive safety alerts, live location updates, and emergency notifications.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.DarkGray.copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 2. FORM CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = childName,
                        onValueChange = { childName = it },
                        label = { Text("Child Name") },
                        placeholder = { Text("Enter child name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF1A1A1A), fontSize = 16.sp),
                        colors = textFieldColors
                    )

                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("Enter phone number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF1A1A1A), fontSize = 16.sp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = textFieldColors
                    )

                    ExposedDropdownMenuBox(
                        expanded = isExpanded,
                        onExpandedChange = { isExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = relationship,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Relationship") },
                            placeholder = { Text("Select relationship") },
                            trailingIcon = { 
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = primaryPurple
                                )
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = TextStyle(color = Color(0xFF1A1A1A), fontSize = 16.sp),
                            colors = textFieldColors
                        )
                        
                        MaterialTheme(
                            colorScheme = MaterialTheme.colorScheme.copy(surface = softLavender),
                            shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(16.dp))
                        ) {
                            ExposedDropdownMenu(
                                expanded = isExpanded,
                                onDismissRequest = { isExpanded = false },
                                modifier = Modifier.background(softLavender)
                            ) {
                                relationships.forEach { option ->
                                    val isSelected = relationship == option
                                    DropdownMenuItem(
                                        text = { 
                                            Text(
                                                text = option,
                                                color = if (isSelected) primaryPurple else Color.Black,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ) 
                                        },
                                        onClick = {
                                            relationship = option
                                            isExpanded = false
                                        },
                                        modifier = Modifier.background(if (isSelected) selectionPurple else Color.Transparent)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (!showSuccess) {
                Button(
                    onClick = { 
                        when {
                            childName.isBlank() -> Toast.makeText(context, "Please enter child name", Toast.LENGTH_SHORT).show()
                            phoneNumber.length < 10 -> Toast.makeText(context, "Please enter a valid phone number", Toast.LENGTH_SHORT).show()
                            relationship.isBlank() -> Toast.makeText(context, "Please select relationship", Toast.LENGTH_SHORT).show()
                            else -> {
                                val uid = auth.currentUser?.uid
                                if (uid != null) {
                                    scope.launch {
                                        isGenerating = true
                                        try {
                                            // Fetch guardian details from Firestore
                                            Log.d(TAG, "Fetching guardian details for UID: $uid")
                                            val guardianDoc = db.collection("users").document(uid).get().await()
                                            val guardianName = guardianDoc.getString("fullName") ?: "Unknown"
                                            val guardianEmail = guardianDoc.getString("email") ?: ""
                                            
                                            val code = "SH-${(10000..99999).random()}"
                                            
                                            // Create invite document
                                            val inviteData = hashMapOf(
                                                "inviteCode" to code,
                                                "guardianUid" to uid,
                                                "guardianName" to guardianName,
                                                "guardianEmail" to guardianEmail,
                                                "childName" to childName,
                                                "childPhone" to phoneNumber,
                                                "relationship" to relationship,
                                                "createdAt" to Timestamp.now(),
                                                "status" to "pending"
                                            )
                                            
                                            db.collection("guardian_invites").document(code).set(inviteData).await()
                                            
                                            generatedCode = code
                                            showSuccess = true
                                            Log.d(TAG, "Guardian invite generated successfully: $inviteData")
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error generating invite code", e)
                                            Toast.makeText(context, "Failed to generate invite code", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isGenerating = false
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Authentication error", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryPurple,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    enabled = !isGenerating
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.VpnKey, null, Modifier.size(20.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Generate Invite Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            if (showSuccess) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = softLavender),
                    border = BorderStroke(1.dp, primaryPurple.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Invite Code Generated",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = primaryPurple
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = generatedCode,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryPurple,
                                modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                                textAlign = TextAlign.Center,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Share this code with your child to securely connect accounts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedButton(
                                onClick = { 
                                    clipboardManager.setText(AnnotatedString(generatedCode))
                                    Toast.makeText(context, "Invite code copied", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryPurple),
                                border = BorderStroke(1.dp, primaryPurple)
                            ) {
                                Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp), tint = primaryPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy", color = primaryPurple)
                            }
                            OutlinedButton(
                                onClick = { 
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, professionalInvite)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Share Invite Code"))
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryPurple),
                                border = BorderStroke(1.dp, primaryPurple)
                            ) {
                                Icon(Icons.Default.Share, null, Modifier.size(18.dp), tint = primaryPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Share", color = primaryPurple)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                val url = "https://api.whatsapp.com/send?text=${Uri.encode(professionalInvite)}"
                                val i = Intent(Intent.ACTION_VIEW)
                                i.data = Uri.parse(url)
                                try {
                                    context.startActivity(i)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Icon(Icons.Default.Send, null, Modifier.size(20.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Share via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = primaryPurple.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.padding(12.dp),
                                tint = primaryPurple
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = childName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "$relationship • $phoneNumber",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(pendingOrange, RoundedCornerShape(4.dp))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pending Connection",
                                    fontSize = 12.sp,
                                    color = pendingOrange,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { 
                        val uid = auth.currentUser?.uid
                        if (uid != null) {
                            Log.d(TAG, "Attempting to save child for guardian UID: $uid")
                            val childId = UUID.randomUUID().toString()
                            val childData = hashMapOf(
                                "id" to childId,
                                "name" to childName,
                                "phone" to phoneNumber,
                                "relationship" to relationship,
                                "inviteCode" to generatedCode,
                                "status" to "Pending Connection",
                                "createdAt" to Timestamp.now()
                            )

                            db.collection("guardian_users").document(uid)
                                .collection("children").document(childId)
                                .set(childData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Firestore save success: $childId")
                                    Toast.makeText(context, "Child added successfully", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Firestore save failure", e)
                                    Toast.makeText(context, "Error adding child: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Log.e(TAG, "Auth user UID is null")
                            Toast.makeText(context, "Authentication error. Please login again.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryPurple,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "Finish & Add Child",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
