package com.example.cinet.feature.calendar.event

import android.content.Context

/** Persists which campus events the user turned reminders on for. */
object CampusEventReminderPreferences {
    private const val PREFS_NAME = "campus_event_reminders"

    /** Returns true when a reminder is currently enabled for the given event occurrence. */
    fun isReminderEnabled(context: Context, eventId: String): Boolean {
        return prefs(context).getBoolean(keyFor(eventId), false)
    }

    /** Saves the latest reminder toggle state for the given event occurrence. */
    fun setReminderEnabled(context: Context, eventId: String, isEnabled: Boolean) {
        prefs(context).edit().putBoolean(keyFor(eventId), isEnabled).apply()
    }

    /** Opens the shared preferences file used by this reminder feature. */
    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Creates one stable preference key for a specific event occurrence. */
    private fun keyFor(eventId: String): String = "reminder_$eventId"
}
