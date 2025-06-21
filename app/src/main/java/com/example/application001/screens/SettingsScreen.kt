package com.example.application001.screens

import com.example.application001.BuildConfig
import com.example.application001.data.SettingsManager
import com.example.application001.data.MotionSensorManager
import com.example.application001.network.NetworkTest

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val motionSensorManager = remember { MotionSensorManager(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Settings state
    val apiKey by settingsManager.apiKey.collectAsState(initial = "")
    val companionPersonality by settingsManager.companionPersonality.collectAsState(initial = "")
    val companionName by settingsManager.companionName.collectAsState(initial = "")
    
    // Local state for editing
    var editApiKey by remember { mutableStateOf("") }
    var editPersonality by remember { mutableStateOf("") }
    var editName by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    
    // Motion detection
    val isMoving by motionSensorManager.isMoving
    val motionIntensity by motionSensorManager.motionIntensity
    
    // Initialize editing fields when settings load
    LaunchedEffect(apiKey, companionPersonality, companionName) {
        if (editApiKey.isEmpty()) editApiKey = apiKey
        if (editPersonality.isEmpty()) editPersonality = companionPersonality
        if (editName.isEmpty()) editName = companionName
    }
    
    // Start motion detection
    LaunchedEffect(Unit) {
        motionSensorManager.startListening()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            motionSensorManager.stopListening()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F4FD))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF4A90E2)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "⚙️",
                    fontSize = 48.sp
                )
                Text(
                    text = "Companion Settings",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Customize your smol companion!",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
        
        // Motion Detection Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isMoving) Color(0xFFE8F5E8) else Color(0xFFF5F5F5)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMoving) "📱" else "📱",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Motion Detection",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isMoving) {
                        "🎉 Motion detected! Intensity: ${String.format("%.1f", motionIntensity)}"
                    } else {
                        "📱 Move your phone to test motion detection"
                    },
                    fontSize = 14.sp,
                    color = if (isMoving) Color(0xFF2E7D32) else Color(0xFF666666)
                )
            }
        }
        
        // Plushie Name
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🦄 Companion Name",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A90E2)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Enter your companion's name") },
                    placeholder = { Text("e.g., Smol Uni, Luna, Sparkle") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
        
        // API Key Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔑 OpenAI API Key",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A90E2)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editApiKey,
                    onValueChange = { editApiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("sk-...") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showApiKey = !showApiKey }) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                            )
                        }
                    },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Get your API key from OpenAI Platform (platform.openai.com)",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
        
        // Custom Personality Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "✨ Companion Personality",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A90E2)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Define how your smart companion should behave and respond:",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = editPersonality,
                    onValueChange = { editPersonality = it },
                    label = { Text("System Prompt") },
                    placeholder = { Text("You are a...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💡 Examples: 'You are a helpful assistant who loves to answer questions' or 'You are a creative companion who enjoys brainstorming ideas'",
                    fontSize = 12.sp,
                    color = Color(0xFF666666)
                )
            }
        }
        
        // Quick Personality Presets
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🎭 Quick Presets",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A90E2)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                val presets = listOf(
                    "🦄 Smol Uni" to "You are Smol Uni, an adorable unicorn companion who's helpful, friendly, and loves to assist with daily tasks. You're smart but keep things cute and approachable.",
                    "🐙 Smol Octopus" to "You are Smol Octopus, a clever and creative companion who loves to multitask and help with various projects. You're witty, resourceful, and always ready to brainstorm.",
                    "🐱 Smol Cat" to "You are Smol Cat, a cozy and wise companion who loves to provide comfort and practical advice. You're calm, observant, and great at listening.",
                    "🐰 Smol Bunny" to "You are Smol Bunny, an energetic and enthusiastic companion who loves to motivate and encourage. You're optimistic, quick-thinking, and always ready for new challenges!"
                )
                
                presets.forEach { (name, prompt) ->
                    OutlinedButton(
                        onClick = { editPersonality = prompt },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = name,
                            modifier = Modifier.fillMaxWidth(),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        // Test API Connection Button
        var testResult by remember { mutableStateOf<String?>(null) }
        var isTestingConnection by remember { mutableStateOf(false) }
        
        Button(
            onClick = {
                coroutineScope.launch {
                    isTestingConnection = true
                    testResult = null
                    val networkTest = NetworkTest()
                    val result = networkTest.testOpenAIConnection(editApiKey.ifBlank { BuildConfig.OPENAI_API_KEY })
                    testResult = if (result.isSuccess) {
                        "✅ ${result.getOrNull()}"
                    } else {
                        "❌ ${result.exceptionOrNull()?.message}"
                    }
                    isTestingConnection = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8F00)),
            enabled = !isTestingConnection
        ) {
            if (isTestingConnection) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isTestingConnection) "Testing..." else "🔍 Test API Connection",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Test Result
        testResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (result.startsWith("✅")) Color(0xFFE8F5E8) else Color(0xFFFFEBEE)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = result,
                    modifier = Modifier.padding(16.dp),
                    color = if (result.startsWith("✅")) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Save Button
        Button(
            onClick = {
                coroutineScope.launch {
                    settingsManager.saveApiKey(editApiKey)
                    settingsManager.saveCompanionPersonality(editPersonality)
                    settingsManager.saveCompanionName(editName)
                    showSaveSuccess = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
        ) {
            Text(
                text = "💾 Save Settings",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Success message
        if (showSaveSuccess) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2000)
                showSaveSuccess = false
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = "✅ Settings saved successfully!",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
} 