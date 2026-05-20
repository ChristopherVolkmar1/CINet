package com.example.cinet.feature.map

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.sp
import com.example.cinet.ui.theme.CINetTheme
import kotlin.String
import androidx.compose.foundation.text.BasicTextField

// Rounded search bar with inline dropdown suggestions for campus location search.
// Includes input field, suggestion items, and result deduplication logic.
// -------------------- Data classes --------------------
data class SearchState(
    val textFieldState: TextFieldState,
    val results: List<String>,
    val onSearch: (String) -> Unit
)

// Holds the search and filter state that the persistent top bar needs on the map page.
data class MapTopBarState(
    val searchState: SearchState,
    val categories: Set<String>,
    val activeFilters: Set<String>,
    val onFiltersChanged: (Set<String>) -> Unit
)

// Displays the map search bar and filter button inside the persistent top bar.
@Composable
fun RowScope.MapTopBarControls(
    state: MapTopBarState
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically)
    ) {
        SearchBar(
            placeholderText = "Search Location",
            textFieldState = state.searchState.textFieldState,
            searchResults = state.searchState.results,
            onSearch = state.searchState.onSearch
        )
    }

    Spacer(modifier = Modifier.width(12.dp))

    Box(
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .padding(bottom = 8.dp)
    ) {
        FilterMenu(
            categories = state.categories,
            activeFilters = state.activeFilters,
            onFiltersChanged = state.onFiltersChanged
        )
    }
}

// -------------------- Search bar --------------------

/** Rounded search bar with an inline dropdown of matching location names. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    placeholderText: String,
    textFieldState: TextFieldState,
    searchResults: List<String>,
    onSearch: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val isKeyboardOpen = WindowInsets.ime.getBottom(density) > 0
    LaunchedEffect(isKeyboardOpen) {
        if (!isKeyboardOpen) {
            isFocused = false
            focusManager.clearFocus()
        }
    }
    val uniqueResults = remember(searchResults) { dedupeSearchResults(searchResults) }
    val showDropdown = isFocused && textFieldState.text.isNotEmpty() && uniqueResults.isNotEmpty()

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Column {
            SearchInputField(
                placeholderText = placeholderText,
                textFieldState = textFieldState,
                onFocusChanged = { isFocused = it },
                onSubmit = {
                    onSearch(textFieldState.text.toString())
                    isFocused = false
                    focusManager.clearFocus()
                }
            )

            if (showDropdown) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f))
                uniqueResults.take(5).forEach { result ->
                    SearchSuggestionItem(
                        label = result,
                        onClick = {
                            onSearch(result)
                            isFocused = false
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }
    }
}

/** Trims, blank-filters, and case-insensitively dedupes a list of search result names. */
private fun dedupeSearchResults(results: List<String>): List<String> =
    results
        .map { it.trim() }
        .distinctBy { it.lowercase() }
        .filter { it.isNotBlank() }

/** Compact single-line input used inside the map top bar search pill. */
@Composable
private fun SearchInputField(
    placeholderText: String,
    textFieldState: TextFieldState,
    onFocusChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 12.dp)
            .onFocusChanged { onFocusChanged(it.isFocused) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        BasicTextField(
            value = textFieldState.text.toString(),
            onValueChange = { textFieldState.edit { replace(0, length, it) } },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 18.sp
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .onFocusChanged { onFocusChanged(it.isFocused) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textFieldState.text.isEmpty()) {
                        Text(
                            text = placeholderText,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            maxLines = 1
                        )
                    }

                    innerTextField()
                }
            }
        )
    }
}

/** One tappable search-suggestion row. */
@Composable
private fun SearchSuggestionItem(
    label: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(label, color = MaterialTheme.colorScheme.onSurface) },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    )
}

@Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewSearch() {
    CINetTheme(darkTheme = true) {
        val textFieldState = rememberTextFieldState()
        SearchBar(
            placeholderText = "Search Location",
            textFieldState = textFieldState,
            searchResults = listOf("Aliso Hall", "Bell Tower", "Student Union"),
            onSearch = {},
        )
    }
}