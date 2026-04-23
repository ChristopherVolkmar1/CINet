package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import com.example.cinet.feature.settings.*
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.calendar.schedule.*
import com.example.cinet.feature.calendar.assignment.*
import com.example.cinet.feature.calendar.classEvent.ClassDialog
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.classEvent.ClassReminderScheduler
import com.example.cinet.feature.calendar.classEvent.ClassesSection
import com.example.cinet.feature.calendar.event.*
import com.example.cinet.feature.calendar.study.*
import com.example.cinet.core.time.*

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
    val markedDates = viewModel.getCalendarMarkedDates()

    var reminderRefreshKey by remember { mutableStateOf(0) }
    val reminderEventsForSelectedDate = remember(eventsForSelectedDate, reminderRefreshKey) {
        buildReminderEventsForSelectedDate(context, eventsForSelectedDate)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshStudySessions()
        viewModel.refreshEvents()
        viewModel.refreshCampusEvents()
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
    var sessionLocation by remember { mutableStateOf<CampusLocation?>(null) }

    var showEventDialog by remember { mutableStateOf(false) }
    var showCampusEventDialog by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventItem?>(null) }
    var selectedCampusEvent by remember { mutableStateOf<EventItem?>(null) }
    var eventName by remember { mutableStateOf("") }
    var eventTime by remember { mutableStateOf("") }
    var eventLocation by remember { mutableStateOf<CampusLocation?>(null) }

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

    fun resetStudySessionForm() {
        editingSession = null
        sessionClassName = ""
        sessionTopic = ""
        sessionStartTime = ""
        sessionLocation = null
    }

    fun resetEventForm() {
        editingEvent = null
        eventName = ""
        eventTime = ""
        eventLocation = null
    }

    fun formatDate(date: LocalDate): String =
        "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)

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
            markedDates = markedDates,
            onDateSelected = { day -> viewModel.selectDate(day) },
            onSameDateClicked = {
                resetAssignmentForm()
                showAssignmentDialog = true
            }
        )

        ScheduleSection(
            selectedDate = selectedDate,
            itemsForSelectedDate = itemsForSelectedDate,
            reminderEventsForSelectedDate = reminderEventsForSelectedDate,
            onItemClick = { item ->
                editingAssignment = item
                assignmentName = item.assignmentName
                dueTime = item.dueTime
                selectedClassId = item.classId
                classDropdownExpanded = false
                showAssignmentDialog = true
            },
            onReminderClick = { event ->
                selectedCampusEvent = event
                showCampusEventDialog = true
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

        StudySessionsSection(
            selectedDate = selectedDate,
            studySessionsForSelectedDate = studySessionsForSelectedDate,
            onSessionClick = { session ->
                editingSession = session
                sessionClassName = session.className
                sessionTopic = session.topic
                sessionStartTime = session.startTime
                sessionLocation = null
                showStudySessionDialog = true
            }
        )

        EventsSection(
            selectedDate = selectedDate,
            eventsForSelectedDate = eventsForSelectedDate,
            onEventClick = { event ->
                if (event.isCampusEvent) {
                    selectedCampusEvent = event
                    showCampusEventDialog = true
                } else {
                    editingEvent = event
                    eventName = event.name
                    eventTime = event.time
                    eventLocation = null
                    showEventDialog = true
                }
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
                openTimePicker(context) { picked -> dueTime = picked }
            },
            onConfirm = {
                val selectedClass = classItems.firstOrNull { it.id == selectedClassId }
                if (selectedClass != null && assignmentName.isNotBlank() && dueTime.isNotBlank()) {
                    val item = editingAssignment
                    val dateStr = formatDate(selectedDate)
                    if (item == null) {
                        viewModel.addScheduleItem(selectedClass, assignmentName, dueTime)
                        AssignmentReminderScheduler.scheduleReminder(
                            context = context,
                            date = dateStr,
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
                        viewModel.updateScheduleItem(item.id, selectedClass, assignmentName, dueTime)
                        AssignmentReminderScheduler.scheduleReminder(
                            context = context,
                            date = dateStr,
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
            onPickStartTime = {
                openTimePicker(context) { picked -> classStartTime = picked }
            },
            onPickEndTime = {
                openTimePicker(context) { picked -> classEndTime = picked }
            },
            onDismiss = {
                showClassDialog = false
                resetClassForm()
            },
            onConfirm = { campusLocation, remindersEnabled ->
                if (
                    className.isNotBlank() &&
                    selectedMeetingDays.isNotEmpty() &&
                    classStartTime.isNotBlank() &&
                    classEndTime.isNotBlank()
                ) {
                    val classToEdit = editingClass
                    val meetingDaysList = selectedMeetingDays.toList()
                    val locationName = when {
                        campusLocation != null -> campusLocation.name
                        classToEdit != null -> classToEdit.location
                        else -> ""
                    }

                    if (classToEdit == null) {
                        viewModel.addClass(
                            name = className,
                            meetingDays = meetingDaysList,
                            startTime = classStartTime,
                            endTime = classEndTime,
                            location = locationName,
                            remindersEnabled = remindersEnabled
                        )
                        val newClass = ClassItem(
                            id = "${className}_${classStartTime}_${meetingDaysList.joinToString("_")}",
                            name = className,
                            meetingDays = meetingDaysList,
                            startTime = classStartTime,
                            endTime = classEndTime,
                            location = locationName,
                            remindersEnabled = remindersEnabled
                        )
                        ClassReminderScheduler.scheduleNextReminder(
                            context = context,
                            classItem = newClass,
                            minutesBefore = AppSettings.classReminderMinutesBefore.toInt()
                        )
                    } else {
                        ClassReminderScheduler.cancelReminder(context, classToEdit)
                        viewModel.updateClass(
                            classId = classToEdit.id,
                            name = className,
                            meetingDays = meetingDaysList,
                            startTime = classStartTime,
                            endTime = classEndTime,
                            location = locationName,
                            remindersEnabled = remindersEnabled
                        )
                        val updatedClass = ClassItem(
                            id = classToEdit.id,
                            name = className,
                            meetingDays = meetingDaysList,
                            startTime = classStartTime,
                            endTime = classEndTime,
                            location = locationName,
                            remindersEnabled = remindersEnabled
                        )
                        ClassReminderScheduler.scheduleNextReminder(
                            context = context,
                            classItem = updatedClass,
                            minutesBefore = AppSettings.classReminderMinutesBefore.toInt()
                        )
                    }
                    showClassDialog = false
                    resetClassForm()
                }
            },
            onDelete = if (editingClass != null) {
                {
                    val c = editingClass!!
                    ClassReminderScheduler.cancelReminder(context, c)
                    viewModel.deleteClass(c.id)
                    showClassDialog = false
                    resetClassForm()
                }
            } else null
        )
    }

    if (showStudySessionDialog && selectedDate != null) {
        val dateStr = formatDate(selectedDate)
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
                    if (s == null) viewModel.addStudySession(dateStr, sessionClassName, sessionTopic, sessionStartTime, sessionLocation?.name ?: "")
                    else viewModel.updateStudySession(s.id, dateStr, sessionClassName, sessionTopic, sessionStartTime, sessionLocation?.name ?: "")
                    showStudySessionDialog = false
                    resetStudySessionForm()
                }
            },
            onDelete = if (editingSession != null) {
                {
                    viewModel.deleteStudySession(editingSession!!.id)
                    showStudySessionDialog = false
                    resetStudySessionForm()
                }
            } else null
        )
    }

    if (showCampusEventDialog && selectedCampusEvent != null) {
        HandleCampusEventReminderToggle(
            event = selectedCampusEvent!!,
            onReminderChanged = { reminderRefreshKey++ },
            onDismiss = {
                showCampusEventDialog = false
                selectedCampusEvent = null
            }
        )
    }

    if (showEventDialog && selectedDate != null) {
        val dateStr = formatDate(selectedDate)
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
                    if (e == null) viewModel.addEvent(dateStr, eventName, eventTime, eventLocation?.name ?: "")
                    else viewModel.updateEvent(e.id, dateStr, eventName, eventTime, eventLocation?.name ?: "")
                    showEventDialog = false
                    resetEventForm()
                }
            },
            onDelete = if (editingEvent != null) {
                {
                    viewModel.deleteEvent(editingEvent!!.id)
                    showEventDialog = false
                    resetEventForm()
                }
            } else null
        )
    }
}

/** Returns only the campus events on the selected day that currently have reminders enabled. */
private fun buildReminderEventsForSelectedDate(
    context: android.content.Context,
    eventsForSelectedDate: List<EventItem>
): List<EventItem> {
    return eventsForSelectedDate.filter(isCampusEventWithReminderEnabled(context))
}

/** Builds the predicate used to keep only reminder-enabled campus events. */
private fun isCampusEventWithReminderEnabled(context: android.content.Context): (EventItem) -> Boolean = { event ->
    event.isCampusEvent && CampusEventReminderPreferences.isReminderEnabled(context, event.id)
}

@Composable
private fun HandleCampusEventReminderToggle(
    event: EventItem,
    onReminderChanged: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isReminderEnabled by remember(event.id) {
        mutableStateOf(CampusEventReminderPreferences.isReminderEnabled(context, event.id))
    }

    CampusEventDetailsDialog(
        event = event,
        isReminderEnabled = isReminderEnabled,
        onReminderToggle = { enabled ->
            if (enabled) {
                val wasScheduled = CampusEventReminderScheduler.scheduleReminder(context, event)
                if (wasScheduled) {
                    CampusEventReminderPreferences.setReminderEnabled(context, event.id, true)
                    isReminderEnabled = true
                    onReminderChanged()
                    Toast.makeText(
                        context,
                        if (event.allDay) "Reminder set for 9:00 AM on the event day" else "Reminder set for 30 minutes before the event",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    CampusEventReminderPreferences.setReminderEnabled(context, event.id, false)
                    isReminderEnabled = false
                    onReminderChanged()
                    Toast.makeText(
                        context,
                        "This event has already started, so a reminder could not be set",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                CampusEventReminderScheduler.cancelReminder(context, event)
                CampusEventReminderPreferences.setReminderEnabled(context, event.id, false)
                isReminderEnabled = false
                onReminderChanged()
                Toast.makeText(context, "Reminder removed", Toast.LENGTH_SHORT).show()
            }
        },
        onDismiss = onDismiss
    )
}