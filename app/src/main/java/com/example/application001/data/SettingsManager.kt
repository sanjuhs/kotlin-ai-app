package com.example.application001.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    
    companion object {
        val API_KEY = stringPreferencesKey("api_key")
        val COMPANION_PERSONALITY = stringPreferencesKey("companion_personality")
        val COMPANION_NAME = stringPreferencesKey("companion_name")
    }
    
    val apiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_KEY] ?: "your-openai-api-key-here"
    }
    
    val companionPersonality: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[COMPANION_PERSONALITY] ?: "You are Smol Uni, an adorable and helpful smart companion. You're friendly, approachable, and love to assist with daily tasks, answer questions, and provide support. Keep your responses warm but intelligent."
    }
    
    val companionName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[COMPANION_NAME] ?: "Smol Uni"
    }
    
    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }
    
    suspend fun saveCompanionPersonality(personality: String) {
        context.dataStore.edit { preferences ->
            preferences[COMPANION_PERSONALITY] = personality
        }
    }
    
    suspend fun saveCompanionName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[COMPANION_NAME] = name
        }
    }
} 