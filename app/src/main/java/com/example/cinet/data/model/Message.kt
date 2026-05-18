package com.example.cinet.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderNickname: String = "",
    val senderPhotoUrl: String = "", // populated from UserProfile on send
    val content: String = "",
    // type: "text", "study_invite", "event_invite"
    val type: String = "text",
    // Stores structured invite data. Values can be String (name, date, time, etc.)
    // or List<String> (acceptedBy, declinedBy) — hence Any.
    val metadata: Map<String, Any> = emptyMap(),
    @ServerTimestamp val createdAt: Date? = null,
    // uid → server timestamp of when each participant read this message.
    // Stored as Map<String, Any> because Firestore returns Timestamp objects,
    // not Longs — same pattern as metadata. Use .keys to check reader presence.
    val readBy: Map<String, Any> = emptyMap(),
)