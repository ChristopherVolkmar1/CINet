package com.example.cinet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.ui.AuthState
import com.example.cinet.ui.theme.CINetTheme
import com.example.cinet.viewmodels.AuthViewModel
import com.example.cinet.viewmodels.AuthViewModelFactory

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
            val authState by authViewModel.authState.collectAsState()

            // When the user is authenticated, load their persistent settings from Firebase
            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated || authState is AuthState.ProfileSetup) {
                    AppSettings.loadFromFirebase()
                }
            }

            // App theme now watches the global dark mode setting
            CINetTheme(darkTheme = AppSettings.isDarkMode) {
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