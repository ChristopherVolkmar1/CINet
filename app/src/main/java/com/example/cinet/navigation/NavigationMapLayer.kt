package com.example.cinet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.CampusMapScreen
import com.example.cinet.feature.map.MapTopBarState

// Keeps the map alive, but moves it behind other pages when Map is not active.
@Composable
internal fun NavigationMapLayer(
    currentScreen: Screen,
    preSelectedLocation: CampusLocation?,
    autoRouteToPreSelectedLocation: Boolean,
    extraLocations: List<CampusLocation>,
    onBack: () -> Unit,
    onFinishedLoading: () -> Unit,
    onRemoveExtraLocation: (CampusLocation) -> Unit,
    onTopBarStateChanged: (MapTopBarState?) -> Unit,
) {
    val isMapVisible = currentScreen == Screen.Map

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(if (isMapVisible) 1f else 0f)
            .graphicsLayer {
                alpha = if (isMapVisible) 1f else 0f
            }
    ) {
        CampusMapScreen(
            onBack = onBack,
            preSelectedLocation = preSelectedLocation,
            autoRouteToPreSelectedLocation = autoRouteToPreSelectedLocation,
            onFinishedLoading = onFinishedLoading,
            extraLocations = extraLocations,
            onRemoveExtraLocation = onRemoveExtraLocation,
            onTopBarStateChanged = onTopBarStateChanged,
        )
    }
}