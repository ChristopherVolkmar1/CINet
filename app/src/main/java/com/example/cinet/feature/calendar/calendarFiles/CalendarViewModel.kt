package com.example.cinet.feature.calendar.calendarFiles

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinet.feature.calendar.calendarFiles.CalendarFirestoreRepository
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.example.cinet.feature.calendar.event.EventItem
import com.example.cinet.feature.calendar.study.StudySession
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


class CalendarViewModel : ViewModel() {

    // mutableStateOf makes Compose automatically recompose UI when these values change.
    var currentMonth by mutableStateOf(YearMonth.now())
        private set

    var selectedDate by mutableStateOf<LocalDate?>(null)
        private set

    var classItems by mutableStateOf<List<ClassItem>>(emptyList())
        private set

    var scheduleItems by mutableStateOf<List<ScheduleItem>>(emptyList())
        private set

    var studySessions by mutableStateOf<List<StudySession>>(emptyList())
        private set

    var eventItems by mutableStateOf<List<EventItem>>(emptyList())
        private set

    private val repository = CalendarFirestoreRepository()

    init {
        // Automatically loads data when ViewModel is created.
        refreshClasses()
        refreshAssignments()
        refreshStudySessions()
        refreshEvents()
    }

    fun nextMonth() { currentMonth = currentMonth.plusMonths(1) }
    fun previousMonth() { currentMonth = currentMonth.minusMonths(1) }
    fun selectDate(day: Int) { selectedDate = currentMonth.atDay(day) }

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

                // Keeps local scheduleItems consistent after class rename
                // (Firestore stores className redundantly in assignments).
                scheduleItems = scheduleItems.map { item ->
                    if (item.classId == classId) item.copy(className = name) else item
                }

                refreshClasses()
            } catch (e: Exception) {
                Log.e("FirestoreDebug", "updateClass failed", e)
            }
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            try {
                repository.deleteClass(classId)
                // Removes assignments tied to the deleted class locally.
                scheduleItems = scheduleItems.filterNot { it.classId == classId }
                refreshClasses()
            } catch (e: Exception) {
                Log.e("FirestoreDebug", "deleteClass failed", e)
            }
        }
    }

    fun addScheduleItem(classItem: ClassItem, assignmentName: String, dueTime: String) {
        val date = selectedDate ?: return
        // Must match the same format used in Firestore + CalendarGrid comparisons.
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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

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
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteScheduleItem(itemId: String) {
        viewModelScope.launch {
            try { repository.deleteAssignment(itemId); refreshAssignments() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addStudySession(date: String, className: String, topic: String, startTime: String, location: String) {
        viewModelScope.launch {
            try { repository.addStudySession(date, className, topic, startTime, location); refreshStudySessions() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updateStudySession(sessionId: String, date: String, className: String, topic: String, startTime: String, location: String) {
        viewModelScope.launch {
            try { repository.updateStudySession(sessionId, date, className, topic, startTime, location); refreshStudySessions() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteStudySession(sessionId: String) {
        viewModelScope.launch {
            try { repository.deleteStudySession(sessionId); refreshStudySessions() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun addEvent(date: String, name: String, time: String, location: String) {
        viewModelScope.launch {
            try { repository.addEvent(date, name, time, location); refreshEvents() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun updateEvent(eventId: String, date: String, name: String, time: String, location: String) {
        viewModelScope.launch {
            try { repository.updateEvent(eventId, date, name, time, location); refreshEvents() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try { repository.deleteEvent(eventId); refreshEvents() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun getItemsForSelectedDate(): List<ScheduleItem> {
        val date = selectedDate ?: return emptyList()
        val formattedDate = formatDate(date)
        return scheduleItems
            // Relies on ScheduleItem.date using same string format.
            .filter { it.date == formattedDate }
            // Converts time string → sortable number (minutes since midnight).
            .sortedBy { parseTimeToSortableValue(it.dueTime) }
    }

    fun getClassesGroupedByDay(): Map<String, List<ClassItem>> {
        val orderedDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return orderedDays.associateWith { day ->
            classItems
                .filter { it.meetingDays.contains(day) }
                .sortedBy { parseTimeToSortableValue(it.startTime) }
        }.filterValues { it.isNotEmpty() }
    }

    fun getClassesForSelectedDate(): List<ClassItem> {
        val date = selectedDate ?: return emptyList()
        // Converts LocalDate → matching string used in ClassItem.meetingDays.
        val dayName = when (date.dayOfWeek.value) {
            1 -> "Mon"; 2 -> "Tue"; 3 -> "Wed"; 4 -> "Thu"
            5 -> "Fri"; 6 -> "Sat"; 7 -> "Sun"; else -> ""
        }
        return classItems
            .filter { it.meetingDays.contains(dayName) }
            .sortedBy { parseTimeToSortableValue(it.startTime) }
    }

    fun getStudySessionsForSelectedDate(): List<StudySession> {
        val date = selectedDate ?: return emptyList()
        val formattedDate = formatDate(date)
        return studySessions
            .filter { it.date == formattedDate }
            .sortedBy { parseTimeToSortableValue(it.startTime) }
    }

    fun getEventsForSelectedDate(): List<EventItem> {
        val date = selectedDate ?: return emptyList()
        val formattedDate = formatDate(date)
        return eventItems
            .filter { it.date == formattedDate }
            .sortedBy { parseTimeToSortableValue(it.time) }
    }

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

    fun refreshAssignments() {
        viewModelScope.launch {
            try { scheduleItems = repository.loadAssignments() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun refreshStudySessions() {
        viewModelScope.launch {
            try { studySessions = repository.loadStudySessions() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun refreshEvents() {
        viewModelScope.launch {
            try { eventItems = repository.loadEvents() }
            catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun formatDate(date: LocalDate): String {
        // Must stay consistent with Firestore storage + CalendarGrid comparison.
        return "%04d-%02d-%02d".format(date.year, date.monthValue, date.dayOfMonth)
    }

    private fun parseTimeToSortableValue(time: String): Int {
        // Expects format like "hh:mm AM/PM"
        val parts = time.split(" ", ":")
        if (parts.size < 3) return Int.MAX_VALUE
        var hour = parts[0].toIntOrNull() ?: return Int.MAX_VALUE
        val minute = parts[1].toIntOrNull() ?: return Int.MAX_VALUE
        val amPm = parts[2]
        // Converts 12-hour → 24-hour for sorting.
        if (amPm == "PM" && hour != 12) hour += 12
        if (amPm == "AM" && hour == 12) hour = 0
        return hour * 60 + minute
    }

    private fun sortClassesByTime(classes: List<ClassItem>): List<ClassItem> =
        classes.sortedBy { parseTimeToSortableValue(it.startTime) }
}