package com.example.cinet.data.remote.canvas

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * High-level reader over the Canvas API.
 *
 * Each public method maps one logical chunk of Canvas data (courses,
 * assignments, etc.) into the lightweight types in [CanvasModels]. JSON
 * parsing is defensive — `optString`/`optLong` are used everywhere so a
 * single malformed item never crashes the whole sync.
 *
 * The course list is sourced from /users/self/favorites/courses rather
 * than /courses, so CINet only ingests the courses the student has
 * starred on Canvas's "All Courses" page. This both matches the user's
 * intent ("only show what I starred") and avoids importing every
 * historical/training/TA enrollment they're technically still in.
 */
class CanvasRepository(
    private val api: CanvasApiClient
) {

    /**
     * Fetches every data type CINet imports in parallel.
     *
     * Per-course assignments and announcements are gathered after the
     * favorited-course list resolves, since they need the course IDs.
     */
    suspend fun fetchAll(): CanvasSyncSnapshot = withContext(Dispatchers.IO) {
        // Favorites first — assignment and announcement queries depend on these IDs.
        val courses = fetchFavoriteCourses()
        val courseIds = courses.map { it.id }

        coroutineScope {
            // Run the four independent slices concurrently so a slow endpoint
            // doesn't serialize the whole sync.
            val assignmentsDeferred = async { fetchAssignmentsForCourses(courseIds) }
            val eventsDeferred = async { fetchUpcomingCalendarEvents() }
            val todosDeferred = async { fetchTodoItems() }
            val announcementsDeferred = async { fetchAnnouncements(courseIds) }

            CanvasSyncSnapshot(
                courses = courses,
                assignments = assignmentsDeferred.await(),
                calendarEvents = eventsDeferred.await(),
                todos = todosDeferred.await(),
                announcements = announcementsDeferred.await()
            )
        }
    }

    // ---- individual endpoints --------------------------------------------

    /**
     * Returns the user's starred courses (the star toggle on Canvas's
     * "All Courses" page). Pagination is handled by [CanvasApiClient].
     *
     * If the user has no favorites, returns an empty list — sync will then
     * do nothing, which is correct: nothing was starred, nothing imports.
     */
    fun fetchFavoriteCourses(): List<CanvasCourse> {
        val array = api.getJsonArrayPaginated(
            path = "users/self/favorites/courses"
        )
        return array.mapObjects { json ->
            val id = json.optLong("id", -1L)
            if (id <= 0) return@mapObjects null
            CanvasCourse(
                id = id,
                courseCode = json.optString("course_code").ifBlank { json.optString("name") },
                name = json.optString("name")
            )
        }
    }

    /**
     * Pulls assignments for every supplied course ID. Canvas doesn't expose a
     * "give me all my assignments" endpoint, so we issue one request per course
     * and concatenate. Failures on a single course are logged and skipped.
     */
    fun fetchAssignmentsForCourses(courseIds: List<Long>): List<CanvasAssignment> {
        val all = mutableListOf<CanvasAssignment>()
        for (courseId in courseIds) {
            try {
                val array = api.getJsonArrayPaginated(path = "courses/$courseId/assignments")
                all += array.mapObjects { json ->
                    val id = json.optLong("id", -1L)
                    if (id <= 0) return@mapObjects null
                    CanvasAssignment(
                        id = id,
                        courseId = courseId,
                        name = json.optString("name"),
                        dueAtIso = json.optStringOrNull("due_at"),
                        htmlUrl = json.optString("html_url")
                    )
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Failed to load assignments for course $courseId: ${ex.message}")
            }
        }
        return all
    }

    /**
     * Calendar events for the current user across all calendars.
     * We deliberately filter out `assignment`-type entries because those are
     * already pulled via [fetchAssignmentsForCourses] and have richer data
     * (course id, due time, html url) there.
     */
    fun fetchUpcomingCalendarEvents(): List<CanvasCalendarEvent> {
        return try {
            val array = api.getJsonArrayPaginated(path = "users/self/upcoming_events")
            array.mapObjects { json ->
                // The /upcoming_events endpoint returns both calendar_event and assignment items.
                // Skip assignments here so we don't double-import them as events.
                val type = json.optString("type")
                if (type == "assignment") return@mapObjects null

                val id = json.optLong("id", -1L)
                if (id <= 0) return@mapObjects null

                CanvasCalendarEvent(
                    id = id,
                    title = json.optString("title"),
                    startAtIso = json.optStringOrNull("start_at"),
                    endAtIso = json.optStringOrNull("end_at"),
                    allDayDate = json.optStringOrNull("all_day_date"),
                    locationName = json.optString("location_name"),
                    allDay = json.optBoolean("all_day", false),
                    htmlUrl = json.optString("html_url")
                )
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to load upcoming events: ${ex.message}")
            emptyList()
        }
    }

    /** Personal to-do list (ungraded submissions, etc.). */
    fun fetchTodoItems(): List<CanvasTodoItem> {
        return try {
            val array = api.getJsonArrayPaginated(path = "users/self/todo")
            array.mapObjects { json ->
                // Each todo wraps an "assignment" sub-object that carries the real fields.
                val assignment = json.optJSONObject("assignment")
                CanvasTodoItem(
                    type = json.optString("type"),
                    courseId = json.optLongOrNull("course_id"),
                    assignmentId = assignment?.optLongOrNull("id"),
                    title = assignment?.optString("name") ?: json.optString("html_url"),
                    dueAtIso = assignment?.optStringOrNull("due_at"),
                    htmlUrl = json.optString("html_url")
                )
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to load todo items: ${ex.message}")
            emptyList()
        }
    }

    /**
     * Pulls recent announcements across the supplied course IDs.
     *
     * The /announcements endpoint requires `context_codes[]=course_<id>` query
     * params — one per course we want covered. Canvas defaults to the last
     * 14 days; that's a reasonable inbox window for an "unread" view.
     */
    fun fetchAnnouncements(courseIds: List<Long>): List<CanvasAnnouncement> {
        if (courseIds.isEmpty()) return emptyList()
        return try {
            // Repeated "context_codes[]" keys — see CanvasApiClient.buildUrl for
            // the encoding rule that keeps Canvas's Rails parser happy.
            val query = courseIds.map { "context_codes[]" to "course_$it" }

            val array = api.getJsonArrayPaginated(path = "announcements", query = query)
            array.mapObjects { json ->
                val id = json.optLong("id", -1L)
                if (id <= 0) return@mapObjects null

                // Canvas returns context_code like "course_12345"; parse the numeric tail.
                val courseId = json.optString("context_code")
                    .removePrefix("course_")
                    .toLongOrNull() ?: return@mapObjects null

                CanvasAnnouncement(
                    id = id,
                    courseId = courseId,
                    title = json.optString("title"),
                    message = json.optString("message"),
                    postedAtIso = json.optStringOrNull("posted_at"),
                    htmlUrl = json.optString("html_url")
                )
            }
        } catch (ex: Exception) {
            Log.w(TAG, "Failed to load announcements: ${ex.message}")
            emptyList()
        }
    }

    companion object {
        private const val TAG = "CanvasRepository"
    }
}

// ---- JSON helpers -------------------------------------------------------
//
// Kept here (not in CanvasModels.kt) because they're only useful for parsing
// API responses, not for representing domain data.

/**
 * Maps a JSONArray of objects through [transform], dropping nulls. Used so
 * each parser can return null for malformed items without crashing the sync.
 */
private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T?): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val obj = optJSONObject(i) ?: continue
        transform(obj)?.let(out::add)
    }
    return out
}

/** optString that returns null instead of empty string when the field is absent or JSON null. */
private fun JSONObject.optStringOrNull(name: String): String? {
    if (isNull(name)) return null
    val v = optString(name, "")
    return if (v.isBlank()) null else v
}

/** optLong that returns null instead of 0 when the field is absent or JSON null. */
private fun JSONObject.optLongOrNull(name: String): Long? {
    if (isNull(name) || !has(name)) return null
    return optLong(name, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
}
