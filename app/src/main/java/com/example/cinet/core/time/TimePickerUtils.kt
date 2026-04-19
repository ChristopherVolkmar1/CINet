package com.example.cinet.core.time

import android.app.TimePickerDialog
import android.content.Context
import android.widget.TimePicker
import java.util.Calendar

fun formatPickedTime(hour24: Int, minute: Int): String {
    val amPm = if (hour24 >= 12) "PM" else "AM"

    // Converts 24-hour time → 12-hour format (e.g., 0 → 12 AM, 13 → 1 PM).
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }

    // Returns a formatted string used directly by UI (no further formatting elsewhere).
    return String.format("%02d:%02d %s", hour12, minute, amPm)
}

fun openTimePicker(
    context: Context,
    onTimeSelected: (String) -> Unit
) {
    // Uses current system time as the default selection when dialog opens.
    val calendar = Calendar.getInstance()

    // Determine theme based on AppSettings
    // Explicitly use Light or Dark themes to avoid system dark mode overrides
    val themeResId = if (AppSettings.isDarkMode) {
        R.style.Theme_CINet_TimePicker_Dark
    } else {
        R.style.Theme_CINet_TimePicker_Light
    }

    TimePickerDialog(
        context,
        themeResId,
        { _: TimePicker, hour: Int, minute: Int ->
            // Converts picked time immediately into display format before returning,
            // so all UI components receive consistent formatted strings.
            onTimeSelected(formatPickedTime(hour, minute))
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        // false = 12-hour mode (matches formatPickedTime output).
        false
    ).show()
}