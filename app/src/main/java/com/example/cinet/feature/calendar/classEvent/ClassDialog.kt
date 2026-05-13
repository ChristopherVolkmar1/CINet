package com.example.cinet.feature.calendar.classEvent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.SearchBar

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
        modifier = Modifier.width(850.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingClass == null) "Create Class" else "Edit Class")
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                        .padding(end = 10.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ){
                    OutlinedTextField(
                        value = className,
                        onValueChange = onClassNameChange,
                        label = { Text("Class Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Meeting Days",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    weekdayOptions.forEach { day ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(36.dp),
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

                            Text(
                                text = day,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .padding(start = 10.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = classStartTime,
                        onValueChange = {},
                        label = { Text("Start Time") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onPickStartTime,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pick Start Time",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = classEndTime,
                        onValueChange = {},
                        label = { Text("End Time") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onPickEndTime,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pick End Time",
                            maxLines = 1,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SearchBar(
                        placeholderText = "Search Location",
                        textFieldState = textFieldState,
                        searchResults = academicNames,
                        onSearch = { query ->
                            locationField = academic.find {
                                it.name.equals(query, ignoreCase = true)
                            }
                            textFieldState.edit { replace(0, length, query) }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reminders",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1
                            )

                            Text(
                                text = "Notify before class",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                        }

                        Switch(
                            checked = remindersEnabled,
                            onCheckedChange = { remindersEnabled = it }
                        )
                    }
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
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete", maxLines = 1)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text("Cancel", maxLines = 1)
                }

                Button(
                    onClick = { onConfirm(locationField, remindersEnabled) },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (editingClass == null) "Save" else "Update",
                        maxLines = 1
                    )
                }
            }
        }
    )
}