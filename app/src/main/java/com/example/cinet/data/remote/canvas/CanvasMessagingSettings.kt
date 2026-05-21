package com.example.cinet.data.remote.canvas

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf

/**
 * Per-device toggle that controls whether the Canvas messaging feature is
 * surfaced anywhere in the app. When off, the home-page inbox icon is hidden
 * and no Canvas messaging UI is reachable. Token storage and the calendar
 * sync are unaffected — this only gates the messaging surface.
 *
 * Architecturally identical to [CanvasDisplaySettings]: Compose-friendly via
 * mutableStateOf, persisted to SharedPreferences in the setter, initialized
 * once from MainActivity.onCreate.
 */
object CanvasMessagingSettings {

    private const val PREFS_FILE = "canvas_messaging_prefs"
    private const val KEY_SHOW_MESSAGING = "show_canvas_messaging"

    private var prefs: SharedPreferences? = null

    private val _showCanvasMessaging = mutableStateOf(true)

    /** True = Canvas messaging is visible; false = the entire feature is hidden. */
    var showCanvasMessaging: Boolean
        get() = _showCanvasMessaging.value
        set(value) {
            _showCanvasMessaging.value = value
            prefs?.edit()?.putBoolean(KEY_SHOW_MESSAGING, value)?.apply()
        }

    /** Called once from MainActivity.onCreate to restore the saved value. */
    fun init(context: Context) {
        val store = context.applicationContext
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs = store
        _showCanvasMessaging.value = store.getBoolean(KEY_SHOW_MESSAGING, true)
    }
}
