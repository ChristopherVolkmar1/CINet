package com.example.cinet.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class FriendRequest(
    val id: String = "",
    val senderId: String = "",
    val senderNickname: String = "",
    val receiverId: String = "",
    // status: "pending", "accepted", "declined"
    val status: String = "pending",
    @ServerTimestamp val createdAt: Date? = null,
)