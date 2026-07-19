package com.sneha.safeherapp.ui.sos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneha.safeherapp.ui.theme.SafeHerAppTheme
import com.sneha.safeherapp.utils.SosManager
import kotlinx.coroutines.delay

class SosCountdownActivity : ComponentActivity() {
    private val TAG = "SosTrigger"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure the activity is shown over the lock screen if needed
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        setContent {
            SafeHerAppTheme {
                SosCountdownScreen(
                    onCancel = {
                        Log.d(TAG, "Countdown Cancelled")
                        SosManager.cancelSos()
                        finish()
                    },
                    onSendNow = {
                        Log.d(TAG, "Countdown Completed") // Immediate send counts as completion
                        SosManager.triggerSos(this)
                        finish()
                    },
                    onTimeout = {
                        Log.d(TAG, "Countdown Completed")
                        SosManager.triggerSos(this)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SosCountdownScreen(
    onCancel: () -> Unit,
    onSendNow: () -> Unit,
    onTimeout: () -> Unit
) {
    var ticks by remember { mutableIntStateOf(3) }

    LaunchedEffect(Unit) {
        while (ticks > 0) {
            delay(1000)
            ticks--
        }
        onTimeout()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFD32F2F) // Red background for emergency
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🚨 Emergency SOS",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Sending emergency alert...",
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Large Countdown Number
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ticks.toString(),
                    fontSize = 80.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(80.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Text("Cancel", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSendNow,
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFD32F2F))
                ) {
                    Text("Send Now", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
