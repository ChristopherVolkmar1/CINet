package com.example.cinet.feature.calendar.classEvent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.cinet.data.model.CampusRegistry
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.SearchBar
import kotlinx.coroutines.flow.MutableStateFlow


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDialog(
    editingClass: ClassItem?,
    className: String,
    onClassNameChange: (String) -> Unit,
    classStartTime: String,
    classEndTime: String,
    onExistingClassSelected: (ClassItem) -> Unit = {},
    classItems: List<ClassItem>,
    selectedMeetingDays: Set<String>,
    onMeetingDaysChange: (Set<String>) -> Unit,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (CampusLocation?, Boolean) -> Unit,
    onDelete: (() -> Unit)?
) {
    var locationField by remember { mutableStateOf<CampusLocation?>(null) }
    val isPreview = LocalInspectionMode.current
    val vm: CampusRegistry? = if (isPreview) null else androidx.lifecycle.viewmodel.compose.viewModel()
    val academic by (vm?.academic ?: MutableStateFlow(emptyList())).collectAsState(initial = emptyList())
    var remindersEnabled by remember {
        mutableStateOf(editingClass?.remindersEnabled ?: true)
    }

    val textFieldState = rememberTextFieldState()

    val academicNames = remember(textFieldState.text, academic) {
        academic
            .filter { it.name.contains(textFieldState.text.toString(), ignoreCase = true) }
            .map { it.name }
    }

    val days = listOf("M", "T", "W", "TH", "F")
    val dayDisplayToStored = mapOf(
        "M" to "Mon",
        "T" to "Tue",
        "W" to "Wed",
        "TH" to "Thu",
        "F" to "Fri"
    )
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        modifier = Modifier.width(850.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingClass == null) "Create Class" else "Edit Class")
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).imePadding()){
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = {onClassNameChange(it)},
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
                    val filtering = classItems
                        .filter { it.name.contains(className, ignoreCase = true) }
                        .distinctBy { it.name }
                        .sortedBy { it.name.lowercase() }
                    if (filtering.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filtering.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = { onClassNameChange(option.name); onExistingClassSelected(option); expanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    days.forEach { day ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = day,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Checkbox(
                                checked = selectedMeetingDays.contains(dayDisplayToStored[day]),
                                onCheckedChange = { checked ->
                                    val stored = dayDisplayToStored[day] ?: day
                                    onMeetingDaysChange(
                                        if (checked) selectedMeetingDays + stored
                                        else selectedMeetingDays - stored
                                    )
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = classStartTime,
                        onValueChange = {},
                        label = { Text("Start Time") },
                        readOnly = true,
                        singleLine = true,
                        maxLines = 1,
                        trailingIcon = {
                            IconButton(onClick = onPickStartTime) {
                                Icon(Icons.Default.Schedule, contentDescription = "Pick time")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedTextField(
                        value = classEndTime,
                        onValueChange = {},
                        label = { Text("End Time") },
                        readOnly = true,
                        singleLine = true,
                        maxLines = 1,
                        trailingIcon = {
                            IconButton(onClick = onPickEndTime) {
                                Icon(Icons.Default.Schedule, contentDescription = "Pick time")
                            }
                        },
                        modifier = Modifier.weight(1f)
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

@Preview(showBackground = true)
@Composable
fun ClassDialogCreatePreview() {
    ClassDialog(
        editingClass = null,
        className = "Computer Science 101",
        onClassNameChange = {},
        classStartTime = "9:00 AM",
        classEndTime = "10:15 AM",
        classItems = emptyList(),
        selectedMeetingDays = setOf("M", "W"),
        onMeetingDaysChange = {},
        onPickStartTime = {},
        onPickEndTime = {},
        onDismiss = {},
        onConfirm = { _, _ -> },
        onDelete = null
    )
}

@Preview(showBackground = true)
@Composable
fun ClassDialogEditPreview() {
    ClassDialog(
        editingClass = ClassItem(
            id = "1",
            name = "Mathematics 201",
            meetingDays = listOf("T", "TH"),
            startTime = "11:00 AM",
            endTime = "12:15 PM",
            location = "Broome Library 1360",
            remindersEnabled = true
        ),
        className = "Mathematics 201",
        onClassNameChange = {},
        classStartTime = "11:00 AM",
        classEndTime = "12:15 PM",
        classItems = emptyList(),
        selectedMeetingDays = setOf("T", "TH"),
        onMeetingDaysChange = {},
        onPickStartTime = {},
        onPickEndTime = {},
        onDismiss = {},
        onConfirm = { _, _ -> },
        onDelete = {}
    )
}