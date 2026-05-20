package com.example.cinet.feature.messages.canvas.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinet.data.remote.canvas.CanvasApiClient
import com.example.cinet.data.remote.canvas.CanvasConversation
import com.example.cinet.data.remote.canvas.CanvasMessage
import com.example.cinet.data.remote.canvas.CanvasMessagingRepository
import com.example.cinet.data.remote.canvas.CanvasTokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the state of the Canvas messaging surface.
 *
 * Two top-level UI states coexist in the same ViewModel:
 *   - Inbox list (browsing conversations)
 *   - Thread view (reading + replying to one conversation)
 *
 * They share this ViewModel because the same data flows between them
 * (clicking a row in the inbox loads the thread; sending a reply may
 * later mark the inbox row as read on refresh).
 *
 * Activity-scoped via Compose's [androidx.lifecycle.viewmodel.compose.viewModel]
 * so it survives the inbox/thread navigation transition.
 */
class CanvasMessagingViewModel(application: Application) : AndroidViewModel(application) {

    private val tokenStore = CanvasTokenStore(application)
    private val apiClient = CanvasApiClient(tokenStore)
    private val repository = CanvasMessagingRepository(apiClient)

    data class InboxState(
        val isLoading: Boolean = false,
        val conversations: List<CanvasConversation> = emptyList(),
        val error: String? = null
    )

    data class ThreadState(
        val conversationId: Long? = null,
        val subject: String = "",
        val participantNames: String = "",
        val messages: List<CanvasMessage> = emptyList(),
        val isLoading: Boolean = false,
        val replyDraft: String = "",
        val isSending: Boolean = false,
        val sendError: String? = null,
        /** Canvas id of the current user, for "is this my message" rendering. */
        val currentUserId: Long? = null
    )

    private val _inboxState = MutableStateFlow(InboxState())
    val inboxState: StateFlow<InboxState> = _inboxState.asStateFlow()

    private val _threadState = MutableStateFlow(ThreadState())
    val threadState: StateFlow<ThreadState> = _threadState.asStateFlow()

    /** Returns whether a Canvas token is currently saved on this device. */
    fun hasToken(): Boolean = tokenStore.hasToken()

    /**
     * Loads the inbox. Called when the user opens the messaging surface and
     * whenever they pull-to-refresh. Errors are captured in state so the UI
     * can show a banner without crashing.
     */
    fun loadInbox() {
        if (!tokenStore.hasToken()) {
            _inboxState.update {
                it.copy(
                    isLoading = false,
                    conversations = emptyList(),
                    error = "Connect Canvas in Settings to view messages."
                )
            }
            return
        }
        _inboxState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val list = repository.fetchInbox()
            _inboxState.update {
                it.copy(
                    isLoading = false,
                    conversations = list,
                    // Empty list isn't an error — it's a clean inbox.
                    error = null
                )
            }
        }
    }

    /**
     * Opens a thread, loading its messages. The thread state is reset
     * before the network call so the previous conversation's content
     * doesn't briefly flash.
     */
    fun openConversation(conversation: CanvasConversation) {
        _threadState.update {
            ThreadState(
                conversationId = conversation.id,
                subject = conversation.subject,
                participantNames = conversation.participantNames,
                isLoading = true
            )
        }
        viewModelScope.launch {
            val userId = repository.currentUserId()
            val detail = repository.fetchConversationDetail(conversation.id)
            _threadState.update {
                if (detail == null) {
                    it.copy(
                        isLoading = false,
                        sendError = "Couldn't load this conversation."
                    )
                } else {
                    it.copy(
                        isLoading = false,
                        subject = detail.subject,
                        participantNames = detail.participantNames,
                        // Canvas returns newest-first; reverse so chat UI reads bottom-newest.
                        messages = detail.messages.reversed(),
                        currentUserId = userId
                    )
                }
            }
        }
    }

    /** Closes the thread view and returns to the inbox list. */
    fun closeConversation() {
        _threadState.update { ThreadState() }
    }

    /** Updates the reply draft as the user types. */
    fun onReplyDraftChange(text: String) {
        _threadState.update { it.copy(replyDraft = text, sendError = null) }
    }

    /**
     * Sends the current draft. After success, refetches the thread so the
     * sent message appears with Canvas's authoritative id and timestamp.
     * No optimistic local insertion — avoids reconciliation headaches.
     */
    fun sendReply() {
        val current = _threadState.value
        val conversationId = current.conversationId ?: return
        val body = current.replyDraft.trim()
        if (body.isEmpty() || current.isSending) return

        _threadState.update { it.copy(isSending = true, sendError = null) }
        viewModelScope.launch {
            val ok = repository.sendReply(conversationId, body)
            if (!ok) {
                _threadState.update {
                    it.copy(
                        isSending = false,
                        sendError = "Couldn't send. Check your connection and try again."
                    )
                }
                return@launch
            }

            // Clear draft, then refetch so the sent message shows up.
            val refreshed = repository.fetchConversationDetail(conversationId)
            _threadState.update {
                it.copy(
                    isSending = false,
                    replyDraft = "",
                    messages = refreshed?.messages?.reversed() ?: it.messages
                )
            }
        }
    }
}
