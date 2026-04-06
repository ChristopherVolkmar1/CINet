package com.example.cinet.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.ui.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
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
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.createOrLoadUserProfile()
                .onSuccess { _authState.value = AuthState.Authenticated(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Unknown error") }
        }
    }

    fun fetchAndStoreToken() {
        val userId = auth.currentUser?.uid
        Log.d("FCM_DEBUG", "fetchAndStoreToken called, userId=$userId")
        if (userId == null) return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("FCM_DEBUG", "Got token: ${task.result}")
                repository.updateFcmToken(userId, task.result)
            } else {
                Log.e("FCM_DEBUG", "Token fetch failed: ${task.exception?.message}")
            }
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
                        .onSuccess {
                            Log.d("FCM_DEBUG", "Profile loaded, calling fetchAndStoreToken")
                            _authState.value = AuthState.Authenticated(it)
                            fetchAndStoreToken()
                        }
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