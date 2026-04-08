package com.example.cinet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.cinet.ui.AuthState

enum class Screen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Social("Social", Icons.AutoMirrored.Filled.Chat),
    Map("Map", Icons.Default.LocationOn),
    Calendar("Calendar", Icons.Default.CalendarMonth),
    Settings("Settings", Icons.Default.Settings)
}

@Composable
fun NavigationHandler(
    authState: AuthState,
    onSignOut: () -> Unit,
    onRetry: () -> Unit
) {
    when (authState) {
        is AuthState.Loading -> LoadingScreen()
        is AuthState.Unauthenticated -> LoginScreen()
        is AuthState.Error -> ErrorScreen(
            message = authState.message,
            onRetry = onRetry
        )
        is AuthState.Authenticated -> MainScaffold(
            onSignOut = onSignOut
        )
    }
}

@Composable
private fun MainScaffold(onSignOut: () -> Unit) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        label = {
                            Text(
                                text = screen.label,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.label
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onMapClick = { currentScreen = Screen.Map },
                    onSettingsClick = { currentScreen = Screen.Settings }
                )
                Screen.Notifications -> NotificationScreen(
                    onBack = { currentScreen = Screen.Home }
                )
                Screen.Map -> CampusMapScreen(
                    onBack = { currentScreen = Screen.Home }
                )
                Screen.Calendar -> CalendarScreen(
                    onBack = { currentScreen = Screen.Home }
                )
                Screen.Settings -> SettingScreen(
                    onBack = { currentScreen = Screen.Home },
                    onSignOut = onSignOut
                )
            }
        }
            // Main content area that swaps between screens
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        onMapClick = { currentScreen = Screen.Map },
                        onSettingsClick = { currentScreen = Screen.Settings }
                    )
                    Screen.Social -> NotificationScreen(
                        onBack = { currentScreen = Screen.Home }
                    )
                    Screen.Map -> CampusMapScreen(
                        onBack = { currentScreen = Screen.Home }
                    )
                    Screen.Calendar -> CalendarScreen(
                        onBack = { currentScreen = Screen.Home }
                    )
                    Screen.Settings -> SettingScreen(
                        onBack = { currentScreen = Screen.Home }
                    )
                }
            }
    }
}