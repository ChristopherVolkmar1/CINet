package com.example.cinet.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cinet.ui.theme.AppThemeColor
import com.example.cinet.ui.theme.ThemeSelector

object AppSettings {
    var isDarkMode by mutableStateOf(false)
    var notificationsEnabled by mutableStateOf(true)
    var classReminderMinutesBefore: Long = 10L
    var assignmentReminderMinutesBefore: Long = 60L
    var selectedTheme by mutableStateOf(AppThemeColor.Green)
}

// settings stuff - Zack
/**
 * Settings screen allows users to manage their app preferences
 */
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    isDarkMode: Boolean,
    notificationsEnabled: Boolean,
    selectedTheme: AppThemeColor,
    onSettingsChange: (Boolean, Boolean, AppThemeColor) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Header section
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

        ThemeSelector(
            selectedTheme = selectedTheme,
            onThemeChange = { newTheme ->
                onSettingsChange(isDarkMode, notificationsEnabled, newTheme)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // dark mode on / off
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Night Mode", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Applies a dark theme to the interface and campus map",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isDarkMode,
                onCheckedChange = { newValue ->
                    onSettingsChange(newValue, notificationsEnabled, selectedTheme)
                }
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        // pop up noti toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Notifications", style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Control whether the application sends alerts and updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = notificationsEnabled,
                onCheckedChange = { newValue ->
                    onSettingsChange(isDarkMode, newValue, selectedTheme)
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // sign out button
        OutlinedButton(
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Sign out")
        }
    }
}