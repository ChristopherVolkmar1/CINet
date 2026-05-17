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

@Composable
private fun MainScaffold(
    userProfile: UserProfile,
    onSignOut: () -> Unit,
    initialConversationId: String? = null,
    onConversationOpened: () -> Unit = {},
    initialMapLocationName: String? = null,
    onMapLocationOpened: () -> Unit = {},
) {
    val authViewModel: AuthViewModel = viewModel()
    val calendarViewModel: CalendarViewModel = viewModel()
    val campusRegistryViewModel: CampusRegistry = viewModel()
    val campusRegistry by campusRegistryViewModel.campusRegistry.collectAsState()

    var preSelectedMapLocation by remember { mutableStateOf<CampusLocation?>(null) }
    var autoRouteToPreSelectedMapLocation by remember { mutableStateOf(false) }

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
    var screenHistory by remember { mutableStateOf(listOf<Screen>()) }

    fun navigateTo(screen: Screen) {
        if (currentScreen != screen) {
            screenHistory = (screenHistory + currentScreen).takeLast(15)
            currentScreen = screen
        }
    }

    fun popScreen() {
        if (screenHistory.isNotEmpty()) {
            currentScreen = screenHistory.last()
            screenHistory = screenHistory.dropLast(1)
        } else if (currentScreen != Screen.Home) {
            currentScreen = Screen.Home
        }
    }

    var showAddClassOnCalendar by remember { mutableStateOf(false) }
    var showProfileEdit by remember { mutableStateOf(false) }

    var activeConversation by remember { mutableStateOf<Conversation?>(null) }
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var showNewConversation by remember { mutableStateOf(false) }
    var showSocialScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val sharedPrefs = remember {
        context.getSharedPreferences("cinet_prefs", android.content.Context.MODE_PRIVATE)
    }

    var openedConversationTimestamps by remember {
        mutableStateOf(
            sharedPrefs.all
                .filter { it.key.startsWith("opened_conv_") }
                .mapKeys { it.key.removePrefix("opened_conv_") }
                .mapValues { (it.value as? Long) ?: 0L }
        )
    }

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
                navigateTo(Screen.Social)
                val now = System.currentTimeMillis()
                openedConversationTimestamps =
                    openedConversationTimestamps + (conversation.id to now)
                sharedPrefs.edit()
                    .putLong("opened_conv_${conversation.id}", now)
                    .apply()
                activeConversation = conversation
                onConversationOpened()
            }
        } catch (e: Exception) {
            android.util.Log.e(
                "NavigationHandler",
                "Failed to open conversation from notification: ${e.message}"
            )
        }
    }

    LaunchedEffect(initialMapLocationName, campusRegistry) {
        val locationName = initialMapLocationName ?: return@LaunchedEffect
        if (campusRegistry.isEmpty()) return@LaunchedEffect

        val location = campusRegistry.values.flatten()
            .find { it.name.equals(locationName, ignoreCase = true) }

        if (location != null) {
            preSelectedMapLocation = location
            autoRouteToPreSelectedMapLocation = true
            navigateTo(Screen.Map)
            onMapLocationOpened()
        } else {
            android.util.Log.e(
                "NavigationHandler",
                "Failed to find map location from notification: $locationName"
            )
        }
    }

    // News / CIView state
    var showCIView by remember { mutableStateOf(false) }
    var selectedNewsArticle by remember { mutableStateOf<NewsArticle?>(null) }

    // Clubs state
    var showClubs by remember { mutableStateOf(false) }
    var selectedClub by remember { mutableStateOf<ClubItem?>(null) }

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

    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30000)
            currentTimeMillis = System.currentTimeMillis()
        }
    }

    val displayUpcomingEventsItems = remember(
        manualUpcomingEventsItems,
        calendarViewModel.campusEventItems,
        calendarViewModel.userEventItems,
        calendarViewModel.classItems,
        currentTimeMillis
    ) {
        buildHomeUpcomingEventItems(
            context = context,
            manualItems = manualUpcomingEventsItems,
            campusEvents = calendarViewModel.campusEventItems,
            userEvents = calendarViewModel.userEventItems,
            classItems = calendarViewModel.classItems,
            currentTimeMillis = currentTimeMillis
        )
    }

    val isShowingNews = showCIView || selectedNewsArticle != null
    val isShowingClubs = showClubs || selectedClub != null
    val isOverlayActive = isShowingNews || isShowingClubs

    val socialBackStackActive = currentScreen == Screen.Social &&
            (activeConversation != null || selectedProfile != null ||
                    showNewConversation || showSocialScreen)

    BackHandler(
        enabled = currentScreen != Screen.Home ||
                socialBackStackActive ||
                showProfileEdit ||
                isOverlayActive ||
                selectedProfile != null ||
                screenHistory.isNotEmpty()
    ) {
        when {
            selectedNewsArticle != null -> {
                val title = selectedNewsArticle?.title
                if (title == "Study Rooms" || title == "Dining") {
                    selectedNewsArticle = null
                    showCIView = false
                    if (title == "Dining") {
                        currentScreen = Screen.Home
                        screenHistory = emptyList()
                    }
                } else {
                    selectedNewsArticle = null
                    showCIView = true
                }
            }

            showCIView -> showCIView = false
            selectedClub != null -> selectedClub = null
            showClubs -> showClubs = false
            activeConversation != null -> activeConversation = null
            showNewConversation -> showNewConversation = false
            selectedProfile != null -> selectedProfile = null
            showSocialScreen -> showSocialScreen = false
            showProfileEdit -> showProfileEdit = false
            else -> popScreen()
        }
    }

    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0

    val hideBottomBarForConversationTyping =
        currentScreen == Screen.Social &&
                activeConversation != null &&
                isKeyboardOpen

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isShowingNews && !hideBottomBarForConversationTyping) {
                NavigationBar {
                    Screen.entries.forEach { screen ->
                        NavigationBarItem(
                            selected = currentScreen == screen,
                            onClick = {
                                navigateTo(screen)
                                showCIView = false
                                selectedNewsArticle = null
                                showClubs = false
                                selectedClub = null

                                if (screen == Screen.Social) {
                                    activeConversation = null
                                    selectedProfile = null
                                    showNewConversation = false
                                    showSocialScreen = false
                                }

                                if (screen == Screen.Settings) {
                                    selectedProfile = null
                                    showProfileEdit = false
                                }

                                if (screen != Screen.Calendar) {
                                    showAddClassOnCalendar = false
                                    if (screen != Screen.Settings) {
                                        showProfileEdit = false
                                    }
                                }

                                if (screen != Screen.Map) {
                                    preSelectedMapLocation = null
                                    autoRouteToPreSelectedMapLocation = false
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

        val contentPadding = if (isShowingNews || hideBottomBarForConversationTyping) {
            PaddingValues(0.dp)
        } else {
            innerPadding
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {

            var sharedLocations by remember { mutableStateOf<List<CampusLocation>>(emptyList()) }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = if (currentScreen == Screen.Map) 1f else 0f }
                    .then(
                        if (currentScreen != Screen.Map)
                            Modifier.pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) { awaitPointerEvent() }
                                }
                            }
                        else Modifier
                    )
            ) {
                CampusMapScreen(
                    onBack = { popScreen() },
                    preSelectedLocation = preSelectedMapLocation,
                    autoRouteToPreSelectedLocation = autoRouteToPreSelectedMapLocation,
                    onFinishedLoading = {
                        preSelectedMapLocation = null
                        autoRouteToPreSelectedMapLocation = false
                    },
                    extraLocations = sharedLocations,
                    onRemoveExtraLocation = { location ->
                        sharedLocations = sharedLocations - location
                    }
                )
            }

            if (currentScreen != Screen.Map) {
                if (isShowingNews) {
                    CIViewScreen(
                        selectedArticleUrl = selectedNewsArticle?.url,
                        selectedArticleTitle = selectedNewsArticle?.title,
                        onArticleClick = { selectedNewsArticle = it },
                        onBack = {
                            if (selectedNewsArticle?.title == "Study Rooms") {
                                selectedNewsArticle = null
                                showCIView = false
                            } else if (selectedNewsArticle?.title == "Dining") {
                                selectedNewsArticle = null
                                showCIView = false
                                currentScreen = Screen.Home
                                screenHistory = emptyList()
                            } else if (selectedNewsArticle != null) {
                                selectedNewsArticle = null
                                showCIView = true
                            } else {
                                showCIView = false
                            }
                        }
                    )
                } else if (isShowingClubs) {
                    ClubsScreen(
                        selectedClubUrl = selectedClub?.url,
                        selectedClubTitle = selectedClub?.title,
                        onClubClick = { selectedClub = it },
                        onBack = {
                            if (selectedClub != null) {
                                selectedClub = null
                            } else {
                                showClubs = false
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
                            onMapClick = { navigateTo(Screen.Map) },
                            onSettingsClick = { navigateTo(Screen.Settings) },
                            onCalendarClick = { navigateTo(Screen.Calendar) },
                            onAddClassClick = {
                                showAddClassOnCalendar = true
                                navigateTo(Screen.Calendar)
                            },
                            onClubsClick = { showClubs = true },
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
                            onSocialClick = {
                                navigateTo(Screen.Social)
                                activeConversation = null
                                selectedProfile = null
                                showNewConversation = false
                                showSocialScreen = false
                            },
                            onNotificationClick = { navigateTo(Screen.Settings) },
                            onProfileClick = {
                                selectedProfile = userProfile
                                navigateTo(Screen.Settings)
                            },
                            onNavigateToLocation = { locationName ->
                                val location = campusRegistry.values.flatten()
                                    .find { it.name.equals(locationName, ignoreCase = true) }

                                preSelectedMapLocation = location
                                autoRouteToPreSelectedMapLocation = false
                                navigateTo(Screen.Map)
                            }
                        )

                        Screen.Social -> when {
                            activeConversation != null -> ConversationScreen(
                                conversation = activeConversation!!,
                                onBack = { activeConversation = null },
                                onNavigateToLocation = { locationName ->
                                    val location = campusRegistry.values.flatten()
                                        .find { it.name.equals(locationName, ignoreCase = true) }

                                    preSelectedMapLocation = location
                                    autoRouteToPreSelectedMapLocation = false
                                    navigateTo(Screen.Map)
                                },
                                onNavigateToCoordinates = { lat, lng, nickname, photoUrl  ->
                                    val sharedLocation = CampusLocation(
                                        name = "$nickname's Location",
                                        category = "SHARED",
                                        description = photoUrl,
                                        coordinates = GeoPoint(lat, lng)
                                    )
                                    val alreadyExists = sharedLocations.any {
                                        it.coordinates.latitude == lat && it.coordinates.longitude == lng
                                    }
                                    if (!alreadyExists) {
                                        sharedLocations = sharedLocations + sharedLocation
                                    }
                                    preSelectedMapLocation = sharedLocation
                                    autoRouteToPreSelectedMapLocation = false
                                    navigateTo(Screen.Map)
                                }
                            )

                            selectedProfile != null -> ProfileScreen(
                                user = if (selectedProfile!!.uid == userProfile.uid) {
                                    userProfile
                                } else {
                                    selectedProfile!!
                                },
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
                                },
                                onBack = { showSocialScreen = false }
                            )

                            else -> {
                                ConversationsListScreen(
                                    onOpenConversation = {
                                        val now = System.currentTimeMillis()
                                        openedConversationTimestamps =
                                            openedConversationTimestamps + (it.id to now)
                                        sharedPrefs.edit()
                                            .putLong("opened_conv_${it.id}", now)
                                            .apply()
                                        activeConversation = it
                                    },
                                    onNewConversation = { showNewConversation = true },
                                    onOpenFriends = { showSocialScreen = true },
                                    openedConversationTimestamps = openedConversationTimestamps,
                                    onSeedTimestamps = { ids ->
                                        val now = System.currentTimeMillis()
                                        val seeded = ids.associateWith { now }
                                        openedConversationTimestamps =
                                            openedConversationTimestamps + seeded
                                        val editor = sharedPrefs.edit()
                                        ids.forEach { id ->
                                            editor.putLong("opened_conv_$id", now)
                                        }
                                        editor.apply()
                                    },
                                )
                            }
                        }

                        Screen.Map -> {  }

                        Screen.Calendar -> CalendarScreen(
                            onBack = {
                                popScreen()
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
                            val displayProfile = if (selectedProfile!!.uid == userProfile.uid) {
                                userProfile
                            } else {
                                selectedProfile!!
                            }

                            ProfileScreen(
                                user = displayProfile,
                                currentUserProfile = userProfile,
                                onOpenConversation = {
                                    activeConversation = it
                                    navigateTo(Screen.Social)
                                },
                                onBack = {
                                    selectedProfile = null
                                },
                                onEditProfile = { showProfileEdit = true },
                            )
                        } else {
                            SettingScreen(
                                onBack = { popScreen() },
                                onSignOut = onSignOut,
                                isDarkMode = userProfile.isDarkMode,
                                notificationsEnabled = userProfile.notificationsEnabled,
                                selectedTheme = AppSettings.selectedTheme,
                                onSettingsChange = { dark, notify, theme ->
                                    authViewModel.updateSettings(dark, notify, theme)
                                },
                                userProfile = userProfile,
                                onViewProfile = {
                                    selectedProfile = userProfile
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}