package com.example.cinet

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.cinet.com.example.cinet.feature.profile.UserProfile

object AppSettings {
    var isDarkMode by mutableStateOf(false)
    var notificationsEnabled by mutableStateOf(true)
    var classReminderMinutesBefore: Long = 10L
    var assignmentReminderMinutesBefore: Long = 60L
}

// settings stuff - Zack
/**
 * Settings screen allows users to manage their app preferences
 */
@Composable
fun SettingScreen(
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onEditProfile: () -> Unit, // navigates to ProfileEditScreen
    userProfile: UserProfile
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

        // Profile avatar — tappable, opens ProfileEditScreen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onEditProfile() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val photoUrl = userProfile.photoUrl.takeIf { it.isNotBlank() }
            if (photoUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            } else {
                // Fallback initials circle if no photo
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userProfile.nickname
                            .firstOrNull()
                            ?.uppercaseChar()
                            ?.toString() ?: "?",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            Column {
                Text(
                    text = userProfile.nickname.ifBlank { "Set a nickname" },
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Edit profile",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

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
                checked = AppSettings.isDarkMode,
                onCheckedChange = { AppSettings.isDarkMode = it }
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
                checked = AppSettings.notificationsEnabled,
                onCheckedChange = { AppSettings.notificationsEnabled = it }
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