package com.example.cinet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.example.cinet.feature.auth.AuthState
import com.example.cinet.feature.auth.ErrorScreen
import com.example.cinet.feature.auth.LoadingScreen
import com.example.cinet.feature.auth.LoginScreen
import com.example.cinet.feature.auth.ProfileSetupScreen
import com.example.cinet.feature.settings.AppSettings


@Composable
fun NavigationHandler(
    authState: AuthState,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit,
    initialConversationId: String? = null,
    onConversationOpened: () -> Unit = {},
    initialMapLocationName: String? = null,
    onMapLocationOpened: () -> Unit = {},
) {
    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            AppSettings.selectedTheme = authState.userProfile.selectedTheme
            AppSettings.isDarkMode = authState.userProfile.isDarkMode
        }
    }

    when (authState) {
        is AuthState.Loading -> LoadingScreen()
        is AuthState.Unauthenticated -> LoginScreen()
        is AuthState.ProfileSetup -> ProfileSetupScreen(onSaveProfile = onSaveProfile)
        is AuthState.Error -> ErrorScreen(
            message = authState.message,
            onRetry = onRetry
        )
        is AuthState.Authenticated -> MainScaffold(
            userProfile = authState.userProfile,
            onSignOut = onSignOut,
            initialConversationId = initialConversationId,
            onConversationOpened = onConversationOpened,
            initialMapLocationName = initialMapLocationName,
            onMapLocationOpened = onMapLocationOpened,
        )
    }
}