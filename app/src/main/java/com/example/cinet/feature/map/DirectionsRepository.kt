package com.example.cinet.feature.map

import android.content.Context
import android.content.pm.PackageManager
import com.google.android.gms.maps.model.LatLng
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.DirectionsRoute
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

// Fetches walking, driving, and biking routes from the Google Directions API.
// Parses polyline points, human-readable duration, and arrival ETA.
// -------------------- Data classes --------------------
data class RouteResult(
    val points: List<LatLng>,
    val duration: String,
    val travelMode: TravelMode,
    val eta: String
)

// -------------------- Directions API --------------------

/** Fetches a directions result for the given travel mode; returns a fallback straight-line result on error. */
suspend fun fetchDirections(
    start: LatLng,
    end: LatLng,
    context: Context,
    mode: TravelMode
): RouteResult = withContext(Dispatchers.IO) {
    try {
        val apiKey = readGoogleMapsApiKey(context)
        val geoContext = GeoApiContext.Builder().apiKey(apiKey).build()
        val route = requestDirectionsRoute(geoContext, start, end, mode)
        buildDirectionsResult(route, start, end, mode)
    } catch (e: Exception) {
        e.printStackTrace()
        RouteResult(listOf(start, end), "", TravelMode.WALKING, "")
    }
}

/** Reads the Google Maps API key from the app's AndroidManifest meta-data. */
private fun readGoogleMapsApiKey(context: Context): String? {
    val ai = context.packageManager.getApplicationInfo(
        context.packageName,
        PackageManager.GET_META_DATA
    )
    return ai.metaData.getString("com.google.android.geo.API_KEY")
}

/** Makes a blocking Directions API request for a single origin/destination pair in the given mode. */
private fun requestDirectionsRoute(
    geoContext: GeoApiContext,
    start: LatLng,
    end: LatLng,
    mode: TravelMode
): DirectionsRoute? {
    val result = DirectionsApi.newRequest(geoContext)
        .mode(mode)
        .origin(com.google.maps.model.LatLng(start.latitude, start.longitude))
        .destination(com.google.maps.model.LatLng(end.latitude, end.longitude))
        .await()
    return result.routes.getOrNull(0)
}


/** Converts a raw Directions API route into the app's DirectionsResult (polyline, duration, ETA). */
private fun buildDirectionsResult(
    route: DirectionsRoute?,
    start: LatLng,
    end: LatLng,
    mode: TravelMode
): RouteResult {
    val leg = route?.legs?.getOrNull(0)
    val totalSeconds = leg?.duration?.inSeconds ?: 0L
    val totalMinutes = (totalSeconds / 60).toInt()
    val duration = if (totalMinutes >= 60) {
        val decimalHours = totalMinutes / 60.0
        String.format(Locale.US, "%.1f hrs", decimalHours)
    } else {
        "$totalMinutes mins"
    }
    val points = route?.overviewPolyline?.decodePath()?.map { LatLng(it.lat, it.lng) }
        ?: listOf(start, end)
    val etaMillis = System.currentTimeMillis() + (leg?.duration?.inSeconds ?: 0) * 1000
    val eta = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
        .format(java.util.Date(etaMillis))
    return RouteResult(points, duration, mode, eta)
}


