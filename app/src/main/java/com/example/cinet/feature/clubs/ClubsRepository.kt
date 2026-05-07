package com.example.cinet.feature.clubs

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Repository to fetch Clubs and Organizations data from CI Sync (CampusLabs).
 */
class ClubsRepository {
    private val client = OkHttpClient()
    private val TAG = "ClubsRepository"
    
    // The public Discovery API for CSUCI CampusLabs (CI Sync)
    private val apiUrl = "https://csuci.campuslabs.com/engage/api/discovery/search/organizations?top=100&skip=0"

    suspend fun fetchClubs(): List<ClubItem> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            
            val jsonData = response.body?.string() ?: return@withContext emptyList()
            parseJson(jsonData)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching clubs", e)
            emptyList()
        }
    }

    private fun parseJson(jsonString: String): List<ClubItem> {
        val clubs = mutableListOf<ClubItem>()
        try {
            val root = JSONObject(jsonString)
            val values = root.getJSONArray("value")
            
            for (i in 0 until values.length()) {
                val item = values.getJSONObject(i)
                val title = item.optString("Name", "")
                val description = item.optString("Summary", "")
                val websiteKey = item.optString("WebsiteKey", "")
                
                if (title.isNotBlank() && websiteKey.isNotBlank()) {
                    // Construct full URL using the WebsiteKey from CI Sync
                    val url = "https://csuci.campuslabs.com/engage/organization/$websiteKey"
                    clubs.add(ClubItem(title, description, url))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing JSON", e)
        }
        return clubs
    }
}
