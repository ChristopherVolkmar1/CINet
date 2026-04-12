package com.example.cinet
// Data model for representing notifications in the app
data class AppNotification
    (
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long
)
// enum class defining notification categories in the app
enum class NotificationType
{
    EVENT,
    REMINDER,
    MESSAGE
}