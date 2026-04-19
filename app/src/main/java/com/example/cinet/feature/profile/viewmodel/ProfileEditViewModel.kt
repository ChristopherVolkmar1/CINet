package com.example.cinet.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinet.data.model.UserProfile
import com.example.cinet.data.remote.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Represents the possible outcomes of a save attempt
sealed class ProfileEditState {
    object Idle    : ProfileEditState()
    object Loading : ProfileEditState()
    object Success : ProfileEditState()
    data class Error(val message: String) : ProfileEditState()
}

class ProfileEditViewModel(
    private val repo: FirestoreRepository = FirestoreRepository()
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileEditState>(ProfileEditState.Idle)
    val state: StateFlow<ProfileEditState> = _state.asStateFlow()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    init { loadProfile() }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = repo.loadCurrentUserProfile()
        }
    }

    fun saveProfile(nickname: String, major: String, pronouns: String) {
        _state.value = ProfileEditState.Loading
        viewModelScope.launch {
            try {
                val result = repo.saveProfileDetails(nickname, major, pronouns)
                result.onFailure {
                    _state.value = ProfileEditState.Error(it.message ?: "Save failed")
                    return@launch
                }
                _state.value = ProfileEditState.Success
            } catch (e: Exception) {
                _state.value = ProfileEditState.Error(e.message ?: "Save failed")
            }
        }
    }

    fun resetState() {
        _state.value = ProfileEditState.Idle
    }
}