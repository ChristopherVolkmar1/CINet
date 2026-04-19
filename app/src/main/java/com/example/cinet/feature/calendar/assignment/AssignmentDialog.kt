package com.example.cinet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentDialog(
    selectedDate: LocalDate,
    classItems: List<ClassItem>,
    editingAssignment: ScheduleItem?,
    assignmentName: String,
    onAssignmentNameChange: (String) -> Unit,
    dueTime: String,
    selectedClassId: String?,
    onSelectedClassIdChange: (String?) -> Unit,
    classDropdownExpanded: Boolean,
    onClassDropdownExpandedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onPickTime: () -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingAssignment == null) "Add Assignment" else "Edit Assignment")
        },
        text = {
            Column {
                Text("Date: $selectedDate")
                Spacer(modifier = Modifier.height(12.dp))

                if (classItems.isEmpty()) {
                    // Prevents assignment creation when no classes exist.
                    // This dependency comes from the app's design (assignments must belong to a class).
                    Text(
                        text = "Create a class first before adding assignments.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = classDropdownExpanded,
                        onExpandedChange = {
                            // Controls dropdown visibility externally (state is owned by parent).
                            onClassDropdownExpandedChange(!classDropdownExpanded)
                        }
                    ) {
                        val selectedClassName = classItems
                            // Converts stored class ID → display name.
                            .firstOrNull { it.id == selectedClassId }
                            ?.name ?: ""

                        OutlinedTextField(
                            value = selectedClassName,
                            // Read-only field: user cannot type, only select via dropdown.
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                // Required for ExposedDropdownMenuBox to correctly anchor dropdown.
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )

                        DropdownMenu(
                            expanded = classDropdownExpanded,
                            onDismissRequest = {
                                onClassDropdownExpandedChange(false)
                            }
                        ) {
                            classItems.forEach { classItem ->
                                DropdownMenuItem(
                                    text = { Text(classItem.name) },
                                    onClick = {
                                        // Stores only ID; actual object is resolved later (in ViewModel usage).
                                        onSelectedClassIdChange(classItem.id)
                                        onClassDropdownExpandedChange(false)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = assignmentName,
                    onValueChange = onAssignmentNameChange,
                    label = { Text("Assignment") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dueTime,
                    // Prevents manual editing; time must come from picker.
                    onValueChange = {},
                    label = { Text("Due Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = onPickTime) {
                    Text("Pick Time")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                // Disabled when no classes exist to enforce class dependency at UI level.
                enabled = classItems.isNotEmpty()
            ) {
                Text(if (editingAssignment == null) "Save" else "Update")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    Button(
                        onClick = onDelete,
                        colors = ButtonDefaults.buttonColors(
                            // Uses theme error color to indicate destructive action.
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                }

                // Null onDelete indicates create mode (no existing assignment to delete).
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}