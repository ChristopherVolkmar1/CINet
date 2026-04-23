package com.example.cinet.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class SettingsRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private val settingsCollection = db.collection("app_settings")

    suspend fun saveSetting(key: String, value: Any) {
        val uid = auth.currentUser?.uid ?: return
        settingsCollection.document(uid).set(
            mapOf(key to value),
            SetOptions.merge()
        ).await()
    }

    suspend fun loadSettings(): Map<String, Any>? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = settingsCollection.document(uid).get().await()
            snapshot.data
        } catch (e: Exception) {
            null
        }
    }
}
