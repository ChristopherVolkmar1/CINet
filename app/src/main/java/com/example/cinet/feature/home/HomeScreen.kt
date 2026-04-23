package com.example.cinet.feature.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinet.NewsArticle
import com.example.cinet.NewsRepository
import com.example.cinet.NewsSection
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.ui.theme.CINetTheme
import com.example.cinet.feature.map.*
import com.example.cinet.core.designsystem.InfoSection
import com.example.cinet.core.designsystem.WeatherDisplay
import java.util.Calendar

@Composable
fun HomeScreen(
    nickname: String,
    scheduleItems: List<Pair<String, String>>,
    manualUpcomingEventsItems: List<Pair<String, String>>,
    displayUpcomingEventsItems: List<HomeUpcomingEventItem>,
    onUpdateSchedule: (List<Pair<String, String>>) -> Unit,
    onUpdateEvents: (List<Pair<String, String>>) -> Unit,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onAddClassClick: () -> Unit = {},
    onCIViewClick: (NewsArticle?) -> Unit = {},
    onArticleClick: (NewsArticle) -> Unit = {},
    viewModel: CampusRegistry = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToLocation: (String) -> Unit
) {
    val academic by viewModel.academic.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var weatherInfo by remember { mutableStateOf(WeatherInfo("...", "Loading...")) }
    
    // Real news data from repository
    val newsRepository = remember { NewsRepository() }
    var newsArticles by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }

    // State for the "Add/Edit" dialog (Now only for Upcoming Events)
    var showDialog by remember { mutableStateOf(false) }
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    
    var nameField by remember { mutableStateOf("") }
    var timeOrDateField by remember { mutableStateOf("") }

    var locationField by remember { mutableStateOf<CampusLocation?>(null) }
    val textFieldState = rememberTextFieldState()
    val academicNames = remember(textFieldState.text, academic) {
        academic
            .filter { it.name.contains(textFieldState.text.toString(), ignoreCase = true) }
            .map { it.name }
    }

    var amPmSelection by remember { mutableStateOf("AM") } // AM/PM choice

    // Support Hours logic
    val supportHoursSubtitle = remember {
        val calendar = Calendar.getInstance()
        val info = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY -> 
                "11 AM - 7 PM | In Person & Online"
            Calendar.FRIDAY -> 
                "10 AM - 2 PM | In Person & Online"
            Calendar.SUNDAY -> 
                "5 PM - 7 PM | Online Only"
            else -> "Closed | No Support Today"
        }
        "LRC Availability: $info"
    }

    // Call the weather fetching logic on launch
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "LaunchedEffect triggered")
        weatherInfo = WeatherHelper.fetchCampusWeather(context)
        // Fetch real news
        newsArticles = newsRepository.fetchLatestNews()
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { 
                Text(if (editingIndex == null) "Add Upcoming Event" else "Edit Upcoming Event") 
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        label = { Text("Event Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = timeOrDateField,
                            onValueChange = { timeOrDateField = it },
                            label = { Text("Date/Time") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // AM/PM Toggle
                        Row(
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            FilterChip(
                                selected = amPmSelection == "AM",
                                onClick = { amPmSelection = "AM" },
                                label = { Text("AM") }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            FilterChip(
                                selected = amPmSelection == "PM",
                                onClick = { amPmSelection = "PM" },
                                label = { Text("PM") }
                            )
                        }
                    }

                    // Location
                    Spacer(modifier = Modifier.height(8.dp))
                    SearchLocationBar(
                        textFieldState = textFieldState,
                        searchResults = academicNames,
                        onSearch = { query ->
                            locationField = academic.find { it.name.equals(query, ignoreCase = true) }
                            textFieldState.edit { replace(0, length, query) }

                        }
                    )
                }
            },
            confirmButton = {
                @Suppress("DEPRECATION")
                Button(onClick = {
                    if (nameField.isNotBlank() && timeOrDateField.isNotBlank() && locationField != null) {
                        val fullTime = "$timeOrDateField $amPmSelection"
                        val newItem = nameField to "$fullTime | ${locationField?.name}"
                        
                        val newList = manualUpcomingEventsItems.toMutableList()
                        if (editingIndex != null) {
                            newList[editingIndex!!] = newItem
                        } else {
                            newList.add(newItem)
                        }
                        onUpdateEvents(newList)
                        
                        showDialog = false
                        editingIndex = null
                        nameField = ""
                        timeOrDateField = ""
                        locationField = null
                    }
                }) {
                    Text(if (editingIndex == null) "Add" else "Update")
                }
            },
            dismissButton = {
                Row {
                    if (editingIndex != null) {
                        TextButton(onClick = {
                            val newList = manualUpcomingEventsItems.toMutableList()
                            newList.removeAt(editingIndex!!)
                            onUpdateEvents(newList)
                            showDialog = false
                            editingIndex = null
                            nameField = ""
                            timeOrDateField = ""
                            locationField = null
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { 
                        showDialog = false
                        editingIndex = null
                        nameField = ""
                        timeOrDateField = ""
                        locationField = null
                    }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header Section
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Welcome back to CINet, $nickname",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                WeatherDisplay(
                    modifier = Modifier.fillMaxWidth(),
                    temp = weatherInfo.temp,
                    condition = weatherInfo.condition + " - Camarillo, CA"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CI View Section
        NewsSection(
            articles = newsArticles,
            onSeeAllClick = { onCIViewClick(null) },
            onArticleClick = { onCIViewClick(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Today's Schedule Section (Sourced from Calendar)
        InfoSection(
            title = "Today's Schedule",
            subtitle = supportHoursSubtitle,
            items = scheduleItems,
            onAddClick = onAddClassClick,
            onItemClick = { _ -> onCalendarClick() }, // Open calendar on item click
            onNavigateToLocation = onNavigateToLocation
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Upcoming Events Section
        InfoSection(
            title = "Upcoming Events",
            items = displayUpcomingEventsItems.map { it.title to it.description },
            onAddClick = {
                editingIndex = null
                nameField = ""
                timeOrDateField = ""
                locationField = null
                showDialog = true
            },
            onItemClick = { index ->
                val selectedItem = displayUpcomingEventsItems[index]
                if (selectedItem.isCampusEvent) {
                    onCalendarClick()
                } else {
                    val manualIndex = manualUpcomingEventsItems.indexOfFirst {
                        it.first == selectedItem.title && it.second == selectedItem.description
                    }

                    if (manualIndex == -1) {
                        onCalendarClick()
                    } else {
                        editingIndex = manualIndex
                        val item = manualUpcomingEventsItems[manualIndex]
                        nameField = item.first
                        // Simple parsing for demo purposes
                        val parts = item.second.split(" | ")
                        val timeParts = parts[0].split(" ")
                        timeOrDateField = if (timeParts.isNotEmpty()) timeParts[0] else ""
                        amPmSelection = if (timeParts.size > 1) timeParts[1] else "AM"
                        locationField = academic.find { it.name == if (parts.size > 1) parts[1] else "" }
                        showDialog = true
                    }
                }
            },
            onNavigateToLocation = onNavigateToLocation
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CINetTheme {
        HomeScreen(
            nickname = "User",
            scheduleItems = emptyList(),
            manualUpcomingEventsItems = emptyList(),
            displayUpcomingEventsItems = emptyList(),
            onUpdateSchedule = {},
            onUpdateEvents = {},
            onNavigateToLocation = { _ -> }
        )
    }
}
