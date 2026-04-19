package com.example.cinet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.*

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
    onConfirm: (CampusLocation?) -> Unit,
    onDelete: (() -> Unit)?,
    viewModel: CampusRegistry = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var locationField by remember { mutableStateOf<CampusLocation?>(null) }
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
                // Makes the dialog body scrollable so all fields remain reachable
                // when content is taller than the available dialog space.
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
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedMeetingDays.contains(day),
                            onCheckedChange = { checked ->
                                // Uses Set add/remove behavior so duplicate day entries
                                // cannot happen and unchecking cleanly removes the day.
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
                    // Empty onValueChange + readOnly means this field is display-only;
                    // time must come from the external picker callback.
                    onValueChange = {},
                    label = { Text("Start Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = onPickStartTime) {
                    Text("Pick Start Time")
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = classEndTime,
                    // Same pattern as start time: prevents manual editing and keeps
                    // time selection controlled by the picker.
                    onValueChange = {},
                    label = { Text("End Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = onPickEndTime) {
                    Text("Pick End Time")
                }

                Spacer(modifier = Modifier.height(8.dp))

                SearchLocationBar(
                    textFieldState = textFieldState,
                    searchResults = academicNames,
                    onSearch = { query ->
                        locationField  = academic.find { it.name.equals(query, ignoreCase = true) }
                        textFieldState.edit { replace(0, length, query) }
                    }
                )
            }
        },
        confirmButton = {
            Button(onClick = {onConfirm(locationField)}) {
                Text(if (editingClass == null) "Save Class" else "Update Class")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            // Uses the theme error color so delete action is visually
                            // distinguished as destructive without hardcoding a color.
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }

                // Null onDelete is how the caller signals "create mode",
                // so the delete action only appears while editing an existing class.
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}