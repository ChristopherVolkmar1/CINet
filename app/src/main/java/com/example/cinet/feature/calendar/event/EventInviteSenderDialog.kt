package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.cinet.feature.map.CampusLocation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import com.example.cinet.core.time.openTimePicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInviteSenderDialog(
    existingEvents: List<EventItem> = emptyList(),
    onDismiss: () -> Unit,
    onSend: (name: String, date: String, time: String, location: String) -> Unit,
) {
    val context = LocalContext.current
    // false = pick from existing, true = create new manually
    var isCreatingNew by remember { mutableStateOf(false) }
    var eventName by remember { mutableStateOf("") }
    var eventDate by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var locationsByCategory by remember { mutableStateOf<Map<String, List<CampusLocation>>>(emptyMap()) }
    var locationCategory by remember { mutableStateOf("academic") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(Unit) {
        try {
            val db = FirebaseFirestore.getInstance()
            val result = mutableMapOf<String, List<CampusLocation>>()
            for (col in listOf("academic", "dining", "commuter_parking")) {
                result[col] = db.collection(col).get().await()
                    .toObjects(CampusLocation::class.java)
                    .sortedBy { it.name }
            }
            locationsByCategory = result
        } catch (_: Exception) { }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Format to yyyy-MM-dd to match CalendarFirestoreRepository date format
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
                    // Manual form for creating a new event invite on the spot
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
                        ) {
                            Text("Pick Date")
                        }
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
                        ) {
                            Text("Pick Time")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = eventLocation,
                            onValueChange = { eventLocation = it },
                            label = { Text("Location (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (locationsByCategory.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Campus locations",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                listOf("academic" to "Academic", "dining" to "Dining", "commuter_parking" to "Parking")
                                    .forEach { (key, label) ->
                                        FilterChip(
                                            selected = locationCategory == key,
                                            onClick = { locationCategory = key },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                        )
                                    }
                            }
                            val categoryLocations = locationsByCategory[locationCategory] ?: emptyList()
                            if (categoryLocations.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    categoryLocations.forEach { loc ->
                                        SuggestionChip(
                                            onClick = { eventLocation = loc.name },
                                            label = { Text(loc.name, style = MaterialTheme.typography.labelSmall) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Pick from existing events
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
                        val filteredEvents = existingEvents.filter {
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
                                        // Tapping sends immediately
                                        .clickable {
                                            onSend(event.name, event.date, event.time, event.location)
                                        }
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(text = event.name, style = MaterialTheme.typography.titleSmall)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { isCreatingNew = true }) {
                        Text("Create new instead")
                    }
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                Button(onClick = {
                    if (eventName.isNotBlank() && eventDate.isNotBlank() && eventTime.isNotBlank()) {
                        onSend(eventName, eventDate, eventTime, eventLocation)
                    }
                }) { Text("Send") }
            }
        },
        dismissButton = {
            // Back returns to picker, Cancel closes entirely
            if (isCreatingNew) {
                OutlinedButton(onClick = { isCreatingNew = false }) { Text("Back") }
            } else {
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}