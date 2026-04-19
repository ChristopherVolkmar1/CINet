package com.example.cinet.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.ui.AuthState
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

    // signs the current user out of firebase
    fun signOut() {
        auth.signOut()
    }

    // re-attempts loading the user's profile after a previous failure
    fun retryProfileLoad() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.createOrLoadUserProfile()
                .onSuccess { resolveState(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Unknown error") }
        }
    }

    // Saves the user's profile details to Firestore and updates state.
    fun saveProfile(nickname: String, major: String, pronouns: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.saveProfileDetails(nickname, major, pronouns)
                .onSuccess { _authState.value = AuthState.Authenticated(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Unknown error") }
        }
    }

    // Routes to ProfileSetup if nickname is blank, otherwise Authenticated.
    private fun resolveState(profile: UserProfile) {
        _authState.value = if (profile.nickname.isBlank()) {
            AuthState.ProfileSetup(profile)
        } else {
            AuthState.Authenticated(profile)
        }
    }

    // Listens for Firebase auth changes and loads the profile on sign-in.
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

    // Removes the auth listener when the ViewModel is destroyed to prevent leaks.
    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }
}

// Factory for creating AuthViewModel with its repository dependency.
class AuthViewModelFactory(
    private val repository: FirestoreRepository
) : ViewModelProvider.Factory {
    // Instantiates the AuthViewModel requested by ViewModelProvider.
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(repository) as T
}