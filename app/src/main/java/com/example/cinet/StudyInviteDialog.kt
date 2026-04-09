package com.example.cinet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyInviteDialog(
    existingItems: List<ScheduleItem>,
    onDismiss: () -> Unit,
    onSendExisting: (ScheduleItem) -> Unit,
    onSendNew: (className: String, assignmentName: String, date: String, time: String) -> Unit,
) {
    val context = LocalContext.current
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
                    if (existingItems.isEmpty()) {
                        Text(
                            "No assignments found — create a new invite below.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("Pick from your assignments:", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(existingItems) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
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
            TextButton(onClick = {
                if (isCreatingNew) isCreatingNew = false else onDismiss()
            }) {
                Text(if (isCreatingNew) "Back" else "Cancel")
            }
        }
    )
}