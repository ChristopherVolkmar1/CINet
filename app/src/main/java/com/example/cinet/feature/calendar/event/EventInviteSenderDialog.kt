package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.core.time.openTimePicker
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.map.SearchLocationBar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// Category key → display label pairs shown as filter chips above the search bar.
private val locationCategories = listOf(
    "academic"         to "Academic",
    "dining"           to "Dining",
    "commuter_parking" to "Parking",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInviteSenderDialog(
    existingEvents: List<EventItem> = emptyList(),
    onDismiss: () -> Unit,
    onSend: (name: String, date: String, time: String, location: String) -> Unit,
    campusRegistryViewModel: CampusRegistry = viewModel<CampusRegistry>(),
) {
    val context = LocalContext.current

    // false = pick from existing, true = create new manually
    var isCreatingNew by remember { mutableStateOf(false) }
    var eventName by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Location state — shared ViewModel instead of a private Firestore fetch
    val campusRegistry by campusRegistryViewModel.campusRegistry.collectAsState()
    var locationCategory by remember { mutableStateOf("academic") }
    val locationTextFieldState = rememberTextFieldState()

    // Names in the selected category, filtered by whatever is typed
    val filteredLocationNames = remember(
        locationTextFieldState.text, locationCategory, campusRegistry
    ) {
        val categoryLocations = campusRegistry[locationCategory] ?: emptyList()
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
                        eventDate = sdf.format(Date(millis))
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
        title = { Text(if (isCreatingNew) "New Event Invite" else "Send Event Invite") },
        text = {
            Column {
                if (isCreatingNew) {
                    // ── Manual form for creating a new event invite ───────────
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        OutlinedTextField(
                            value = eventName,
                            onValueChange = { eventName = it },
                            label = { Text("Event Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = eventDate,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Date") },
                            placeholder = { Text("Tap to pick a date") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Pick Date") }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = eventTime,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Time") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { openTimePicker(context) { eventTime = it } },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Pick Time") }
                        Spacer(modifier = Modifier.height(12.dp))

                        // ── Location: category chips + search bar ─────────────
                        Text(
                            text = "Location (optional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            locationCategories.forEach { (key, label) ->
                                FilterChip(
                                    selected = locationCategory == key,
                                    onClick = { locationCategory = key },
                                    label = {
                                        Text(label, style = MaterialTheme.typography.labelSmall)
                                    },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        SearchLocationBar(
                            textFieldState = locationTextFieldState,
                            searchResults = filteredLocationNames,
                            onSearch = { query ->
                                locationTextFieldState.edit { replace(0, length, query) }
                            }
                        )
                    }

                } else {
                    // ── Pick from existing events ─────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Your Events", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { isCreatingNew = true }) {
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

                    if (existingEvents.isEmpty()) {
                        Text(
                            "No events found — create a new invite below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val filteredEvents = existingEvents
                            .distinctBy { it.id }
                            .filter {
                                searchQuery.isBlank() ||
                                        it.name.contains(searchQuery, ignoreCase = true) ||
                                        it.location.contains(searchQuery, ignoreCase = true)
                            }
                        LazyColumn {
                            items(filteredEvents) { event ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            onSend(
                                                event.name,
                                                event.date,
                                                event.time,
                                                event.location
                                            )
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = event.name,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        Text(
                                            text = "${event.time} on ${event.date}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (event.location.isNotBlank()) {
                                            Text(
                                                text = event.location,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(onClick = {
                    if (eventName.isNotBlank() && eventDate.isNotBlank() && eventTime.isNotBlank()) {
                        onSend(
                            eventName,
                            eventDate,
                            eventTime,
                            locationTextFieldState.text.toString(),
                        )
                    }
                }) { Text("Send") }
            }
        },
        dismissButton = {
            if (isCreatingNew) {
                OutlinedButton(onClick = { isCreatingNew = false }) { Text("Back") }
            } else {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}