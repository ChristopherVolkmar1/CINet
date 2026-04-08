package com.example.cinet

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getEvents(date: String): List<String> {
        return try {
            val snapshot = db.collection("events")
                .document(date)
                .get()
                .await()

            snapshot.get("events") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun addEvent(date: String, event: String) {
        val docRef = db.collection("events").document(date)

        val snapshot = docRef.get().await()
        val currentEvents = snapshot.get("events") as? List<String> ?: emptyList()

        val updated = currentEvents + event

        docRef.set(mapOf("events" to updated)).await()
    }

    suspend fun getAllEventDates(): Set<String> {
        return try {
            val snapshot = db.collection("events").get().await()
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }
}