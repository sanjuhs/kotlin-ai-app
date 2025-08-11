package com.example.application001.voice

import android.content.Context
import android.media.AudioManager
import android.util.Log

class AudioDebugHelper(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    companion object {
        private const val TAG = "AudioDebugHelper"
    }
    
    fun logCurrentAudioState() {
        try {
            Log.d(TAG, "=== AUDIO STATE DEBUG ===")
            Log.d(TAG, "Audio Mode: ${audioManager.mode} (${getAudioModeString(audioManager.mode)})")
            Log.d(TAG, "Speaker Phone: ${audioManager.isSpeakerphoneOn}")
            Log.d(TAG, "Bluetooth SCO: ${audioManager.isBluetoothScoOn}")
            Log.d(TAG, "Wired Headset: ${audioManager.isWiredHeadsetOn}")
            Log.d(TAG, "Music Active: ${audioManager.isMusicActive}")
            
            // Volume levels
            Log.d(TAG, "Voice Call Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)}")
            Log.d(TAG, "Music Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)}")
            Log.d(TAG, "System Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)}/${audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)}")
            
            // Audio devices
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                Log.d(TAG, "Available output devices: ${devices.size}")
                devices.forEach { device ->
                    Log.d(TAG, "  - ${device.productName} (Type: ${device.type})")
                }
            }
            
            Log.d(TAG, "=== END AUDIO STATE ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error logging audio state", e)
        }
    }
    
    private fun getAudioModeString(mode: Int): String {
        return when (mode) {
            AudioManager.MODE_NORMAL -> "NORMAL"
            AudioManager.MODE_RINGTONE -> "RINGTONE"
            AudioManager.MODE_IN_CALL -> "IN_CALL"
            AudioManager.MODE_IN_COMMUNICATION -> "IN_COMMUNICATION"
            else -> "UNKNOWN($mode)"
        }
    }
    
    fun testSpeakerMode(): Boolean {
        return try {
            Log.d(TAG, "🔊 Testing speaker mode...")
            
            // Log before
            Log.d(TAG, "BEFORE - Mode: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}")
            
            // Set communication mode
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            
            // Enable speaker
            audioManager.isSpeakerphoneOn = true
            
            // Log after
            Log.d(TAG, "AFTER - Mode: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}")
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            Log.d(TAG, "Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)}/$maxVolume")
            
            // Verify
            val success = audioManager.isSpeakerphoneOn && audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
            Log.d(TAG, "Speaker test result: $success")
            
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error testing speaker mode", e)
            false
        }
    }
} 