package com.example.cinet

import android.app.TimePickerDialog
import android.widget.TimePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit) {
    val viewModel: CalendarViewModel = viewModel()
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    val classItems = viewModel.classItems

    val currentMonth = viewModel.currentMonth
    val selectedDate = viewModel.selectedDate
    val itemsForSelectedDate = viewModel.getItemsForSelectedDate()
    val classesForSelectedDate = viewModel.getClassesForSelectedDate()

    var showAssignmentDialog by remember { mutableStateOf(false) }
    var showClassDialog by remember { mutableStateOf(false) }

    var editingAssignment by remember { mutableStateOf<ScheduleItem?>(null) }
    var editingClass by remember { mutableStateOf<ClassItem?>(null) }

    var assignmentName by remember { mutableStateOf("") }
    var dueTime by remember { mutableStateOf("") }

    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    var className by remember { mutableStateOf("") }
    var classStartTime by remember { mutableStateOf("") }
    var classEndTime by remember { mutableStateOf("") }
    var selectedMeetingDays by remember { mutableStateOf(setOf<String>()) }

    val weekdayOptions = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    fun formatPickedTime(hour24: Int, minute: Int): String {
        val amPm = if (hour24 >= 12) "PM" else "AM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        return String.format("%02d:%02d %s", hour12, minute, amPm)
    }

    fun openTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _: TimePicker, hour: Int, minute: Int ->
                onTimeSelected(formatPickedTime(hour, minute))
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val days = mutableListOf<Int?>()
    repeat(startDayOfWeek) { days.add(null) }
    for (day in 1..daysInMonth) days.add(day)
    while (days.size % 7 != 0) days.add(null)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Month")
                }

                Text(
                    text = "${currentMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${currentMonth.year}",
                    style = MaterialTheme.typography.titleLarge
                )

                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Month")
                }
            }

            Spacer(modifier = Modifier.width(48.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = {
                    editingClass = null
                    className = ""
                    classStartTime = ""
                    classEndTime = ""
                    selectedMeetingDays = emptySet()
                    showClassDialog = true
                }
            ) {
                Text("Manage Classes")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { dayName ->
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = dayName)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column {
            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (day != null) {
                                val cellDate = currentMonth.atDay(day)
                                val isToday = cellDate == today
                                val isSelected = selectedDate == cellDate

                                val dateString = "%04d-%02d-%02d".format(
                                    cellDate.year,
                                    cellDate.monthValue,
                                    cellDate.dayOfMonth
                                )

                                val hasItems = viewModel.scheduleItems.any { it.date == dateString }

                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isToday) MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    2.dp,
                                                    MaterialTheme.colorScheme.tertiary,
                                                    CircleShape
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable {
                                            val tappedDate = currentMonth.atDay(day)

                                            if (selectedDate == tappedDate) {
                                                editingAssignment = null
                                                assignmentName = ""
                                                dueTime = ""
                                                selectedClassId = null
                                                showAssignmentDialog = true
                                            } else {
                                                viewModel.selectDate(day)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            color = if (isToday) Color.White
                                            else MaterialTheme.colorScheme.onSurface
                                        )

                                        if (hasItems) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .background(
                                                        if (isToday) Color.White
                                                        else MaterialTheme.colorScheme.primary,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Schedule", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDate == null) {
            Text("Select a date to view scheduled items.")
        } else if (itemsForSelectedDate.isEmpty()) {
            Text("No items scheduled for $selectedDate")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsForSelectedDate.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingAssignment = item
                                assignmentName = item.assignmentName
                                dueTime = item.dueTime
                                selectedClassId = item.classId
                                showAssignmentDialog = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.className,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = item.assignmentName)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Due: ${item.dueTime}")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Classes", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDate == null) {
            Text("Select a date to view classes for that day.")
        } else if (classesForSelectedDate.isEmpty()) {
            Text("No classes scheduled for $selectedDate")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                classesForSelectedDate.forEach { classItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingClass = classItem
                                className = classItem.name
                                classStartTime = classItem.startTime
                                classEndTime = classItem.endTime
                                selectedMeetingDays = classItem.meetingDays.toSet()
                                showClassDialog = true
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = classItem.name,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${classItem.startTime} - ${classItem.endTime}"
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = classItem.meetingDays.joinToString(", ")
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAssignmentDialog && selectedDate != null) {
        AlertDialog(
            onDismissRequest = {
                showAssignmentDialog = false
                editingAssignment = null
            },
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
                            onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
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

                            ExposedDropdownMenu(
                                expanded = classDropdownExpanded,
                                onDismissRequest = { classDropdownExpanded = false }
                            ) {
                                classItems.forEach { classItem ->
                                    DropdownMenuItem(
                                        text = { Text(classItem.name) },
                                        onClick = {
                                            selectedClassId = classItem.id
                                            classDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = assignmentName,
                        onValueChange = { assignmentName = it },
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

                    Button(
                        onClick = {
                            openTimePicker { picked ->
                                dueTime = picked
                            }
                        }
                    ) {
                        Text("Pick Time")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val selectedClass = classItems.firstOrNull { it.id == selectedClassId }

                        if (
                            selectedClass != null &&
                            assignmentName.isNotBlank() &&
                            dueTime.isNotBlank()
                        ) {
                            val item = editingAssignment
                            if (item == null) {
                                viewModel.addScheduleItem(
                                    classItem = selectedClass,
                                    assignmentName = assignmentName,
                                    dueTime = dueTime
                                )
                            } else {
                                viewModel.updateScheduleItem(
                                    itemId = item.id,
                                    classItem = selectedClass,
                                    assignmentName = assignmentName,
                                    dueTime = dueTime
                                )
                            }

                            showAssignmentDialog = false
                            editingAssignment = null
                            assignmentName = ""
                            dueTime = ""
                            selectedClassId = null
                        }
                    },
                    enabled = classItems.isNotEmpty()
                ) {
                    Text(if (editingAssignment == null) "Save" else "Update")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingAssignment != null) {
                        Button(
                            onClick = {
                                viewModel.deleteScheduleItem(editingAssignment!!.id)
                                showAssignmentDialog = false
                                editingAssignment = null
                                assignmentName = ""
                                dueTime = ""
                                selectedClassId = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showAssignmentDialog = false
                            editingAssignment = null
                            assignmentName = ""
                            dueTime = ""
                            selectedClassId = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showClassDialog) {
        AlertDialog(
            onDismissRequest = {
                showClassDialog = false
                editingClass = null
            },
            title = {
                Text(if (editingClass == null) "Create Class" else "Edit Class")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = className,
                        onValueChange = { className = it },
                        label = { Text("Class Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Meeting Days")

                    weekdayOptions.forEach { day ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = selectedMeetingDays.contains(day),
                                onCheckedChange = { checked ->
                                    selectedMeetingDays = if (checked) {
                                        selectedMeetingDays + day
                                    } else {
                                        selectedMeetingDays - day
                                    }
                                }
                            )
                            Text(day)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = classStartTime,
                        onValueChange = {},
                        label = { Text("Start Time") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        openTimePicker { picked ->
                            classStartTime = picked
                        }
                    }) {
                        Text("Pick Start Time")
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = classEndTime,
                        onValueChange = {},
                        label = { Text("End Time") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(onClick = {
                        openTimePicker { picked ->
                            classEndTime = picked
                        }
                    }) {
                        Text("Pick End Time")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (
                            className.isNotBlank() &&
                            selectedMeetingDays.isNotEmpty() &&
                            classStartTime.isNotBlank() &&
                            classEndTime.isNotBlank()
                        ) {
                            val classToEdit = editingClass
                            if (classToEdit == null) {
                                viewModel.addClass(
                                    name = className,
                                    meetingDays = selectedMeetingDays.toList(),
                                    startTime = classStartTime,
                                    endTime = classEndTime
                                )
                            } else {
                                viewModel.updateClass(
                                    classId = classToEdit.id,
                                    name = className,
                                    meetingDays = selectedMeetingDays.toList(),
                                    startTime = classStartTime,
                                    endTime = classEndTime
                                )
                            }

                            showClassDialog = false
                            editingClass = null
                            className = ""
                            classStartTime = ""
                            classEndTime = ""
                            selectedMeetingDays = emptySet()
                        }
                    }
                ) {
                    Text(if (editingClass == null) "Save Class" else "Update Class")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (editingClass != null) {
                        Button(
                            onClick = {
                                viewModel.deleteClass(editingClass!!.id)
                                showClassDialog = false
                                editingClass = null
                                className = ""
                                classStartTime = ""
                                classEndTime = ""
                                selectedMeetingDays = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showClassDialog = false
                            editingClass = null
                            className = ""
                            classStartTime = ""
                            classEndTime = ""
                            selectedMeetingDays = emptySet()
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}