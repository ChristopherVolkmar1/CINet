package com.example.cinet.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.ui.theme.CINetTheme
import com.example.cinet.feature.auth.viewmodel.AuthViewModel
import com.example.cinet.feature.auth.viewmodel.AuthViewModelFactory
import com.example.cinet.core.permissions.PermissionManager
import com.example.cinet.core.notifications.NotificationHelper
import com.example.cinet.navigation.NavigationHandler
import com.example.cinet.feature.settings.*

// settings stuff - Zack
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
            // App theme now watches the global dark mode setting
            CINetTheme(darkTheme = AppSettings.isDarkMode) {
                val authState by authViewModel.authState.collectAsState()
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
}