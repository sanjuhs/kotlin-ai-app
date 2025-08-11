package com.example.application001.voice

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.webrtc.*
import java.util.*

data class VoiceEvent(
    val type: String,
    val event_id: String? = null,
    val session: SessionConfig? = null,
    val response: ResponseConfig? = null,
    val item: ConversationItem? = null,
    val error: ErrorDetails? = null
)

data class SessionConfig(
    val instructions: String,
    val voice: String = "shimmer",
    val temperature: Double = 0.8,
    val turn_detection: TurnDetection? = null
)

data class TurnDetection(
    val type: String = "server_vad",
    val create_response: Boolean = true
)

data class ResponseConfig(
    val type: String = "response.create"
)

data class ConversationItem(
    val id: String? = null,
    val role: String? = null,
    val content: String? = null
)

data class ErrorDetails(
    val message: String,
    val type: String? = null
)

interface VoiceConnectionListener {
    fun onConnected()
    fun onDisconnected()
    fun onSpeakingStarted()
    fun onSpeakingStopped()
    fun onError(error: String)
    fun onAudioReceived()
}

class WebRTCManager(
    private val context: Context,
    private val listener: VoiceConnectionListener
) {
    private var peerConnection: PeerConnection? = null
    private var dataChannel: DataChannel? = null
    private var audioTrack: AudioTrack? = null
    private var localAudioSource: AudioSource? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private val gson = Gson()
    private val audioModeManager = AudioModeManager(context)
    
    companion object {
        private const val TAG = "WebRTCManager"
        private const val DATA_CHANNEL_LABEL = "events"
    }

    init {
        Log.d(TAG, "🏗️ Initializing WebRTCManager...")
        try {
            initializePeerConnectionFactory()
            initializeAudioManager()
            Log.d(TAG, "✅ WebRTCManager initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize WebRTCManager", e)
        }
    }

    private fun initializePeerConnectionFactory() {
        Log.d(TAG, "🏭 Creating PeerConnectionFactory...")
        
        val initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()
        
        Log.d(TAG, "📋 Initializing PeerConnectionFactory with options...")
        PeerConnectionFactory.initialize(initializationOptions)
        
        val options = PeerConnectionFactory.Options()
        
        // Create custom audio device module for better speaker control
        val audioDeviceModule = audioModeManager.createWebRTCAudioDeviceModule()
        
        Log.d(TAG, "🔨 Building PeerConnectionFactory with custom audio module...")
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .createPeerConnectionFactory()
            
        Log.d(TAG, "✅ PeerConnectionFactory created successfully with custom audio module")
    }
    
    private fun initializeAudioManager() {
        Log.d(TAG, "🎵 Initializing audio manager...")
        try {
            // Start voice call audio setup through our dedicated manager
            val success = audioModeManager.startVoiceCall()
            Log.d(TAG, "✅ Audio manager initialized: $success")
            Log.d(TAG, "🔊 Current audio state: ${audioModeManager.getCurrentAudioState()}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize audio manager", e)
        }
    }

    suspend fun connectToOpenAI(sessionToken: String, personality: String, companionName: String): Boolean {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "🚀 Starting WebRTC connection process...")
                
                // Create peer connection
                Log.d(TAG, "1️⃣ Creating peer connection...")
                createPeerConnection()
                if (peerConnection == null) {
                    Log.e(TAG, "❌ Failed to create peer connection")
                    listener.onError("Failed to create peer connection")
                    return@withContext false
                }
                Log.d(TAG, "✅ Peer connection created")
                
                // Set up audio
                Log.d(TAG, "2️⃣ Setting up audio...")
                setupAudio()
                
                // Create data channel
                Log.d(TAG, "3️⃣ Creating data channel...")
                createDataChannel()
                if (dataChannel == null) {
                    Log.e(TAG, "❌ Failed to create data channel")
                    listener.onError("Failed to create data channel")
                    return@withContext false
                }
                Log.d(TAG, "✅ Data channel created")
                
                // Create offer
                Log.d(TAG, "4️⃣ Creating offer...")
                val offer = createOffer()
                if (offer == null) {
                    Log.e(TAG, "❌ Failed to create offer")
                    listener.onError("Failed to create offer")
                    return@withContext false
                }
                Log.d(TAG, "✅ Offer created: ${offer.type}")
                
                // Connect to OpenAI API
                Log.d(TAG, "5️⃣ Connecting to OpenAI API...")
                val success = connectToOpenAIAPI(offer, sessionToken)
                if (success) {
                    Log.d(TAG, "✅ OpenAI API connected")
                    // Configure the session
                    Log.d(TAG, "6️⃣ Configuring session...")
                    configureSession(personality, companionName)
                    listener.onConnected()
                    Log.d(TAG, "🎉 WebRTC connection fully established!")
                } else {
                    Log.e(TAG, "❌ Failed to connect to OpenAI API")
                    listener.onError("Failed to connect to OpenAI API")
                }
                
                success
            } catch (e: Exception) {
                Log.e(TAG, "❌ Connection failed with exception", e)
                listener.onError("Connection failed: ${e.message}")
                false
            }
        }
    }

    private fun createPeerConnection() {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            iceCandidatePoolSize = 10
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "📡 Signaling state: $state")
            }

            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "🧊 ICE connection state: $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED -> {
                        Log.d(TAG, "✅ ICE connection established!")
                    }
                    PeerConnection.IceConnectionState.FAILED -> {
                        Log.e(TAG, "❌ ICE connection failed")
                        listener.onError("Connection failed")
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED -> {
                        listener.onDisconnected()
                    }
                    else -> {}
                }
            }

            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "🧊 ICE gathering state: $state")
            }
            override fun onIceCandidate(candidate: IceCandidate?) {}
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}

            override fun onTrack(rtpTransceiver: RtpTransceiver?) {
                Log.d(TAG, "🎵 Audio track received from OpenAI")
                listener.onAudioReceived()
                
                rtpTransceiver?.receiver?.track()?.let { track ->
                    if (track.kind() == "audio") {
                        track.setEnabled(true)
                        Log.d(TAG, "✅ Audio track enabled for playback")
                        
                        // Ensure the audio track is properly configured for speaker output
                        if (track is AudioTrack) {
                            track.setVolume(1.0) // Set maximum volume
                            Log.d(TAG, "🔊 Audio track volume set to maximum")
                        }
                        
                        // Re-enforce speaker mode when audio track is received
                        audioModeManager.enableSpeakerPhone(true)
                        Log.d(TAG, "🔊 Re-enforced speaker mode for incoming audio")
                    }
                }
            }

            override fun onAddStream(stream: MediaStream?) {
                // Deprecated - using onTrack instead
                Log.d(TAG, "🎵 Audio stream received (deprecated callback)")
                listener.onAudioReceived()
            }

            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "🎵 Audio stream removed")
            }
            override fun onDataChannel(dataChannel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
        })
    }

    private fun setupAudio() {
        Log.d(TAG, "🎵 Setting up audio...")
        
        try {
            // Create audio source
            val audioConstraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("echoCancellation", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("noiseSuppression", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("sampleRate", "24000"))
            }
            
            Log.d(TAG, "🎤 Creating audio source...")
            localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
            audioTrack = peerConnectionFactory?.createAudioTrack("audio_track", localAudioSource)
            
            Log.d(TAG, "➕ Adding audio track to peer connection...")
            // Use addTrack instead of addStream for Unified Plan compatibility
            audioTrack?.let { track ->
                val rtpSender = peerConnection?.addTrack(track, listOf("local_stream"))
                Log.d(TAG, "✅ Audio track added successfully: $rtpSender")
            }
            
            Log.d(TAG, "✅ Audio setup completed")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to setup audio", e)
        }
    }

    private fun createDataChannel() {
        val dataChannelInit = DataChannel.Init().apply {
            ordered = true
        }
        
        dataChannel = peerConnection?.createDataChannel(DATA_CHANNEL_LABEL, dataChannelInit)
        
        dataChannel?.registerObserver(object : DataChannel.Observer {
            override fun onBufferedAmountChange(amount: Long) {}
            
            override fun onStateChange() {
                when (dataChannel?.state()) {
                    DataChannel.State.OPEN -> {
                        Log.d(TAG, "✅ Data channel opened!")
                    }
                    DataChannel.State.CLOSED -> {
                        Log.d(TAG, "📪 Data channel closed")
                        listener.onDisconnected()
                    }
                    else -> {}
                }
            }

            override fun onMessage(buffer: DataChannel.Buffer?) {
                buffer?.data?.let { data ->
                    val bytes = ByteArray(data.remaining())
                    data.get(bytes)
                    val message = String(bytes)
                    handleDataChannelMessage(message)
                }
            }
        })
    }

    private suspend fun createOffer(): SessionDescription? {
        return withContext(Dispatchers.Main) {
            try {
                Log.d(TAG, "📝 Creating SDP offer...")
                
                // Use a CompletableDeferred to properly wait for the callback
                val offerDeferred = kotlinx.coroutines.CompletableDeferred<SessionDescription?>()
                
                val sdpObserver = object : SdpObserver {
                    override fun onCreateSuccess(sessionDescription: SessionDescription?) {
                        Log.d(TAG, "✅ Offer created successfully")
                        if (sessionDescription != null) {
                            // Set local description first
                            peerConnection?.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(p0: SessionDescription?) {}
                                override fun onCreateFailure(error: String?) {
                                    Log.e(TAG, "Failed to set local description: $error")
                                    offerDeferred.complete(null)
                                }
                                override fun onSetSuccess() {
                                    Log.d(TAG, "✅ Local description set successfully")
                                    offerDeferred.complete(sessionDescription)
                                }
                                override fun onSetFailure(error: String?) {
                                    Log.e(TAG, "Failed to set local description: $error")
                                    offerDeferred.complete(null)
                                }
                            }, sessionDescription)
                        } else {
                            Log.e(TAG, "❌ Offer is null")
                            offerDeferred.complete(null)
                        }
                    }
                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "❌ Failed to create offer: $error")
                        offerDeferred.complete(null)
                    }
                    override fun onSetSuccess() {}
                    override fun onSetFailure(error: String?) {}
                }
                
                Log.d(TAG, "🎯 Calling createOffer...")
                peerConnection?.createOffer(sdpObserver, MediaConstraints())
                
                // Wait for the offer to be created with timeout
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.withTimeoutOrNull(10000) { // 10 second timeout
                        offerDeferred.await()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in createOffer", e)
                null
            }
        }
    }

    private suspend fun connectToOpenAIAPI(offer: SessionDescription, sessionToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "🌐 Connecting to OpenAI Realtime API...")
                
                val voiceClient = VoiceClient(sessionToken)
                val response = voiceClient.connectWithWebRTC(offer.description)
                
                if (response.isSuccess) {
                    val sdpAnswer = response.getOrNull()
                    if (sdpAnswer != null) {
                        withContext(Dispatchers.Main) {
                            val answerDescription = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
                            val sdpObserver = object : SdpObserver {
                                override fun onCreateSuccess(sessionDescription: SessionDescription?) {}
                                override fun onCreateFailure(error: String?) {}
                                override fun onSetSuccess() {
                                    Log.d(TAG, "✅ Remote description set successfully")
                                }
                                override fun onSetFailure(error: String?) {
                                    Log.e(TAG, "Failed to set remote description: $error")
                                }
                            }
                            peerConnection?.setRemoteDescription(sdpObserver, answerDescription)
                        }
                        Log.d(TAG, "✅ OpenAI API connected successfully")
                        true
                    } else {
                        false
                    }
                } else {
                    Log.e(TAG, "Failed to connect to OpenAI API: ${response.exceptionOrNull()?.message}")
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "API connection error", e)
                false
            }
        }
    }

    private fun configureSession(personality: String, companionName: String) {
        val sessionConfig = VoiceEvent(
            type = "session.update",
            event_id = "config_${System.currentTimeMillis()}",
            session = SessionConfig(
                instructions = "You are $companionName, an adorable and helpful smart companion. $personality",
                voice = "shimmer",
                temperature = 0.8,
                turn_detection = TurnDetection(
                    type = "server_vad",
                    create_response = true
                )
            )
        )
        
        sendDataChannelMessage(sessionConfig)
    }

    private fun handleDataChannelMessage(message: String) {
        try {
            val event = gson.fromJson(message, VoiceEvent::class.java)
            
            when (event.type) {
                "error" -> {
                    Log.e(TAG, "❌ OpenAI error: ${event.error?.message}")
                    listener.onError("OpenAI error: ${event.error?.message}")
                }
                "output_audio_buffer.started" -> {
                    Log.d(TAG, "🗣️ OpenAI started speaking")
                    listener.onSpeakingStarted()
                }
                "output_audio_buffer.stopped", "output_audio_buffer.cleared" -> {
                    Log.d(TAG, "🤫 OpenAI stopped speaking")
                    listener.onSpeakingStopped()
                }
                "conversation.item.created" -> {
                    if (event.item?.role == "assistant") {
                        Log.d(TAG, "💬 Assistant message created: ${event.item.id}")
                    }
                }
                else -> {
                    Log.d(TAG, "📨 Received event: ${event.type}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing data channel message", e)
        }
    }

    private fun sendDataChannelMessage(event: VoiceEvent) {
        try {
            val json = gson.toJson(event)
            val buffer = DataChannel.Buffer(
                java.nio.ByteBuffer.wrap(json.toByteArray()),
                false
            )
            dataChannel?.send(buffer)
            Log.d(TAG, "📤 Sent event: ${event.type}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send data channel message", e)
        }
    }

    fun enableListeningMode() {
        val event = VoiceEvent(
            type = "session.update",
            event_id = "listen_mode_${System.currentTimeMillis()}",
            session = SessionConfig(
                instructions = "You are in listening mode. Only listen, don't respond automatically.",
                turn_detection = TurnDetection(
                    type = "server_vad",
                    create_response = false
                )
            )
        )
        sendDataChannelMessage(event)
    }

    fun disableListeningMode() {
        val event = VoiceEvent(
            type = "session.update",
            event_id = "auto_mode_${System.currentTimeMillis()}",
            session = SessionConfig(
                instructions = "You can now respond automatically when the user finishes speaking.",
                turn_detection = TurnDetection(
                    type = "server_vad",
                    create_response = true
                )
            )
        )
        sendDataChannelMessage(event)
    }

    fun triggerResponse() {
        val event = VoiceEvent(
            type = "response.create",
            event_id = "manual_response_${System.currentTimeMillis()}"
        )
        sendDataChannelMessage(event)
    }

    fun disconnect() {
        Log.d(TAG, "💤 Disconnecting WebRTC...")
        
        // Stop voice call audio through our dedicated manager
        audioModeManager.stopVoiceCall()
        Log.d(TAG, "🔊 Audio mode manager stopped")
        
        dataChannel?.close()
        dataChannel = null
        
        audioTrack?.dispose()
        audioTrack = null
        
        localAudioSource?.dispose()
        localAudioSource = null
        
        peerConnection?.close()
        peerConnection = null
        
        listener.onDisconnected()
    }

    fun setSpeakerMode(enabled: Boolean) {
        audioModeManager.enableSpeakerPhone(enabled)
        Log.d(TAG, "🔊 Speaker mode set to: $enabled")
        Log.d(TAG, "🔊 Current audio state: ${audioModeManager.getCurrentAudioState()}")
    }
    
    fun getCurrentAudioState(): String {
        return audioModeManager.getCurrentAudioState()
    }

    fun dispose() {
        disconnect()
        peerConnectionFactory?.dispose()
        peerConnectionFactory = null
    }
} 