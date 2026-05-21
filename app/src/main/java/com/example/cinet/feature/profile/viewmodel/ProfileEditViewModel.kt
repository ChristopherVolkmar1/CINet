package com.example.cinet.feature.profile.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Represents the possible outcomes of a save attempt
sealed class ProfileEditState {
    object Idle    : ProfileEditState()
    object Loading : ProfileEditState()
    object Success : ProfileEditState()
    data class Error(val message: String) : ProfileEditState()
}

// Separate lightweight state for photo/banner uploads so upload progress
// doesn't interfere with the save flow (and doesn't trigger navigation).
sealed class UploadState {
    object Idle    : UploadState()
    object Loading : UploadState()
    data class Error(val message: String) : UploadState()
}

class ProfileEditViewModel(application: Application) : AndroidViewModel(application) {

    // Instantiated internally so the default AndroidViewModelFactory can
    // construct this class by passing only Application — no custom factory needed.
    private val repo = FirestoreRepository()

    private val _state = MutableStateFlow<ProfileEditState>(ProfileEditState.Idle)
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val storage = FirebaseStorage.getInstance()
    private val auth    = FirebaseAuth.getInstance()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = repo.loadCurrentUserProfile()
        }
    }

    fun saveProfile(
        nickname: String,
        major: String,
        minor: String,
        pronouns: String,
        year: String = "",
        bio: String = "",
        interests: List<String> = emptyList(),
    ) {
        _state.value = ProfileEditState.Loading
        viewModelScope.launch {
            try {
                val result = repo.saveProfileDetails(nickname, major, minor, pronouns, year, bio, interests)
                result.onFailure {
                    _state.value = ProfileEditState.Error(it.message ?: "Save failed")
                    return@launch
                }
                // Reload so the next edit session shows fresh data rather than
                // the pre-save snapshot the ViewModel was holding in memory.
                _profile.value = repo.loadCurrentUserProfile()
                _state.value = ProfileEditState.Success
            } catch (e: Exception) {
                _state.value = ProfileEditState.Error(e.message ?: "Save failed")
            }
        }
    }

    /** Upload a new profile photo and persist the download URL to Firestore. */
    fun uploadProfilePhoto(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                // Read bytes on IO — Google Drive openInputStream downloads over the
                // network and throws NetworkOnMainThreadException on Dispatchers.Main.
                val bytes = withContext(Dispatchers.IO) { readBytes(uri) }
                // putBytes uploads the raw array directly — no file:// URI, no
                // internal FileInputStream, auth token always attached correctly.
                val ref = storage.reference.child("profile_photos/$uid.jpg")
                ref.putBytes(bytes).await()
                val url = ref.downloadUrl.await().toString()
                repo.updatePhotoUrl(uid, url)
                _profile.value = repo.loadCurrentUserProfile()
                _uploadState.value = UploadState.Idle
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    /** Upload a new profile banner and persist the download URL to Firestore. */
    fun uploadBannerPhoto(uri: Uri) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val bytes = withContext(Dispatchers.IO) { readBytes(uri) }
                val ref = storage.reference.child("profile_banners/$uid.jpg")
                ref.putBytes(bytes).await()
                val url = ref.downloadUrl.await().toString()
                repo.updateBannerUrl(uid, url)
                _profile.value = repo.loadCurrentUserProfile()
                _uploadState.value = UploadState.Idle
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    "${e.javaClass.simpleName}: ${e.message}"
                )
            }
        }
    }

    /**
     * Reads all bytes from [uri] via the ContentResolver.
     * Handles virtual/cloud URIs (Google Drive, cloud pickers) that need a
     * network download before the data is accessible locally.
     * Must be called from Dispatchers.IO.
     */
    private fun readBytes(uri: Uri): ByteArray {
        return getApplication<Application>()
            .contentResolver
            .openInputStream(uri)
            ?.use { it.readBytes() }
            ?: error("Could not open URI: $uri")
    }

    fun resetState() {
        _state.value = ProfileEditState.Idle
    }

    fun resetUploadError() {
        _uploadState.value = UploadState.Idle
    }
}