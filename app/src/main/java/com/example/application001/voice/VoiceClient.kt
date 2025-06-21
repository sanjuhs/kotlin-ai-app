package com.example.application001.voice

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit

data class RealtimeSession(
    val client_secret: ClientSecret,
    val model: String
)

data class ClientSecret(
    val value: String
)

class VoiceClient(private val apiKey: String) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun createRealtimeSession(): Result<RealtimeSession> = withContext(Dispatchers.IO) {
        try {
            Log.d("VoiceClient", "Creating realtime session with OpenAI API")
            
            val requestBody = JsonObject().apply {
                addProperty("model", "gpt-4o-mini-realtime-preview-2024-12-17")
            }.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.openai.com/v1/realtime/sessions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("VoiceClient", "Session API response status: ${response.code}")

            if (response.isSuccessful && responseBody != null) {
                val session = gson.fromJson(responseBody, RealtimeSession::class.java)
                Log.d("VoiceClient", "Session created successfully")
                Result.success(session)
            } else {
                Log.e("VoiceClient", "OpenAI API Error: ${response.code} - $responseBody")
                Result.failure(Exception("OpenAI API Error: ${response.code} - ${responseBody ?: "Unknown error"}"))
            }
        } catch (e: IOException) {
            Log.e("VoiceClient", "Network error: ${e.message}")
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("VoiceClient", "Unexpected error: ${e.message}")
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }

    suspend fun connectWithWebRTC(sdpOffer: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("VoiceClient", "Connecting to OpenAI Realtime API with WebRTC")
            
            // Create request body with exact Content-Type to avoid charset being added
            val requestBody = object : RequestBody() {
                override fun contentType(): MediaType? = "application/sdp".toMediaType()
                override fun writeTo(sink: BufferedSink) {
                    sink.writeUtf8(sdpOffer)
                }
            }
            
            val request = Request.Builder()
                .url("https://api.openai.com/v1/realtime?model=gpt-4o-mini-realtime-preview-2024-12-17")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/sdp") // Explicitly set without charset
                .build()

            val response = client.newCall(request).execute()
            val sdpAnswer = response.body?.string()

            Log.d("VoiceClient", "WebRTC API response status: ${response.code}")

            if (response.isSuccessful && sdpAnswer != null) {
                Log.d("VoiceClient", "WebRTC connection successful")
                Result.success(sdpAnswer)
            } else {
                Log.e("VoiceClient", "WebRTC API Error: ${response.code} - $sdpAnswer")
                Result.failure(Exception("WebRTC API Error: ${response.code} - ${sdpAnswer ?: "Unknown error"}"))
            }
        } catch (e: IOException) {
            Log.e("VoiceClient", "WebRTC Network error: ${e.message}")
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Log.e("VoiceClient", "WebRTC Unexpected error: ${e.message}")
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }
} 