package com.example.cinet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.cinet.ui.theme.CINetTheme

// Simple enum to handle screen state
enum class Screen { Home, Map, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CINetTheme {
                // State to keep track of the current screen
                var currentScreen by remember { mutableStateOf(Screen.Home) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        Screen.Home -> HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            onMapClick = { currentScreen = Screen.Map },
                            onSettingsClick = { currentScreen = Screen.Settings }
                        )
                        Screen.Map -> MapScreen(
                            onBack = { currentScreen = Screen.Home }
                        )
                        Screen.Settings -> SettingScreen(
                            onBack = { currentScreen = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}
