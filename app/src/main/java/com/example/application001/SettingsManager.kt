package com.example.application001

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
        val PLUSHIE_PERSONALITY = stringPreferencesKey("plushie_personality")
        val PLUSHIE_NAME = stringPreferencesKey("plushie_name")
    }
    
    val apiKey: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[API_KEY] ?: "your-openai-api-key-here"
    }
    
    val plushiePersonality: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PLUSHIE_PERSONALITY] ?: "You are a cute, friendly plushie companion for children. Respond in a warm, encouraging, and playful way. Keep responses short and age-appropriate."
    }
    
    val plushieName: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PLUSHIE_NAME] ?: "Plushie"
    }
    
    suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }
    
    suspend fun savePlushiePersonality(personality: String) {
        context.dataStore.edit { preferences ->
            preferences[PLUSHIE_PERSONALITY] = personality
        }
    }
    
    suspend fun savePlushieName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[PLUSHIE_NAME] = name
        }
    }
} 