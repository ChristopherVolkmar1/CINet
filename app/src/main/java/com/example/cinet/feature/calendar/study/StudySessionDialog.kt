package com.example.cinet.feature.calendar.study


import android.text.TextUtils.replace
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.feature.calendar.study.StudySession
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.SearchLocationBar
import com.example.cinet.data.model.CampusRegistry
import androidx.compose.runtime.getValue

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
    val academic by viewModel.academic.collectAsState(initial = emptyList())
    val textFieldState = rememberTextFieldState()
    val academicNames = remember(textFieldState.text, academic) {
        academic
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
                Text(if (editingSession == null) "Save" else "Update")
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