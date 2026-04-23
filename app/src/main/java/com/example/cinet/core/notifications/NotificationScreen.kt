package com.example.cinet.core.notifications

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun NotificationScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section
        Text(
            text = "Notification Simulator",
            style = MaterialTheme.typography.headlineMedium
        )

        Button(onClick = onBack) {
            Text("Back to Home")
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()

        // Debug/Simulation Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Debug/Simulation:", style = MaterialTheme.typography.labelLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Message Button
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

                // Reminder Button
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

                // Event Button
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

                // Load Repository Button
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