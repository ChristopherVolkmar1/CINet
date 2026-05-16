package com.example.cinet.feature.calendar.classEvent

data class ClassItem(
    val id: String = "",
    val name: String,
    val meetingDays: List<String>,
    val startTime: String,
    val endTime: String,
    val location: String,
    val remindersEnabled: Boolean = true, // per-class reminder toggle
    /**
     * Non-null when this class was imported from the Canvas LMS sync.
     * Equals the Canvas course id (as a string). Used to detect duplicates
     * on re-sync so we update the existing row instead of creating a new one.
     * Stays null for manually-created classes.
     */
    val canvasId: String? = null,
    /**
     * Whether this class should be shown in the calendar UI.
     *
     * - Manual classes default to true and stay true (the field is effectively ignored).
     * - Canvas-synced classes derive this from the user's Canvas favorites list
     *   (the star on Canvas's "All Courses" page) on each sync. A class that
     *   was previously starred but no longer is keeps its Firestore doc (so
     *   manual meeting-day edits aren't lost) but flips to false and disappears
     *   from the calendar.
     *
     * Default true so legacy docs that predate this field continue to render.
     */
    val isFavorite: Boolean = true,
)
