package com.example.cinet.com.example.cinet.feature.auth

import com.example.cinet.com.example.cinet.feature.profile.UserProfile

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class ProfileSetup(val userProfile: UserProfile) : AuthState()
    data class Authenticated(val userProfile: UserProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}