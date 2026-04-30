package com.example.cinet.data.remote

import android.util.Log
import com.example.cinet.feature.calendar.event.EventItem
import com.example.cinet.feature.calendar.schedule.ScheduleItem
import com.example.cinet.feature.calendar.study.StudySession
import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.FriendRequest
import com.example.cinet.data.model.Message
import com.example.cinet.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SocialRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    /** Returns the current signed-in user's uid. */
    private val currentUid: String
        get() = auth.currentUser?.uid ?: error("No signed-in user.")

    /** Searches for users whose nickname matches the typed query (case-insensitive). */
    /**
     * Searches for users whose nickname starts with [query], case-insensitively.
     *
     * Two parallel server queries are merged to handle both new and legacy documents:
     *  1. nicknameLower range — hits users whose nicknameLower field has been populated
     *     (written on every profile save / backfilled at login post-deploy).
     *  2. Capitalised nickname range — hits legacy users whose nickname is Title Case
     *     (the most common format) and whose nicknameLower hasn\'t been written yet.
     *     A client-side toLowerCase check ensures only genuine prefix matches surface.
     *
     * Once all users have logged in at least once after the deploy, query 2 becomes
     * redundant and can be removed.
     */
    suspend fun searchUsersByNickname(query: String): Result<List<UserProfile>> {
        return try {
            val lowerQuery = query.lowercase()
            val capitalisedQuery = lowerQuery.replaceFirstChar { it.uppercaseChar() }

            // Query 1: users with nicknameLower populated (new / backfilled)
            val byLower = db.collection("users")
                .whereGreaterThanOrEqualTo("nicknameLower", lowerQuery)
                .whereLessThanOrEqualTo("nicknameLower", lowerQuery + "\uf8ff")
                .get()
                .await()
                .toObjects(UserProfile::class.java)

            // Query 2: Title Case legacy fallback — client-side filter confirms the match
            val byNickname = db.collection("users")
                .whereGreaterThanOrEqualTo("nickname", capitalisedQuery)
                .whereLessThanOrEqualTo("nickname", capitalisedQuery + "\uf8ff")
                .get()
                .await()
                .toObjects(UserProfile::class.java)
                .filter { it.nickname.lowercase().startsWith(lowerQuery) }

            val users = (byLower + byNickname)
                .distinctBy { it.uid }
                .filter { it.uid != currentUid }

            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Sends a friend request only when no duplicate or existing relationship exists. */
    suspend fun sendFriendRequest(receiver: UserProfile): Result<Unit> {
        return try {
            if (!canSendFriendRequest(receiver.uid)) {
                return Result.success(Unit)
            }

            val currentUser = getCurrentUserProfile()
            val request = buildFriendRequest(
                senderId = currentUid,
                senderNickname = currentUser.nickname,
                receiverId = receiver.uid,
            )

            saveFriendRequest(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Gets incoming pending friend requests for the current user. */
    suspend fun getPendingRequests(): Result<List<FriendRequest>> {
        return try {
            val requests = loadPendingRequestsForReceiver(currentUid)
                .sortedByDescending { it.createdAt?.time ?: 0L }
                .distinctBy { it.senderId }

            Log.d(
                "SocialRepository",
                "getPendingRequests: found ${requests.size} for uid $currentUid"
            )

            Result.success(requests)
        } catch (e: Exception) {
            Log.e("SocialRepository", "getPendingRequests failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Gets outgoing pending friend requests sent by the current user. */
    suspend fun getSentRequests(): Result<List<FriendRequest>> {
        return try {
            val requests = loadPendingRequestsForSender(currentUid)
                .sortedByDescending { it.createdAt?.time ?: 0L }
                .distinctBy { it.receiverId }

            Result.success(requests)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Accepts a friend request and adds both users to each other's friends list. */
    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            markRequestsAcceptedBetweenUsers(request.senderId, currentUid)
            addUsersAsFriends(currentUid, request.senderId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Declines a friend request and marks matching pending requests as declined. */
    suspend fun declineFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            markPendingRequestsBetween(
                senderId = request.senderId,
                receiverId = currentUid,
                status = "declined",
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Gets the current user's friends as full user profiles. */
    suspend fun getFriends(): Result<List<UserProfile>> {
        return try {
            val friendIds = loadFriendIds(currentUid)
            val profiles = loadUserProfiles(friendIds)
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Removes a friend by deleting the friend doc from both users' subcollections. */
    suspend fun removeFriend(friendUid: String): Result<Unit> {
        return try {
            db.collection("users")
                .document(currentUid)
                .collection("friends")
                .document(friendUid)
                .delete()
                .await()

            db.collection("users")
                .document(friendUid)
                .collection("friends")
                .document(currentUid)
                .delete()
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SocialRepository", "removeFriend failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Renames a group conversation. */
    suspend fun renameConversation(conversationId: String, newName: String): Result<Unit> {
        return try {
            db.collection("conversations")
                .document(conversationId)
                .update("groupName", newName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SocialRepository", "renameConversation failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Finds an existing conversation or creates a new one. */
    suspend fun getOrCreateConversation(
        participantIds: List<String>,
        participantNicknames: Map<String, String>,
        isGroup: Boolean = false,
        groupName: String = "",
    ): Result<Conversation> {
        return try {
            Log.d(
                "SocialRepository",
                "getOrCreateConversation called with participantIds: $participantIds"
            )

            if (!isGroup) {
                val existingConversation = findExistingDirectConversation(participantIds)
                if (existingConversation != null) {
                    Log.d(
                        "SocialRepository",
                        "Found existing conversation: ${existingConversation.id}"
                    )
                    return Result.success(existingConversation)
                }
            }

            val conversation = createConversation(
                participantIds = participantIds,
                participantNicknames = participantNicknames,
                isGroup = isGroup,
                groupName = groupName,
            )

            Log.d("SocialRepository", "Conversation created successfully")
            Result.success(conversation)
        } catch (e: Exception) {
            Log.e("SocialRepository", "getOrCreateConversation failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Gets all conversations for the current user. */
    suspend fun getConversations(): Result<List<Conversation>> {
        return try {
            val snapshot = db.collection("conversations")
                .whereArrayContains("participantIds", currentUid)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .get()
                .await()

            Result.success(snapshot.toObjects(Conversation::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Sends a message inside the given conversation. */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: String = "text",
        metadata: Map<String, String> = emptyMap(),
    ): Result<Unit> {
        return try {
            val currentUser = getCurrentUserProfile()
            val message = buildMessage(
                sender = currentUser,
                content = content,
                type = type,
                metadata = metadata,
            )

            saveMessage(conversationId, message)
            updateConversationLastMessage(conversationId, content)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SocialRepository", "sendMessage failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Gets all messages for a conversation ordered by creation time. */
    suspend fun getMessages(conversationId: String): Result<List<Message>> {
        return try {
            val snapshot = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get()
                .await()

            Result.success(snapshot.toObjects(Message::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Gets a user's nickname from Firestore, or falls back to the uid. */
    suspend fun getUserNickname(uid: String): String {
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .get()
                .await()

            snapshot.getString("nickname") ?: uid
        } catch (e: Exception) {
            uid
        }
    }

    /** Loads the current user's assignment-based schedule items. */
    suspend fun getMyScheduleItems(): Result<List<ScheduleItem>> {
        return try {
            val snapshot = db.collection("users")
                .document(currentUid)
                .collection("assignments")
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                mapDocumentToScheduleItem(doc.id, doc.data ?: emptyMap())
            }

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Updates the response value for a study invite message. */
    suspend fun respondToInvite(
        conversationId: String,
        messageId: String,
        response: String,
    ): Result<Unit> {
        return try {
            db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(messageId)
                .update("metadata.response", response)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Loads the current user's saved study sessions. */
    suspend fun getMyStudySessions(): Result<List<StudySession>> {
        return try {
            val snapshot = db.collection("users")
                .document(currentUid)
                .collection("studySessions")
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                mapDocumentToStudySession(doc.id, doc.data ?: emptyMap())
            }

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Loads the current user's saved events. */
    suspend fun getMyEvents(): Result<List<EventItem>> {
        return try {
            val snapshot = db.collection("users")
                .document(currentUid)
                .collection("events")
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                mapDocumentToEventItem(doc.id, doc.data ?: emptyMap())
            }

            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Checks whether a new friend request is allowed to be sent. */
    private suspend fun canSendFriendRequest(receiverUid: String): Boolean {
        if (receiverUid == currentUid) return false
        if (areAlreadyFriends(receiverUid)) return false
        if (hasPendingRequest(currentUid, receiverUid)) return false
        if (hasPendingRequest(receiverUid, currentUid)) return false
        return true
    }

    /** Builds a stable friend request document id from sender and receiver ids. */
    private fun friendRequestDocumentId(senderId: String, receiverId: String): String {
        return "${senderId}_${receiverId}"
    }

    /** Builds a friend request object from the provided values. */
    private fun buildFriendRequest(
        senderId: String,
        senderNickname: String,
        receiverId: String,
    ): FriendRequest {
        val requestId = friendRequestDocumentId(senderId, receiverId)

        return FriendRequest(
            id = requestId,
            senderId = senderId,
            senderNickname = senderNickname,
            receiverId = receiverId,
            status = "pending",
        )
    }

    /** Saves a friend request to Firestore. */
    private suspend fun saveFriendRequest(request: FriendRequest) {
        db.collection("friendRequests")
            .document(request.id)
            .set(request)
            .await()
    }

    /** Loads the current user's full profile from Firestore. */
    private suspend fun getCurrentUserProfile(): UserProfile {
        return db.collection("users")
            .document(currentUid)
            .get()
            .await()
            .toObject(UserProfile::class.java)
            ?: error("Current user not found.")
    }

    /** Checks whether the current user is already friends with the other user. */
    private suspend fun areAlreadyFriends(otherUid: String): Boolean {
        val friendDoc = db.collection("users")
            .document(currentUid)
            .collection("friends")
            .document(otherUid)
            .get()
            .await()

        return friendDoc.exists()
    }

    /** Checks whether a pending request exists between two users. */
    private suspend fun hasPendingRequest(senderId: String, receiverId: String): Boolean {
        val snapshot = db.collection("friendRequests")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("status", "pending")
            .limit(1)
            .get()
            .await()

        return !snapshot.isEmpty
    }

    /** Loads pending requests where the current user is the receiver. */
    private suspend fun loadPendingRequestsForReceiver(receiverId: String): List<FriendRequest> {
        val snapshot = db.collection("friendRequests")
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("status", "pending")
            .get()
            .await()

        return snapshot.toObjects(FriendRequest::class.java)
    }

    /** Loads pending requests where the current user is the sender. */
    private suspend fun loadPendingRequestsForSender(senderId: String): List<FriendRequest> {
        val snapshot = db.collection("friendRequests")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("status", "pending")
            .get()
            .await()

        return snapshot.toObjects(FriendRequest::class.java)
    }

    /** Marks matching pending requests between two users with the given status. */
    private suspend fun markPendingRequestsBetween(
        senderId: String,
        receiverId: String,
        status: String,
    ) {
        val snapshot = db.collection("friendRequests")
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("receiverId", receiverId)
            .whereEqualTo("status", "pending")
            .get()
            .await()

        for (document in snapshot.documents) {
            document.reference.update("status", status).await()
        }
    }

    /** Marks both directions of pending requests as accepted. */
    private suspend fun markRequestsAcceptedBetweenUsers(
        firstUid: String,
        secondUid: String,
    ) {
        markPendingRequestsBetween(firstUid, secondUid, "accepted")
        markPendingRequestsBetween(secondUid, firstUid, "accepted")
    }

    /** Adds two users to each other's friends subcollection. */
    private suspend fun addUsersAsFriends(firstUid: String, secondUid: String) {
        addSingleFriend(firstUid, secondUid)
        addSingleFriend(secondUid, firstUid)
    }

    /** Adds one friend document under a user's friends subcollection. */
    private suspend fun addSingleFriend(ownerUid: String, friendUid: String) {
        db.collection("users")
            .document(ownerUid)
            .collection("friends")
            .document(friendUid)
            .set(mapOf("uid" to friendUid))
            .await()
    }

    /** Loads the friend ids for the given user. */
    private suspend fun loadFriendIds(userUid: String): List<String> {
        val snapshot = db.collection("users")
            .document(userUid)
            .collection("friends")
            .get()
            .await()

        return snapshot.documents.mapNotNull { it.getString("uid") }
    }

    /** Loads user profiles for a list of user ids. */
    private suspend fun loadUserProfiles(uids: List<String>): List<UserProfile> {
        return uids.mapNotNull { uid ->
            db.collection("users")
                .document(uid)
                .get()
                .await()
                .toObject(UserProfile::class.java)
        }
    }

    /** Looks for an existing one-on-one conversation with the exact participants. */
    private suspend fun findExistingDirectConversation(
        participantIds: List<String>,
    ): Conversation? {
        return db.collection("conversations")
            .whereArrayContains("participantIds", currentUid)
            .get()
            .await()
            .toObjects(Conversation::class.java)
            .firstOrNull {
                !it.isGroup &&
                        it.participantIds.size == participantIds.size &&
                        it.participantIds.containsAll(participantIds)
            }
    }

    /** Creates and saves a new conversation document. */
    private suspend fun createConversation(
        participantIds: List<String>,
        participantNicknames: Map<String, String>,
        isGroup: Boolean,
        groupName: String,
    ): Conversation {
        val docRef = db.collection("conversations").document()

        val conversation = Conversation(
            id = docRef.id,
            participantIds = participantIds,
            participantNicknames = participantNicknames,
            isGroup = isGroup,
            groupName = groupName,
        )

        Log.d("SocialRepository", "Creating new conversation: ${docRef.id}")
        docRef.set(conversation).await()

        return conversation
    }

    /** Builds a message object using the current sender's profile data. */
    private fun buildMessage(
        sender: UserProfile,
        content: String,
        type: String,
        metadata: Map<String, String>,
    ): Message {
        val messageId = db.collection("conversations")
            .document()
            .id

        return Message(
            id = messageId,
            senderId = currentUid,
            senderNickname = sender.nickname,
            senderPhotoUrl = sender.photoUrl,
            content = content,
            type = type,
            metadata = metadata,
        )
    }

    /** Saves a message inside a conversation's messages subcollection. */
    private suspend fun saveMessage(conversationId: String, message: Message) {
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(message.id)
            .set(message)
            .await()
    }

    /**
     * Updates the last message preview and lastUpdated timestamp for a conversation.
     * lastUpdated is written as a server timestamp so sort order in the list is correct.
     */
    private suspend fun updateConversationLastMessage(
        conversationId: String,
        content: String,
    ) {
        db.collection("conversations")
            .document(conversationId)
            .set(
                mapOf(
                    "lastMessage" to content,
                    "lastUpdated" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .await()
    }

    /** Converts Firestore data into a ScheduleItem when all required fields exist. */
    private fun mapDocumentToScheduleItem(
        id: String,
        data: Map<String, Any>,
    ): ScheduleItem? {
        val date = data["date"] as? String ?: return null
        val classId = data["classId"] as? String ?: return null
        val className = data["className"] as? String ?: return null
        val assignmentName = data["assignmentName"] as? String ?: return null
        val dueTime = data["dueTime"] as? String ?: return null

        return ScheduleItem(
            id = id,
            date = date,
            classId = classId,
            className = className,
            assignmentName = assignmentName,
            dueTime = dueTime,
        )
    }

    /** Converts Firestore data into a StudySession when all required fields exist. */
    private fun mapDocumentToStudySession(
        id: String,
        data: Map<String, Any>,
    ): StudySession? {
        val date = data["date"] as? String ?: return null
        val className = data["className"] as? String ?: return null
        val topic = data["topic"] as? String ?: return null
        val startTime = data["startTime"] as? String ?: return null
        val location = data["location"] as? String ?: ""

        return StudySession(
            id = id,
            date = date,
            className = className,
            topic = topic,
            startTime = startTime,
            location = location,
        )
    }

    /** Converts Firestore data into an EventItem when all required fields exist. */
    private fun mapDocumentToEventItem(
        id: String,
        data: Map<String, Any>,
    ): EventItem? {
        val date = data["date"] as? String ?: return null
        val name = data["name"] as? String ?: return null
        val time = data["time"] as? String ?: return null
        val location = data["location"] as? String ?: ""

        return EventItem(
            id = id,
            date = date,
            name = name,
            time = time,
            location = location,
        )
    }
}