package com.example.cinet.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Conversation(
    val id: String = "",
    val participantIds: List<String> = emptyList(),
    // maps uid → nickname for display purposes
    val participantNicknames: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    @get:PropertyName("isGroup")
    @set:PropertyName("isGroup")
    var isGroup: Boolean = false,
    val groupName: String = "",
    @ServerTimestamp val lastUpdated: Date? = null,
)