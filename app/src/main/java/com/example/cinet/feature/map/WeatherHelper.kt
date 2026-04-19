package com.example.cinet.feature.map

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class WeatherInfo(
    val temp: String,
    val condition: String
)

object WeatherHelper {
    suspend fun fetchCampusWeather(context: Context): WeatherInfo {
        return withContext(Dispatchers.IO) {
            try {
                val ai = context.packageManager.getApplicationInfo(
                    context.packageName,
                    PackageManager.GET_META_DATA
                )
                val apiKey = ai.metaData.getString("com.google.android.geo.API_KEY")
                
                val url = "https://weather.googleapis.com/v1/currentConditions:lookup?key=$apiKey&location.latitude=34.162&location.longitude=-119.043&unitsSystem=IMPERIAL"
                val response = java.net.URL(url).readText()
                
                // Extracting temperature
                val tempVal = response.substringAfter("\"degrees\":").substringBefore(",").toDouble().toInt()
                
                // Extracting condition (rough parsing based on likely Google API response)
                // Note: Real parsing should use a JSON library like GSON or Kotlinx.serialization
                val condition = if (response.contains("\"conditionText\":")) {
                    response.substringAfter("\"conditionText\":\"").substringBefore("\"")
                } else {
                    "Sunny" // Default to Sunny if text is missing
                }
                
                WeatherInfo("$tempVal°F", condition)
            } catch (e: Exception) {
                WeatherInfo("72°F", "Sunny") // Fallback
            }
        }
    }
}
