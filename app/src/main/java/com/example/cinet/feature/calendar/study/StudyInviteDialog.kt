package com.example.cinet.feature.calendar.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.core.time.openTimePicker
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.calendar.calendarFiles.CalendarFirestoreRepository
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import com.example.cinet.feature.map.SearchBar
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Category key → display label pairs shown as filter chips above the search bar.
private val locationCategories = listOf(
    "academic"         to "Academic",
    "dining"           to "Dining",
    "commuter_parking" to "Parking",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyInviteDialog(
    existingItems: List<ScheduleItem>,
    existingStudySessions: List<StudySession> = emptyList(),
    onDismiss: () -> Unit,
    onSendExisting: (ScheduleItem) -> Unit,
    onSendExistingSession: (StudySession) -> Unit = {},
    onSendNew: (className: String, assignmentName: String, date: String, time: String, location: String) -> Unit,
    campusRegistryViewModel: CampusRegistry = viewModel<CampusRegistry>(),
) {
    val context = LocalContext.current

    // false = pick from existing, true = create new
    var isCreatingNew by remember { mutableStateOf(false) }
    var newClassName by remember { mutableStateOf("") }
    var newAssignmentName by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val locationTextFieldState = rememberTextFieldState()

    val campusRegistry by campusRegistryViewModel.campusRegistry.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var classList by remember { mutableStateOf<List<ClassItem>>(emptyList()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                classList = CalendarFirestoreRepository().loadClasses()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    val validClass = classList.any { it.name == newClassName }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        newDate = sdf.format(Date(millis))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { showDatePicker = false }) { Text("Cancel") }
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
                    // ── Create new study session on the spot ──────────────────
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = newClassName,
                                onValueChange = { newClassName = it },
                                isError = newClassName.isNotBlank() && !validClass,
                                supportingText = {
                                    if (newClassName.isNotBlank() && !validClass)
                                        Text("Please select a valid class.")
                                },
                                readOnly = false,
                                label = { Text("Class Name") },
                                placeholder = { Text("Select your class") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                            )
                            val filtering = classList.filter { it.name.contains(newClassName, ignoreCase = true) }
                            if (filtering.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    filtering.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option.name) },
                                            onClick = { newClassName = option.name; expanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newAssignmentName,
                            onValueChange = { newAssignmentName = it },
                            label = { Text("Topic") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            placeholder = { Text("Tap to pick a date") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Pick date")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = newTime,
                            onValueChange = {},
                            label = { Text("Start Time") },
                            readOnly = true,
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { openTimePicker(context) { newTime = it } }) {
                                    Icon(Icons.Default.Schedule, contentDescription = "Pick time")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Box(modifier = Modifier.imePadding()) {
                            SearchBar(
                                placeholderText = "Add a location...",
                                textFieldState = locationTextFieldState,
                                searchResults = campusRegistry.values.flatten()
                                    .filter { it.name.contains(locationTextFieldState.text.toString(), ignoreCase = true) }
                                    .map { it.name }
                                    .distinct(),
                                onSearch = { query ->
                                    locationTextFieldState.edit { replace(0, length, query) }
                                }
                            )
                        }
                    }
                } else {
                    // ── Pick from existing calendar items ────────────────────
                    val hasAnyItems =
                        existingItems.isNotEmpty() || existingStudySessions.isNotEmpty()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Sessions", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { isCreatingNew = true }) {
                            Text("Create new")
                        }
                    }
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
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
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
                                            Text(
                                                text = item.className,
                                                style = MaterialTheme.typography.titleSmall
                                            )
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
                                            Text(
                                                text = session.className,
                                                style = MaterialTheme.typography.titleSmall
                                            )
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

                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(
                    onClick = {
                        if (newClassName.isNotBlank() && newAssignmentName.isNotBlank()
                            && newDate.isNotBlank() && newTime.isNotBlank()
                        ) {
                            onSendNew(
                                newClassName,
                                newAssignmentName,
                                newDate,
                                newTime,
                                locationTextFieldState.text.toString(),
                            )
                        }
                    }
                ) { Text("Send") }
            }
        },
        dismissButton = {
            Button(onClick = {
                if (isCreatingNew) isCreatingNew = false else onDismiss()
            }) {
                Text(if (isCreatingNew) "Back" else "Cancel")
            }
        }
    )
}