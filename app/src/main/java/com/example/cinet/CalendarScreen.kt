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
    val viewModel: CalendarViewModel = viewModel()
    val context = LocalContext.current
    val today = remember { LocalDate.now() }

    val classItems = viewModel.classItems
    val currentMonth = viewModel.currentMonth
    val selectedDate = viewModel.selectedDate

    val itemsForSelectedDate = viewModel.getItemsForSelectedDate()
    val classesForSelectedDate = viewModel.getClassesForSelectedDate()

    var showAssignmentDialog by remember { mutableStateOf(false) }
    var showClassDialog by remember { mutableStateOf(initialShowClassDialog) }

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
    }

    fun formatDate(date: LocalDate): String {
        return "%04d-%02d-%02d".format(
            date.year,
            date.monthValue,
            date.dayOfMonth
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        CalendarHeader(
            currentMonth = currentMonth,
            onBack = onBack,
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
                resetAssignmentForm()
                showAssignmentDialog = true
            }
        )

        ScheduleSection(
            selectedDate = selectedDate,
            itemsForSelectedDate = itemsForSelectedDate,
            onItemClick = { item ->
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
                openTimePicker(context) { picked ->
                    dueTime = picked
                }
            },
            onConfirm = {
                val selectedClass = classItems.firstOrNull { it.id == selectedClassId }

                if (
                    selectedClass != null &&
                    assignmentName.isNotBlank() &&
                    dueTime.isNotBlank()
                ) {
                    val item = editingAssignment
                    val selectedDateString = formatDate(selectedDate)

                    if (item == null) {
                        viewModel.addScheduleItem(
                            classItem = selectedClass,
                            assignmentName = assignmentName,
                            dueTime = dueTime
                        )

                        AssignmentReminderScheduler.scheduleReminder(
                            context = context,
                            date = selectedDateString,
                            classId = selectedClass.id,
                            className = selectedClass.name,
                            assignmentName = assignmentName,
                            dueTime = dueTime,
                            minutesBefore = AppSettings.assignmentReminderMinutesBefore
                        )
                    } else {
                        AssignmentReminderScheduler.cancelReminder(
                            context = context,
                            date = item.date,
                            classId = item.classId,
                            assignmentName = item.assignmentName,
                            dueTime = item.dueTime
                        )

                        viewModel.updateScheduleItem(
                            itemId = item.id,
                            classItem = selectedClass,
                            assignmentName = assignmentName,
                            dueTime = dueTime
                        )

                        AssignmentReminderScheduler.scheduleReminder(
                            context = context,
                            date = selectedDateString,
                            classId = selectedClass.id,
                            className = selectedClass.name,
                            assignmentName = assignmentName,
                            dueTime = dueTime,
                            minutesBefore = AppSettings.assignmentReminderMinutesBefore
                        )
                    }

                    showAssignmentDialog = false
                    resetAssignmentForm()
                }
            },
            onDelete = if (editingAssignment != null) {
                {
                    val item = editingAssignment!!

                    AssignmentReminderScheduler.cancelReminder(
                        context = context,
                        date = item.date,
                        classId = item.classId,
                        assignmentName = item.assignmentName,
                        dueTime = item.dueTime
                    )

                    viewModel.deleteScheduleItem(item.id)
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
            onConfirm = {
                if (
                    className.isNotBlank() &&
                    selectedMeetingDays.isNotEmpty() &&
                    classStartTime.isNotBlank() &&
                    classEndTime.isNotBlank()
                ) {
                    val classToEdit = editingClass
                    val meetingDaysList = selectedMeetingDays.toList()

                    if (classToEdit == null) {
                        viewModel.addClass(
                            name = className,
                            meetingDays = meetingDaysList,
                            startTime = classStartTime,
                            endTime = classEndTime
                        )

                        val newClass = ClassItem(
                            id = "${className}_${classStartTime}_${meetingDaysList.joinToString("_")}",
                            name = className,
                            meetingDays = meetingDaysList,
                            startTime = classStartTime,
                            endTime = classEndTime
                        )

                        ClassReminderScheduler.scheduleNextReminder(
                            context = context,
                            classItem = newClass,
                            minutesBefore = AppSettings.classReminderMinutesBefore
                        )
                    } else {
                        ClassReminderScheduler.cancelReminder(context, classToEdit)

                        viewModel.updateClass(
                            classId = classToEdit.id,
                            name = className,
                            meetingDays = meetingDaysList,
                            startTime = classStartTime,
                            endTime = classEndTime
                        )

                        val updatedClass = ClassItem(
                            id = classToEdit.id,
                            name = className,
                            meetingDays = meetingDaysList,
                            startTime = classStartTime,
                            endTime = classEndTime
                        )

                        ClassReminderScheduler.scheduleNextReminder(
                            context = context,
                            classItem = updatedClass,
                            minutesBefore = AppSettings.classReminderMinutesBefore
                        )
                    }

                    showClassDialog = false
                    resetClassForm()
                }
            },
            onDelete = if (editingClass != null) {
                {
                    val classToDelete = editingClass!!
                    ClassReminderScheduler.cancelReminder(context, classToDelete)
                    viewModel.deleteClass(classToDelete.id)
                    showClassDialog = false
                    resetClassForm()
                }
            } else {
                null
            }
        )
    }
}