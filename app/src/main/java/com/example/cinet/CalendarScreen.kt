package com.example.cinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    initialShowClassDialog: Boolean = false
) {
    // Provided by Compose; automatically scopes this ViewModel to the current UI lifecycle.
    val viewModel: CalendarViewModel = viewModel()

    // Required because openTimePicker(...) depends on Android context (not visible here).
    val context = LocalContext.current

    // remember prevents recomputing today's date on every recomposition.
    val today = remember { LocalDate.now() }

    // These are backed by ViewModel state (likely StateFlow or mutableState),
    // so UI updates automatically when they change.
    val classItems = viewModel.classItems
    val currentMonth = viewModel.currentMonth
    val selectedDate = viewModel.selectedDate

    // These methods encapsulate filtering logic inside the ViewModel.
    // The UI does not know how items/classes are filtered.
    val itemsForSelectedDate = viewModel.getItemsForSelectedDate()
    val classesForSelectedDate = viewModel.getClassesForSelectedDate()

    var showAssignmentDialog by remember { mutableStateOf(false) }
    var showClassDialog by remember { mutableStateOf(initialShowClassDialog) }

    // Null = create mode, non-null = edit mode (used throughout dialogs).
    var editingAssignment by remember { mutableStateOf<ScheduleItem?>(null) }
    var editingClass by remember { mutableStateOf<ClassItem?>(null) }

    var assignmentName by remember { mutableStateOf("") }
    var dueTime by remember { mutableStateOf("") }

    // Stores only the ID; actual ClassItem is resolved later from classItems.
    var selectedClassId by remember { mutableStateOf<String?>(null) }

    var classDropdownExpanded by remember { mutableStateOf(false) }

    var className by remember { mutableStateOf("") }
    var classStartTime by remember { mutableStateOf("") }
    var classEndTime by remember { mutableStateOf("") }

    // Must match whatever format the ViewModel uses for meeting-day matching.
    var selectedMeetingDays by remember { mutableStateOf(setOf<String>()) }

    val weekdayOptions = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    var locationField by remember { mutableStateOf<CampusLocation?>(null) }

    fun resetAssignmentForm() {
        editingAssignment = null
        assignmentName = ""
        dueTime = ""
        selectedClassId = null
        classDropdownExpanded = false
    }

    fun resetClassForm() {
        editingClass = null
        className = ""
        classStartTime = ""
        classEndTime = ""
        selectedMeetingDays = emptySet()
        locationField = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            // Enables scrolling for entire screen (important for smaller devices).
            .verticalScroll(rememberScrollState())
    ) {
        CalendarHeader(
            currentMonth = currentMonth,
            onBack = onBack,
            // Month navigation logic is handled inside ViewModel.
            onPreviousMonth = { viewModel.previousMonth() },
            onNextMonth = { viewModel.nextMonth() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(
                onClick = {
                    // Always reset before opening to avoid leftover state from edits.
                    resetClassForm()
                    showClassDialog = true
                }
            ) {
                Text("Manage Classes")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            today = today,
            scheduleItems = viewModel.scheduleItems,
            onDateSelected = { day ->
                viewModel.selectDate(day)
            },
            onSameDateClicked = {
                // Behavior depends on CalendarGrid detecting "same date" clicks.
                resetAssignmentForm()
                showAssignmentDialog = true
            }
        )

        ScheduleSection(
            selectedDate = selectedDate,
            itemsForSelectedDate = itemsForSelectedDate,
            onItemClick = { item ->
                // Pre-fills dialog fields so it behaves as an edit form.
                editingAssignment = item
                assignmentName = item.assignmentName
                dueTime = item.dueTime
                selectedClassId = item.classId
                classDropdownExpanded = false
                showAssignmentDialog = true
            }
        )

        ClassesSection(
            selectedDate = selectedDate,
            classesForSelectedDate = classesForSelectedDate,
            onClassClick = { classItem ->
                // Converts stored list into Set for UI selection handling.
                editingClass = classItem
                className = classItem.name
                classStartTime = classItem.startTime
                classEndTime = classItem.endTime
                selectedMeetingDays = classItem.meetingDays.toSet()
                showClassDialog = true
            }
        )
    }

    if (showAssignmentDialog && selectedDate != null) {
        AssignmentDialog(
            selectedDate = selectedDate,
            classItems = classItems,
            editingAssignment = editingAssignment,
            assignmentName = assignmentName,
            onAssignmentNameChange = { assignmentName = it },
            dueTime = dueTime,
            selectedClassId = selectedClassId,
            onSelectedClassIdChange = { selectedClassId = it },
            classDropdownExpanded = classDropdownExpanded,
            onClassDropdownExpandedChange = { classDropdownExpanded = it },
            onDismiss = {
                showAssignmentDialog = false
                resetAssignmentForm()
            },
            onPickTime = {
                // External helper; likely launches Android TimePickerDialog.
                openTimePicker(context) { picked ->
                    dueTime = picked
                }
            },
            onConfirm = {
                // Resolves class reference from ID → object (required by ViewModel).
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
                    resetAssignmentForm()
                }
            },
            onDelete = if (editingAssignment != null) {
                {
                    // Delete only available in edit mode.
                    viewModel.deleteScheduleItem(editingAssignment!!.id)
                    showAssignmentDialog = false
                    resetAssignmentForm()
                }
            } else {
                null
            }
        )
    }

    if (showClassDialog) {
        ClassDialog(
            editingClass = editingClass,
            className = className,
            onClassNameChange = { className = it },
            classStartTime = classStartTime,
            classEndTime = classEndTime,
            selectedMeetingDays = selectedMeetingDays,
            onMeetingDaysChange = { selectedMeetingDays = it },
            weekdayOptions = weekdayOptions,
            onPickStartTime = {
                openTimePicker(context) { picked ->
                    classStartTime = picked
                }
            },
            onPickEndTime = {
                openTimePicker(context) { picked ->
                    classEndTime = picked
                }
            },
            onDismiss = {
                showClassDialog = false
                resetClassForm()
            },
            onConfirm = { location ->
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
                            endTime = classEndTime,
                            location = location?.name ?: ""
                        )
                    } else {
                        viewModel.updateClass(
                            classId = classToEdit.id,
                            name = className,
                            meetingDays = selectedMeetingDays.toList(),
                            startTime = classStartTime,
                            endTime = classEndTime,
                            location = location?.name ?: ""
                        )
                    }

                    showClassDialog = false
                    resetClassForm()
                }
            },
            onDelete = if (editingClass != null) {
                {
                    viewModel.deleteClass(editingClass!!.id)
                    showClassDialog = false
                    resetClassForm()
                }
            } else null
        )
    }
}