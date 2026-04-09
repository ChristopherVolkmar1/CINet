package com.example.cinet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import java.time.LocalDate
import java.time.YearMonth
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

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

    fun nextMonth() {
        currentMonth = currentMonth.plusMonths(1)
    }

    fun previousMonth() {
        currentMonth = currentMonth.minusMonths(1)
    }

    fun selectDate(day: Int) {
        selectedDate = currentMonth.atDay(day)
    }

    fun addClass(
        name: String,
        meetingDays: List<String>,
        startTime: String,
        endTime: String,
        location: String
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
                    location = location
                )

                refreshClasses()

                android.util.Log.d(
                    "FirestoreDebug",
                    "New class created: ${newClass.name}, id=${newClass.id}"
                )
            } catch (e: Exception) {
                android.util.Log.e("FirestoreDebug", "addClass failed", e)
            }
        }
    }
    fun updateClass(
        classId: String,
        name: String,
        meetingDays: List<String>,
        startTime: String,
        endTime: String,
        location: String
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
                    location = location
                )

                // Keeps local scheduleItems consistent after class rename
                // (Firestore stores className redundantly in assignments).
                scheduleItems = scheduleItems.map { item ->
                    if (item.classId == classId) {
                        item.copy(className = name)
                    } else item
                }

                refreshClasses()
            } catch (e: Exception) {
                android.util.Log.e("FirestoreDebug", "updateClass failed", e)
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
                android.util.Log.e("FirestoreDebug", "updateClass failed", e)
            }
        }
    }

    fun addScheduleItem(
        classItem: ClassItem,
        assignmentName: String,
        dueTime: String
    ) {
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateScheduleItem(
        itemId: String,
        classItem: ClassItem,
        assignmentName: String,
        dueTime: String
    ) {
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

    private fun formatDate(date: LocalDate): String {
        // Must stay consistent with Firestore storage + CalendarGrid comparison.
        return "%04d-%02d-%02d".format(
            date.year,
            date.monthValue,
            date.dayOfMonth
        )
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

    private fun sortClassesByTime(classes: List<ClassItem>): List<ClassItem> {
        return classes.sortedBy { parseTimeToSortableValue(it.startTime) }
    }

    private val repository = CalendarFirestoreRepository()

    init {
        // Automatically loads data when ViewModel is created.
        refreshClasses()
        refreshAssignments()
    }

    fun refreshClasses() {
        viewModelScope.launch {
            try {
                classItems = sortClassesByTime(repository.loadClasses())

                android.util.Log.d("FirestoreDebug", "Loaded classItems count: ${classItems.size}")

                classItems.forEach {
                    android.util.Log.d(
                        "FirestoreDebug",
                        "Class in ViewModel: ${it.name}, days=${it.meetingDays}, start=${it.startTime}, end=${it.endTime}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("FirestoreDebug", "refreshClasses failed", e)
            }
        }
    }

    fun refreshAssignments() {
        viewModelScope.launch {
            try {
                scheduleItems = repository.loadAssignments()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}