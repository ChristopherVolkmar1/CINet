package com.example.cinet.feature.calendar.study

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.SearchBar

// Category key → display label pairs shown as filter chips above the search bar.
private val locationCategories = listOf(
    "academic"         to "Academic",
    "dining"           to "Dining",
    "commuter_parking" to "Parking",
)

@Composable
fun StudySessionDialog(
    editingSession: StudySession?,
    date: String,
    className: String,
    onClassNameChange: (String) -> Unit,
    topic: String,
    onTopicChange: (String) -> Unit,
    startTime: String,
    location: CampusLocation?,
    onLocationChange: (CampusLocation?) -> Unit,
    onPickStartTime: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?,
    viewModel: CampusRegistry = viewModel<CampusRegistry>()
) {
    // Full registry — contains academic, dining, commuter_parking, transit
    val campusRegistry by viewModel.campusRegistry.collectAsState()

    val textFieldState = rememberTextFieldState()

    // Which category the user has selected (default: Academic)
    var locationCategory by remember { mutableStateOf("academic") }

    // Locations in the selected category, filtered by whatever is typed
    val filteredNames = remember(textFieldState.text, locationCategory, campusRegistry) {
        val categoryLocations = campusRegistry[locationCategory] ?: emptyList()
        categoryLocations
            .filter { it.name.contains(textFieldState.text.toString(), ignoreCase = true) }
            .map { it.name }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingSession == null) "Add Study Session" else "Edit Study Session") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Date: $date")
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = className,
                    onValueChange = onClassNameChange,
                    label = { Text("Class Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = topic,
                    onValueChange = onTopicChange,
                    label = { Text("Topic / What to study") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    label = { Text("Start Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = onPickStartTime) { Text("Pick Start Time") }
                Spacer(modifier = Modifier.height(12.dp))

                // Category filter chips — Academic / Dining / Parking
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    locationCategories.forEach { (key, label) ->
                        FilterChip(
                            selected = locationCategory == key,
                            onClick = {
                                locationCategory = key
                                // Clear selection when switching category
                                onLocationChange(null)
                            },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                // Search bar — results come from the selected category only
                SearchBar(
                    placeholderText = "Search Location",
                    textFieldState = textFieldState,
                    searchResults = filteredNames,
                    onSearch = { query ->
                        val categoryLocations = campusRegistry[locationCategory] ?: emptyList()
                        val found = categoryLocations.find { it.name.equals(query, ignoreCase = true) }
                        onLocationChange(found)
                        textFieldState.edit { replace(0, length, query) }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(if (editingSession == null) "Save" else "Update")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) { Text("Delete") }
                }
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}