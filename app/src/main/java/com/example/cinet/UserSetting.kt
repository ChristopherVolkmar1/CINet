package com.example.cinet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.data.remote.SettingsRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppSettings {
    var isDarkMode by mutableStateOf(false)
    var notificationsEnabled by mutableStateOf(true)
    var classReminderMinutesBefore by mutableStateOf(10L)
    var assignmentReminderMinutesBefore by mutableStateOf(60L)

    private val repository = SettingsRepository()
    private val scope = CoroutineScope(Dispatchers.Main)

    /**
     * Call this when the user logs in to fetch their saved settings from Firebase
     */
    fun loadFromFirebase() {
        scope.launch {
            val settings = repository.loadSettings()
            settings?.let {
                isDarkMode = it["isDarkMode"] as? Boolean ?: false
                notificationsEnabled = it["notificationsEnabled"] as? Boolean ?: true
                classReminderMinutesBefore = (it["classReminder"] as? Long) ?: 10L
                assignmentReminderMinutesBefore = (it["assignmentReminder"] as? Long) ?: 60L
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        isDarkMode = enabled
        scope.launch { repository.saveSetting("isDarkMode", enabled) }
    }

    fun toggleNotifications(enabled: Boolean) {
        notificationsEnabled = enabled
        scope.launch { repository.saveSetting("notificationsEnabled", enabled) }
    }
}

// settings stuff - Zack
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    // Refresh settings when entering the screen to ensure they are up to date
    LaunchedEffect(Unit) {
        AppSettings.loadFromFirebase()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Return to previous screen"
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Night Mode", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Applies a dark theme to the interface and campus map",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = AppSettings.isDarkMode,
                onCheckedChange = { AppSettings.toggleDarkMode(it) }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Notifications", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Control whether the application sends alerts and updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = AppSettings.notificationsEnabled,
                onCheckedChange = { AppSettings.toggleNotifications(it) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Sign out")
        }
    }
}
