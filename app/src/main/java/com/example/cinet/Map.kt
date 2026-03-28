package com.example.cinet

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.PropertyName
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.tasks.await

enum class LocationCategory {
    ACADEMIC, COMMUTER_PARKING, DINING, HOUSING
}

data class CampusLocation(
    val name: String = "",
    val category: String = "",
    val coordinates: GeoPoint = GeoPoint(0.0, 0.0)
) {
    val latLng: LatLng
        get() = LatLng(coordinates.latitude, coordinates.longitude)
}

@Composable
fun CampusMapScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    val csuciBounds = LatLngBounds(
        LatLng(34.155, -119.055),
        LatLng(34.168, -119.035)
    )

    var hasPermission by remember {
        mutableStateOf(PermissionManager.hasAllPermissions(context))
    }
    val mapProperties by remember(hasPermission) {
        mutableStateOf(
            MapProperties(
                latLngBoundsForCameraTarget = csuciBounds,
                minZoomPreference = 14f,
                maxZoomPreference = 20f,
                isMyLocationEnabled = hasPermission
            )
        )
    }

    var campusRegistry by remember {
        mutableStateOf<Map<String, List<CampusLocation>>>(emptyMap())
    }

    val coroutineScope = rememberCoroutineScope()
    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    fun updateLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                }
            }
        } catch (_: SecurityException) {
        }
    }

    // This is where permissions are verified and the map gets the location data
    LaunchedEffect(Unit) {
        if (!hasPermission) {
            // If missing permissions, request them
            val activity = context as? Activity
            activity?.let {
                PermissionManager.requestAllPermissions(it)
                hasPermission = PermissionManager.hasAllPermissions(context)
            }
        }
        if (hasPermission) {
            // Retrieve the users last location
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let { userLocation = LatLng(it.latitude, it.longitude) }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }

        // Get the location data from Firestore
        try {
            val collections = listOf("academic", "dining", "commuter_parking")
            val finalRegistry = mutableMapOf<String, List<CampusLocation>>()
            collections.forEach { collectionName ->
                val snapshot = db.collection(collectionName).get().await()
                val list = snapshot.toObjects(CampusLocation::class.java)
                finalRegistry[collectionName] = list
                Log.d("Firestore", "Fetched ${list.size} items from $collectionName")
            }
            // Update the state once at the end with all data
            campusRegistry = finalRegistry.toMap()
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching data: ${e.message}")
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(34.162, -119.043), 16f)
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = mapProperties,
            cameraPositionState = cameraPositionState
        ) {
            if (polylinePoints.isNotEmpty()) {
                Polyline(
                    points = polylinePoints,
                    color = Color(0xff0000ff),
                    width = 10f,
                    jointType = JointType.ROUND
                )
            }

            val markersToDraw = campusRegistry.values.flatten()
            markersToDraw.forEach { location ->
                val markerState = remember(location.name) {
                    MarkerState(position = location.latLng)
                }

                Marker(
                    state = markerState,
                    title = location.name,
                    snippet = "Category: ${location.category.lowercase()}",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (location.category) {
                            "ACADEMIC" -> BitmapDescriptorFactory.HUE_RED
                            "COMMUTER_PARKING" -> BitmapDescriptorFactory.HUE_AZURE
                            "DINING" -> BitmapDescriptorFactory.HUE_ORANGE
                            else -> BitmapDescriptorFactory.HUE_VIOLET
                        }
                    ),
                    onInfoWindowClick = {
                        userLocation?.let { start ->
                            coroutineScope.launch {
                                val path = fetchDirections(start, location.latLng, context)
                                polylinePoints = path
                            }
                        }
                    }
                )
            }
        }
    }
}

suspend fun fetchDirections(
    start: LatLng,
    end: LatLng,
    context: android.content.Context
): List<LatLng> {
    return withContext(Dispatchers.IO) {
        try {
            val ai = context.packageManager.getApplicationInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_META_DATA
            )
            val bundle = ai.metaData
            val apiKey = bundle.getString("com.google.android.geo.API_KEY")

            val geoContext = GeoApiContext.Builder()
                .apiKey(apiKey)
                .build()

            val result = DirectionsApi.newRequest(geoContext)
                .mode(TravelMode.WALKING)
                .origin(com.google.maps.model.LatLng(start.latitude, start.longitude))
                .destination(com.google.maps.model.LatLng(end.latitude, end.longitude))
                .await()

            result.routes.getOrNull(0)?.overviewPolyline?.decodePath()?.map {
                LatLng(it.lat, it.lng)
            } ?: listOf(start, end)
        } catch (e: Exception) {
            e.printStackTrace()
            listOf(start, end)
        }
    }
}
