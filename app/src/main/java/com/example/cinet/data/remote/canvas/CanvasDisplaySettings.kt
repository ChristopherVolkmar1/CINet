package com.example.cinet.data.remote.canvas

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * Per-device toggle that controls whether Canvas-sourced classes,
 * assignments, and events are shown in the calendar UI.
 *
 * Value reads from any composable register as a Compose snapshot read
 * (because the custom getter touches a mutableStateOf), so composables
 * recompose when the toggle flips. The setter persists the new value
 * to SharedPreferences in the same call.
 *
 * Must be initialized once from MainActivity.onCreate before any
 * composable reads the value (otherwise the in-memory default `true`
 * is used until init).
 */
object CanvasDisplaySettings {

    private const val PREFS_FILE = "canvas_display_prefs"
    private const val KEY_SHOW_CANVAS = "show_canvas_in_calendar"

    private var prefs: SharedPreferences? = null

    // Backing state — reads/writes go through the public property below so
    // persistence stays in one place.
    private val _showCanvasInCalendar = mutableStateOf(true)

    /** True = Canvas items appear in the calendar; false = hidden. */
    var showCanvasInCalendar: Boolean
        get() = _showCanvasInCalendar.value
        set(value) {
            _showCanvasInCalendar.value = value
            prefs?.edit()?.putBoolean(KEY_SHOW_CANVAS, value)?.apply()
        }

    /** Called once from MainActivity.onCreate to restore the saved value. */
    fun init(context: Context) {
        val store = context.applicationContext
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs = store
        // Bypass the public setter on restore so we don't re-write the same
        // value back to prefs immediately.
        _showCanvasInCalendar.value = store.getBoolean(KEY_SHOW_CANVAS, true)
    }
}