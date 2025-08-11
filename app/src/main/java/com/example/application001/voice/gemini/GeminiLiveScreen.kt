package com.example.application001.voice.gemini

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.application001.BuildConfig
import kotlinx.coroutines.launch

@Composable
fun GeminiLiveScreen(
    onBackToOpenAI: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Voice state
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastResponse by remember { mutableStateOf("") }
    
    // Gemini client
    var geminiClient by remember { mutableStateOf<GeminiVoiceClient?>(null) }
    
    // Permission handling
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
        if (!isGranted) {
            errorMessage = "Microphone permission is required for voice chat"
        }
    }
    
    // Gemini connection listener
    val connectionListener = remember {
        object : GeminiVoiceListener {
            override fun onConnected() {
                isConnected = true
                isConnecting = false
                errorMessage = null
                Log.d("GeminiLiveScreen", "✅ Connected to Gemini")
            }
            
            override fun onDisconnected() {
                isConnected = false
                isConnecting = false
                Log.d("GeminiLiveScreen", "📱 Disconnected from Gemini")
            }
            
            override fun onSpeakingStarted() {
                Log.d("GeminiLiveScreen", "🗣️ Gemini started speaking")
            }
            
            override fun onSpeakingStopped() {
                Log.d("GeminiLiveScreen", "🤫 Gemini stopped speaking")
            }
            
            override fun onError(error: String) {
                errorMessage = error
                isConnecting = false
                isConnected = false
                Log.e("GeminiLiveScreen", "❌ Error: $error")
            }
        }
    }
    
    // Initialize Gemini client
    LaunchedEffect(Unit) {
        val apiKey = BuildConfig.GEMINI_API_KEY.takeIf { it.isNotBlank() }
        if (apiKey != null) {
            geminiClient = GeminiVoiceClient(context, apiKey, connectionListener)
        } else {
            errorMessage = "Gemini API key not configured"
        }
    }
    
    // Connect function
    fun connectToGemini() {
        Log.d("GeminiLiveScreen", "🎤 Connect to Gemini pressed")
        
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        scope.launch {
            isConnecting = true
            errorMessage = null
            
            val success = geminiClient?.startVoiceChat() ?: false
            if (!success) {
                errorMessage = "Failed to connect to Gemini"
                isConnecting = false
            }
        }
    }
    
    // Disconnect function
    fun disconnectFromGemini() {
        geminiClient?.stopVoiceChat()
    }
    
    // Test message function
    fun sendTestMessage() {
        scope.launch {
            try {
                val response = geminiClient?.sendTextMessage("Hello, how are you today?")
                lastResponse = response ?: "No response"
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF4285F4), // Google Blue
                        Color(0xFF34A853), // Google Green
                        Color(0xFF0F9D58)  // Darker Green
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                OutlinedButton(
                    onClick = onBackToOpenAI,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                    )
                ) {
                    Text("← Back to OpenAI")
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Title
            Text(
                text = "🤖 Gemini Live",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Chat with Google's Gemini AI!",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 30.dp)
            )
            
            // Gemini Face
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4285F4),
                                Color(0xFF1A73E8)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🤖",
                    fontSize = 80.sp,
                    color = Color.White
                )
                
                // Connection indicator
                if (isConnected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset((-10).dp, 10.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF48BB78))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(30.dp))
            
            // Status
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = when {
                        isConnecting -> "🔄 Connecting to Gemini..."
                        isConnected -> "✅ Connected to Gemini"
                        else -> "💤 Disconnected"
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Error message
            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE53E3E).copy(alpha = 0.9f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "❌ $error",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        
                        TextButton(
                            onClick = { errorMessage = null },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Text("Dismiss")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            // Last response
            if (lastResponse.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Last Response:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = lastResponse,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Main Action Button
            Button(
                onClick = {
                    if (isConnected) {
                        disconnectFromGemini()
                    } else {
                        connectToGemini()
                    }
                },
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFFE53E3E) else Color(0xFF4285F4)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isConnecting) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Connecting...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                } else {
                    Text(
                        text = if (isConnected) "🔴 Stop Gemini Chat" else "🎤 Start Gemini Chat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
            
            // Test button when connected
            if (isConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = { sendTestMessage() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                    )
                ) {
                    Text("💬 Send Test Message")
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
} 