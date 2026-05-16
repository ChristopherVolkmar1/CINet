package com.example.cinet.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cinet.core.notifications.NotificationHelper
import com.example.cinet.core.permissions.PermissionManager
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.feature.settings.AppSettings
import com.example.cinet.navigation.NavigationHandler
import com.example.cinet.feature.auth.viewmodel.AuthViewModel
import com.example.cinet.feature.auth.viewmodel.AuthViewModelFactory
import com.example.cinet.feature.auth.AuthState
import com.example.cinet.ui.theme.CINetTheme
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    private val repository by lazy { FirestoreRepository() }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(repository)
    }

    // Destinations to open immediately — set from notification tap intents.
    // Using mutableStateOf so Compose recomposes when onNewIntent updates them.
    private var pendingConversationId by mutableStateOf<String?>(null)
    private var pendingMapLocationName by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)

        if (!PermissionManager.hasAllPermissions(this)) {
            PermissionManager.requestAllPermissions(this)
        }

        // Read notification tap extras (cold start or task not running)
        pendingConversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        pendingMapLocationName = intent.getStringExtra(EXTRA_OPEN_MAP_FOR_LOCATION)

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        setContent {
            val authState by authViewModel.authState.collectAsState()

            val isDarkMode = when (val state = authState) {
                is AuthState.Authenticated -> state.userProfile.isDarkMode
                is AuthState.ProfileSetup -> state.userProfile.isDarkMode
                else -> AppSettings.isDarkMode
            }

            val currentTheme = when (val state = authState) {
                is AuthState.Authenticated -> state.userProfile.selectedTheme
                is AuthState.ProfileSetup -> state.userProfile.selectedTheme
                else -> AppSettings.selectedTheme
            }

            LaunchedEffect(isDarkMode) {
                AppSettings.isDarkMode = isDarkMode
                AppSettings.selectedTheme = currentTheme
            }

            CINetTheme(
                darkTheme = isDarkMode,
                selectedColor = currentTheme
            ) {
                NavigationHandler(
                    authState = authState,
                    onSignOut = { authViewModel.signOut() },
                    onRetry = { authViewModel.retryProfileLoad() },
                    onSaveProfile = { nickname, major, minor, pronouns ->
                        authViewModel.saveProfile(nickname, major, minor, pronouns)
                    },
                    initialConversationId = pendingConversationId,
                    onConversationOpened = { pendingConversationId = null },
                    initialMapLocationName = pendingMapLocationName,
                    onMapLocationOpened = { pendingMapLocationName = null },
                )
            }
        }
    }

    // Called when the app is already running and the user taps a notification.
    // FLAG_ACTIVITY_SINGLE_TOP ensures this is called instead of a new onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingConversationId = intent.getStringExtra(EXTRA_CONVERSATION_ID)
        pendingMapLocationName = intent.getStringExtra(EXTRA_OPEN_MAP_FOR_LOCATION)
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversationId"
        const val EXTRA_OPEN_MAP_FOR_LOCATION = "extra_open_map_for_location"
    }
}