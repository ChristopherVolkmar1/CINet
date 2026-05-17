package com.example.cinet.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.UserProfile
import com.example.cinet.feature.social.ConversationScreen
import com.example.cinet.feature.social.ConversationsListScreen
import com.example.cinet.feature.social.NewConversationScreen
import com.example.cinet.feature.social.SocialScreen
import com.example.cinet.feature.profile.ProfileScreen

// Chooses which social subpage is currently visible.
@Composable
internal fun NavigationSocialRoute(
    activeConversation: Conversation?,
    selectedProfile: UserProfile?,
    showNewConversation: Boolean,
    showSocialScreen: Boolean,
    userProfile: UserProfile,
    openedConversationTimestamps: Map<String, Long>,
    onConversationBack: () -> Unit,
    onNavigateToLocation: (String) -> Unit,
    onNavigateToCoordinates: (Double, Double, String, String) -> Unit,
    onProfileBack: () -> Unit,
    onEditProfile: () -> Unit,
    onNewConversationBack: () -> Unit,
    onOpenConversationFromNew: (Conversation) -> Unit,
    onOpenProfile: (UserProfile) -> Unit,
    onOpenConversationWithFriend: (UserProfile) -> Unit,
    onOpenConversationFromList: (Conversation) -> Unit,
    onNewConversation: () -> Unit,
    onOpenFriends: () -> Unit,
    onSeedTimestamps: (List<String>) -> Unit,
) {
    // Intercept system back when a profile is visible — ensures we return
    // to the conversation (or wherever we came from) rather than letting
    // NavigationBackHandler's fallback fire and go home.
    if (selectedProfile != null) {
        BackHandler { onProfileBack() }
    }

    when {
        // selectedProfile checked FIRST — lets tapping a user from inside a conversation
        // navigate to their ProfileScreen without clearing the active conversation.
        // Back press clears selectedProfile and returns to the conversation.
        selectedProfile != null -> ProfileScreen(
            user = if (selectedProfile.uid == userProfile.uid) userProfile else selectedProfile,
            currentUserProfile = userProfile,
            onOpenConversation = onOpenConversationFromNew,
            onBack = onProfileBack,
            onEditProfile = onEditProfile,
        )

        activeConversation != null -> ConversationScreen(
            conversation = activeConversation,
            onBack = onConversationBack,
            onNavigateToLocation = onNavigateToLocation,
            onNavigateToCoordinates = onNavigateToCoordinates,
            onOpenProfile = onOpenProfile,
        )

        showNewConversation -> NewConversationScreen(
            currentUserProfile = userProfile,
            onBack = onNewConversationBack,
            onOpenConversation = onOpenConversationFromNew
        )

        showSocialScreen -> SocialScreen(
            onOpenProfile = onOpenProfile,
            onOpenConversation = onOpenConversationWithFriend
        )

        else -> ConversationsListScreen(
            onOpenConversation = onOpenConversationFromList,
            onNewConversation = onNewConversation,
            onOpenFriends = onOpenFriends,
            openedConversationTimestamps = openedConversationTimestamps,
            onSeedTimestamps = onSeedTimestamps,
        )
    }
}