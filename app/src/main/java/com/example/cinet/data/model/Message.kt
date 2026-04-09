package com.example.cinet.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderNickname: String = "",
    val content: String = "",
    // type: "text", "study_invite", "event_invite"
    val type: String = "text",
    @ServerTimestamp val createdAt: Date? = null,
)