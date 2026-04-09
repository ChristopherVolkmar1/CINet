package com.example.cinet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.ui.AuthState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

enum class Screen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Social("Social", Icons.Default.People),
    Social("Social", Icons.AutoMirrored.Filled.Chat),
    Map("Map", Icons.Default.LocationOn),
    Calendar("Calendar", Icons.Default.CalendarMonth),
    Settings("Settings", Icons.Default.Settings)
}

val PairListSaver: Saver<List<Pair<String, String>>, Any> = listSaver(
    save = { list ->
        list.flatMap { listOf(it.first, it.second) }
    },
    restore = { flattened ->
        val list = flattened as List<String>
        list.chunked(2).map { it[0] to it[1] }
    }
)

@Composable
fun NavigationHandler(
    authState: AuthState,
    onSignOut: () -> Unit,
    onRetry: () -> Unit,
    onSaveProfile: (String, String, String) -> Unit
) {
    when (authState) {
        is AuthState.Loading -> LoadingScreen()
        is AuthState.Unauthenticated -> LoginScreen()
        is AuthState.ProfileSetup -> ProfileSetupScreen(
            onSaveProfile = onSaveProfile
        )
        is AuthState.Error -> ErrorScreen(
            message = authState.message,
            onRetry = onRetry
        )
        is AuthState.Authenticated -> MainScaffold(
            userProfile = authState.userProfile,
            onSignOut = onSignOut
        )
    }
}

@Composable
private fun MainScaffold(
    userProfile: UserProfile,
    onSignOut: () -> Unit
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("cinet_prefs", android.content.Context.MODE_PRIVATE) }

    // Helper to load from SharedPreferences
    fun loadItems(key: String): List<Pair<String, String>> {
        val saved = sharedPrefs.getString(key, null) ?: return emptyList()
        return saved.split("||").filter { it.contains("|") }.map {
            val parts = it.split("|")
            parts[0] to parts[1]
        }
    }

    // Helper to save to SharedPreferences
    fun saveItems(key: String, items: List<Pair<String, String>>) {
        val stringified = items.joinToString("||") { "${it.first}|${it.second}" }
        sharedPrefs.edit().putString(key, stringified).apply()
    }

    // State for the schedule items
    var scheduleItems by remember { mutableStateOf(loadItems("schedule_items")) }
    // State for the upcoming events items
    var upcomingEventsItems by remember { mutableStateOf(loadItems("event_items")) }

    // Sub-navigation state for Social tab
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            // Reset social sub-navigation when leaving the tab
                            if (screen != Screen.Social) {
                                selectedProfile = null
                                activeConversation = null
                            }
                        },
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
                        scheduleItems = scheduleItems,
                        upcomingEventsItems = upcomingEventsItems,
                        onUpdateSchedule = {
                            scheduleItems = it
                            saveItems("schedule_items", it)
                        },
                        onUpdateEvents = {
                            upcomingEventsItems = it
                            saveItems("event_items", it)
                        },
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
                        onBack = { currentScreen = Screen.Home },
                        onSignOut = onSignOut
                    )
                }
            }
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
                Screen.Social -> when {
                    activeConversation != null -> ConversationScreen(
                        conversation = activeConversation!!,
                        onBack = { activeConversation = null }
                    )
                    selectedProfile != null -> ProfileScreen(
                        user = selectedProfile!!,
                        currentUserProfile = userProfile,
                        onOpenConversation = { activeConversation = it },
                        onBack = { selectedProfile = null }
                    )
                    else -> SocialScreen(
                        onOpenProfile = { selectedProfile = it }
                    )
                }
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
    }
}
