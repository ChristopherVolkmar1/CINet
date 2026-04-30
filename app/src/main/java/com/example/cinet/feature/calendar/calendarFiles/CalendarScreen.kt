package com.example.cinet.feature.calendar.calendarFiles

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.YearMonth
import com.example.cinet.feature.settings.*
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.calendar.schedule.*
import com.example.cinet.feature.calendar.assignment.*
import com.example.cinet.feature.calendar.classEvent.ClassDialog
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.classEvent.ClassReminderScheduler
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
    val activeDate = selectedDate ?: today
    val calendarMode = viewModel.mode
    var reminderRefreshKey by remember { mutableStateOf(0) }
    val agendaCountByDate = remember(
        classItems,
        viewModel.studySessions,
        viewModel.userEventItems,
        viewModel.campusEventItems,
        currentMonth,
        reminderRefreshKey
    ) {
        buildAgendaActivityCountByDate(
            context = context,
            currentMonth = currentMonth,
            classes = classItems,
            studySessions = viewModel.studySessions,
            customEvents = viewModel.userEventItems,
            campusEvents = viewModel.campusEventItems
        )
    }

    val classesForSelectedDate = viewModel.getClassesForSelectedDate()
    val studySessionsForSelectedDate = viewModel.getStudySessionsForSelectedDate()
    val eventsForSelectedDate = viewModel.getEventsForSelectedDate()
    val customEventsForSelectedDate = viewModel.getCustomEventsForSelectedDate()
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
    var selectedQuickAccessType by remember { mutableStateOf<CalendarQuickAccessType?>(null) }

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

    fun openAssignmentEditor(item: ScheduleItem) {
        editingAssignment = item
        assignmentName = item.assignmentName
        dueTime = item.dueTime
        selectedClassId = item.classId
        classDropdownExpanded = false
        showAssignmentDialog = true
    }

    fun openClassEditor(classItem: ClassItem) {
        editingClass = classItem
        className = classItem.name
        classStartTime = classItem.startTime
        classEndTime = classItem.endTime
        selectedMeetingDays = classItem.meetingDays.toSet()
        showClassDialog = true
    }

    fun openStudySessionEditor(session: StudySession) {
        editingSession = session
        sessionClassName = session.className
        sessionTopic = session.topic
        sessionStartTime = session.startTime
        sessionLocation = null
        showStudySessionDialog = true
    }

    fun openEventEditor(event: EventItem) {
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

    fun formatDate(date: LocalDate): String =
        "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)

    fun handleDateClick(date: LocalDate) {
        if (selectedDate == date) {
            resetAssignmentForm()
            showAssignmentDialog = true
        } else {
            viewModel.onDateSelected(date)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        CalendarHeader(onBack = onBack)

        Spacer(modifier = Modifier.height(12.dp))

        CalendarModeTabs(
            selectedMode = calendarMode,
            onModeSelected = { viewModel.updateMode(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        CalendarModeContent(
            mode = calendarMode,
            currentMonth = currentMonth,
            selectedDate = activeDate,
            activityCountByDate = agendaCountByDate,
            onDateSelected = ::handleDateClick,
            onPreviousDay = { viewModel.previousDay() },
            onNextDay = { viewModel.nextDay() },
            onPreviousWeek = { viewModel.previousWeek() },
            onNextWeek = { viewModel.nextWeek() },
            onPreviousMonth = { viewModel.previousMonth() },
            onNextMonth = { viewModel.nextMonth() }
        )

        Spacer(modifier = Modifier.height(18.dp))

        CalendarDailyAgendaCard(
            selectedDate = activeDate,
            classes = classesForSelectedDate,
            studySessions = studySessionsForSelectedDate,
            events = customEventsForSelectedDate,
            reminderCampusEvents = reminderEventsForSelectedDate,
            onTodayClick = { viewModel.onDateSelected(today) },
            onClassClick = ::openClassEditor,
            onStudySessionClick = ::openStudySessionEditor,
            onEventClick = ::openEventEditor
        )

        Spacer(modifier = Modifier.height(24.dp))

        CalendarQuickAccessCards(
            onClassesClick = { selectedQuickAccessType = CalendarQuickAccessType.CLASSES },
            onStudyClick = { selectedQuickAccessType = CalendarQuickAccessType.STUDY },
            onEventsClick = { selectedQuickAccessType = CalendarQuickAccessType.EVENTS }
        )


    }

    selectedQuickAccessType?.let { quickAccessType ->
        CalendarQuickAccessPopup(
            type = quickAccessType,
            selectedDate = activeDate,
            classes = classesForSelectedDate,
            studySessions = studySessionsForSelectedDate,
            events = eventsForSelectedDate,
            onDismiss = { selectedQuickAccessType = null },
            onAddClick = {
                selectedQuickAccessType = null
                when (quickAccessType) {
                    CalendarQuickAccessType.CLASSES -> {
                        resetClassForm()
                        showClassDialog = true
                    }

                    CalendarQuickAccessType.STUDY -> {
                        resetStudySessionForm()
                        showStudySessionDialog = true
                    }

                    CalendarQuickAccessType.EVENTS -> {
                        resetEventForm()
                        showEventDialog = true
                    }
                }
            },
            onClassClick = { classItem ->
                selectedQuickAccessType = null
                openClassEditor(classItem)
            },
            onStudySessionClick = { session ->
                selectedQuickAccessType = null
                openStudySessionEditor(session)
            },
            onEventClick = { event ->
                selectedQuickAccessType = null
                openEventEditor(event)
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
                    val locationName = campusLocation?.name ?: ""

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
                            minutesBefore = AppSettings.classReminderMinutesBefore
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
                            minutesBefore = AppSettings.classReminderMinutesBefore
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


/** Builds the day-dot map for the content shown in the main agenda card. */
private fun buildAgendaActivityCountByDate(
    context: android.content.Context,
    currentMonth: YearMonth,
    classes: List<ClassItem>,
    studySessions: List<StudySession>,
    customEvents: List<EventItem>,
    campusEvents: List<EventItem>
): Map<LocalDate, Int> {
    val counts = mutableMapOf<LocalDate, Int>()

    addDatesToAgendaCountMap(counts, studySessions.map { it.date })
    addDatesToAgendaCountMap(counts, customEvents.map { it.date })
    addDatesToAgendaCountMap(
        counts,
        campusEvents
            .filter(isCampusEventWithReminderEnabled(context))
            .map { it.date }
    )
    addClassDatesToAgendaCountMap(
        counts = counts,
        currentMonth = currentMonth,
        classes = classes
    )

    return counts
}

/** Adds recurring class meetings to the dot map for the visible calendar window. */
private fun addClassDatesToAgendaCountMap(
    counts: MutableMap<LocalDate, Int>,
    currentMonth: YearMonth,
    classes: List<ClassItem>
) {
    val start = currentMonth.atDay(1).minusDays(7)
    val end = currentMonth.atEndOfMonth().plusDays(7)
    var date = start

    while (!date.isAfter(end)) {
        val dayName = dayNameForAgendaDate(date)
        val classCount = classes.count { classItem -> classItem.meetingDays.contains(dayName) }
        if (classCount > 0) {
            counts[date] = (counts[date] ?: 0) + classCount
        }
        date = date.plusDays(1)
    }
}

/** Adds parseable yyyy-MM-dd dates to the agenda dot count map. */
private fun addDatesToAgendaCountMap(
    counts: MutableMap<LocalDate, Int>,
    dateStrings: List<String>
) {
    dateStrings.forEach { dateString ->
        parseAgendaDateOrNull(dateString)?.let { date ->
            counts[date] = (counts[date] ?: 0) + 1
        }
    }
}

/** Converts a LocalDate into the short weekday labels stored by ClassItem. */
private fun dayNameForAgendaDate(date: LocalDate): String {
    return when (date.dayOfWeek.value) {
        1 -> "Mon"
        2 -> "Tue"
        3 -> "Wed"
        4 -> "Thu"
        5 -> "Fri"
        6 -> "Sat"
        7 -> "Sun"
        else -> ""
    }
}

/** Parses a date string without crashing the calendar UI. */
private fun parseAgendaDateOrNull(dateString: String): LocalDate? {
    return try {
        LocalDate.parse(dateString)
    } catch (_: Exception) {
        null
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