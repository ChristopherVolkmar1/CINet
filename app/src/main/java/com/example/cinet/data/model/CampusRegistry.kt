package com.example.cinet.data.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cinet.feature.map.CampusLocation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CampusRegistry : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    val campusRegistry = MutableStateFlow<Map<String, List<CampusLocation>>>(emptyMap())
    val academic = campusRegistry.map { it["academic"] ?: emptyList() }
    init {
        fetchCampusData()
    }

    private fun fetchCampusData() {
        viewModelScope.launch {
            try {
                val collections = listOf("academic", "dining", "commuter_parking")
                val results = coroutineScope {
                    collections.map { collectionName ->
                        async(Dispatchers.IO) {
                            val snapshot = db.collection(collectionName).get().await()
                            val list = snapshot.toObjects(CampusLocation::class.java)
                            Log.d("Firestore", "Fetched ${list.size} items from $collectionName")
                            collectionName to list
                        }
                    }.awaitAll()
                }
                campusRegistry.value = results.toMap()
            } catch (e: Exception) {
                Log.e("Firestore", "Error fetching data: ${e.message}")
            }
        }
    }
}