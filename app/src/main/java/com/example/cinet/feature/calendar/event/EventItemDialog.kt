package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.SearchLocationBar

@Composable
fun EventItemDialog(
    editingEvent: EventItem?,
    date: String,
    eventName: String,
    onEventNameChange: (String) -> Unit,
    eventTime: String,
    location: CampusLocation?,
    onLocationChange: (CampusLocation?) -> Unit,
    onPickTime: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?,
    viewModel: CampusRegistry = viewModel<CampusRegistry>()
) {
    val campusRegistry by viewModel.campusRegistry.collectAsState()
    val textFieldState = rememberTextFieldState()
    var locationCategory by remember { mutableStateOf("academic") }

    val categoryLocations = remember(campusRegistry, locationCategory) {
        campusRegistry[locationCategory] ?: emptyList()
    }
    val searchResults = remember(textFieldState.text, categoryLocations) {
        categoryLocations
            .filter { it.name.contains(textFieldState.text.toString(), ignoreCase = true) }
            .map { it.name }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingEvent == null) "Add Event" else "Edit Event") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Date: $date")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = eventName,
                    onValueChange = onEventNameChange,
                    label = { Text("Event Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = eventTime,
                    onValueChange = {},
                    label = { Text("Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onPickTime) { Text("Pick Time") }
                Spacer(modifier = Modifier.height(12.dp))

                // Category picker
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
                                textFieldState.edit { replace(0, length, "") }
                                onLocationChange(null)
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                SearchLocationBar(
                    textFieldState = textFieldState,
                    searchResults = searchResults,
                    onSearch = { query ->
                        val found = categoryLocations.find { it.name.equals(query, ignoreCase = true) }
                        onLocationChange(found)
                        textFieldState.edit { replace(0, length, query) }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(if (editingEvent == null) "Save" else "Update")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Delete") }
                }
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}