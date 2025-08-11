package com.example.application001.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.application001.BuildConfig
import com.example.application001.data.SettingsManager
import kotlinx.coroutines.launch

@Composable
fun LiveVoiceScreen(
    onNavigateToGemini: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsManager = remember { SettingsManager(context) }
    
    // Voice state
    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showInstructions by remember { mutableStateOf(true) }
    var isSpeakerMode by remember { mutableStateOf(true) } // Default to speaker mode
    var showDebugDialog by remember { mutableStateOf(false) }
    
    // Settings
    var currentPersonality by remember { mutableStateOf("") }
    var companionName by remember { mutableStateOf("Smol Uni") }
    
    // WebRTC Manager
    var webRTCManager by remember { mutableStateOf<WebRTCManager?>(null) }
    
    // Audio debug helper
    val audioDebugHelper = remember { AudioDebugHelper(context) }
    
    // Speaker mode control functions
    fun setSpeakerMode(enabled: Boolean) {
        try {
            Log.d("LiveVoiceScreen", "🔊 Setting speaker mode through WebRTC: $enabled")
            webRTCManager?.setSpeakerMode(enabled)
            isSpeakerMode = enabled
            
            // Log current audio state
            webRTCManager?.getCurrentAudioState()?.let { state ->
                Log.d("LiveVoiceScreen", "🔊 Audio state after change: $state")
            }
        } catch (e: Exception) {
            Log.e("LiveVoiceScreen", "❌ Failed to set speaker mode", e)
        }
    }
    
    fun toggleSpeakerMode() {
        setSpeakerMode(!isSpeakerMode)
    }
    
    // Load settings
    LaunchedEffect(Unit) {
        settingsManager.companionPersonality.collect { personality ->
            currentPersonality = personality
        }
    }
    
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
    
    // WebRTC connection listener
    val connectionListener = remember {
        object : VoiceConnectionListener {
            override fun onConnected() {
                isConnected = true
                isConnecting = false
                showInstructions = false
                errorMessage = null
                // Enable speaker mode by default when connected with a slight delay
                // to ensure WebRTC has finished its audio setup
                scope.launch {
                    kotlinx.coroutines.delay(1000) // Wait 1 second for WebRTC to fully settle
                    setSpeakerMode(true)
                    Log.d("LiveVoiceScreen", "🔊 Speaker mode enabled after connection")
                }
            }
            
            override fun onDisconnected() {
                isConnected = false
                isConnecting = false
                isSpeaking = false
                showInstructions = true
                // Audio cleanup is handled by WebRTCManager
                Log.d("LiveVoiceScreen", "📱 Voice chat disconnected")
            }
            
            override fun onSpeakingStarted() {
                isSpeaking = true
                // Ensure speaker mode is active when AI starts speaking
                if (isSpeakerMode) {
                    Log.d("LiveVoiceScreen", "🗣️ AI started speaking, re-enforcing speaker mode")
                    setSpeakerMode(true)
                }
            }
            
            override fun onSpeakingStopped() {
                isSpeaking = false
            }
            
            override fun onError(error: String) {
                errorMessage = error
                isConnecting = false
                isConnected = false
            }
            
            override fun onAudioReceived() {
                // Audio stream received - ensure speaker mode is still active
                Log.d("LiveVoiceScreen", "🎵 Audio received from OpenAI")
                if (isConnected && isSpeakerMode) {
                    // Re-enforce speaker mode when audio is received
                    scope.launch {
                        kotlinx.coroutines.delay(200) // Small delay to let audio start
                        setSpeakerMode(true)
                        Log.d("LiveVoiceScreen", "🔊 Re-enforced speaker mode for incoming audio")
                    }
                }
            }
        }
    }
    
    // Initialize WebRTC manager
    LaunchedEffect(Unit) {
        webRTCManager = WebRTCManager(context, connectionListener)
    }
    
    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            webRTCManager?.dispose()
        }
    }
    
    // Connect function
    fun connectToVoiceChat() {
        Log.d("LiveVoiceScreen", "🎤 Connect button pressed")
        
        if (!hasAudioPermission) {
            Log.d("LiveVoiceScreen", "❌ No audio permission, requesting...")
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        
        Log.d("LiveVoiceScreen", "✅ Audio permission granted, starting connection...")
        
        scope.launch {
            isConnecting = true
            errorMessage = null
            
            try {
                Log.d("LiveVoiceScreen", "🔑 Getting API key...")
                // Get API key
                val apiKey = BuildConfig.OPENAI_API_KEY.takeIf { it.isNotBlank() }
                    ?: run {
                        Log.e("LiveVoiceScreen", "❌ API key not configured")
                        errorMessage = "API key not configured"
                        isConnecting = false
                        return@launch
                    }
                
                Log.d("LiveVoiceScreen", "✅ API key found, creating session...")
                // Create session
                val voiceClient = VoiceClient(apiKey)
                val sessionResult = voiceClient.createRealtimeSession()
                
                if (sessionResult.isSuccess) {
                    val session = sessionResult.getOrNull()!!
                    val sessionToken = session.client_secret.value
                    Log.d("LiveVoiceScreen", "✅ Session created, token: ${sessionToken.take(10)}...")
                    
                    Log.d("LiveVoiceScreen", "🔗 Connecting with WebRTC...")
                    // Connect with WebRTC
                    val success = webRTCManager?.connectToOpenAI(
                        sessionToken = sessionToken,
                        personality = currentPersonality,
                        companionName = companionName
                    ) ?: false
                    
                    if (!success) {
                        Log.e("LiveVoiceScreen", "❌ WebRTC connection failed")
                        errorMessage = "Failed to establish voice connection"
                        isConnecting = false
                    }
                } else {
                    Log.e("LiveVoiceScreen", "❌ Session creation failed: ${sessionResult.exceptionOrNull()?.message}")
                    errorMessage = "Failed to create session: ${sessionResult.exceptionOrNull()?.message}"
                    isConnecting = false
                }
            } catch (e: Exception) {
                Log.e("LiveVoiceScreen", "❌ Connection error", e)
                errorMessage = "Connection error: ${e.message}"
                isConnecting = false
            }
        }
    }
    
    // Disconnect function
    fun disconnectVoiceChat() {
        webRTCManager?.disconnect()
    }
    
    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
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
            // Switch to Gemini button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onNavigateToGemini,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                    )
                ) {
                    Text("🤖 Try Gemini Live")
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            // Title
            Text(
                text = "🎙️ OpenAI Voice Chat",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "Talk with your Smol Companion!",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 30.dp)
            )
            
            // Companion Face
            CompanionFace(
                isSpeaking = isSpeaking,
                isConnected = isConnected,
                modifier = Modifier.padding(bottom = 30.dp)
            )
            
            // Connection Status
            ConnectionStatus(
                isConnected = isConnected,
                isSpeaking = isSpeaking,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Instructions or Error
            if (showInstructions && !isConnected && errorMessage == null) {
                InstructionsCard(modifier = Modifier.padding(bottom = 20.dp))
            }
            
            errorMessage?.let { error ->
                ErrorCard(
                    error = error,
                    onDismiss = { errorMessage = null },
                    modifier = Modifier.padding(bottom = 20.dp)
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Main Action Button
            Button(
                onClick = {
                    if (isConnected) {
                        disconnectVoiceChat()
                    } else {
                        connectToVoiceChat()
                    }
                },
                enabled = !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isConnected) Color(0xFFE53E3E) else Color(0xFF4299E1)
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
                        text = if (isConnected) "🔴 End Voice Chat" else "🎤 Start Voice Chat",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
            
            // Control buttons when connected
            if (isConnected) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Speaker toggle button
                OutlinedButton(
                    onClick = { toggleSpeakerMode() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                    )
                ) {
                    Text(
                        text = if (isSpeakerMode) "🔊 Speaker Mode (Tap for Phone)" else "📱 Phone Mode (Tap for Speaker)",
                        fontSize = 14.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { webRTCManager?.enableListeningMode() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                        )
                    ) {
                        Text("👂 Listen Only", fontSize = 12.sp)
                    }
                    
                    OutlinedButton(
                        onClick = { webRTCManager?.disableListeningMode() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                        )
                    ) {
                        Text("🗣️ Auto Chat", fontSize = 12.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { webRTCManager?.triggerResponse() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(listOf(Color.White, Color.White))
                    )
                ) {
                    Text("⚡ Ask for Response", fontSize = 14.sp)
                }
                

            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
        
        // Small debug button in top-right corner
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            IconButton(
                onClick = { showDebugDialog = true },
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        Color.White.copy(alpha = 0.1f),
                        CircleShape
                    )
            ) {
                Text(
                    text = "🔧",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }
        
        // Debug Dialog
        if (showDebugDialog) {
            DebugDialog(
                audioDebugHelper = audioDebugHelper,
                onDismiss = { showDebugDialog = false }
            )
        }
    }
}

@Composable
fun CompanionFace(
    isSpeaking: Boolean,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    // Blinking animation
    var isBlinking by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay((2000..5000).random().toLong())
            isBlinking = true
            kotlinx.coroutines.delay(150)
            isBlinking = false
        }
    }
    
    // Speaking animation
    val scale by animateFloatAsState(
        targetValue = if (isSpeaking) 1.1f else 1.0f,
        animationSpec = tween(300),
        label = "speaking_scale"
    )
    
    Box(
        modifier = modifier
            .size(200.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow for speaking
        if (isSpeaking) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4299E1).copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        // Main face
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF9F7AEA),
                            Color(0xFF805AD5)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Eyes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .size(
                                    width = 16.dp,
                                    height = if (isBlinking) 2.dp else 16.dp
                                )
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                
                // Mouth
                Box(
                    modifier = Modifier
                        .size(
                            width = if (isSpeaking) 24.dp else 16.dp,
                            height = if (isSpeaking) 16.dp else 8.dp
                        )
                        .clip(CircleShape)
                        .background(Color.White)
                )
            }
            
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
    }
}

@Composable
fun ConnectionStatus(
    isConnected: Boolean,
    isSpeaking: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isConnected) Color(0xFF48BB78) else Color(0xFF718096)
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = when {
                    isSpeaking -> "🎵 Companion is speaking..."
                    isConnected -> "✅ Connected and listening"
                    else -> "💤 Disconnected"
                },
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun InstructionsCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "🎙️ How to Voice Chat",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val instructions = listOf(
                "1️⃣ Tap 'Start Voice Chat' to connect",
                "2️⃣ Allow microphone access",
                "3️⃣ Start talking naturally!",
                "4️⃣ Your companion will respond when you pause"
            )
            
            instructions.forEach { instruction ->
                Text(
                    text = instruction,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
fun DebugDialog(
    audioDebugHelper: AudioDebugHelper,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🔧 Audio Debug Tools",
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        text = {
            Column {
                Text(
                    text = "Test audio functionality and view debug information.",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        Log.d("LiveVoiceScreen", "🔧 Debug button pressed")
                        audioDebugHelper.logCurrentAudioState()
                        val testResult = audioDebugHelper.testSpeakerMode()
                        Log.d("LiveVoiceScreen", "🔊 Speaker test result: $testResult")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4299E1)
                    )
                ) {
                    Text("🔊 Test Speaker Mode")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = {
                        Log.d("LiveVoiceScreen", "📊 Logging audio state")
                        audioDebugHelper.logCurrentAudioState()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38A169)
                    )
                ) {
                    Text("📊 Log Audio State")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White
                )
            ) {
                Text("Close")
            }
        },
        containerColor = Color(0xFF2D3748),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
} 