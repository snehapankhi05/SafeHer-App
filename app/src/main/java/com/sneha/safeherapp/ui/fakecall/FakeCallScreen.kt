package com.sneha.safeherapp.ui.fakecall

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FakeCallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var callStatus by remember { mutableStateOf("Incoming Call...") }
    var isCallAccepted by remember { mutableStateOf(false) }
    
    val mediaPlayer = remember {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        MediaPlayer.create(context, notification)
    }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    LaunchedEffect(Unit) {
        delay(3000) // 3 seconds delay before "ringing" starts
        if (!isCallAccepted) {
            mediaPlayer?.isLooping = true
            mediaPlayer?.start()
            
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            vibrator.cancel()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 80.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = Color.DarkGray
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(24.dp).size(60.dp),
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Mom",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = callStatus,
                    fontSize = 18.sp,
                    color = Color.Gray
                )
            }

            if (!isCallAccepted) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Decline Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LargeIconButton(
                            icon = Icons.Default.CallEnd,
                            backgroundColor = Color.Red,
                            onClick = onBack
                        )
                        Text("Decline", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    }

                    // Accept Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LargeIconButton(
                            icon = Icons.Default.Call,
                            backgroundColor = Color.Green,
                            onClick = {
                                isCallAccepted = true
                                callStatus = "00:01"
                                mediaPlayer?.stop()
                                vibrator.cancel()
                            }
                        )
                        Text("Accept", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                // Ongoing Fake Call UI
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "\"Hi, where are you? I've been waiting for you. Let's meet up soon.\"",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 40.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(60.dp))
                    
                    LargeIconButton(
                        icon = Icons.Default.CallEnd,
                        backgroundColor = Color.Red,
                        onClick = onBack
                    )
                }
            }
        }
    }
}

@Composable
fun LargeIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = backgroundColor)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = Color.White
        )
    }
}
