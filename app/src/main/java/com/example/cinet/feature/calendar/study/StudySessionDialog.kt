package com.example.cinet.feature.calendar.study

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.calendar.calendarFiles.CalendarFirestoreRepository
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.map.SearchBar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySessionDialog(
    editingSession: StudySession?,
    date: String,
    topic: String,
    onTopicChange: (String) -> Unit,
    startTime: String,
    className: String,
    onClassNameChange: (String) -> Unit,
    onPickStartTime: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val isPreview = LocalInspectionMode.current
    val vm: CampusRegistry? = if (isPreview) null else viewModel<CampusRegistry>()
    val campusRegistry by (vm?.campusRegistry ?: MutableStateFlow(emptyMap())).collectAsState(initial = emptyMap())

    val textFieldState = rememberTextFieldState()

    var expanded by remember { mutableStateOf(false) }
    var classList by remember { mutableStateOf<List<ClassItem>>(emptyList()) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                classList = CalendarFirestoreRepository().loadClasses()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    val validClass = classList.any { it.name == className }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingSession == null) "Add Study Session" else "Edit Study Session") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Date: $date")
                Spacer(modifier = Modifier.height(12.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = {onClassNameChange(it)},
                        isError = className.isNotBlank() && !validClass,
                        supportingText = {
                            if(className.isNotBlank() && !validClass)
                                Text("Please select a valid class.")
                        },
                        readOnly = false,
                        label = { Text("Class Name") },
                        placeholder = { Text("Select your class") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    val filtering = classList.filter { it.name.contains(className, ignoreCase = true) }
                    if (filtering.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filtering.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = { onClassNameChange(option.name); expanded = false }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = topic,
                    onValueChange = onTopicChange,
                    label = { Text("Topic") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = startTime,
                    onValueChange = {},
                    label = { Text("Start Time") },
                    readOnly = true,
                    singleLine = true,
                    maxLines = 1,
                    trailingIcon = {
                        IconButton(onClick = onPickStartTime) {
                            Icon(Icons.Default.Schedule, contentDescription = "Pick time")
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Search bar
                Box(modifier = Modifier.imePadding()) {
                    SearchBar(
                        placeholderText = "Add a location...",
                        textFieldState = textFieldState,
                        searchResults = campusRegistry.values.flatten()
                            .filter { it.name.contains(textFieldState.text.toString(), ignoreCase = true) }
                            .map { it.name }
                            .distinct(),
                        onSearch = { query ->
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
                OutlinedButton(onClick = onDismiss) { Text("Cancel") }
                Button(onClick = onConfirm) {
                    Text(if (editingSession == null) "Save" else "Update")
                }
            }
        }
    )
}
@Preview(showBackground = true)
@Composable
fun StudySessionDialogCreatePreview() {
    StudySessionDialog(
        editingSession = null,
        date = "2026-05-17",
        topic = "",
        onTopicChange = {},
        startTime = "",
        className = "",
        onClassNameChange = {},
        onPickStartTime = {},
        onDismiss = {},
        onConfirm = {},
        onDelete = null
    )
}

@Preview(showBackground = true)
@Composable
fun StudySessionDialogEditPreview() {
    StudySessionDialog(
        editingSession = StudySession(
            id = "1",
            date = "2026-05-17",
            className = "Computer Science 101",
            topic = "Binary Trees",
            startTime = "3:00 PM",
            location = "Broome Library"
        ),
        date = "2026-05-17",
        topic = "Binary Trees",
        onTopicChange = {},
        startTime = "3:00 PM",
        className = "",
        onClassNameChange = {},
        onPickStartTime = {},
        onDismiss = {},
        onConfirm = {},
        onDelete = {}
    )
}