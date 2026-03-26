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
    // Requests Notification Permission, required for android 13+
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        // Create a Notification Channel for this app which is required to make notifications appear
        NotificationHelper.createChannel(this)
        // Test notification on app launch to verify notifications but also set up pop up notifications
        val testNotification = AppNotification(
            title = "Test Event",
            message = "Study Session in 10 Minutes",
            type = NotificationType.EVENT,
            timestamp = System.currentTimeMillis()
        )

        NotificationHelper.showNotification(this, testNotification)


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
                    // Stores Activity context for use inside Compose UI
                    val context = this@MainActivity
                    // Column Layout to stack buttons vertically
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(innerPadding)
                    ) {

                        //  MESSAGE Channel, simulated recieving a direct message
                        androidx.compose.material3.Button(
                            onClick = {
                                val notification = AppNotification(
                                    "New Message",
                                    "Alex: Want to study at 7?",
                                    NotificationType.MESSAGE,
                                    System.currentTimeMillis()
                                )
                                NotificationHelper.showNotification(context, notification)
                            }
                        ) {
                            Text("Simulate Message")
                        }

                        // REMINDER Channel, Simulates a class / schedule reminder notification
                        androidx.compose.material3.Button(
                            onClick = {
                                val notification = AppNotification(
                                    "Class Reminder",
                                    "CS Class starts in 15 minutes",
                                    NotificationType.REMINDER,
                                    System.currentTimeMillis()
                                )
                                NotificationHelper.showNotification(context, notification)
                            }
                        ) {
                            Text("Simulate Reminder")
                        }

                        // EVENT Channel, Simulates a Campus Event Notification
                        androidx.compose.material3.Button(
                            onClick = {
                                val notification = AppNotification(
                                    "Campus Event",
                                    "Free pizza at Student Center",
                                    NotificationType.EVENT,
                                    System.currentTimeMillis()
                                )
                                NotificationHelper.showNotification(context, notification)
                            }
                        ) {
                            Text("Simulate Event")
                        }

                        // REPOSITORY DEMO Until I integrate Firebase, can load notifications from a data source (i.e a Database)
                        androidx.compose.material3.Button(
                            onClick = {
                                val repo = FakeNotificationRepository()
                                repo.getSampleNotifications().forEach {
                                    NotificationHelper.showNotification(context, it)
                                }
                            }
                        ) {
                            Text("Load Campus Activity")
                        }
                    }
                }
            }
        }
    }
}


        //@Preview(showBackground = true)
        //@Composable
        //fun GreetingPreview() {
        //    CINetTheme {
        //        Greeting("Android")

        @Composable
        fun Greeting(name: String, modifier: Modifier = Modifier) {
            Text(
                text = "Hello $name!",
                modifier = modifier
            )
        }
