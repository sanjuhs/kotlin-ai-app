package com.example.application001

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.application001.screens.MainMenuScreen
import com.example.application001.screens.SettingsScreen
import com.example.application001.screens.RealtimeMotionScreen
import com.example.application001.utils.ChatViewModel
import com.example.application001.voice.LiveVoiceScreen
import com.example.application001.voice.gemini.GeminiLiveScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object MainMenu : Screen("main_menu", "Home", Icons.Filled.Home)
    object Chat : Screen("chat", "Chat", Icons.AutoMirrored.Filled.Chat)
    object LiveVoice : Screen("live_voice", "Voice", Icons.Filled.Mic)
    object GeminiLive : Screen("gemini_live", "Gemini", Icons.Filled.Mic)
    object RealtimeMotion : Screen("realtime_motion", "Motion", Icons.Filled.PhoneAndroid)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings)
}

@Composable
fun SmolCompanionApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                listOf(Screen.MainMenu, Screen.Chat, Screen.LiveVoice, Screen.RealtimeMotion, Screen.Settings).forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.MainMenu.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.MainMenu.route) {
                MainMenuScreen()
            }
            composable(Screen.Chat.route) {
                PlushieChatScreen(
                    viewModel = viewModel { ChatViewModel(context) }
                )
            }
            composable(Screen.LiveVoice.route) {
                LiveVoiceScreen(
                    onNavigateToGemini = {
                        navController.navigate(Screen.GeminiLive.route)
                    }
                )
            }
            composable(Screen.GeminiLive.route) {
                GeminiLiveScreen(
                    onBackToOpenAI = {
                        navController.popBackStack()
                    }
                )
            }
            composable(Screen.RealtimeMotion.route) {
                RealtimeMotionScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
} 