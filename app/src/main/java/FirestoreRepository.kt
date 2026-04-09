package com.example.cinet

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getEvents(date: String): List<String> {
        return try {
            val snapshot = db.collection("events")
                // Assumes each date is used as the Firestore document ID.
                .document(date)
                .get()
                .await()

            // Expects each document to store an "events" field as List<String>.
            snapshot.get("events") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            // Any Firestore failure is hidden and treated as "no events".
            emptyList()
        }
    }

    suspend fun addEvent(date: String, event: String) {
        val docRef = db.collection("events").document(date)

        val snapshot = docRef.get().await()

        // Reads the existing list first because this implementation replaces
        // the whole array instead of appending with a Firestore atomic update.
        val currentEvents = snapshot.get("events") as? List<String> ?: emptyList()

        val updated = currentEvents + event

        // Overwrites the document with the rebuilt events list.
        docRef.set(mapOf("events" to updated)).await()
    }

    suspend fun getAllEventDates(): Set<String> {
        return try {
            val snapshot = db.collection("events").get().await()

            // Uses document IDs as the source of truth for which dates have events.
            snapshot.documents.map { it.id }.toSet()
        } catch (e: Exception) {
            // Any failure is treated as "no event dates found".
            emptySet()
        }
    }
}