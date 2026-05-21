package com.example.cinet.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cinet.data.remote.canvas.CanvasConversation
import com.example.cinet.feature.messages.canvas.CanvasConversationScreen
import com.example.cinet.feature.messages.canvas.CanvasInboxScreen
import com.example.cinet.feature.messages.canvas.viewmodel.CanvasMessagingViewModel

/**
 * Overlay route for the Canvas messaging surface. Decides between the
 * inbox list and the thread view based on whether a conversation has
 * been selected at the navigation level.
 *
 * The ViewModel is shared between inbox and thread sub-views — same
 * activity-scoped instance — so opening a thread doesn't lose the inbox
 * data, and a reply that updates the thread can also surface in the
 * inbox on the next refresh.
 *
 * Mirrors the [NavigationClubsRoute] / [NavigationNewsRoute] pattern:
 * one route owns the overlay regardless of internal sub-state.
 */
@Composable
internal fun NavigationCanvasMessagesRoute(
    selectedConversation: CanvasConversation?,
    onOpenConversation: (CanvasConversation) -> Unit,
) {
    val viewModel: CanvasMessagingViewModel = viewModel()

    // Drive the ViewModel from the navigation-level selection so that
    // back-button handling in MainScaffold (which only knows about nav
    // state) keeps the screen content in sync. When selection becomes
    // non-null, load that thread; when it becomes null, the inbox is
    // displayed instead.
    LaunchedEffect(selectedConversation?.id) {
        val sel = selectedConversation
        if (sel != null) {
            viewModel.openConversation(sel)
        }
    }

    if (selectedConversation == null) {
        CanvasInboxScreen(
            onOpenConversation = onOpenConversation,
            viewModel = viewModel
        )
    } else {
        CanvasConversationScreen(viewModel = viewModel)
    }
}
