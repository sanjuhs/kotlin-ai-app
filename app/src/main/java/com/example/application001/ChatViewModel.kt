package com.example.application001

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
    private var openAIClient = OpenAIClient("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")

    init {
        viewModelScope.launch {
            // Initialize OpenAI client with saved API key
            val apiKey = settingsManager.apiKey.first()
            if (apiKey != "your-openai-api-key-here") {
                openAIClient = OpenAIClient(apiKey)
            }
            
            // Get plushie name for welcome message
            val plushieName = settingsManager.plushieName.first()
            
            // Add a welcome message from the plushie
            _uiState.value = _uiState.value.copy(
                messages = listOf(
                    UiChatMessage(
                        content = "Hi there! I'm $plushieName, your friendly plushie companion! 🧸✨ What would you like to chat about today?",
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
            val personality = settingsManager.plushiePersonality.first()
            
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