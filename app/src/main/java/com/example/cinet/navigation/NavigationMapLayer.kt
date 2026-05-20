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

// Keeps the map alive in the composition and hides it when another page is active.
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (currentScreen == Screen.Map) 1f else 0f }
            .then(
                if (currentScreen != Screen.Map) {
                    Modifier.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent()
                            }
                        }
                    }
                } else {
                    Modifier
                }
            )
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
