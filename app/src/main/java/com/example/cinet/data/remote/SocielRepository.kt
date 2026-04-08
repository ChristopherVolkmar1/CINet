package com.example.cinet.data.remote

import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.FriendRequest
import com.example.cinet.data.model.Message
import com.example.cinet.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class SocialRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val currentUid get() = auth.currentUser?.uid ?: error("No signed-in user.")

    // --- Friend search ---

    suspend fun searchUsersByNickname(query: String): Result<List<UserProfile>> {
        return try {
            val snapshot = db.collection("users")
                .whereGreaterThanOrEqualTo("nickname", query)
                .whereLessThanOrEqualTo("nickname", query + "\uf8ff")
                .get()
                .await()
            val users = snapshot.toObjects(UserProfile::class.java)
                .filter { it.uid != currentUid }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Friend requests ---

    suspend fun sendFriendRequest(receiver: UserProfile): Result<Unit> {
        return try {
            val currentUser = db.collection("users").document(currentUid).get().await()
                .toObject(UserProfile::class.java) ?: error("Current user not found.")

            val docRef = db.collection("friendRequests").document()
            val request = FriendRequest(
                id = docRef.id,
                senderId = currentUid,
                senderNickname = currentUser.nickname,
                receiverId = receiver.uid,
                status = "pending",
            )
            docRef.set(request).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPendingRequests(): Result<List<FriendRequest>> {
        return try {
            val snapshot = db.collection("friendRequests")
                .whereEqualTo("receiverId", currentUid)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            Result.success(snapshot.toObjects(FriendRequest::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            db.collection("friendRequests").document(request.id)
                .update("status", "accepted")
                .await()

            // Add each user to the other's friends subcollection
            db.collection("users").document(currentUid)
                .collection("friends").document(request.senderId)
                .set(mapOf("uid" to request.senderId)).await()

            db.collection("users").document(request.senderId)
                .collection("friends").document(currentUid)
                .set(mapOf("uid" to currentUid)).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            db.collection("friendRequests").document(request.id)
                .update("status", "declined")
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFriends(): Result<List<UserProfile>> {
        return try {
            val friendDocs = db.collection("users").document(currentUid)
                .collection("friends").get().await()

            val profiles = friendDocs.documents.mapNotNull { doc ->
                val uid = doc.getString("uid") ?: return@mapNotNull null
                db.collection("users").document(uid).get().await()
                    .toObject(UserProfile::class.java)
            }
            Result.success(profiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Conversations ---

    suspend fun getOrCreateConversation(
        participantIds: List<String>,
        participantNicknames: Map<String, String>,
        isGroup: Boolean = false,
        groupName: String = "",
    ): Result<Conversation> {
        return try {
            // For 1-on-1, check if conversation already exists
            if (!isGroup) {
                val existing = db.collection("conversations")
                    .whereArrayContains("participantIds", currentUid)
                    .get().await()
                    .toObjects(Conversation::class.java)
                    .firstOrNull { it.participantIds.containsAll(participantIds) }

                if (existing != null) return Result.success(existing)
            }

            val docRef = db.collection("conversations").document()
            val conversation = Conversation(
                id = docRef.id,
                participantIds = participantIds,
                participantNicknames = participantNicknames,
                isGroup = isGroup,
                groupName = groupName,
            )
            docRef.set(conversation).await()
            Result.success(conversation)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConversations(): Result<List<Conversation>> {
        return try {
            val snapshot = db.collection("conversations")
                .whereArrayContains("participantIds", currentUid)
                .orderBy("lastUpdated", Query.Direction.DESCENDING)
                .get().await()
            Result.success(snapshot.toObjects(Conversation::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(conversationId: String, content: String, type: String = "text"): Result<Unit> {
        return try {
            val currentUser = db.collection("users").document(currentUid).get().await()
                .toObject(UserProfile::class.java) ?: error("Current user not found.")

            val docRef = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document()

            val message = Message(
                id = docRef.id,
                senderId = currentUid,
                senderNickname = currentUser.nickname,
                content = content,
                type = type,
            )
            docRef.set(message).await()

            // Update lastMessage on the conversation
            db.collection("conversations").document(conversationId)
                .set(mapOf("lastMessage" to content), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(conversationId: String): Result<List<Message>> {
        return try {
            val snapshot = db.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .get().await()
            Result.success(snapshot.toObjects(Message::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun getSentRequests(): Result<List<FriendRequest>> {
        return try {
            val snapshot = db.collection("friendRequests")
                .whereEqualTo("senderId", currentUid)
                .whereEqualTo("status", "pending")
                .get()
                .await()
            Result.success(snapshot.toObjects(FriendRequest::class.java))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}