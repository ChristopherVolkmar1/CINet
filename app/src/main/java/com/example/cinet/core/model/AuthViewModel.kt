package com.example.cinet.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cinet.com.example.cinet.feature.profile.UserProfile
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.com.example.cinet.feature.auth.AuthState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repository: FirestoreRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AuthViewModel"
    }

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        observeAuthState()
    }

    fun signOut() {
        auth.signOut()
    }

    fun retryProfileLoad() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.createOrLoadUserProfile()
                .onSuccess { resolveState(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Unknown error") }
        }
    }

    fun saveProfile(nickname: String, major: String, pronouns: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.saveProfileDetails(nickname, major, pronouns)
                .onSuccess { _authState.value = AuthState.Authenticated(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Unknown error") }
        }
    }

    private fun resolveState(profile: UserProfile) {
        _authState.value = if (profile.nickname.isBlank()) {
            AuthState.ProfileSetup(profile)
        } else {
            AuthState.Authenticated(profile)
        }
    }

    private fun observeAuthState() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                _authState.value = AuthState.Unauthenticated
            } else {
                viewModelScope.launch {
                    _authState.value = AuthState.Loading
                    repository.createOrLoadUserProfile()
                        .onSuccess { resolveState(it) }
                        .onFailure { _authState.value = AuthState.Error(it.message ?: "Unknown error") }
                }
            }
        }
        auth.addAuthStateListener(authStateListener!!)
    }

    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }
}

class AuthViewModelFactory(
    private val repository: FirestoreRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(repository) as T
}