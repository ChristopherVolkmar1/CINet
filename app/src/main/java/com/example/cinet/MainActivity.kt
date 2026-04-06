package com.example.cinet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.ui.theme.CINetTheme

// Simple enum to handle screen state
enum class Screen { Home, Map, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = AppPreferences(this)
        
        // Requests Notification Permission, required for android 13+
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        // Create a Notification Channel for this app which is required to make notifications appear
        NotificationHelper.createChannel(this)

        enableEdgeToEdge()
        setContent {
            var isNightMode by remember { mutableStateOf(prefs.isNightMode) }
            var notificationsEnabled by remember { mutableStateOf(prefs.notificationsEnabled) }
            
            CINetTheme(darkTheme = isNightMode) {
                // State to keep track of the current screen
                var currentScreen by remember { mutableStateOf(Screen.Home) }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Main content area that swaps between screens
                        Box(modifier = Modifier.weight(1f)) {
                            when (currentScreen) {
                                Screen.Home -> HomeScreen(
                                    onMapClick = { currentScreen = Screen.Map },
                                    onSettingsClick = { currentScreen = Screen.Settings }
                                )
                                Screen.Map -> CampusMapScreen(
                                    onBack = { currentScreen = Screen.Home }
                                )
                                Screen.Settings -> SettingScreen(
                                    prefs = prefs,
                                    onBack = { currentScreen = Screen.Home },
                                    onThemeChange = { isNightMode = it },
                                    onNotificationChange = { notificationsEnabled = it }
                                )
                            }
                        }

                        // Debug/Simulation buttons
                        val context = this@MainActivity
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("Debug/Simulation:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val notification = AppNotification(
                                            "New Message",
                                            "Alex: Want to study at 7?",
                                            NotificationType.MESSAGE,
                                            System.currentTimeMillis()
                                        )
                                        NotificationHelper.showNotification(context, notification)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Msg", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        val notification = AppNotification(
                                            "Class Reminder",
                                            "CS Class starts in 15 minutes",
                                            NotificationType.REMINDER,
                                            System.currentTimeMillis()
                                        )
                                        NotificationHelper.showNotification(context, notification)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Rem", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        val notification = AppNotification(
                                            "Campus Event",
                                            "Free pizza at Student Center",
                                            NotificationType.EVENT,
                                            System.currentTimeMillis()
                                        )
                                        NotificationHelper.showNotification(context, notification)
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Evt", style = MaterialTheme.typography.labelSmall)
                                }

                                Button(
                                    onClick = {
                                        val repo = FakeNotificationRepository()
                                        repo.getSampleNotifications().forEach {
                                            NotificationHelper.showNotification(context, it)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Load", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
