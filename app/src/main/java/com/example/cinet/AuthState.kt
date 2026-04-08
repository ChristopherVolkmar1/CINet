package com.example.cinet.ui

import com.example.cinet.data.model.UserProfile

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class ProfileSetup(val userProfile: UserProfile) : AuthState()
    data class Authenticated(val userProfile: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}