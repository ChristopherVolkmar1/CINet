package com.example.cinet.feature.calendar.study

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import com.example.cinet.core.time.openTimePicker
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
    onSendNew: (className: String, assignmentName: String, date: String, time: String) -> Unit,
) {
    val context = LocalContext.current
    // false = pick from existing, true = create new
    var isCreatingNew by remember { mutableStateOf(false) }
    var newClassName by remember { mutableStateOf("") }
    var newAssignmentName by remember { mutableStateOf("") }
    var newDate by remember { mutableStateOf("") }
    var newTime by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Format to yyyy-MM-dd to match CalendarFirestoreRepository date format
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
                } else {
                    // Option A — pick from existing calendar items
                    val hasAnyItems = existingItems.isNotEmpty() || existingStudySessions.isNotEmpty()

                    if (!hasAnyItems) {
                        Text(
                            "No items found — create a new invite below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn {
                            if (existingItems.isNotEmpty()) {
                                item {
                                    Text(
                                        "Assignments",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(existingItems) { item ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            // Tapping sends immediately without going to create mode
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

                            if (existingStudySessions.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "Study Sessions",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(existingStudySessions) { session ->
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
                            onSendNew(newClassName, newAssignmentName, newDate, newTime)
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