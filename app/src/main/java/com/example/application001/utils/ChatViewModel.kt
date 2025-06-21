package com.example.application001.utils

import com.example.application001.BuildConfig
import com.example.application001.data.SettingsManager
import com.example.application001.network.OpenAIClient
import com.example.application001.network.ChatMessage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiChatMessage(
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatUiState(
    val messages: List<UiChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class ChatViewModel(private val context: Context) : ViewModel() {
    private val _uiState = mutableStateOf(ChatUiState())
    val uiState: State<ChatUiState> = _uiState

    private val settingsManager = SettingsManager(context)
    private var openAIClient = OpenAIClient(BuildConfig.OPENAI_API_KEY)


    init {
        println("OPENAI_API_KEY: ${BuildConfig.OPENAI_API_KEY}")
        viewModelScope.launch {
            // Initialize OpenAI client with saved API key or fallback to BuildConfig
            val savedApiKey = settingsManager.apiKey.first()
            val apiKey = if (savedApiKey.isNotBlank() && savedApiKey != "your-openai-api-key-here") {
                savedApiKey
            } else {
                BuildConfig.OPENAI_API_KEY
            }
            
            if (apiKey.isNotBlank()) {
                openAIClient = OpenAIClient(apiKey)
            }
            
            // Get companion name for welcome message
            val companionName = settingsManager.companionName.first()
            
            // Add a welcome message from the companion
            _uiState.value = _uiState.value.copy(
                messages = listOf(
                    UiChatMessage(
                        content = "Hi there! I'm $companionName, your adorable smart companion! 🦄✨ What can I help you with today?",
                        isFromUser = false
                    )
                )
            )
        }
    }

    fun sendMessage(userMessage: String) {
        if (userMessage.isBlank()) return

        // Add user message to the chat
        val userChatMessage = UiChatMessage(content = userMessage.trim(), isFromUser = true)
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userChatMessage,
            isLoading = true,
            error = null
        )

        // Prepare conversation history for OpenAI
        val conversationHistory = _uiState.value.messages
            .takeLast(10) // Keep last 10 messages for context
            .filter { it.isFromUser } // Only include user messages for now to keep it simple
            .map { ChatMessage(role = "user", content = it.content) }

        viewModelScope.launch {
            // Get current personality from settings
            val personality = settingsManager.companionPersonality.first()
            
            openAIClient.sendMessage(conversationHistory, personality)
                .onSuccess { response ->
                    val aiMessage = UiChatMessage(content = response, isFromUser = false)
                    _uiState.value = _uiState.value.copy(
                        messages = _uiState.value.messages + aiMessage,
                        isLoading = false
                    )
                }
                .onFailure { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Oops! Something went wrong: ${exception.message}"
                    )
                }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
} 