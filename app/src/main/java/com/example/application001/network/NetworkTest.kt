package com.example.application001.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class NetworkTest {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun testOpenAIConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            println("NetworkTest: Testing connection to OpenAI API...")
            println("NetworkTest: Using API key: ${apiKey.take(10)}...")
            
            val request = Request.Builder()
                .url("https://api.openai.com/v1/models")
                .get()
                .addHeader("Authorization", "Bearer $apiKey")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            println("NetworkTest: Response code: ${response.code}")
            println("NetworkTest: Response body (first 200 chars): ${responseBody?.take(200)}")
            
            if (response.isSuccessful) {
                Result.success("Connection successful! Response code: ${response.code}")
            } else {
                Result.failure(Exception("HTTP Error: ${response.code} - ${responseBody ?: "Unknown error"}"))
            }
        } catch (e: IOException) {
            println("NetworkTest: IOException: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Network error: ${e.message}"))
        } catch (e: Exception) {
            println("NetworkTest: Exception: ${e.message}")
            e.printStackTrace()
            Result.failure(Exception("Unexpected error: ${e.message}"))
        }
    }
} 