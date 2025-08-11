package com.example.application001.voice.gemini

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder

interface GeminiVoiceListener {
    fun onConnected()
    fun onDisconnected()
    fun onSpeakingStarted()
    fun onSpeakingStopped()
    fun onError(error: String)
}

class GeminiVoiceClient(
    private val context: Context,
    private val apiKey: String,
    private val listener: GeminiVoiceListener
) {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var isRecording = false
    private var isPlaying = false
    
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = apiKey
    )
    
    companion object {
        private const val TAG = "GeminiVoiceClient"
    }
    
    fun startVoiceChat(): Boolean {
        return try {
            Log.d(TAG, "🎤 Starting Gemini voice chat...")
            
            // Initialize audio components
            initializeAudio()
            
            // Start recording
            startRecording()
            
            listener.onConnected()
            Log.d(TAG, "✅ Gemini voice chat started")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start voice chat", e)
            listener.onError("Failed to start voice chat: ${e.message}")
            false
        }
    }
    
    private fun initializeAudio() {
        try {
            // Check for audio permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                throw SecurityException("Audio recording permission not granted")
            }
            
            // Initialize AudioRecord for capturing microphone
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize * 2
            )
            
            // Initialize AudioTrack for playback
            val playbackBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat
            )
            
            audioTrack = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat,
                playbackBufferSize,
                AudioTrack.MODE_STREAM
            )
            
            Log.d(TAG, "🎵 Audio components initialized")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize audio", e)
            throw e
        }
    }
    
    private fun startRecording() {
        try {
            audioRecord?.startRecording()
            isRecording = true
            Log.d(TAG, "🎤 Recording started")
            
            // Start processing audio in background
            // Note: This is a simplified implementation
            // In a real app, you'd need to process audio chunks and send to Gemini
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start recording", e)
            throw e
        }
    }
    
    fun stopVoiceChat() {
        try {
            Log.d(TAG, "🛑 Stopping Gemini voice chat...")
            
            isRecording = false
            isPlaying = false
            
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            
            listener.onDisconnected()
            Log.d(TAG, "✅ Gemini voice chat stopped")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error stopping voice chat", e)
        }
    }
    
    // Simplified text-based interaction for now
    suspend fun sendTextMessage(message: String): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📤 Sending message to Gemini: $message")
                
                val response = generativeModel.generateContent(
                    content {
                        text("You are a helpful AI assistant. Respond naturally and conversationally. User says: $message")
                    }
                )
                
                val responseText = response.text ?: "I'm sorry, I couldn't generate a response."
                Log.d(TAG, "📥 Received response from Gemini: $responseText")
                
                responseText
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending message to Gemini", e)
                "Sorry, there was an error communicating with Gemini: ${e.message}"
            }
        }
    }
} 