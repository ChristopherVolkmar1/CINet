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
import com.example.cinet.data.remote.canvas.CanvasDisplaySettings

/**
 * Holds the state of the Canvas messaging surface.
 *
 * Race-safety: all suspending operations that mutate thread state capture
 * the target conversation id up front, then re-check the current
 * conversationId before applying their update. Drops late responses
 * when the user has already navigated to a different thread.
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
        val currentUserId: Long? = null
    )

    private val _inboxState = MutableStateFlow(InboxState())
    val inboxState: StateFlow<InboxState> = _inboxState.asStateFlow()

    private val _threadState = MutableStateFlow(ThreadState())
    val threadState: StateFlow<ThreadState> = _threadState.asStateFlow()

    fun hasToken(): Boolean = tokenStore.hasToken()

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
                    error = null
                )
            }
        }
    }

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
            // Capture target id so a late response can't overwrite a newer thread.
            val targetId = conversation.id
            val userId = repository.currentUserId()
            val detail = repository.fetchConversationDetail(targetId)
            _threadState.update {
                if (it.conversationId != targetId) return@update it
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
                        messages = detail.messages.reversed(),
                        currentUserId = userId
                    )
                }
            }
        }
    }

    fun closeConversation() {
        _threadState.update { ThreadState() }
    }

    fun onReplyDraftChange(text: String) {
        _threadState.update { it.copy(replyDraft = text, sendError = null) }
    }

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
                    if (it.conversationId != conversationId) return@update it
                    it.copy(
                        isSending = false,
                        sendError = "Couldn't send. Check your connection and try again."
                    )
                }
                return@launch
            }

            val refreshed = repository.fetchConversationDetail(conversationId)
            _threadState.update {
                if (it.conversationId != conversationId) return@update it
                it.copy(
                    isSending = false,
                    replyDraft = "",
                    messages = refreshed?.messages?.reversed() ?: it.messages
                )
            }
        }
    }
}
