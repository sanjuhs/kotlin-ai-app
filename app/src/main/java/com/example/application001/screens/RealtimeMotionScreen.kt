package com.example.application001.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*

class RealtimeMotionManager(private val context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    // Motion states
    private val _flipDetected = mutableStateOf(false)
    val flipDetected: State<Boolean> = _flipDetected
    
    private val _shakeDetected = mutableStateOf(false)
    val shakeDetected: State<Boolean> = _shakeDetected
    
    private val _loudSoundDetected = mutableStateOf(false)
    val loudSoundDetected: State<Boolean> = _loudSoundDetected
    
    // Motion detection variables
    private var lastAcceleration = 9.8f
    private var currentAcceleration = 9.8f
    private var shakeThreshold = 12f
    
    // Flip detection
    private var lastZ = 0f
    private var flipThreshold = 7f
    
    // Audio recording for sound detection
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    fun startListening() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_UI)
        startAudioRecording()
    }
    
    fun stopListening() {
        sensorManager.unregisterListener(this)
        stopAudioRecording()
    }
    
    private fun startAudioRecording() {
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
            
            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                isRecording = true
                
                // Start background thread for audio monitoring
                Thread {
                    val buffer = ShortArray(bufferSize)
                    while (isRecording) {
                        val read = audioRecord?.read(buffer, 0, bufferSize) ?: 0
                        if (read > 0) {
                            val amplitude = buffer.maxOrNull()?.let { abs(it.toInt()) } ?: 0
                            if (amplitude > 8000) { // Loud sound threshold
                                _loudSoundDetected.value = true
                                vibrate()
                            }
                        }
                    }
                }.start()
            }
        } catch (e: SecurityException) {
            // Microphone permission not granted
        }
    }
    
    private fun stopAudioRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                
                // Shake detection
                currentAcceleration = sqrt(x * x + y * y + z * z)
                val delta = abs(currentAcceleration - lastAcceleration)
                
                if (delta > shakeThreshold) {
                    _shakeDetected.value = true
                    vibrate()
                }
                
                lastAcceleration = currentAcceleration
                
                // Flip detection (based on Z-axis change)
                if (abs(z - lastZ) > flipThreshold) {
                    _flipDetected.value = true
                    vibrate()
                }
                lastZ = z
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    
    private fun vibrate() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    
    fun resetFlip() { _flipDetected.value = false }
    fun resetShake() { _shakeDetected.value = false }
    fun resetLoudSound() { _loudSoundDetected.value = false }
}

@Composable
fun RealtimeMotionScreen() {
    val context = LocalContext.current
    val motionManager = remember { RealtimeMotionManager(context) }
    
    val flipDetected by motionManager.flipDetected
    val shakeDetected by motionManager.shakeDetected
    val loudSoundDetected by motionManager.loudSoundDetected
    
    // Auto-reset states after delays
    LaunchedEffect(flipDetected) {
        if (flipDetected) {
            delay(3000) // Show for 3 seconds
            motionManager.resetFlip()
        }
    }
    
    LaunchedEffect(shakeDetected) {
        if (shakeDetected) {
            delay(2500) // Show for 2.5 seconds
            motionManager.resetShake()
        }
    }
    
    LaunchedEffect(loudSoundDetected) {
        if (loudSoundDetected) {
            delay(4000) // Show for 4 seconds
            motionManager.resetLoudSound()
        }
    }
    
    // Start/stop motion detection
    LaunchedEffect(Unit) {
        motionManager.startListening()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            motionManager.stopListening()
        }
    }
    
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
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
                        text = "📱",
                        fontSize = 48.sp
                    )
                    Text(
                        text = "Real-Time Motion Mode",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Flip, shake, or make noise to trigger!",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Detection Area
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main plushie
                Card(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            flipDetected -> Color(0xFFFFE0E6)
                            shakeDetected -> Color(0xFFE8F5E8)
                            loudSoundDetected -> Color(0xFFFFF3E0)
                            else -> Color(0xFFF0F8FF)
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "🧸",
                            fontSize = 80.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Detection Messages
                AnimatedVisibility(
                    visible = flipDetected,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE0E6)),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "🔄 FLIPPED!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = shakeDetected,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "🤝 SHAKEN!",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
                
                AnimatedVisibility(
                    visible = loudSoundDetected,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "🔊 LOUD SOUND HEARD!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8F00),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
                
                if (!flipDetected && !shakeDetected && !loudSoundDetected) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F8FF)),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Text(
                            text = "Waiting for motion or sound...",
                            fontSize = 16.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            
            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C3E50)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🎮 Try These Actions:",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "🔄 Flip your phone quickly\n🤝 Shake vigorously\n🔊 Make a loud noise near mic",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
} 