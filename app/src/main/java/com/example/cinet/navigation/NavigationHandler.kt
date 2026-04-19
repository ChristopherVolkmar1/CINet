package com.example.cinet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.feature.calendar.calendarFiles.CalendarViewModel
import com.example.cinet.ui.AuthState
import java.util.Calendar
import java.util.Locale
import com.example.cinet.feature.auth.*
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.CampusMapScreen

enum class Screen(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Default.Home),
    Social("Social", Icons.Default.People),
    Map("Map", Icons.Default.LocationOn),
    Calendar("Calendar", Icons.Default.CalendarMonth),
    Settings("Settings", Icons.Default.Settings)
}

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
    onSignOut: () -> Unit,
    viewModel: CampusRegistry = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val calendarViewModel: CalendarViewModel = viewModel()
    val campusRegistry by viewModel.campusRegistry.collectAsState()
    var preSelectedMapLocation by remember { mutableStateOf<CampusLocation?>(null) }

    val calendarScheduleItems = remember(calendarViewModel.classItems, calendarViewModel.scheduleItems) {
        val cal = Calendar.getInstance()
        val dayName = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US) ?: ""
        val dateStr = "%04d-%02d-%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val list = mutableListOf<Pair<String, String>>()
        calendarViewModel.classItems
        calendarViewModel.classItems
            .filter { it.meetingDays.contains(dayName) }
            .forEach { list.add(it.name to "${it.startTime} - ${it.endTime} | ${it.location}") }
        // Add assignments/tasks for today
        calendarViewModel.scheduleItems
            .filter { it.date == dateStr }
            .forEach { list.add(it.assignmentName to "Due: ${it.dueTime} (${it.className})") }
        list
    }

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var showAddClassOnCalendar by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var showProfileEdit by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("cinet_prefs", android.content.Context.MODE_PRIVATE) }

    fun loadItems(key: String): List<Pair<String, String>> {
        val saved = sharedPrefs.getString(key, null) ?: return emptyList()
        return saved.split("||").filter { it.contains("|") }.map {
            val parts = it.split("|")
            parts[0] to parts[1]
        }
    }

    fun saveItems(key: String, items: List<Pair<String, String>>) {
        val stringified = items.joinToString("||") { "${it.first}|${it.second}" }
        sharedPrefs.edit().putString(key, stringified).apply()
    }

    var upcomingEventsItems by remember { mutableStateOf(loadItems("event_items")) }

    BackHandler(enabled = currentScreen != Screen.Home || selectedProfile != null || activeConversation != null || showProfileEdit) {
        when {
            activeConversation != null -> activeConversation = null
            selectedProfile != null -> selectedProfile = null
            else -> currentScreen = Screen.Home
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = {
                            currentScreen = screen
                            if (screen != Screen.Social) {
                                selectedProfile = null
                                activeConversation = null
                            }
                            if (screen != Screen.Calendar) {
                                showAddClassOnCalendar = false
                                if (screen != Screen.Settings)
                                    showProfileEdit = false
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
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                    nickname = userProfile.nickname,
                    scheduleItems = calendarScheduleItems,
                    upcomingEventsItems = upcomingEventsItems,
                    onUpdateSchedule = { },
                    onUpdateEvents = {
                        upcomingEventsItems = it
                        saveItems("event_items", it)
                    },
                    onMapClick = { currentScreen = Screen.Map },
                    onSettingsClick = { currentScreen = Screen.Settings },
                    onCalendarClick = { currentScreen = Screen.Calendar },
                    onAddClassClick = {
                        showAddClassOnCalendar = true
                        currentScreen = Screen.Calendar
                    },
                    onNavigateToLocation = { locationName ->
                        val location = campusRegistry["academic"]?.find { it.name == locationName }
                        preSelectedMapLocation = location
                        currentScreen = Screen.Map
                    }
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
                    onBack = { currentScreen = Screen.Home },
                    preSelectedLocation = preSelectedMapLocation,
                    onFinishedLoading = {
                        preSelectedMapLocation = null
                    }
                )
                Screen.Calendar -> CalendarScreen(
                    onBack = {
                        currentScreen = Screen.Home
                        showAddClassOnCalendar = false
                    },
                    initialShowClassDialog = showAddClassOnCalendar
                )
                Screen.Settings -> if (showProfileEdit) {
                    ProfileEditScreen(
                        onBack = { showProfileEdit = false }
                    )
                } else {
                    SettingScreen(
                        onBack        = { currentScreen = Screen.Home },
                        onSignOut     = onSignOut,
                        onEditProfile = { showProfileEdit = true },
                        userProfile   = userProfile  // ← add this
                    )
                }
            }
        }
    }
}