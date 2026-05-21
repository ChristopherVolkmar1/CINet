package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.SearchBar
import kotlinx.coroutines.flow.MutableStateFlow

@OptIn(ExperimentalMaterial3Api::class)
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
    onDelete: (() -> Unit)?
) {
    val isPreview = LocalInspectionMode.current
    val vm: CampusRegistry? = if (isPreview) null else viewModel<CampusRegistry>()
    val campusRegistry by (vm?.campusRegistry ?: MutableStateFlow(emptyMap())).collectAsState(initial = emptyMap())

    val textFieldState = rememberTextFieldState()

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
                    readOnly = true,
                    singleLine = true,
                    maxLines = 1,
                    trailingIcon = {
                        IconButton(onClick = onPickTime) {
                            Icon(Icons.Default.Schedule, contentDescription = "Pick time")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.imePadding()) {
                    SearchBar(
                        placeholderText = "Add a location...",
                        textFieldState = textFieldState,
                        searchResults = campusRegistry.values.flatten()
                            .filter { it.name.contains(textFieldState.text.toString(), ignoreCase = true) }
                            .map { it.name }
                            .distinct(),
                        onSearch = { query ->
                            val found = (campusRegistry["academic"] ?: emptyList())
                                .find { it.name.equals(query, ignoreCase = true) }
                            onLocationChange(found)
                            textFieldState.edit { replace(0, length, query) }
                        }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) { Text("Delete") }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) { Text("Cancel") }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(if (editingEvent == null) "Save" else "Update")
                }
            }
        }
    )
}