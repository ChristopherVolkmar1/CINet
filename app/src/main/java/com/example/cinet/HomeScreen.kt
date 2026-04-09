package com.example.cinet

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cinet.ui.theme.CINetTheme

@Composable
fun HomeScreen(
    scheduleItems: List<Pair<String, String>>,
    upcomingEventsItems: List<Pair<String, String>>,
    onUpdateSchedule: (List<Pair<String, String>>) -> Unit,
    onUpdateEvents: (List<Pair<String, String>>) -> Unit,
    modifier: Modifier = Modifier,
    onMapClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var weatherInfo by remember { mutableStateOf(WeatherInfo("...", "Loading...")) }
    
    // State for the "Add/Edit" dialog
    var showDialog by remember { mutableStateOf(false) }
    var addingToSchedule by remember { mutableStateOf(true) } // To distinguish between Schedule and Events
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    
    var nameField by remember { mutableStateOf("") }
    var timeOrDateField by remember { mutableStateOf("") }
    var locationField by remember { mutableStateOf("") }
    var amPmSelection by remember { mutableStateOf("AM") } // AM/PM choice

    // Call the weather fetching logic on launch
    LaunchedEffect(Unit) {
        Log.d("HomeScreen", "LaunchedEffect triggered")
        weatherInfo = WeatherHelper.fetchCampusWeather(context)
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { 
                Text(
                    if (editingIndex == null) {
                        if (addingToSchedule) "Add Schedule Item" else "Add Upcoming Event"
                    } else {
                        if (addingToSchedule) "Edit Schedule Item" else "Edit Upcoming Event"
                    }
                ) 
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = nameField,
                        onValueChange = { nameField = it },
                        label = { Text(if (addingToSchedule) "Class Name" else "Event Name") },
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
                            label = { Text(if (addingToSchedule) "Time (e.g. 10:00)" else "Date/Time") },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // AM/PM Toggle Segmented Control-like Buttons
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
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = locationField,
                        onValueChange = { locationField = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nameField.isNotBlank() && timeOrDateField.isNotBlank() && locationField.isNotBlank()) {
                        val fullTime = "$timeOrDateField $amPmSelection"
                        val newItem = nameField to "$fullTime - $locationField"
                        
                        if (addingToSchedule) {
                            val newList = scheduleItems.toMutableList()
                            if (editingIndex != null) {
                                newList[editingIndex!!] = newItem
                            } else {
                                newList.add(newItem)
                            }
                            onUpdateSchedule(newList)
                        } else {
                            val newList = upcomingEventsItems.toMutableList()
                            if (editingIndex != null) {
                                newList[editingIndex!!] = newItem
                            } else {
                                newList.add(newItem)
                            }
                            onUpdateEvents(newList)
                        }
                        
                        showDialog = false
                        editingIndex = null
                        nameField = ""
                        timeOrDateField = ""
                        locationField = ""
                    }
                }) {
                    Text(if (editingIndex == null) "Add" else "Update")
                }
            },
            dismissButton = {
                Row {
                    if (editingIndex != null) {
                        TextButton(onClick = {
                            if (addingToSchedule) {
                                val newList = scheduleItems.toMutableList()
                                newList.removeAt(editingIndex!!)
                                onUpdateSchedule(newList)
                            } else {
                                val newList = upcomingEventsItems.toMutableList()
                                newList.removeAt(editingIndex!!)
                                onUpdateEvents(newList)
                            }
                            showDialog = false
                            editingIndex = null
                            nameField = ""
                            timeOrDateField = ""
                            locationField = ""
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { 
                        showDialog = false
                        editingIndex = null
                        nameField = ""
                        timeOrDateField = ""
                        locationField = ""
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
                    text = "Welcome back to CINet, [user]",
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

        // Today's Schedule Section
        InfoSection(
            title = "Today's Schedule",
            items = scheduleItems,
            onAddClick = { 
                addingToSchedule = true
                editingIndex = null
                nameField = ""
                timeOrDateField = ""
                locationField = ""
                showDialog = true 
            },
            onItemClick = { index ->
                addingToSchedule = true
                editingIndex = index
                val item = scheduleItems[index]
                nameField = item.first
                // Simple parsing for demo purposes
                val parts = item.second.split(" - ")
                val timeParts = parts[0].split(" ")
                timeOrDateField = if (timeParts.isNotEmpty()) timeParts[0] else ""
                amPmSelection = if (timeParts.size > 1) timeParts[1] else "AM"
                locationField = if (parts.size > 1) parts[1] else ""
                showDialog = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Upcoming Events Section
        InfoSection(
            title = "Upcoming Events",
            items = upcomingEventsItems,
            onAddClick = {
                addingToSchedule = false
                editingIndex = null
                nameField = ""
                timeOrDateField = ""
                locationField = ""
                showDialog = true
            },
            onItemClick = { index ->
                addingToSchedule = false
                editingIndex = index
                val item = upcomingEventsItems[index]
                nameField = item.first
                // Simple parsing for demo purposes
                val parts = item.second.split(" - ")
                val timeParts = parts[0].split(" ")
                timeOrDateField = if (timeParts.isNotEmpty()) timeParts[0] else ""
                amPmSelection = if (timeParts.size > 1) timeParts[1] else "AM"
                locationField = if (parts.size > 1) parts[1] else ""
                showDialog = true
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    CINetTheme {
        HomeScreen(
            scheduleItems = emptyList(),
            upcomingEventsItems = emptyList(),
            onUpdateSchedule = {},
            onUpdateEvents = {}
        )
    }
}
