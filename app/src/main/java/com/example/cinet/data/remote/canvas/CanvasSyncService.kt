package com.example.cinet.data.remote.canvas

import android.util.Log
import com.example.cinet.feature.calendar.calendarFiles.CalendarFirestoreRepository
import com.example.cinet.feature.calendar.classEvent.ClassItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Imports a [CanvasSyncSnapshot] into Firestore, merging with whatever is
 * already there. Sync is keyed by `canvasId` so re-running the same sync is
 * idempotent (updates existing rows instead of duplicating).
 *
 * Source of truth: the user's Canvas favorites list (the star toggle on
 * Canvas's "All Courses" page). Only starred courses are imported and their
 * assignments fetched. A previously-synced course that is no longer starred
 * has its Firestore doc flipped to `isFavorite = false` — the doc isn't
 * deleted (so manual meeting-day edits aren't lost), but the calendar UI
 * filters it out.
 *
 * Merge semantics:
 *   - Classes: when the same canvasId is already in Firestore, we update only
 *     the `name` and `isFavorite` fields. Anything the user filled in
 *     manually after import (meeting days, start/end time, location) is
 *     preserved.
 *   - Assignments: all fields refresh, since Canvas is the system of record.
 *     If users manually edited a Canvas-synced assignment, the next sync will
 *     overwrite their edit.
 *   - Events: all fields refresh (events are entirely Canvas-owned).
 *   - Todos & announcements: stored in dedicated collections
 *     (`canvasTodos`, `canvasAnnouncements`) keyed by Canvas id.
 *
 * Deletions in Canvas do NOT propagate — we never delete Firestore rows
 * automatically. Un-starring is a hide-not-delete operation.
 */
class CanvasSyncService(
    private val canvasRepo: CanvasRepository,
    private val calendarRepo: CalendarFirestoreRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /** Pulls Canvas data and writes it into Firestore. Returns a per-bucket count. */
    suspend fun syncAll(): CanvasSyncResult = withContext(Dispatchers.IO) {
        val snapshot = canvasRepo.fetchAll()
        val skipped = mutableListOf<String>()

        // Existing classes — used both to detect previously-synced courses no
        // longer favorited, and to look up Firestore doc ids by canvasId so
        // assignments can be attached to the right class.
        val existingClasses = calendarRepo.loadClasses()
        val classByCanvasId: MutableMap<Long, ClassItem> = existingClasses
            .mapNotNull { ci -> ci.canvasId?.toLongOrNull()?.let { it to ci } }
            .toMap()
            .toMutableMap()

        val favoriteIds: Set<Long> = snapshot.courses.map { it.id }.toSet()

        // -- Courses: upsert favorites, mark unfavorited Canvas docs as hidden -
        var coursesImported = 0
        for (course in snapshot.courses) {
            val existing = classByCanvasId[course.id]
            val displayName = course.courseCode.ifBlank { course.name }
            try {
                if (existing == null) {
                    val created = calendarRepo.addCanvasClass(
                        name = displayName,
                        canvasId = course.id.toString(),
                        isFavorite = true
                    )
                    classByCanvasId[course.id] = created
                } else {
                    // Refresh name and ensure isFavorite is true — covers the
                    // "user un-favorited then re-favorited" round trip.
                    calendarRepo.updateCanvasClassNameAndFavorite(
                        classId = existing.id,
                        name = displayName,
                        isFavorite = true
                    )
                }
                coursesImported++
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to upsert course ${course.id}", ex)
                skipped += "Course ${course.courseCode}"
            }
        }

        // Flip previously-favorited Canvas classes that are no longer in the
        // favorites list to isFavorite=false. The doc stays in Firestore so
        // any manual meeting-day edits are preserved against a future re-star.
        var coursesHidden = 0
        for ((canvasCourseId, classItem) in classByCanvasId) {
            if (canvasCourseId !in favoriteIds && classItem.isFavorite) {
                try {
                    calendarRepo.updateCanvasClassFavorite(classItem.id, isFavorite = false)
                    coursesHidden++
                } catch (ex: Exception) {
                    Log.w(TAG, "Failed to mark ${classItem.id} as unfavorited", ex)
                }
            }
        }
        if (coursesHidden > 0) {
            skipped += "$coursesHidden previously-imported course(s) un-starred in Canvas (now hidden)"
        }

        // -- Assignments: only for currently-favorited courses -----------------
        val existingAssignments = calendarRepo.loadAssignments()
        val assignmentByCanvasId = existingAssignments
            .mapNotNull { it.canvasId?.let { cid -> cid to it } }
            .toMap()

        var assignmentsImported = 0
        for (assignment in snapshot.assignments) {
            // CanvasRepository already only fetched assignments for favorited
            // courses, but double-check defensively in case the favorites
            // list and assignment list disagree mid-sync.
            if (assignment.courseId !in favoriteIds) continue

            val dueIso = assignment.dueAtIso
            if (dueIso == null) {
                skipped += "Assignment '${assignment.name}' (no due date)"
                continue
            }
            val parentClass = classByCanvasId[assignment.courseId]
            if (parentClass == null) {
                skipped += "Assignment '${assignment.name}' (parent course not imported)"
                continue
            }

            val pair = formatIsoForCalendar(dueIso)
            if (pair == null) {
                skipped += "Assignment '${assignment.name}' (bad due date format)"
                continue
            }
            val (dateStr, timeStr) = pair
            val canvasAssignmentKey = "assignment_${assignment.id}"

            try {
                val existing = assignmentByCanvasId[canvasAssignmentKey]
                if (existing == null) {
                    calendarRepo.addCanvasAssignment(
                        date = dateStr,
                        classId = parentClass.id,
                        className = parentClass.name,
                        assignmentName = assignment.name,
                        dueTime = timeStr,
                        canvasId = canvasAssignmentKey
                    )
                } else {
                    calendarRepo.updateCanvasAssignment(
                        assignmentId = existing.id,
                        date = dateStr,
                        classId = parentClass.id,
                        className = parentClass.name,
                        assignmentName = assignment.name,
                        dueTime = timeStr,
                        canvasId = canvasAssignmentKey
                    )
                }
                assignmentsImported++
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to upsert assignment ${assignment.id}", ex)
                skipped += "Assignment '${assignment.name}'"
            }
        }

        // -- Calendar events ---------------------------------------------------
        var eventsImported = 0
        for (event in snapshot.calendarEvents) {
            val pair: Pair<String, String>? = when {
                event.allDay && !event.allDayDate.isNullOrBlank() ->
                    event.allDayDate to "All Day"
                event.startAtIso != null ->
                    formatIsoForCalendar(event.startAtIso, event.allDay)
                else -> null
            }
            if (pair == null) {
                skipped += "Event '${event.title}' (no start time)"
                continue
            }
            val (dateStr, timeStr) = pair
            val canvasEventKey = "event_${event.id}"

            try {
                calendarRepo.upsertCanvasEvent(
                    canvasId = canvasEventKey,
                    date = dateStr,
                    name = event.title,
                    time = timeStr,
                    location = event.locationName
                )
                eventsImported++
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to upsert event ${event.id}", ex)
                skipped += "Event '${event.title}'"
            }
        }

        // -- Todos & announcements --------------------------------------------
        val todosImported = writeTodos(snapshot.todos)
        val announcementsImported = writeAnnouncements(snapshot.announcements)

        CanvasSyncResult(
            coursesImported = coursesImported,
            assignmentsImported = assignmentsImported,
            eventsImported = eventsImported,
            todosImported = todosImported,
            announcementsImported = announcementsImported,
            skipped = skipped
        )
    }

    // ---- todos / announcements: dedicated collections --------------------

    private suspend fun writeTodos(todos: List<CanvasTodoItem>): Int {
        if (todos.isEmpty()) return 0
        val uid = currentUid() ?: return 0
        var written = 0
        val collection = db.collection("users").document(uid).collection("canvasTodos")
        for (todo in todos) {
            val docId = todo.assignmentId?.let { "assignment_$it" }
                ?: "todo_${todo.htmlUrl.hashCode()}"
            try {
                collection.document(docId).set(
                    mapOf(
                        "type" to todo.type,
                        "courseId" to todo.courseId,
                        "assignmentId" to todo.assignmentId,
                        "title" to todo.title,
                        "dueAtIso" to todo.dueAtIso,
                        "htmlUrl" to todo.htmlUrl
                    )
                ).await()
                written++
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to write todo $docId", ex)
            }
        }
        return written
    }

    private suspend fun writeAnnouncements(announcements: List<CanvasAnnouncement>): Int {
        if (announcements.isEmpty()) return 0
        val uid = currentUid() ?: return 0
        var written = 0
        val collection = db.collection("users").document(uid).collection("canvasAnnouncements")
        for (a in announcements) {
            try {
                collection.document(a.id.toString()).set(
                    mapOf(
                        "courseId" to a.courseId,
                        "title" to a.title,
                        "message" to a.message,
                        "postedAtIso" to a.postedAtIso,
                        "htmlUrl" to a.htmlUrl
                    )
                ).await()
                written++
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to write announcement ${a.id}", ex)
            }
        }
        return written
    }

    private fun currentUid(): String? = auth.currentUser?.uid

    /**
     * Converts a Canvas ISO-8601 UTC timestamp into the (date, time) pair the
     * rest of the app uses:
     *   - date string: yyyy-MM-dd in the device's local zone (matches LocalDate.toString())
     *   - time string: hh:mm AM/PM (matches formatPickedTime in TimePickerUtils)
     */
    private fun formatIsoForCalendar(iso: String, allDay: Boolean = false): Pair<String, String>? {
        return try {
            val instant = Instant.parse(iso)
            val local = instant.atZone(ZoneId.systemDefault())
            val date = local.format(DATE_FMT)
            val time = if (allDay) "All Day" else local.format(TIME_FMT)
            date to time
        } catch (ex: Exception) {
            Log.w(TAG, "Unparseable ISO timestamp: $iso", ex)
            null
        }
    }

    companion object {
        private const val TAG = "CanvasSyncService"
        private val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        private val TIME_FMT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US)
    }
}
