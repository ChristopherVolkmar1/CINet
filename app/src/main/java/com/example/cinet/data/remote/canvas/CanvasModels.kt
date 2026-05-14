package com.example.cinet.data.remote.canvas

/**
 * Lightweight domain types for the Instructure Canvas LMS REST API.
 *
 * Only the fields CINet actually uses are modeled here — Canvas responses
 * contain many more fields that we deliberately ignore to keep the surface
 * area small and the JSON parsing forgiving.
 *
 * API reference: https://canvas.instructure.com/doc/api/
 */

/** A single Canvas course the student is enrolled in. */
data class CanvasCourse(
    val id: Long,
    /** Short code like "COMP-350" — preferred for display because [name] is often verbose. */
    val courseCode: String,
    /** Full course title like "Fall 2024 - COMP 350 - Software Engineering". */
    val name: String
)

/** A single Canvas assignment within a course. */
data class CanvasAssignment(
    val id: Long,
    val courseId: Long,
    val name: String,
    /** ISO-8601 UTC string from Canvas (e.g. "2024-12-09T07:59:00Z"). Nullable when no due date is set. */
    val dueAtIso: String?,
    val htmlUrl: String
)

/** A Canvas calendar event (lecture slot, exam slot, etc. — NOT an assignment). */
data class CanvasCalendarEvent(
    val id: Long,
    val title: String,
    /** ISO-8601 UTC start time, or null for all-day events with only a date. */
    val startAtIso: String?,
    val endAtIso: String?,
    val locationName: String,
    val allDay: Boolean,
    val htmlUrl: String
)

/** A Canvas to-do item (upcoming assignment/quiz the user hasn't submitted yet). */
data class CanvasTodoItem(
    val type: String,             // "submitting", "grading", etc.
    val courseId: Long?,
    val assignmentId: Long?,
    val title: String,
    val dueAtIso: String?,
    val htmlUrl: String
)

/** A Canvas announcement scoped to one course. */
data class CanvasAnnouncement(
    val id: Long,
    val courseId: Long,
    val title: String,
    val message: String,
    val postedAtIso: String?,
    val htmlUrl: String
)

/** Wraps the four data slices CINet pulls in one sync pass. */
data class CanvasSyncSnapshot(
    val courses: List<CanvasCourse>,
    val assignments: List<CanvasAssignment>,
    val calendarEvents: List<CanvasCalendarEvent>,
    val todos: List<CanvasTodoItem>,
    val announcements: List<CanvasAnnouncement>
)

/** Outcome of a Canvas sync into Firestore — used to show a summary on the UI. */
data class CanvasSyncResult(
    val coursesImported: Int,
    val assignmentsImported: Int,
    val eventsImported: Int,
    val todosImported: Int,
    val announcementsImported: Int,
    val skipped: List<String> = emptyList()
)

/** Result of probing the token by hitting /users/self. */
sealed class CanvasAuthResult {
    data class Success(val userName: String) : CanvasAuthResult()
    data class Failure(val reason: String) : CanvasAuthResult()
}
