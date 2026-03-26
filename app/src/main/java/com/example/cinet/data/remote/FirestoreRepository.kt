package com.example.cinet.data.remote

import com.example.cinet.data.FirestoreCollections
import com.example.cinet.data.model.CampusEvent
import com.example.cinet.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
    suspend fun saveCurrentUserProfile() {
        val user = auth.currentUser ?: error("No signed-in user.")

        val profile = UserProfile(
            uid = user.uid,
            displayName = user.displayName ?: "Unknown User",
            email = user.email ?: "",
            photoUrl = user.photoUrl?.toString(),
            createdAt = Timestamp.now(),
        )

        db.collection(FirestoreCollections.USERS)
            .document(user.uid)
            .set(profile)
            .await()
    }

    suspend fun loadCurrentUserProfile(): UserProfile? {
        val uid = auth.currentUser?.uid ?: error("No signed-in user.")

        val snapshot = db.collection(FirestoreCollections.USERS)
            .document(uid)
            .get()
            .await()

        return snapshot.toObject(UserProfile::class.java)
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

        return snapshot.documents.mapNotNull { document ->
            document.toObject(CampusEvent::class.java)
        }
    }
}