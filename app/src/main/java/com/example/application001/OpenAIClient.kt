package com.example.application001

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

// Data classes for OpenAI API
data class ChatMessage(
    val role: String, // "user", "assistant", or "system"
    val content: String
)

data class ChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatMessage>,
    @SerializedName("max_tokens") val maxTokens: Int = 150,
    val temperature: Double = 0.7
)

data class ChatChoice(
    val message: ChatMessage,
    @SerializedName("finish_reason") val finishReason: String
)

data class ChatResponse(
    val choices: List<ChatChoice>,
    val error: ErrorResponse? = null
)

data class ErrorResponse(
    val message: String,
    val type: String? = null
)

class OpenAIClient(private val apiKey: String) {
    private val client = OkHttpClient()
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendMessage(messages: List<ChatMessage>, systemPrompt: String = "You are a cute, friendly plushie companion for children. Respond in a warm, encouraging, and playful way. Keep responses short and age-appropriate."): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = ChatRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    ChatMessage("system", systemPrompt),
                    *messages.toTypedArray()
                ),
                maxTokens = 150,
                temperature = 0.7
            )

            val requestBody = gson.toJson(request).toRequestBody(mediaType)

            val httpRequest = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val chatResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                if (chatResponse.error != null) {
                    Result.failure(Exception("OpenAI API Error: ${chatResponse.error.message}"))
                } else {
                    val reply = chatResponse.choices.firstOrNull()?.message?.content
                        ?: "Sorry, I didn't understand that! 🧸"
                    Result.success(reply)
                }
            } else {
                Result.failure(Exception("HTTP Error: ${response.code} - ${responseBody ?: "Unknown error"}"))
            }
        } catch (e: IOException) {
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }
} 