package com.sneha.safeherapp.ui.home

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun GuardianHomeScreen(
    onChildClick: (Child) -> Unit = {},
    onAddChildClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val primaryPurple = Color(0xFF6A3CC3)
    val TAG = "SafeHerFirestore"
    
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUser = auth.currentUser
    
    val children = remember { mutableStateListOf<Child>() }
    var isLoading by remember { mutableStateOf(true) }

    // Security State: Track pending connection dialog
    var showPendingDialog by remember { mutableStateOf(false) }
    var selectedChildName by remember { mutableStateOf("") }

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            val uid = currentUser.uid
            Log.d(TAG, "Fetching children for Guardian UID: $uid")
            
            val listenerRegistration = db.collection("guardian_users")
                .document(uid)
                .collection("children")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Listen failed: ", error)
                        Toast.makeText(context, "Failed to load children", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val childrenList = snapshot.toObjects(Child::class.java)
                        children.clear()
                        children.addAll(childrenList)
                        Log.d(TAG, "Snapshot listener update: ${childrenList.size} children loaded")
                        isLoading = false
                    }
                }
        } else {
            Log.e(TAG, "No authenticated user found in GuardianHomeScreen")
            isLoading = false
        }
    }

    // Connection Pending Dialog
    if (showPendingDialog) {
        AlertDialog(
            onDismissRequest = { showPendingDialog = false },
            title = { 
                Text(
                    text = "Connection Pending", 
                    fontWeight = FontWeight.Bold,
                    color = primaryPurple
                ) 
            },
            text = { 
                Text(
                    text = "This child has not accepted your connection request yet. You will be able to track them once they approve.",
                    color = Color.Black
                ) 
            },
            confirmButton = {
                TextButton(onClick = { showPendingDialog = false }) {
                    Text("OK", color = primaryPurple, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ADD CHILD BUTTON (Always visible)
        Button(
            onClick = { onAddChildClick() },
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = primaryPurple,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add Child",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your Children",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = primaryPurple)
            }
        } else if (children.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = primaryPurple.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "No children added yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Add your child to track safety alerts, live location, and emergency activity.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            children.forEach { child ->
                ChildItem(
                    child = child,
                    onClick = {
                        // SECURITY: Check connection status before allowing navigation
                        if (child.status == "Pending Connection") {
                            selectedChildName = child.name
                            showPendingDialog = true
                        } else {
                            onChildClick(child)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildItem(child: Child, onClick: (Child) -> Unit) {
    val primaryPurple = Color(0xFF6A3CC3)
    val pendingOrange = Color(0xFFFFA500)
    val connectedGreen = Color(0xFF4CAF50)
    val isPending = child.status == "Pending Connection"
    val isAccepted = child.status == "accepted"

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onClick(child) },
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = child.name, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 17.sp, 
                    color = Color.Black
                )
                Text(
                    text = "${child.relationship} • ${child.phone}", 
                    fontSize = 13.sp, 
                    color = Color(0xFF444444)
                )
                
                if (child.inviteCode.isNotEmpty() && isPending) {
                    Text(
                        text = "Invite Code: ${child.inviteCode}",
                        fontSize = 11.sp,
                        color = primaryPurple.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (isPending) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(pendingOrange, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Waiting for approval",
                            fontSize = 12.sp,
                            color = pendingOrange,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (isAccepted) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(connectedGreen, RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Connected",
                            fontSize = 12.sp,
                            color = connectedGreen,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            Icon(
                imageVector = if (isPending) Icons.Default.Lock else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null, 
                tint = Color.LightGray
            )
        }
    }
}
