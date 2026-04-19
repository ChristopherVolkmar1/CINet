package com.example.cinet.feature.calendar.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EventItemDialog(
    editingEvent: EventItem?,
    date: String,
    eventName: String,
    onEventNameChange: (String) -> Unit,
    eventTime: String,
    location: String,
    onLocationChange: (String) -> Unit,
    onPickTime: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?
) {
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
                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text("Location (optional)") },
                    modifier = Modifier.fillMaxWidth()
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