package com.example.cinet.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.feature.home.news.CIViewScreen
import com.example.cinet.feature.home.news.NewsArticle
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.SocialRepository
import com.example.cinet.feature.auth.AuthState
import com.example.cinet.feature.auth.ErrorScreen
import com.example.cinet.feature.auth.LoadingScreen
import com.example.cinet.feature.auth.LoginScreen
import com.example.cinet.feature.auth.ProfileSetupScreen
import com.example.cinet.feature.auth.viewmodel.AuthViewModel
import com.example.cinet.feature.calendar.calendarFiles.CalendarScreen
import com.example.cinet.feature.calendar.calendarFiles.CalendarViewModel
import com.example.cinet.feature.home.HomeScreen
import com.example.cinet.feature.home.buildHomeUpcomingEventItems
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.CampusMapScreen
import com.example.cinet.feature.profile.ProfileEditScreen
import com.example.cinet.feature.profile.ProfileScreen
import com.example.cinet.feature.settings.AppSettings
import com.example.cinet.feature.settings.SettingScreen
import com.example.cinet.feature.social.ConversationScreen
import com.example.cinet.feature.social.ConversationsListScreen
import com.example.cinet.feature.social.NewConversationScreen
import com.example.cinet.feature.social.SocialScreen
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Locale

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
    onSaveProfile: (String, String, String) -> Unit,
    initialConversationId: String? = null,
    onConversationOpened: () -> Unit = {},
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
        is AuthState.ProfileSetup -> ProfileSetupScreen(
            onSaveProfile = onSaveProfile
        )
        is AuthState.Error -> ErrorScreen(
            message = authState.message,
            onRetry = onRetry
        )
        is AuthState.Authenticated -> MainScaffold(
            userProfile = authState.userProfile,
            onSignOut = onSignOut,
            initialConversationId = initialConversationId,
            onConversationOpened = onConversationOpened,
        )
    }
}

@Composable
private fun MainScaffold(
    userProfile: UserProfile,
    onSignOut: () -> Unit,
    initialConversationId: String? = null,
    onConversationOpened: () -> Unit = {},
) {
    val authViewModel: AuthViewModel = viewModel()
    val calendarViewModel: CalendarViewModel = viewModel()
    val campusRegistryViewModel: CampusRegistry = viewModel()
    val campusRegistry by campusRegistryViewModel.campusRegistry.collectAsState()
    var preSelectedMapLocation by remember { mutableStateOf<CampusLocation?>(null) }

    val socialScope = rememberCoroutineScope()
    val socialRepository = remember { SocialRepository() }

    // Sync global AppSettings object with the user profile from Firebase
    LaunchedEffect(userProfile) {
        AppSettings.isDarkMode = userProfile.isDarkMode
        AppSettings.notificationsEnabled = userProfile.notificationsEnabled
    }

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
            .filter { it.meetingDays.contains(dayName) }
            .forEach { list.add(it.name to "${it.startTime} - ${it.endTime} | ${it.location}") }
        calendarViewModel.scheduleItems
            .filter { it.date == dateStr }
            .forEach { list.add(it.assignmentName to "Due: ${it.dueTime} (${it.className})") }
        list
    }

    var currentScreen by remember { mutableStateOf(Screen.Home) }
    var showAddClassOnCalendar by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }

    // Social sub-navigation stack
    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showNewConversation by remember { mutableStateOf(false) }
    var showSocialScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("cinet_prefs", android.content.Context.MODE_PRIVATE) }
    // Persisted: last time the user had the conversations list visible.
    // Default to currentTimeMillis on first launch so existing conversations
    // don't all appear as unread.
    var lastConversationsVisit by remember {
        mutableStateOf(
            sharedPrefs.getLong("last_conversations_visit", 0L).let { saved ->
                if (saved == 0L) System.currentTimeMillis() else saved
            }
        )
    }
    // Maps conversationId -> timestamp when opened; dot shows if new message arrived after
    var openedConversationTimestamps by remember { mutableStateOf(mapOf<String, Long>()) }

    // When the user taps a push notification, open the referenced conversation directly.
    LaunchedEffect(initialConversationId) {
        val id = initialConversationId ?: return@LaunchedEffect
        try {
            val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("conversations")
                .document(id)
                .get()
                .await()
            val conversation = snap.toObject(Conversation::class.java)
            if (conversation != null) {
                currentScreen = Screen.Social
                openedConversationTimestamps = openedConversationTimestamps + (conversation.id to System.currentTimeMillis())
                activeConversation = conversation
                onConversationOpened()
            }
        } catch (e: Exception) {
            android.util.Log.e("NavigationHandler", "Failed to open conversation from notification: ${e.message}")
        }
    }

    // News / CIView state
    var showCIView by remember { mutableStateOf(false) }
    var selectedNewsArticle by remember { mutableStateOf<NewsArticle?>(null) }

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

    var manualUpcomingEventsItems by remember { mutableStateOf(loadItems("event_items")) }

    val displayUpcomingEventsItems = remember(manualUpcomingEventsItems, calendarViewModel.campusEventItems) {
        buildHomeUpcomingEventItems(context, manualUpcomingEventsItems, calendarViewModel.campusEventItems)
    }

    val isShowingNews = showCIView || selectedNewsArticle != null
    val socialBackStackActive = currentScreen == Screen.Social &&
            (activeConversation != null || selectedProfile != null ||
                    showNewConversation || showSocialScreen)

    BackHandler(enabled = currentScreen != Screen.Home || socialBackStackActive || showProfileEdit || isShowingNews || selectedProfile != null) {
        when {
            selectedNewsArticle != null -> {
                if (selectedNewsArticle?.title == "Study Rooms") {
                    selectedNewsArticle = null
                    showCIView = false
                } else {
                    selectedNewsArticle = null
                    showCIView = true
                }
            }
            showCIView -> showCIView = false
            activeConversation != null -> activeConversation = null
            showNewConversation -> showNewConversation = false
            selectedProfile != null -> selectedProfile = null
            showSocialScreen -> showSocialScreen = false
            showProfileEdit -> showProfileEdit = false
            else -> currentScreen = Screen.Home
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isShowingNews) {
                NavigationBar {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                currentScreen = screen
                                showCIView = false
                                selectedNewsArticle = null
                                // Tapping Social from any other tab OR while already on Social
                                // always returns to the Messages (ConversationsListScreen) root.
                                if (screen == Screen.Social) {
                                    activeConversation = null
                                    selectedProfile = null
                                    showNewConversation = false
                                    showSocialScreen = false
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
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isShowingNews) androidx.compose.foundation.layout.PaddingValues(0.dp) else innerPadding)
        ) {
            if (isShowingNews) {
                CIViewScreen(
                    selectedArticleUrl = selectedNewsArticle?.url,
                    selectedArticleTitle = selectedNewsArticle?.title,
                    onArticleClick = { selectedNewsArticle = it },
                    onBack = {
                        if (selectedNewsArticle?.title == "Study Rooms") {
                            selectedNewsArticle = null
                            showCIView = false
                        } else if (selectedNewsArticle != null) {
                            selectedNewsArticle = null
                            showCIView = true
                        } else {
                            showCIView = false
                        }
                    }
                )
            } else {
                when (currentScreen) {
                    Screen.Home -> HomeScreen(
                        nickname = userProfile.nickname,
                        scheduleItems = calendarScheduleItems,
                        manualUpcomingEventsItems = manualUpcomingEventsItems,
                        displayUpcomingEventsItems = displayUpcomingEventsItems,
                        onUpdateSchedule = { },
                        onUpdateEvents = {
                            manualUpcomingEventsItems = it
                            saveItems("event_items", it)
                        },
                        onMapClick = { currentScreen = Screen.Map },
                        onSettingsClick = { currentScreen = Screen.Settings },
                        onCalendarClick = { currentScreen = Screen.Calendar },
                        onAddClassClick = {
                            showAddClassOnCalendar = true
                            currentScreen = Screen.Calendar
                        },
                        onCIViewClick = { article ->
                            if (article != null) {
                                selectedNewsArticle = article
                            } else {
                                showCIView = true
                            }
                        },
                        onArticleClick = { article ->
                            selectedNewsArticle = article
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
                            onBack = { activeConversation = null },
                            onNavigateToLocation = { locationName ->
                                // Search all categories so any campus location works
                                val location = campusRegistry.values.flatten()
                                    .find { it.name.equals(locationName, ignoreCase = true) }
                                preSelectedMapLocation = location
                                currentScreen = Screen.Map
                            }
                        )
                        selectedProfile != null -> ProfileScreen(
                            user = if (selectedProfile!!.uid == userProfile.uid) userProfile else selectedProfile!!,
                            currentUserProfile = userProfile,
                            onOpenConversation = { activeConversation = it },
                            onBack = { selectedProfile = null },
                            onEditProfile = { showProfileEdit = true },
                        )
                        showNewConversation -> NewConversationScreen(
                            currentUserProfile = userProfile,
                            onBack = { showNewConversation = false },
                            onOpenConversation = {
                                showNewConversation = false
                                activeConversation = it
                            }
                        )
                        showSocialScreen -> SocialScreen(
                            onOpenProfile = { selectedProfile = it },
                            onOpenConversation = { friend ->
                                socialScope.launch {
                                    socialRepository.getOrCreateConversation(
                                        participantIds = listOf(userProfile.uid, friend.uid),
                                        participantNicknames = mapOf(
                                            userProfile.uid to userProfile.nickname,
                                            friend.uid to friend.nickname
                                        )
                                    ).onSuccess {
                                        showSocialScreen = false
                                        activeConversation = it
                                    }
                                }
                            }
                        )
                        else -> {
                            // Write visit time to prefs for next session — do NOT update
                            // lastConversationsVisit state so dots stay visible this session
                            LaunchedEffect(Unit) {
                                sharedPrefs.edit()
                                    .putLong("last_conversations_visit", System.currentTimeMillis())
                                    .apply()
                            }
                            ConversationsListScreen(
                                onOpenConversation = {
                                    openedConversationTimestamps = openedConversationTimestamps + (it.id to System.currentTimeMillis())
                                    activeConversation = it
                                },
                                onNewConversation = { showNewConversation = true },
                                onOpenFriends = { showSocialScreen = true },
                                sessionStartTime = lastConversationsVisit,
                                openedConversationTimestamps = openedConversationTimestamps,
                            )
                        }
                    }

                    Screen.Map -> CampusMapScreen(
                        onBack = { currentScreen = Screen.Home },
                        preSelectedLocation = preSelectedMapLocation,
                        onFinishedLoading = { preSelectedMapLocation = null }
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
                            onBack = { showProfileEdit = false },
                            onSaved = { authViewModel.silentReloadProfile() },
                        )
                    } else if (selectedProfile != null) {
                        // For own profile, pass userProfile (live) so saved changes
                        // are reflected immediately without needing a refresh.
                        val displayProfile = if (selectedProfile!!.uid == userProfile.uid)
                            userProfile else selectedProfile!!
                        ProfileScreen(
                            user = displayProfile,
                            currentUserProfile = userProfile,
                            onOpenConversation = { activeConversation = it; currentScreen = Screen.Social },
                            onBack = { selectedProfile = null },
                            onEditProfile = { showProfileEdit = true },
                        )
                    } else {
                        SettingScreen(
                            onBack = { currentScreen = Screen.Home },
                            onSignOut = onSignOut,
                            isDarkMode = userProfile.isDarkMode,
                            notificationsEnabled = userProfile.notificationsEnabled,
                            selectedTheme = AppSettings.selectedTheme,
                            onSettingsChange = { dark, notify, theme ->
                                authViewModel.updateSettings(dark, notify, theme)
                            },
                            userProfile = userProfile,
                            onViewProfile = { selectedProfile = userProfile },
                        )
                    }
                }
            }
        }
    }
}