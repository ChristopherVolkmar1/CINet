package com.example.cinet.navigation

import androidx.compose.runtime.Composable
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.feature.profile.ProfileEditScreen
import com.example.cinet.feature.profile.ProfileScreen
import com.example.cinet.feature.settings.AppSettings
import com.example.cinet.feature.settings.SettingScreen
import com.example.cinet.feature.settings.canvas.CanvasConnectionScreen
import com.example.cinet.ui.theme.AppThemeColor

// Chooses which settings/profile subpage is currently visible.
@Composable
internal fun NavigationSettingsRoute(
    showCanvasScreen: Boolean,
    showProfileEdit: Boolean,
    selectedProfile: UserProfile?,
    userProfile: UserProfile,
    onCanvasBack: () -> Unit,
    onCanvasSyncComplete: () -> Unit,
    onProfileEditBack: () -> Unit,
    onProfileSaved: () -> Unit,
    onOpenConversation: (Conversation) -> Unit,
    onSelectedProfileBack: () -> Unit,
    onEditProfile: () -> Unit,
    onSettingsBack: () -> Unit,
    onSignOut: () -> Unit,
    onSettingsChange: (Boolean, Boolean, AppThemeColor) -> Unit,
    onViewProfile: () -> Unit,
    onOpenCanvas: () -> Unit,
) {
    when {
        showCanvasScreen -> CanvasConnectionScreen(
            onBack = onCanvasBack,
            onSyncComplete = onCanvasSyncComplete
        )

        showProfileEdit -> ProfileEditScreen(
            onBack = onProfileEditBack,
            onSaved = onProfileSaved,
        )

        selectedProfile != null -> {
            val displayProfile = if (selectedProfile.uid == userProfile.uid) {
                userProfile
            } else {
                selectedProfile
            }

            ProfileScreen(
                user = displayProfile,
                currentUserProfile = userProfile,
                onOpenConversation = onOpenConversation,
                onBack = onSelectedProfileBack,
                onEditProfile = onEditProfile,
            )
        }

        else -> SettingScreen(
            onBack = onSettingsBack,
            onSignOut = onSignOut,
            isDarkMode = userProfile.isDarkMode,
            notificationsEnabled = userProfile.notificationsEnabled,
            selectedTheme = AppSettings.selectedTheme,
            onSettingsChange = onSettingsChange,
            userProfile = userProfile,
            onViewProfile = onViewProfile,
            onOpenCanvas = onOpenCanvas,
        )
    }
}
