package com.example.cinet.feature.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.FirestoreRepository
import com.example.cinet.feature.auth.AuthState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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

    // Signs the current user out of Firebase
    fun signOut() {
        auth.signOut()
    }

    // Re-attempts loading the user's profile after a previous failure
    fun retryProfileLoad() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.createOrLoadUserProfile()
                .onSuccess { resolveState(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Unknown error") }
        }
    }

    // Saves the user's profile details to Firestore and updates state
    fun saveProfile(nickname: String, major: String, pronouns: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.saveProfileDetails(nickname, major, pronouns)
                .onSuccess { _authState.value = AuthState.Authenticated(it) }
                .onFailure { _authState.value = AuthState.Error(it.message ?: "Unknown error") }
        }
    }

    /**
     * Updates user settings in Firestore and local state.
     * Local state is updated immediately (optimistic update) for responsiveness.
     */
    fun updateSettings(isDarkMode: Boolean, notificationsEnabled: Boolean) {
        val currentState = _authState.value
        
        // Optimistic update of local state
        if (currentState is AuthState.Authenticated) {
            _authState.value = AuthState.Authenticated(
                currentState.userProfile.copy(
                    isDarkMode = isDarkMode,
                    notificationsEnabled = notificationsEnabled
                )
            )
        } else if (currentState is AuthState.ProfileSetup) {
            _authState.value = AuthState.ProfileSetup(
                currentState.userProfile.copy(
                    isDarkMode = isDarkMode,
                    notificationsEnabled = notificationsEnabled
                )
            )
        }

        viewModelScope.launch {
            repository.updateUserSettings(isDarkMode, notificationsEnabled)
                .onFailure { e ->
                    android.util.Log.e(TAG, "Failed to update settings in Firestore: ${e.message}")
                    // Rollback could be implemented here if necessary
                }
        }
    }

    // Routes to ProfileSetup if nickname is blank, otherwise Authenticated
    private fun resolveState(profile: UserProfile) {
        _authState.value = if (profile.nickname.isBlank()) {
            AuthState.ProfileSetup(profile)
        } else {
            AuthState.Authenticated(profile)
        }
    }

    /**
     * Proactively fetches the current FCM token and saves it to the user's
     * Firestore document. Called on every sign-in so the token is always
     * up to date even if onNewToken hasn't fired since the last install.
     */
    private fun saveFcmToken(uid: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .update("fcmToken", token)
                .addOnFailureListener { e ->
                    android.util.Log.e(TAG, "Failed to save FCM token: ${e.message}")
                }
        }
    }

    // Listens for Firebase auth changes and loads the profile on sign-in
    private fun observeAuthState() {
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user == null) {
                _authState.value = AuthState.Unauthenticated
            } else {
                saveFcmToken(user.uid)
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

    // Removes the auth listener when the ViewModel is destroyed to prevent leaks
    override fun onCleared() {
        super.onCleared()
        authStateListener?.let { auth.removeAuthStateListener(it) }
    }
}

// Factory for creating AuthViewModel with its repository dependency
class AuthViewModelFactory(
    private val repository: FirestoreRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        AuthViewModel(repository) as T
}
