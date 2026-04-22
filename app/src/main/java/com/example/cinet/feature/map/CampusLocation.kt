package com.example.cinet.feature.map

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

// CampusLocation data model and filtering utilities. Supports category and
// search query filtering across the full campus location registry.
// -------------------- Data classes --------------------

data class CampusLocation(
    val name: String = "",
    val category: String = "",
    val coordinates: GeoPoint = GeoPoint(0.0, 0.0),
    val description: String = ""
) {
    val latLng: LatLng
        get() = LatLng(coordinates.latitude, coordinates.longitude)
}

val csuciTransitStop = CampusLocation(
    name = "CSUCI Transit Stop",
    category = "TRANSIT",
    coordinates = GeoPoint(34.16546748540989, -119.04402834425024)
)

// -------------------- Location filtering --------------------

/** Returns every location, alphabetically sorted, that matches the given category filter and search query. */
fun getFilteredLocations(
    fullRegistry: Map<String, List<CampusLocation>>,
    selectedCategory: String?, // null means "all"
    searchQuery: String
): List<CampusLocation> {
    val allLocations = fullRegistry.values.flatten()
    return allLocations
        .filter { matchesFilter(it, selectedCategory, searchQuery) }
        .sortedBy { it.name }
}

/** Returns true when a location matches both the category filter and the search query. */
private fun matchesFilter(
    location: CampusLocation,
    selectedCategory: String?,
    searchQuery: String
): Boolean {
    val matchesCategory = selectedCategory == null || location.category.equals(selectedCategory, ignoreCase = true)
    val matchesSearch = searchQuery.isEmpty() || location.name.contains(searchQuery, ignoreCase = true)
    return matchesCategory && matchesSearch
}
