package com.example.cinet.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.example.cinet.feature.map.CampusLocation
import com.example.cinet.feature.map.CampusMapScreen
import com.example.cinet.feature.map.MapTopBarState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

// Displays the campus map only when the navigation layer asks for the map screen.
@Composable
internal fun NavigationMapLayer(
    preSelectedLocation: CampusLocation?,
    autoRouteToPreSelectedLocation: Boolean,
    extraLocations: List<CampusLocation>,
    onBack: () -> Unit,
    onFinishedLoading: () -> Unit,
    onRemoveExtraLocation: (CampusLocation) -> Unit,
    onTopBarStateChanged: (MapTopBarState?) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CampusMapScreen(
            onBack = onBack,
            preSelectedLocation = preSelectedLocation,
            autoRouteToPreSelectedLocation = autoRouteToPreSelectedLocation,
            onFinishedLoading = onFinishedLoading,
            extraLocations = extraLocations,
            onRemoveExtraLocation = onRemoveExtraLocation,
            onTopBarStateChanged = onTopBarStateChanged
        )
    }
}
