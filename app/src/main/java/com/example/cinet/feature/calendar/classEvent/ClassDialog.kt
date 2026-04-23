package com.example.cinet.feature.calendar.classEvent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.SearchLocationBar

@Composable
fun ClassDialog(
    editingClass: ClassItem?,
    className: String,
    onClassNameChange: (String) -> Unit,
    classStartTime: String,
    classEndTime: String,
    selectedMeetingDays: Set<String>,
    onMeetingDaysChange: (Set<String>) -> Unit,
    weekdayOptions: List<String>,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (CampusLocation?, Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    viewModel: CampusRegistry = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var locationField by remember { mutableStateOf<CampusLocation?>(null) }
    var remindersEnabled by remember {
        mutableStateOf(editingClass?.remindersEnabled ?: true)
    }
    val academic by viewModel.academic.collectAsState(initial = emptyList())
    val textFieldState = rememberTextFieldState()
    val academicNames = remember(textFieldState.text, academic) {
        academic
            .filter { it.name.contains(textFieldState.text.toString(), ignoreCase = true) }
            .map { it.name }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingClass == null) "Create Class" else "Edit Class")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = className,
                    onValueChange = onClassNameChange,
                    label = { Text("Class Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Meeting Days")

                weekdayOptions.forEach { day ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedMeetingDays.contains(day),
                            onCheckedChange = { checked ->
                                onMeetingDaysChange(
                                    if (checked) selectedMeetingDays + day
                                    else selectedMeetingDays - day
                                )
                            }
                        )
                        Text(day)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = classStartTime,
                    onValueChange = {},
                    label = { Text("Start Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onPickStartTime) { Text("Pick Start Time") }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = classEndTime,
                    onValueChange = {},
                    label = { Text("End Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onPickEndTime) { Text("Pick End Time") }

                Spacer(modifier = Modifier.height(8.dp))

                SearchLocationBar(
                    textFieldState = textFieldState,
                    searchResults = academicNames,
                    onSearch = { query ->
                        locationField = academic.find { it.name.equals(query, ignoreCase = true) }
                        textFieldState.edit { replace(0, length, query) }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Per-class reminder toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reminders",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Notify me before each class",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = remindersEnabled,
                        onCheckedChange = { remindersEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(locationField, remindersEnabled) }) {
                Text(if (editingClass == null) "Save Class" else "Update Class")
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
                    ) {
                        Text("Delete")
                    }
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}