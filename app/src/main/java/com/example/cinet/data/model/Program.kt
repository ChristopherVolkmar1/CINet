package com.example.cinet.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Program (
    val name: String,
    val type: String,
    val department: String,
    val units: String
)