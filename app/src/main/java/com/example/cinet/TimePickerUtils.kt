package com.example.cinet

import android.app.TimePickerDialog
import android.content.Context
import android.widget.TimePicker
import java.util.Calendar

fun formatPickedTime(hour24: Int, minute: Int): String {
    val amPm = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format("%02d:%02d %s", hour12, minute, amPm)
}

fun openTimePicker(
    context: Context,
    onTimeSelected: (String) -> Unit
) {
    val calendar = Calendar.getInstance()
    TimePickerDialog(
        context,
        { _: TimePicker, hour: Int, minute: Int ->
            onTimeSelected(formatPickedTime(hour, minute))
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false
    ).show()
}