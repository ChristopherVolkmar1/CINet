package com.example.cinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                    Text(
                        text = "Create a class first before adding assignments.",
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = classDropdownExpanded,
                        onExpandedChange = {
                            onClassDropdownExpandedChange(!classDropdownExpanded)
                        }
                    ) {
                        val selectedClassName = classItems
                            .firstOrNull { it.id == selectedClassId }
                            ?.name ?: ""

                        OutlinedTextField(
                            value = selectedClassName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Class") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
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