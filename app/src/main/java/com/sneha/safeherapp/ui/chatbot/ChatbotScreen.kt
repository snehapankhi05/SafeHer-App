package com.sneha.safeherapp.ui.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneha.safeherapp.ui.theme.LightPurple
import com.sneha.safeherapp.ui.theme.SoftPink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class Message(
    val text: String,
    val isUser: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatbotScreen(onBack: () -> Unit) {
    var inputText by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(Message("Hello! I'm your SafeHer Assistant. How can I help you today?", false)) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val gradient = Brush.verticalGradient(
        colors = listOf(SoftPink, LightPurple, Color.White)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Assistant", fontWeight = FontWeight.Bold, color = Color.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
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
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    state = listState,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages) { message ->
                        ChatBubble(message)
                    }
                }

                // Input Area
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type your message...") },
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF6A3CC3),
                                unfocusedBorderColor = Color.LightGray
                            ),
                            maxLines = 3
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val userMsg = inputText
                                    messages.add(Message(userMsg, true))
                                    inputText = ""
                                    
                                    scope.launch {
                                        listState.animateScrollToItem(messages.size - 1)
                                        delay(500)
                                        val response = getBotResponse(userMsg)
                                        messages.add(Message(response, false))
                                        listState.animateScrollToItem(messages.size - 1)
                                    }
                                }
                            },
                            containerColor = Color(0xFF6A3CC3),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) Color(0xFF6A3CC3) else Color.White,
            contentColor = if (message.isUser) Color.White else Color.Black,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            ),
            tonalElevation = 2.dp,
            shadowElevation = 1.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                fontSize = 15.sp
            )
        }
    }
}

fun getBotResponse(input: String): String {
    val lowerInput = input.lowercase()
    return when {
        lowerInput.contains("sos") -> "Press the SOS button on the home screen to instantly send your location and an alert to your emergency contacts."
        lowerInput.contains("unsafe") || lowerInput.contains("danger") -> "Stay calm. If you feel unsafe, move to a crowded, well-lit area. Use the SOS feature immediately if needed."
        lowerInput.contains("fake call") -> "You can use the Fake Call feature to simulate an incoming call, giving you a reason to leave uncomfortable situations."
        lowerInput.contains("help") -> "I can help with safety information. Try asking about 'SOS', 'Fake Call', or 'Safety Tips'."
        lowerInput.contains("tip") -> "Always keep your phone charged, share your live location with trusted people, and stay aware of your surroundings."
        lowerInput.contains("contact") -> "You can manage your trusted contacts on the Home screen. These people will be notified during an SOS."
        lowerInput.contains("hi") || lowerInput.contains("hello") -> "Hello! How can I assist you with your safety today?"
        else -> "I'm here to help with your safety. You can ask about SOS, Fake Call, or general safety tips."
    }
}
