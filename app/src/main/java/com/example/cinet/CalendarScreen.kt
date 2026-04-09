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
import androidx.compose.runtime.LaunchedEffect

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
    val studySessionsForSelectedDate = viewModel.getStudySessionsForSelectedDate()
    val eventsForSelectedDate = viewModel.getEventsForSelectedDate()
    LaunchedEffect(Unit) {
        viewModel.refreshStudySessions()
        viewModel.refreshEvents()
    }

    var showAssignmentDialog by remember { mutableStateOf(false) }
    var showClassDialog by remember { mutableStateOf(initialShowClassDialog) }
    var editingAssignment by remember { mutableStateOf<ScheduleItem?>(null) }
    var assignmentName by remember { mutableStateOf("") }
    var dueTime by remember { mutableStateOf("") }
    var selectedClassId by remember { mutableStateOf<String?>(null) }
    var classDropdownExpanded by remember { mutableStateOf(false) }

    var editingClass by remember { mutableStateOf<ClassItem?>(null) }
    var className by remember { mutableStateOf("") }
    var classStartTime by remember { mutableStateOf("") }
    var classEndTime by remember { mutableStateOf("") }
    var selectedMeetingDays by remember { mutableStateOf(setOf<String>()) }
    val weekdayOptions = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    var showStudySessionDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<StudySession?>(null) }
    var sessionClassName by remember { mutableStateOf("") }
    var sessionTopic by remember { mutableStateOf("") }
    var sessionStartTime by remember { mutableStateOf("") }
    var sessionLocation by remember { mutableStateOf("") }

    var showEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventItem?>(null) }
    var eventName by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf("") }

    fun resetAssignmentForm() {
        editingAssignment = null; assignmentName = ""; dueTime = ""; selectedClassId = null; classDropdownExpanded = false
    }
    fun resetClassForm() {
        editingClass = null; className = ""; classStartTime = ""; classEndTime = ""; selectedMeetingDays = emptySet()
    }
    fun resetStudySessionForm() {
        editingSession = null; sessionClassName = ""; sessionTopic = ""; sessionStartTime = ""; sessionLocation = ""
    }
    fun resetEventForm() {
        editingEvent = null; eventName = ""; eventTime = ""; eventLocation = ""
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { resetClassForm(); showClassDialog = true }) {
                Text("Classes")
            }
            OutlinedButton(onClick = { resetStudySessionForm(); showStudySessionDialog = true }) {
                Text("Study")
            }
            OutlinedButton(onClick = { resetEventForm(); showEventDialog = true }) {
                Text("Events")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        CalendarGrid(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            today = today,
            scheduleItems = viewModel.scheduleItems,
            onDateSelected = { day -> viewModel.selectDate(day) },
            onSameDateClicked = { resetAssignmentForm(); showAssignmentDialog = true }
        )

        ScheduleSection(
            selectedDate = selectedDate,
            itemsForSelectedDate = itemsForSelectedDate,
            onItemClick = { item ->
                editingAssignment = item; assignmentName = item.assignmentName
                dueTime = item.dueTime; selectedClassId = item.classId
                classDropdownExpanded = false; showAssignmentDialog = true
            }
        )

        ClassesSection(
            selectedDate = selectedDate,
            classesForSelectedDate = classesForSelectedDate,
            onClassClick = { classItem ->
                editingClass = classItem; className = classItem.name
                classStartTime = classItem.startTime; classEndTime = classItem.endTime
                selectedMeetingDays = classItem.meetingDays.toSet(); showClassDialog = true
            }
        )

        StudySessionsSection(
            selectedDate = selectedDate,
            studySessionsForSelectedDate = studySessionsForSelectedDate,
            onSessionClick = { session ->
                editingSession = session; sessionClassName = session.className
                sessionTopic = session.topic; sessionStartTime = session.startTime
                sessionLocation = session.location; showStudySessionDialog = true
            }
        )

        EventsSection(
            selectedDate = selectedDate,
            eventsForSelectedDate = eventsForSelectedDate,
            onEventClick = { event ->
                editingEvent = event; eventName = event.name
                eventTime = event.time; eventLocation = event.location
                showEventDialog = true
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
            onDismiss = { showAssignmentDialog = false; resetAssignmentForm() },
            onPickTime = { openTimePicker(context) { picked -> dueTime = picked } },
            onConfirm = {
                val selectedClass = classItems.firstOrNull { it.id == selectedClassId }
                if (selectedClass != null && assignmentName.isNotBlank() && dueTime.isNotBlank()) {
                    val item = editingAssignment
                    if (item == null) viewModel.addScheduleItem(selectedClass, assignmentName, dueTime)
                    else viewModel.updateScheduleItem(item.id, selectedClass, assignmentName, dueTime)
                    showAssignmentDialog = false; resetAssignmentForm()
                }
            },
            onDelete = if (editingAssignment != null) {
                { viewModel.deleteScheduleItem(editingAssignment!!.id); showAssignmentDialog = false; resetAssignmentForm() }
            } else null
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
            onPickStartTime = { openTimePicker(context) { picked -> classStartTime = picked } },
            onPickEndTime = { openTimePicker(context) { picked -> classEndTime = picked } },
            onDismiss = { showClassDialog = false; resetClassForm() },
            onConfirm = {
                if (className.isNotBlank() && selectedMeetingDays.isNotEmpty() && classStartTime.isNotBlank() && classEndTime.isNotBlank()) {
                    val c = editingClass
                    if (c == null) viewModel.addClass(className, selectedMeetingDays.toList(), classStartTime, classEndTime)
                    else viewModel.updateClass(c.id, className, selectedMeetingDays.toList(), classStartTime, classEndTime)
                    showClassDialog = false; resetClassForm()
                }
            },
            onDelete = if (editingClass != null) {
                { viewModel.deleteClass(editingClass!!.id); showClassDialog = false; resetClassForm() }
            } else null
        )
    }

    if (showStudySessionDialog && selectedDate != null) {
        val dateStr = "%04d-%02d-%02d".format(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth)
        StudySessionDialog(
            editingSession = editingSession,
            date = dateStr,
            className = sessionClassName,
            onClassNameChange = { sessionClassName = it },
            topic = sessionTopic,
            onTopicChange = { sessionTopic = it },
            startTime = sessionStartTime,
            location = sessionLocation,
            onLocationChange = { sessionLocation = it },
            onPickStartTime = { openTimePicker(context) { picked -> sessionStartTime = picked } },
            onDismiss = { showStudySessionDialog = false; resetStudySessionForm() },
            onConfirm = {
                if (sessionClassName.isNotBlank() && sessionTopic.isNotBlank() && sessionStartTime.isNotBlank()) {
                    val s = editingSession
                    if (s == null) viewModel.addStudySession(dateStr, sessionClassName, sessionTopic, sessionStartTime, sessionLocation)
                    else viewModel.updateStudySession(s.id, dateStr, sessionClassName, sessionTopic, sessionStartTime, sessionLocation)
                    showStudySessionDialog = false; resetStudySessionForm()
                }
            },
            onDelete = if (editingSession != null) {
                { viewModel.deleteStudySession(editingSession!!.id); showStudySessionDialog = false; resetStudySessionForm() }
            } else null
        )
    }

    if (showEventDialog && selectedDate != null) {
        val dateStr = "%04d-%02d-%02d".format(selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth)
        EventItemDialog(
            editingEvent = editingEvent,
            date = dateStr,
            eventName = eventName,
            onEventNameChange = { eventName = it },
            eventTime = eventTime,
            location = eventLocation,
            onLocationChange = { eventLocation = it },
            onPickTime = { openTimePicker(context) { picked -> eventTime = picked } },
            onDismiss = { showEventDialog = false; resetEventForm() },
            onConfirm = {
                if (eventName.isNotBlank() && eventTime.isNotBlank()) {
                    val e = editingEvent
                    if (e == null) viewModel.addEvent(dateStr, eventName, eventTime, eventLocation)
                    else viewModel.updateEvent(e.id, dateStr, eventName, eventTime, eventLocation)
                    showEventDialog = false; resetEventForm()
                }
            },
            onDelete = if (editingEvent != null) {
                { viewModel.deleteEvent(editingEvent!!.id); showEventDialog = false; resetEventForm() }
            } else null
        )
    }
}