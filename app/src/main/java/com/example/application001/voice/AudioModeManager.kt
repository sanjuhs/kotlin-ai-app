package com.example.application001.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import org.webrtc.audio.AudioDeviceModule
import org.webrtc.audio.JavaAudioDeviceModule

class AudioModeManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var originalAudioMode: Int = AudioManager.MODE_NORMAL
    private var originalSpeakerState: Boolean = false
    private var isInVoiceCall = false
    private val debugHelper = AudioDebugHelper(context)
    
    companion object {
        private const val TAG = "AudioModeManager"
    }
    
    fun startVoiceCall(): Boolean {
        return try {
            Log.d(TAG, "🎤 Starting voice call audio setup...")
            
            // Debug current state
            debugHelper.logCurrentAudioState()
            
            // Save original state
            originalAudioMode = audioManager.mode
            originalSpeakerState = audioManager.isSpeakerphoneOn
            
            // Request audio focus
            requestAudioFocus()
            
            // Set audio mode for voice communication
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            Log.d(TAG, "📞 Audio mode set to: ${audioManager.mode}")
            
            // Enable speaker phone
            enableSpeakerPhone(true)
            
            isInVoiceCall = true
            
            // Debug final state
            debugHelper.logCurrentAudioState()
            
            Log.d(TAG, "✅ Voice call audio setup complete")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to start voice call audio", e)
            false
        }
    }
    
    fun enableSpeakerPhone(enable: Boolean) {
        try {
            Log.d(TAG, "🔊 Setting speaker phone: $enable")
            
            // Debug before
            Log.d(TAG, "BEFORE enableSpeakerPhone - Mode: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}")
            
            if (enable) {
                // Ensure we're in communication mode first
                if (audioManager.mode != AudioManager.MODE_IN_COMMUNICATION) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    Log.d(TAG, "🔧 Set audio mode to IN_COMMUNICATION")
                }
                
                // Force speaker mode
                audioManager.isSpeakerphoneOn = true
                Log.d(TAG, "🔊 Speaker enabled - Speaker state: ${audioManager.isSpeakerphoneOn}")
            } else {
                audioManager.isSpeakerphoneOn = false
                Log.d(TAG, "📱 Speaker disabled - Speaker state: ${audioManager.isSpeakerphoneOn}")
            }
            
            // Debug after
            Log.d(TAG, "AFTER enableSpeakerPhone - Mode: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}")
            
            // Force update audio routing
            forceAudioRouting(enable)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set speaker phone", e)
        }
    }
    
    private fun forceAudioRouting(speakerOn: Boolean) {
        try {
            // Use reflection to access hidden AudioManager methods if needed
            val method = audioManager.javaClass.getDeclaredMethod("setWiredHeadsetOn", Boolean::class.java)
            method.isAccessible = true
            method.invoke(audioManager, false) // Disable wired headset routing
            
            Log.d(TAG, "🔄 Forced audio routing update")
        } catch (e: Exception) {
            Log.d(TAG, "⚠️ Could not force audio routing (this is normal on newer Android versions)")
        }
    }
    
    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener { focusChange ->
                        handleAudioFocusChange(focusChange)
                    }
                    .build()
                
                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                Log.d(TAG, "🎯 Audio focus request result: $result")
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    { focusChange -> handleAudioFocusChange(focusChange) },
                    AudioManager.STREAM_VOICE_CALL,
                    AudioManager.AUDIOFOCUS_GAIN
                )
                Log.d(TAG, "🎯 Audio focus request result (legacy): $result")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to request audio focus", e)
        }
    }
    
    private fun handleAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "🎯 Audio focus gained")
                if (isInVoiceCall) {
                    enableSpeakerPhone(true)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "🎯 Audio focus lost")
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "🎯 Audio focus lost temporarily")
            }
        }
    }
    
    private fun setVoiceCallVolume() {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            val targetVolume = (maxVolume * 0.8).toInt()
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, targetVolume, 0)
            Log.d(TAG, "🔊 Voice call volume set to: $targetVolume/$maxVolume")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to set voice call volume", e)
        }
    }
    
    fun stopVoiceCall() {
        try {
            Log.d(TAG, "🔚 Stopping voice call audio...")
            
            isInVoiceCall = false
            
            // Restore original speaker state
            audioManager.isSpeakerphoneOn = originalSpeakerState
            
            // Restore original audio mode
            audioManager.mode = originalAudioMode
            
            // Release audio focus
            releaseAudioFocus()
            
            Log.d(TAG, "✅ Voice call audio stopped - Mode: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to stop voice call audio", e)
        }
    }
    
    private fun releaseAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    audioManager.abandonAudioFocusRequest(request)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus { focusChange ->
                    Log.d(TAG, "🎯 Audio focus abandoned: $focusChange")
                }
            }
            audioFocusRequest = null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to release audio focus", e)
        }
    }
    
    fun createWebRTCAudioDeviceModule(): AudioDeviceModule {
        Log.d(TAG, "🎵 Creating WebRTC AudioDeviceModule...")
        
        return JavaAudioDeviceModule.builder(context)
            .setUseHardwareAcousticEchoCanceler(true)
            .setUseHardwareNoiseSuppressor(true)
            .createAudioDeviceModule()
    }
    
    fun getCurrentAudioState(): String {
        return "Mode: ${audioManager.mode}, Speaker: ${audioManager.isSpeakerphoneOn}, " +
                "Volume: ${audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL)}/" +
                "${audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)}"
    }
} 