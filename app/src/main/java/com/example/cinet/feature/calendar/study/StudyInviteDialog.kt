package com.example.cinet.feature.calendar.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.core.time.openTimePicker
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import com.example.cinet.feature.map.SearchLocationBar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyInviteDialog(
    existingItems: List<ScheduleItem>,
    existingStudySessions: List<StudySession> = emptyList(),
    onDismiss: () -> Unit,
    onSendExisting: (ScheduleItem) -> Unit,
    onSendExistingSession: (StudySession) -> Unit = {},
    onSendNew: (className: String, assignmentName: String, date: String, time: String, location: String) -> Unit,
    campusRegistry: CampusRegistry = viewModel<CampusRegistry>()
) {
    val context = LocalContext.current
    // false = pick from existing, true = create new
    var isCreatingNew by remember { mutableStateOf(false) }
    var newClassName by remember { mutableStateOf("") }
    var newAssignmentName by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var locationCategory by remember { mutableStateOf("academic") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val registryMap by campusRegistry.campusRegistry.collectAsState()
    val locationTextFieldState = rememberTextFieldState()

    val categoryLocations = remember(registryMap, locationCategory) {
        registryMap[locationCategory] ?: emptyList()
    }
    val locationSearchResults = remember(locationTextFieldState.text, categoryLocations) {
        categoryLocations
            .filter { it.name.contains(locationTextFieldState.text.toString(), ignoreCase = true) }
            .map { it.name }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        newDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isCreatingNew) "New Study Invite" else "Send Study Invite") },
        text = {
            Column {
                if (isCreatingNew) {
                    // Option B — create new study session on the spot
                    OutlinedTextField(
                        value = newClassName,
                        onValueChange = { newClassName = it },
                        label = { Text("Class name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newAssignmentName,
                        onValueChange = { newAssignmentName = it },
                        label = { Text("What to study") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newDate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date") },
                        placeholder = { Text("Tap to pick a date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Pick Date")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTime,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Time") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { openTimePicker(context) { newTime = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Pick Time")
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Category filter chips above location search bar
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "academic" to "Academic",
                            "dining" to "Dining",
                            "commuter_parking" to "Parking"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = locationCategory == key,
                                onClick = {
                                    locationCategory = key
                                    locationTextFieldState.edit { replace(0, length, "") }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    SearchLocationBar(
                        textFieldState = locationTextFieldState,
                        searchResults = locationSearchResults,
                        onSearch = { query ->
                            locationTextFieldState.edit { replace(0, length, query) }
                        }
                    )
                } else {
                    // Option A — pick from existing calendar items
                    val hasAnyItems = existingItems.isNotEmpty() || existingStudySessions.isNotEmpty()

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search…") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (!hasAnyItems) {
                        Text(
                            "No items found — create a new invite below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val filteredItems = existingItems.filter {
                            searchQuery.isBlank() ||
                                    it.className.contains(searchQuery, ignoreCase = true) ||
                                    it.assignmentName.contains(searchQuery, ignoreCase = true)
                        }
                        val filteredSessions = existingStudySessions.filter {
                            searchQuery.isBlank() ||
                                    it.className.contains(searchQuery, ignoreCase = true) ||
                                    it.topic.contains(searchQuery, ignoreCase = true)
                        }
                        LazyColumn {
                            if (filteredItems.isNotEmpty()) {
                                item {
                                    Text(
                                        "Assignments",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(filteredItems) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onSendExisting(item) }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(text = item.className, style = MaterialTheme.typography.titleSmall)
                                            Text(text = item.assignmentName)
                                            Text(
                                                text = "Due: ${item.dueTime} on ${item.date}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }

                            if (filteredSessions.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Study Sessions",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(filteredSessions) { session ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable { onSendExistingSession(session) }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(text = session.className, style = MaterialTheme.typography.titleSmall)
                                            Text(text = session.topic)
                                            Text(
                                                text = "${session.startTime} on ${session.date}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { isCreatingNew = true }) {
                        Text("Create new instead")
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                TextButton(
                    onClick = {
                        if (newClassName.isNotBlank() && newAssignmentName.isNotBlank()
                            && newDate.isNotBlank() && newTime.isNotBlank()
                        ) {
                            onSendNew(
                                newClassName,
                                newAssignmentName,
                                newDate,
                                newTime,
                                locationTextFieldState.text.toString()
                            )
                        }
                    }
                ) { Text("Send") }
            }
        },
        dismissButton = {
            // Back returns to picker, Cancel closes entirely
            TextButton(onClick = {
                if (isCreatingNew) isCreatingNew = false else onDismiss()
            }) {
                Text(if (isCreatingNew) "Back" else "Cancel")
            }
        }
    )
}