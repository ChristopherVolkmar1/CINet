package com.example.cinet.data.remote

import android.util.Log
import com.example.cinet.data.FirestoreCollections
import com.example.cinet.com.example.cinet.feature.calendar.model.CampusEvent
import com.example.cinet.com.example.cinet.feature.profile.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun createOrLoadUserProfile(): Result<UserProfile> {
        return try {
            val user = auth.currentUser ?: error("No signed-in user.")
            val docRef = db.collection(FirestoreCollections.USERS).document(user.uid)

            val loginUpdate = mapOf(
                "uid"         to user.uid,
                "email"       to (user.email ?: ""),
                "photoUrl"    to (user.photoUrl?.toString() ?: ""),
                "lastLoginAt" to FieldValue.serverTimestamp(),
            )

            docRef.set(loginUpdate, SetOptions.merge()).await()

            val snapshot = docRef.get().await()
            if (snapshot.getTimestamp("createdAt") == null) {
                docRef.update("createdAt", FieldValue.serverTimestamp()).await()
            }

            val profile = snapshot.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Failed to parse UserProfile"))

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveProfileDetails(
        nickname: String,
        major: String,
        pronouns: String,
    ): Result<UserProfile> {
        return try {
            val user = auth.currentUser ?: error("No signed-in user.")
            val docRef = db.collection(FirestoreCollections.USERS).document(user.uid)

            val profileUpdate = mapOf(
                "nickname" to nickname,
                "major"    to major,
                "pronouns" to pronouns,
            )

            docRef.set(profileUpdate, SetOptions.merge()).await()

            val snapshot = docRef.get().await()
            val profile = snapshot.toObject(UserProfile::class.java)
                ?: return Result.failure(Exception("Failed to parse UserProfile"))

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loadCurrentUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: error("No signed-in user.")
        val snapshot = db.collection(FirestoreCollections.USERS)
            .document(uid)
            .get()
            .await()
        return snapshot.toObject(UserProfile::class.java)
    }

    // Updates only the photoUrl field, called after a successful Storage upload
    suspend fun updatePhotoUrl(uid: String, photoUrl: String) {
        db.collection(FirestoreCollections.USERS)
            .document(uid)
            .set(mapOf("photoUrl" to photoUrl), SetOptions.merge())
            .await()
    }

    suspend fun createEvent(
        title: String,
        location: String,
        description: String,
    ): String {
        val docRef = db.collection(FirestoreCollections.EVENTS).document()

        val event = CampusEvent(
            id = docRef.id,
            title = title,
            location = location,
            description = description,
            createdAt = Timestamp.now(),
        )

        docRef.set(event).await()
        return docRef.id
    }

    suspend fun loadEvents(): List<CampusEvent> {
        val snapshot = db.collection(FirestoreCollections.EVENTS)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(CampusEvent::class.java) }
    }

    fun updateFcmToken(userId: String, token: String) {
        Log.d("FCM_DEBUG", "Saving token for userId=$userId")
        db.collection(FirestoreCollections.USERS)
            .document(userId)
            .set(mapOf("fcmToken" to token), SetOptions.merge())
            .addOnSuccessListener {
                Log.d("FCM_DEBUG", "Token saved successfully!")
            }
            .addOnFailureListener { e ->
                Log.e("FCM_DEBUG", "Failed to save token", e)
            }
    }
}