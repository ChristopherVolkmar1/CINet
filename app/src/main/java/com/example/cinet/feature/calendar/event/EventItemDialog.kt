package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val academic by viewModel.academic.collectAsState(initial = emptyList())
    val textFieldState = rememberTextFieldState()
    val academicNames = remember(textFieldState.text, academic) {
        academic
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
                Spacer(modifier = Modifier.height(8.dp))
                SearchLocationBar(
                    textFieldState = textFieldState,
                    searchResults = academicNames,
                    onSearch = { query ->
                        val found = academic.find { it.name.equals(query, ignoreCase = true) }
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