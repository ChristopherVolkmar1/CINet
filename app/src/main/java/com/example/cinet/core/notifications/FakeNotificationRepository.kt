package com.example.cinet
// !!! DO NOT CHANGE PACKAGE PATH !!!

class FakeNotificationRepository {
    // Returns a list of sample notifications to simulate backend / api data, use to simulate firebase
    fun getSampleNotifications(): List<AppNotification> {
        return listOf(
            // Simulated Reminder Notification
            AppNotification(
                "Math Class",
                "Starts in 15 min",
                NotificationType.REMINDER,
                System.currentTimeMillis()
            ),
            // Simulated Campus Event Notification
            AppNotification(
                "Campus Event",
                "Free pizza at 5pm",
                NotificationType.EVENT,
                System.currentTimeMillis()
            )
        )
    }
}