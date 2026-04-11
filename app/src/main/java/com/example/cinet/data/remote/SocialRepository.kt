package com.example.cinet.data.remote

import com.example.cinet.data.model.Conversation
import com.example.cinet.data.model.FriendRequest
import com.example.cinet.data.model.Message
import com.example.cinet.data.model.UserProfile
import com.example.cinet.ScheduleItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.example.cinet.StudySession
import com.example.cinet.EventItem

class SocialRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    private val currentUid get() = auth.currentUser?.uid ?: error("No signed-in user.")

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
            val requests = snapshot.toObjects(FriendRequest::class.java)
            android.util.Log.d("SocialRepository", "getPendingRequests: found ${requests.size} for uid $currentUid")
            Result.success(requests)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "getPendingRequests failed: ${e.message}")
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

    suspend fun acceptFriendRequest(request: FriendRequest): Result<Unit> {
        return try {
            db.collection("friendRequests").document(request.id)
                .update("status", "accepted")
                .await()

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

    suspend fun getOrCreateConversation(
        participantIds: List<String>,
        participantNicknames: Map<String, String>,
        isGroup: Boolean = false,
        groupName: String = "",
    ): Result<Conversation> {
        return try {
            android.util.Log.d("SocialRepository", "getOrCreateConversation called with participantIds: $participantIds")

            if (!isGroup) {
                val existing = db.collection("conversations")
                    .whereArrayContains("participantIds", currentUid)
                    .get().await()
                    .toObjects(Conversation::class.java)
                    .firstOrNull { it.participantIds.containsAll(participantIds) }

                if (existing != null) {
                    android.util.Log.d("SocialRepository", "Found existing conversation: ${existing.id}")
                    return Result.success(existing)
                }
            }

            val docRef = db.collection("conversations").document()
            val conversation = Conversation(
                id = docRef.id,
                participantIds = participantIds,
                participantNicknames = participantNicknames,
                isGroup = isGroup,
                groupName = groupName,
            )
            android.util.Log.d("SocialRepository", "Creating new conversation: ${docRef.id}")
            docRef.set(conversation).await()
            android.util.Log.d("SocialRepository", "Conversation created successfully")
            Result.success(conversation)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "getOrCreateConversation failed: ${e.message}")
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

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        type: String = "text",
        metadata: Map<String, String> = emptyMap(),
    ): Result<Unit> {
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
                senderPhotoUrl = currentUser.photoUrl,
                content = content,
                type = type,
                metadata = metadata,
            )
            docRef.set(message).await()

            db.collection("conversations").document(conversationId)
                .set(mapOf("lastMessage" to content), SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("SocialRepository", "sendMessage failed: ${e.message}")
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

    suspend fun getUserNickname(uid: String): String {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.getString("nickname") ?: uid
        } catch (e: Exception) {
            uid
        }
    }

    // Loads the current user's schedule items to pick from for study invites
    suspend fun getMyScheduleItems(): Result<List<ScheduleItem>> {
        return try {
            val snapshot = db.collection("users")
                .document(currentUid)
                .collection("assignments")
                .get()
                .await()
            val items = snapshot.documents.mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                val classId = doc.getString("classId") ?: return@mapNotNull null
                val className = doc.getString("className") ?: return@mapNotNull null
                val assignmentName = doc.getString("assignmentName") ?: return@mapNotNull null
                val dueTime = doc.getString("dueTime") ?: return@mapNotNull null
                ScheduleItem(
                    id = doc.id,
                    date = date,
                    classId = classId,
                    className = className,
                    assignmentName = assignmentName,
                    dueTime = dueTime
                )
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun respondToInvite(conversationId: String, messageId: String, response: String): Result<Unit> {
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

    suspend fun getMyStudySessions(): Result<List<StudySession>> {
        return try {
            val snapshot = db.collection("users")
                .document(currentUid)
                .collection("studySessions")
                .get()
                .await()
            val items = snapshot.documents.mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                val className = doc.getString("className") ?: return@mapNotNull null
                val topic = doc.getString("topic") ?: return@mapNotNull null
                val startTime = doc.getString("startTime") ?: return@mapNotNull null
                val location = doc.getString("location") ?: ""
                StudySession(id = doc.id, date = date, className = className, topic = topic, startTime = startTime, location = location)
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMyEvents(): Result<List<EventItem>> {
        return try {
            val snapshot = db.collection("users")
                .document(currentUid)
                .collection("events")
                .get()
                .await()
            val items = snapshot.documents.mapNotNull { doc ->
                val date = doc.getString("date") ?: return@mapNotNull null
                val name = doc.getString("name") ?: return@mapNotNull null
                val time = doc.getString("time") ?: return@mapNotNull null
                val location = doc.getString("location") ?: ""
                EventItem(id = doc.id, date = date, name = name, time = time, location = location)
            }
            Result.success(items)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}