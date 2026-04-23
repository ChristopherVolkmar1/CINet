package com.example.cinet.core.time

import android.app.TimePickerDialog
import android.content.Context
import android.widget.TimePicker
import java.util.Calendar
import com.example.cinet.feature.settings.AppSettings

// Converts a picked 24-hour time into the app's 12-hour display format.
fun formatPickedTime(hour24: Int, minute: Int): String {
    val amPm = if (hour24 >= 12) "PM" else "AM"

    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }

    return String.format("%02d:%02d %s", hour12, minute, amPm)
}

// Opens a time picker dialog using the same dark-mode setting as the app theme.
fun openTimePicker(
    context: Context,
    onTimeSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()

    val themeResId = if (AppSettings.isDarkMode) {
        android.R.style.Theme_DeviceDefault_Dialog
    } else {
        android.R.style.Theme_DeviceDefault_Light_Dialog
    }

    TimePickerDialog(
        context,
        themeResId,
        { _: TimePicker, hour: Int, minute: Int ->
            onTimeSelected(formatPickedTime(hour, minute))
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}