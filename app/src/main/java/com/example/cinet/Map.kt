package com.example.cinet

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable //dummy screen for demo nav
fun MapScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Dummy Screen Content", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack) {
            Text("Back to Home")
        }
    }
}
import android.R.attr.end
import android.graphics.Color.blue
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberUpdatedMarkerState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.maps.DirectionsApi
import com.google.maps.GeoApiContext
import com.google.maps.model.TravelMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LocationCategory {
    ACADEMIC, COMMUTER_PARKING, DINING, HOUSING
}

data class CampusLocation(
    val name: String,
    val coordinates: LatLng,
    val category: LocationCategory
)

@OptIn(ExperimentalPermissionsApi::class)
@ExperimentalPermissionsApi
@Composable
@GoogleMapComposable
fun Map(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val csuci = LatLng(34.1621, -119.0435)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(csuci, 10f)
    }
    //Bounds to prevent the user from leaving the campus
    val csuciBounds = LatLngBounds(
        LatLng(34.155, -119.055),
        LatLng(34.168, -119.035)
    )
    val mapProperties by remember {
        mutableStateOf(
            MapProperties(
                latLngBoundsForCameraTarget = csuciBounds,
                //min prevents from zooming out too far
                minZoomPreference = 14f,
                maxZoomPreference = 20f,
                isMyLocationEnabled = true
            )
        )
    }

    val academicLocations = listOf(
        CampusLocation("Aliso Hall", LatLng(34.161033533317216, -119.04554291019717), LocationCategory.ACADEMIC),
        CampusLocation("Arroyo Hall", LatLng(34.160362571139856, -119.0449651371382), LocationCategory.ACADEMIC),
        CampusLocation("Bell Tower", LatLng(34.161033426860186, -119.04327405083124), LocationCategory.ACADEMIC),
        CampusLocation("Bell Tower East", LatLng(34.1613627873466, -119.04193224072041), LocationCategory.ACADEMIC),
        CampusLocation("Bell Tower West", LatLng(34.16077413754563, -119.04413838504449), LocationCategory.ACADEMIC),
        CampusLocation("Broome Library", LatLng(34.16272379037882, -119.04085866409868), LocationCategory.ACADEMIC),
        CampusLocation("Chaparral Hall", LatLng(34.1620975730738, -119.04571940179336), LocationCategory.ACADEMIC),
        CampusLocation("Del Norte Hall", LatLng(34.16314332680665, -119.04409404414939), LocationCategory.ACADEMIC),
        CampusLocation("El Dorado Hall", LatLng(34.16421806224397, -119.04713710663997), LocationCategory.ACADEMIC),
        CampusLocation("Gateway Hall", LatLng(34.164917303238425, -119.04526653254618), LocationCategory.ACADEMIC),
        CampusLocation("Ironwood Hall", LatLng(34.16278192070162, -119.04589286088351), LocationCategory.ACADEMIC),
        CampusLocation("Lindero Hall", LatLng(34.15952716715866, -119.04147320447237), LocationCategory.ACADEMIC),
        CampusLocation("Madera Hall", LatLng(34.16291249920953, -119.04407142302078), LocationCategory.ACADEMIC),
        CampusLocation("Manzanita Hall", LatLng(34.162752145230215, -119.04505967422949), LocationCategory.ACADEMIC),
        CampusLocation("Malibu Hall", LatLng(34.16127971034514, -119.04036363349768), LocationCategory.ACADEMIC),
        CampusLocation("Marin Hall", LatLng(34.16449348416405, -119.04518033627755), LocationCategory.ACADEMIC),
        CampusLocation("Modoc Hall", LatLng(34.164118027818034, -119.0483579800719), LocationCategory.ACADEMIC),
        CampusLocation("Napa Hall", LatLng(34.163790888257665, -119.04540396658717), LocationCategory.ACADEMIC),
        CampusLocation("Ojai Hall", LatLng(34.16165367514373, -119.04266024098733), LocationCategory.ACADEMIC),
        CampusLocation("Placer Hall", LatLng(34.163271303102604, -119.04319384405241), LocationCategory.ACADEMIC),
        CampusLocation("Sage Hall", LatLng(34.164091848406024, -119.04219128128422), LocationCategory.ACADEMIC),
        CampusLocation("Sierra Hall", LatLng(34.16234093150404, -119.04430321122032), LocationCategory.ACADEMIC),
        CampusLocation("Shasta Hall", LatLng(34.16457201602076, -119.04463593717513), LocationCategory.ACADEMIC),
        CampusLocation("Solano Hall", LatLng(34.16334765267999, -119.04519807735882), LocationCategory.ACADEMIC),
        CampusLocation("Topanga Hall", LatLng(34.16009541167767, -119.04166559195076), LocationCategory.ACADEMIC),
        CampusLocation("Yuba Hall", LatLng(34.164022422925036, -119.04109646938987), LocationCategory.ACADEMIC)
    )
    val diningLocations = listOf(
        CampusLocation("American Pie Company", LatLng(34.16321171837447, -119.03931197917322), LocationCategory.DINING),
        CampusLocation("Ekho's Café", LatLng(34.16309792014503, -119.03946822433113), LocationCategory.DINING),
        CampusLocation("Freudian Sip", LatLng(34.162524752647805, -119.04086278682107), LocationCategory.DINING),
        CampusLocation("Islands Cafe", LatLng(34.160430282161435, -119.04160590722023), LocationCategory.DINING),
        CampusLocation("Mom Wong Kitchen", LatLng(34.16281772412409, -119.0392259681462), LocationCategory.DINING),
        CampusLocation("Sea Store Market", LatLng(34.16137041992328, -119.04409100603398), LocationCategory.DINING),
        CampusLocation("Tortillas Grill & Cantina", LatLng(34.16304343294648, -119.03946353999095), LocationCategory.DINING)
    )
    val commuterParking = listOf(
        CampusLocation("A1 Parking", LatLng(34.163600395901376, -119.04259463208324), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A2 Parking", LatLng(34.16420505882201, -119.04162581055532), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A3 Parking", LatLng(34.16662463372068, -119.04684159985), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A4 Parking", LatLng(34.16422642302123, -119.0466532417237), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A5 Parking", LatLng(34.160303180725336, -119.04458319659938), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A6 Parking", LatLng(34.16325112229069, -119.0421188239597), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A7 Parking", LatLng(34.16064329832965, -119.04108600595718), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A8 Parking", LatLng(34.16305514764397, -119.04031656504462), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A10 Parking", LatLng(34.15935028132672, -119.04041059647916), LocationCategory.COMMUTER_PARKING),
        CampusLocation("A11 Parking", LatLng(34.164527100140525, -119.04799474404118), LocationCategory.COMMUTER_PARKING),
    )
    //Map of all campus markers

    val campusRegistry: Map<LocationCategory, List<CampusLocation>> = mapOf(
        LocationCategory.ACADEMIC to academicLocations,
        LocationCategory.DINING to diningLocations,
        LocationCategory.COMMUTER_PARKING to commuterParking
    )
    val coroutineScope = rememberCoroutineScope()
    var polylinePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }

    //Getting user location data and requesting permissions
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    fun updateLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = LatLng(location.latitude, location.longitude)
                }
            }
        } catch (e: SecurityException) {
            // Handle case where user denied permission
        }
    }
    val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(Unit) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        } else {
            updateLocation() // Get the location if we already have permission
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        properties = mapProperties,
        cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(LatLng(34.162, -119.043), 16f)
        }
    ) {
        if (polylinePoints.isNotEmpty()) {
            Polyline(
                points = polylinePoints,
                color = Color(0xff0000ff),
                width = 10f,
                jointType = JointType.ROUND
            )
        }
        //Flatten all markers into a single list to easily draw them onto the map
        val markersToDraw = campusRegistry.values.flatten()
        markersToDraw.forEach { location ->
            // Create a specific state for each marker so we can control it
            val markerState = rememberUpdatedMarkerState(position = location.coordinates)

            Marker(
                state = markerState,
                title = location.name,
                snippet = "Category: ${location.category.name.lowercase()}",
                // Categorizing by color
                icon = BitmapDescriptorFactory.defaultMarker(
                    when (location.category) {
                        LocationCategory.ACADEMIC -> BitmapDescriptorFactory.HUE_RED
                        LocationCategory.COMMUTER_PARKING -> BitmapDescriptorFactory.HUE_AZURE
                        LocationCategory.DINING -> BitmapDescriptorFactory.HUE_ORANGE
                        LocationCategory.HOUSING -> BitmapDescriptorFactory.HUE_VIOLET
                    }
                ),
                onInfoWindowClick = { _ ->
                    //A null check before to make sure we have the users location to create the polyline
                    userLocation?.let { start ->
                        coroutineScope.launch {
                            val path = fetchDirections(start, location.coordinates, context)
                            polylinePoints = path
                        }
                    }
                }
            )
        }
    }
}

//Implements google directions api to create paths around buildings using the users location
suspend fun fetchDirections(
    start: LatLng,
    end: LatLng,
    context: android.content.Context
): List<LatLng> {
    return withContext(Dispatchers.IO) {
        try {
            val ai = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            val apiKey = bundle.getString("com.google.android.geo.API_KEY")

            val geoContext = GeoApiContext.Builder()
                .apiKey(apiKey)
                .build()
            val result = DirectionsApi.newRequest(geoContext)
                .mode(TravelMode.WALKING) // Makes sure routes are set to walkable
                .origin(com.google.maps.model.LatLng(start.latitude, start.longitude)) // The users location
                .destination(com.google.maps.model.LatLng(end.latitude, end.longitude)) // Where the user wants to go
                .await()

            result.routes.getOrNull(0)?.overviewPolyline?.decodePath()?.map {
                LatLng(it.lat, it.lng)
            } ?: listOf(start, end)
        } catch (e: Exception) { // "e" Represents the error object containing details about what happened
            e.printStackTrace()
            listOf(start, end)
        }
    }
}
