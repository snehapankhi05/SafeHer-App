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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
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
import com.sneha.safeherapp.util.FakeCallPrefs
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun FakeCallScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val activeProfile = remember { FakeCallPrefs.getActiveProfile(context) }
    
    val callerName = activeProfile?.name ?: "Mom"
    val audioPath = activeProfile?.audioPath
    
    var isCallAccepted by remember { mutableStateOf(false) }
    var callTimer by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    
    // MediaPlayer for Ringtone
    val ringtonePlayer = remember {
        val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        MediaPlayer.create(context, notification)?.apply { isLooping = true }
    }

    // MediaPlayers for Voice Conversation
    var voicePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // Ringing and Vibration Effect
    LaunchedEffect(Unit) {
        delay(500) // Reduced delay for almost immediate trigger
        if (!isCallAccepted) {
            ringtonePlayer?.start()
            val pattern = longArrayOf(0, 1000, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, 0)
            }
        }
    }

    // Timer and Voice Logic
    LaunchedEffect(isCallAccepted) {
        if (isCallAccepted) {
            try {
                if (audioPath != null && File(audioPath).exists()) {
                    voicePlayer = MediaPlayer().apply {
                        setDataSource(audioPath)
                        prepare()
                        start()
                    }
                } else {
                    val resId = context.resources.getIdentifier("fake_call_hello", "raw", context.packageName)
                    if (resId != 0) {
                        voicePlayer = MediaPlayer.create(context, resId)
                        voicePlayer?.start()
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }

            while (true) {
                delay(1000)
                callTimer++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ringtonePlayer?.stop()
            ringtonePlayer?.release()
            voicePlayer?.stop()
            voicePlayer?.release()
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
                    text = callerName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isCallAccepted) formatTime(callTimer) else "Incoming Call...",
                    fontSize = 18.sp,
                    color = if (isCallAccepted) Color.Green else Color.Gray
                )
            }

            if (!isCallAccepted) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LargeIconButton(
                            icon = Icons.Default.CallEnd,
                            backgroundColor = Color.Red,
                            onClick = onBack
                        )
                        Text("Decline", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LargeIconButton(
                            icon = Icons.Default.Call,
                            backgroundColor = Color.Green,
                            onClick = {
                                isCallAccepted = true
                                ringtonePlayer?.stop()
                                vibrator.cancel()
                            }
                        )
                        Text("Accept", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 60.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SmallIconButton(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                onClick = { isMuted = !isMuted }
                            )
                            Text("Mute", color = Color.White, fontSize = 12.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SmallIconButton(icon = Icons.Default.Call, onClick = {})
                            Text("Keypad", color = Color.White, fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))

                    LargeIconButton(
                        icon = Icons.Default.CallEnd,
                        backgroundColor = Color.Red,
                        onClick = onBack
                    )
                    Text("End Call", color = Color.White, modifier = Modifier.padding(top = 8.dp))
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

@Composable
fun SmallIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(56.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White.copy(alpha = 0.1f))
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White)
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
