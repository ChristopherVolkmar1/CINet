package com.example.cinet.navigation

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
    when {
        // Priority 1: Active Chat (Chat is the "deepest" view in the social stack)
        activeConversation != null -> ConversationScreen(
            conversation = activeConversation,
            onBack = onConversationBack,
            onNavigateToLocation = onNavigateToLocation,
            onNavigateToCoordinates = onNavigateToCoordinates
        )

        // Priority 2: Profile view (can be opened from Friends list or indirectly from Home/Settings)
        selectedProfile != null -> ProfileScreen(
            user = if (selectedProfile.uid == userProfile.uid) userProfile else selectedProfile,
            currentUserProfile = userProfile,
            onOpenConversation = onOpenConversationFromNew,
            onBack = onProfileBack,
            onEditProfile = onEditProfile,
        )

        // Priority 3: Setup for a new conversation
        showNewConversation -> NewConversationScreen(
            currentUserProfile = userProfile,
            onBack = onNewConversationBack,
            onOpenConversation = onOpenConversationFromNew
        )

        // Priority 4: Social (Friends List)
        showSocialScreen -> SocialScreen(
            onOpenProfile = onOpenProfile,
            onOpenConversation = onOpenConversationWithFriend
        )

        // Root: Messages List
        else -> ConversationsListScreen(
            onOpenConversation = onOpenConversationFromList,
            onNewConversation = onNewConversation,
            onOpenFriends = onOpenFriends,
            openedConversationTimestamps = openedConversationTimestamps,
            onSeedTimestamps = onSeedTimestamps,
        )
    }
}
