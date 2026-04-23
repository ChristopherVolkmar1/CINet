package com.example.cinet.app


import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.cinet.core.notifications.NotificationHelper
import com.example.cinet.core.permissions.PermissionManager
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.feature.auth.AuthState
import com.example.cinet.feature.auth.viewmodel.AuthViewModel
import com.example.cinet.feature.auth.viewmodel.AuthViewModelFactory
import com.example.cinet.feature.settings.AppSettings
import com.example.cinet.navigation.NavigationHandler
import com.example.cinet.ui.theme.CINetTheme


class MainActivity : ComponentActivity() {


    companion object {
        const val EXTRA_OPEN_MAP_FOR_LOCATION = "extra_open_map_for_location"
    }


    private var notificationMapLocation by mutableStateOf<String?>(null)


    private val repository by lazy { FirestoreRepository() }


    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(repository)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        notificationMapLocation = intent.getStringExtra(EXTRA_OPEN_MAP_FOR_LOCATION)


        NotificationHelper.createChannel(this)


        if (!PermissionManager.hasAllPermissions(this)) {
            PermissionManager.requestAllPermissions(this)
        }


        enableEdgeToEdge()


        setContent {
            val authState by authViewModel.authState.collectAsState()


            val isDarkMode = when (val state = authState) {
                is AuthState.Authenticated -> state.userProfile.isDarkMode
                is AuthState.ProfileSetup -> state.userProfile.isDarkMode
                else -> AppSettings.isDarkMode
            }


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
                    },
                    initialMapLocationName = notificationMapLocation,
                    onInitialMapLocationConsumed = { notificationMapLocation = null }
                )
            }
        }
    }


    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationMapLocation = intent.getStringExtra(EXTRA_OPEN_MAP_FOR_LOCATION)
    }
}

