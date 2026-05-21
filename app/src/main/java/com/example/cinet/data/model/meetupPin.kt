package com.example.cinet.data.model

import com.google.firebase.Timestamp
import com.google.android.gms.maps.model.LatLng

data class MeetupPin(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isTemporary: Boolean = true,
    val sharedToSocial: Boolean = true,
    val creatorUid: String = "test",
    val creatorName: String = "Bryan",
    val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
) {
    val latLng: LatLng
        get() = LatLng(latitude, longitude)

    val isExpired: Boolean
        get() = expiresAt != null && expiresAt.toDate().time < System.currentTimeMillis()
}