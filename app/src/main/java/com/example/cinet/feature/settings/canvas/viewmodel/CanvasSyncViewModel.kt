package com.example.cinet.feature.settings.canvas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinet.data.remote.canvas.CanvasApiClient
import com.example.cinet.data.remote.canvas.CanvasAuthResult
import com.example.cinet.data.remote.canvas.CanvasRepository
import com.example.cinet.data.remote.canvas.CanvasSyncResult
import com.example.cinet.data.remote.canvas.CanvasSyncService
import com.example.cinet.data.remote.canvas.CanvasTokenStore
import com.example.cinet.feature.calendar.calendarFiles.CalendarFirestoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Orchestrates the Canvas connection UI:
 *   - Holds the token-entry field state.
 *   - Tests the token against /users/self.
 *   - Persists the token (via [CanvasTokenStore]).
 *   - Kicks off the full sync (via [CanvasSyncService]) and surfaces results.
 *
 * One ViewModel per screen instance — uses AndroidViewModel because the
 * encrypted prefs store needs a Context for its master key.
 */
class CanvasSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStore = CanvasTokenStore(application)
    private val apiClient = CanvasApiClient(tokenStore)
    private val canvasRepo = CanvasRepository(apiClient)
    private val syncService = CanvasSyncService(
        canvasRepo = canvasRepo,
        calendarRepo = CalendarFirestoreRepository()
    )

    private val _uiState = MutableStateFlow(
        CanvasUiState(hasToken = tokenStore.hasToken())
    )
    val uiState: StateFlow<CanvasUiState> = _uiState.asStateFlow()

    /** Updates the editable token field as the user types/pastes. */
    fun onTokenInputChange(newValue: String) {
        _uiState.update { it.copy(tokenInput = newValue, statusMessage = null) }
    }

    /**
     * Saves the entered token, then probes /users/self to confirm it works.
     * Clears the input field afterwards so the secret isn't sitting in
     * memory longer than needed.
     */
    fun onSaveAndTest() {
        val raw = _uiState.value.tokenInput.trim()
        if (raw.isEmpty()) {
            _uiState.update { it.copy(statusMessage = "Paste your Canvas token first.") }
            return
        }
        _uiState.update { it.copy(isBusy = true, statusMessage = "Testing connection…") }
        viewModelScope.launch {
            tokenStore.saveToken(raw)
            // Probing is a network call — run off the main thread.
            val result = withContext(Dispatchers.IO) { apiClient.probeAuth() }
            when (result) {
                is CanvasAuthResult.Success -> _uiState.update {
                    it.copy(
                        isBusy = false,
                        hasToken = true,
                        tokenInput = "",
                        statusMessage = "Connected as ${result.userName}."
                    )
                }
                is CanvasAuthResult.Failure -> {
                    // Token is bad — wipe it so subsequent runs don't hit Canvas with a dud.
                    tokenStore.clear()
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            hasToken = false,
                            statusMessage = result.reason
                        )
                    }
                }
            }
        }
    }

    /** Pulls all Canvas data and merges into Firestore. */
    fun onSyncNow() {
        if (!tokenStore.hasToken()) {
            _uiState.update { it.copy(statusMessage = "Connect Canvas before syncing.") }
            return
        }
        _uiState.update { it.copy(isBusy = true, statusMessage = "Syncing from Canvas…", lastResult = null) }
        viewModelScope.launch {
            val outcome = runCatching { syncService.syncAll() }
            outcome.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        lastResult = result,
                        statusMessage = formatSummary(result)
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        statusMessage = "Sync failed: ${ex.message ?: "unknown error"}"
                    )
                }
            }
        }
    }

    /** Removes the stored token and resets the UI to disconnected. */
    fun onDisconnect() {
        tokenStore.clear()
        _uiState.update {
            CanvasUiState(
                hasToken = false,
                statusMessage = "Canvas disconnected."
            )
        }
    }

    private fun formatSummary(r: CanvasSyncResult): String {
        val parts = mutableListOf<String>()
        if (r.coursesImported > 0) parts += "${r.coursesImported} courses"
        if (r.assignmentsImported > 0) parts += "${r.assignmentsImported} assignments"
        if (r.eventsImported > 0) parts += "${r.eventsImported} events"
        if (r.todosImported > 0) parts += "${r.todosImported} to-dos"
        if (r.announcementsImported > 0) parts += "${r.announcementsImported} announcements"
        val main = if (parts.isEmpty()) "Sync finished — nothing to import." else "Imported " + parts.joinToString(", ") + "."
        val skipNote = if (r.skipped.isNotEmpty()) " ${r.skipped.size} skipped." else ""
        return main + skipNote
    }
}

/** UI state for the Canvas connection screen. */
data class CanvasUiState(
    val hasToken: Boolean = false,
    val tokenInput: String = "",
    val isBusy: Boolean = false,
    val statusMessage: String? = null,
    val lastResult: CanvasSyncResult? = null
)
