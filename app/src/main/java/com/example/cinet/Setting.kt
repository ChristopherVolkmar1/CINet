package com.example.cinet

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/* Settings stuff -Zack 
 * this screen lets u change how the app looks and works
 */
@Composable
fun SettingScreen(
    prefs: AppPreferences,
    onBack: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onNotificationChange: (Boolean) -> Unit
) {
    // remembers what u picked
    var nightMode by remember { mutableStateOf(prefs.isNightMode) }
    var notifications by remember { mutableStateOf(prefs.notificationsEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        /* Settings stuff -Zack 
         * back arrow at the top
         */
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }

        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // button to turn night mode on or off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Night Mode", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = nightMode,
                onCheckedChange = {
                    nightMode = it
                    prefs.isNightMode = it
                    onThemeChange(it) // makes it update right now
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // button to turn notifications on or off
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Enable Notifications", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = notifications,
                onCheckedChange = {
                    notifications = it
                    prefs.notificationsEnabled = it
                    onNotificationChange(it) // stops or starts notifs
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}
