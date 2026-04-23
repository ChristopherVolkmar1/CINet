package com.example.cinet.feature.calendar.calendarFiles

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.event.CampusEventFeedRepository
import com.example.cinet.feature.calendar.event.EventItem
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import com.example.cinet.feature.calendar.study.StudySession
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel : ViewModel() {

    // Holds the month currently shown in the calendar header/grid.
    var currentMonth by mutableStateOf(YearMonth.now())
        private set

    // Holds the day currently selected by the user.
    var selectedDate by mutableStateOf<LocalDate?>(null)
        private set

    // Stores the user's saved classes from Firestore.
    var classItems by mutableStateOf<List<ClassItem>>(emptyList())
        private set

    // Stores the user's saved assignments from Firestore.
    var scheduleItems by mutableStateOf<List<ScheduleItem>>(emptyList())
        private set

    // Stores the user's saved study sessions from Firestore.
    var studySessions by mutableStateOf<List<StudySession>>(emptyList())
        private set

    // Stores the user's manually created events from Firestore.
    var userEventItems by mutableStateOf<List<EventItem>>(emptyList())
        private set

    // Stores the live campus events fetched from the ICS feed.
    var campusEventItems by mutableStateOf<List<EventItem>>(emptyList())
        private set

    private val repository = CalendarFirestoreRepository()
    private val campusEventRepository = CampusEventFeedRepository()

    init {
        // Automatically loads calendar data when the ViewModel is created.
        refreshClasses()
        refreshAssignments()
        refreshStudySessions()
        refreshEvents()
        refreshCampusEvents()
    }

    // Advances the visible month by one.
    fun nextMonth() {
        currentMonth = currentMonth.plusMonths(1)
    }

    // Moves the visible month back by one.
    fun previousMonth() {
        currentMonth = currentMonth.minusMonths(1)
    }

    // Marks one day as selected inside the current month.
    fun selectDate(day: Int) {
        selectedDate = currentMonth.atDay(day)
    }

    // Saves a new class for the current user.
    fun addClass(
        name: String,
        meetingDays: List<String>,
        startTime: String,
        endTime: String,
        location: String,
        remindersEnabled: Boolean = true
    ) {
        val orderedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sortedMeetingDays = meetingDays.sortedBy { orderedDays.indexOf(it) }

        viewModelScope.launch {
            try {
                val newClass = repository.addClass(
                    name = name,
                    meetingDays = sortedMeetingDays,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    remindersEnabled = remindersEnabled
                )
                refreshClasses()
                Log.d("FirestoreDebug", "New class created: ${newClass.name}, id=${newClass.id}")
            } catch (e: Exception) {
                Log.e("FirestoreDebug", "addClass failed", e)
            }
        }
    }

    // Updates an existing class and keeps related assignments aligned with a class rename.
    fun updateClass(
        classId: String,
        name: String,
        meetingDays: List<String>,
        startTime: String,
        endTime: String,
        location: String,
        remindersEnabled: Boolean = true
    ) {
        val orderedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val sortedMeetingDays = meetingDays.sortedBy { orderedDays.indexOf(it) }

        viewModelScope.launch {
            try {
                repository.updateClass(
                    classId = classId,
                    name = name,
                    meetingDays = sortedMeetingDays,
                    startTime = startTime,
                    endTime = endTime,
                    location = location,
                    remindersEnabled = remindersEnabled
                )

                scheduleItems = scheduleItems.map { item ->
                    if (item.classId == classId) item.copy(className = name) else item
                }

                refreshClasses()
            } catch (e: Exception) {
                Log.e("FirestoreDebug", "updateClass failed", e)
            }
        }
    }

    // Deletes one class and removes any local assignments tied to it.
    fun deleteClass(classId: String) {
        viewModelScope.launch {
            try {
                repository.deleteClass(classId)
                scheduleItems = scheduleItems.filterNot { it.classId == classId }
                refreshClasses()
            } catch (e: Exception) {
                Log.e("FirestoreDebug", "deleteClass failed", e)
            }
        }
    }

    // Adds a new assignment to the currently selected date.
    fun addScheduleItem(classItem: ClassItem, assignmentName: String, dueTime: String) {
        val date = selectedDate ?: return
        val formattedDate = formatDate(date)

        viewModelScope.launch {
            try {
                repository.addAssignment(
                    date = formattedDate,
                    classId = classItem.id,
                    className = classItem.name,
                    assignmentName = assignmentName,
                    dueTime = dueTime
                )
                refreshAssignments()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Updates one assignment on the currently selected date.
    fun updateScheduleItem(itemId: String, classItem: ClassItem, assignmentName: String, dueTime: String) {
        val date = selectedDate ?: return
        val formattedDate = formatDate(date)

        viewModelScope.launch {
            try {
                repository.updateAssignment(
                    assignmentId = itemId,
                    date = formattedDate,
                    classId = classItem.id,
                    className = classItem.name,
                    assignmentName = assignmentName,
                    dueTime = dueTime
                )
                refreshAssignments()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Deletes one assignment.
    fun deleteScheduleItem(itemId: String) {
        viewModelScope.launch {
            try {
                repository.deleteAssignment(itemId)
                refreshAssignments()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Adds one study session for the selected day.
    fun addStudySession(date: String, className: String, topic: String, startTime: String, location: String) {
        viewModelScope.launch {
            try {
                repository.addStudySession(date, className, topic, startTime, location)
                refreshStudySessions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Updates one study session.
    fun updateStudySession(sessionId: String, date: String, className: String, topic: String, startTime: String, location: String) {
        viewModelScope.launch {
            try {
                repository.updateStudySession(sessionId, date, className, topic, startTime, location)
                refreshStudySessions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Deletes one study session.
    fun deleteStudySession(sessionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteStudySession(sessionId)
                refreshStudySessions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Adds one user-created event for the selected day.
    fun addEvent(date: String, name: String, time: String, location: String) {
        viewModelScope.launch {
            try {
                repository.addEvent(date, name, time, location)
                refreshEvents()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Updates one user-created event.
    fun updateEvent(eventId: String, date: String, name: String, time: String, location: String) {
        viewModelScope.launch {
            try {
                repository.updateEvent(eventId, date, name, time, location)
                refreshEvents()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Deletes one user-created event.
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEvent(eventId)
                refreshEvents()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Returns assignments for the selected day, ordered by due time.
    fun getItemsForSelectedDate(): List<ScheduleItem> {
        val date = selectedDate ?: return emptyList()
        val formattedDate = formatDate(date)
        return scheduleItems
            .filter { it.date == formattedDate }
            .sortedBy { parseTimeToSortableValue(it.dueTime) }
    }

    // Groups classes by weekday so other calendar views can reuse the same data.
    fun getClassesGroupedByDay(): Map<String, List<ClassItem>> {
        val orderedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return orderedDays.associateWith { day ->
            classItems
                .filter { it.meetingDays.contains(day) }
                .sortedBy { parseTimeToSortableValue(it.startTime) }
        }.filterValues { it.isNotEmpty() }
    }

    // Returns only the classes that meet on the selected day.
    fun getClassesForSelectedDate(): List<ClassItem> {
        val date = selectedDate ?: return emptyList()
        val dayName = when (date.dayOfWeek.value) {
            1 -> "Mon"
            2 -> "Tue"
            3 -> "Wed"
            4 -> "Thu"
            5 -> "Fri"
            6 -> "Sat"
            7 -> "Sun"
            else -> ""
        }

        return classItems
            .filter { it.meetingDays.contains(dayName) }
            .sortedBy { parseTimeToSortableValue(it.startTime) }
    }

    // Returns study sessions for the selected day.
    fun getStudySessionsForSelectedDate(): List<StudySession> {
        val date = selectedDate ?: return emptyList()
        val formattedDate = formatDate(date)
        return studySessions
            .filter { it.date == formattedDate }
            .sortedBy { parseTimeToSortableValue(it.startTime) }
    }

    // Returns both user-created and live campus events for the selected day.
    fun getEventsForSelectedDate(): List<EventItem> {
        val date = selectedDate ?: return emptyList()
        val formattedDate = formatDate(date)
        return allEventItems()
            .filter { it.date == formattedDate }
            .sortedBy { parseTimeToSortableValue(it.time) }
    }

    // Returns every date string that should show an activity dot on the calendar grid.
    fun getCalendarMarkedDates(): Set<String> {
        return buildSet {
            addAll(scheduleItems.map { it.date })
            addAll(allEventItems().map { it.date })
        }
    }

    // Reloads the user's class list from Firestore.
    fun refreshClasses() {
        viewModelScope.launch {
            try {
                classItems = sortClassesByTime(repository.loadClasses())
                Log.d("FirestoreDebug", "Loaded classItems count: ${classItems.size}")
                classItems.forEach {
                    Log.d(
                        "FirestoreDebug",
                        "Class in ViewModel: ${it.name}, days=${it.meetingDays}, start=${it.startTime}, end=${it.endTime}"
                    )
                }
            } catch (e: Exception) {
                Log.e("FirestoreDebug", "refreshClasses failed", e)
            }
        }
    }

    // Reloads the user's assignments from Firestore.
    fun refreshAssignments() {
        viewModelScope.launch {
            try {
                scheduleItems = repository.loadAssignments()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Reloads the user's study sessions from Firestore.
    fun refreshStudySessions() {
        viewModelScope.launch {
            try {
                studySessions = repository.loadStudySessions()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Reloads the user's custom events from Firestore.
    fun refreshEvents() {
        viewModelScope.launch {
            try {
                userEventItems = repository.loadEvents()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Reloads live campus events from the ICS feed.
    fun refreshCampusEvents() {
        viewModelScope.launch {
            try {
                campusEventItems = campusEventRepository.loadCampusEvents()
            } catch (e: Exception) {
                Log.e("CampusEventFeed", "refreshCampusEvents failed", e)
            }
        }
    }

    // Formats a LocalDate into the string format already used across this calendar feature.
    private fun formatDate(date: LocalDate): String {
        return "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
    }

    // Converts displayed times into sortable minute values.
    private fun parseTimeToSortableValue(time: String): Int {
        if (time.equals("All day", ignoreCase = true)) return Int.MIN_VALUE

        val normalizedTime = time.substringBefore("-").trim()
        val parts = normalizedTime.split(" ", ":")
        if (parts.size < 3) return Int.MAX_VALUE

        var hour = parts[0].toIntOrNull() ?: return Int.MAX_VALUE
        val minute = parts[1].toIntOrNull() ?: return Int.MAX_VALUE
        val amPm = parts[2]

        if (amPm == "PM" && hour != 12) hour += 12
        if (amPm == "AM" && hour == 12) hour = 0
        return hour * 60 + minute
    }

    // Combines the two event sources into one list for display/filtering.
    private fun allEventItems(): List<EventItem> {
        return (userEventItems + campusEventItems).distinctBy { it.id }
    }

    // Sorts classes by their starting time for consistent display.
    private fun sortClassesByTime(classes: List<ClassItem>): List<ClassItem> {
        return classes.sortedBy { parseTimeToSortableValue(it.startTime) }
    }
}
