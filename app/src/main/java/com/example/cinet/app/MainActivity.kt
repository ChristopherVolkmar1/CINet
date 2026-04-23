package com.example.cinet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cinet.core.notifications.NotificationHelper
import com.example.cinet.core.permissions.PermissionManager
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.feature.settings.AppSettings
import com.example.cinet.navigation.NavigationHandler
import com.example.cinet.feature.auth.viewmodel.AuthViewModel
import com.example.cinet.feature.auth.viewmodel.AuthViewModelFactory
import com.example.cinet.feature.auth.AuthState
import com.example.cinet.ui.theme.CINetTheme

class MainActivity : ComponentActivity() {

    private val repository by lazy { FirestoreRepository() }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannel(this)
        
        if (!PermissionManager.hasAllPermissions(this)) {
            PermissionManager.requestAllPermissions(this)
        }
        
        enableEdgeToEdge()
        
        setContent {
            val authState by authViewModel.authState.collectAsState()
            
            // Derive dark mode from the current authenticated user profile
            val isDarkMode = when (val state = authState) {
                is AuthState.Authenticated -> state.userProfile.isDarkMode
                is AuthState.ProfileSetup -> state.userProfile.isDarkMode
                else -> AppSettings.isDarkMode // Fallback to local default if not logged in
            }

            // Sync global AppSettings for other components (like the Map) - Zack
            LaunchedEffect(isDarkMode) {
                AppSettings.isDarkMode = isDarkMode
            }

            CINetTheme(darkTheme = isDarkMode) {
                NavigationHandler(
                    authState = authState,
                    onSignOut = { authViewModel.signOut() },
                    onRetry = { authViewModel.retryProfileLoad() },
                    onSaveProfile = { nickname, major, pronouns ->
                        authViewModel.saveProfile(nickname, major, pronouns)
                    }
                )
            }
        }
    }

    companion object {
        // map location key - Zack
        const val EXTRA_OPEN_MAP_FOR_LOCATION = "extra_open_map_for_location"
    }
}
