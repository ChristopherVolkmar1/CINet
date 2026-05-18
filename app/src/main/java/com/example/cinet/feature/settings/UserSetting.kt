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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.data.model.UserProfile
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
    userProfile: UserProfile? = null,
    onViewProfile: () -> Unit = {},
    onOpenCanvas: () -> Unit = {},
    showHeader: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Profile header card - tap to view own profile
        if (userProfile != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable { onViewProfile() },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val photoUrl = userProfile.photoUrl.takeIf { it.isNotBlank() }
                    if (photoUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(photoUrl).crossfade(true).build(),
                            contentDescription = "Profile photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userProfile.nickname.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile.nickname,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (userProfile.major.isNotBlank()) {
                            Text(
                                text = userProfile.major,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "View Profile",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Header section
        if (showHeader) {
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

        // Canvas LMS sync entry
        OutlinedButton(
            onClick = onOpenCanvas,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Canvas Sync")
        }

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
