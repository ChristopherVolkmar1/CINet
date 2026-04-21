package com.example.cinet.feature.map

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

// Rounded search bar with inline dropdown suggestions for campus location search.
// Includes input field, suggestion items, and result deduplication logic.
// -------------------- Data classes --------------------
data class SearchState(
    val textFieldState: TextFieldState,
    val results: List<String>,
    val onSearch: (String) -> Unit
)

// -------------------- Search bar --------------------

/** Rounded search bar with an inline dropdown of matching location names. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchLocationBar(
    textFieldState: TextFieldState,
    searchResults: List<String>,
    onSearch: (String) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    val uniqueResults = remember(searchResults) { dedupeSearchResults(searchResults) }
    val showDropdown = isFocused && textFieldState.text.isNotEmpty() && uniqueResults.isNotEmpty()

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(bottom = 8.dp)
    ) {
        Column {
            SearchInputField(
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

/** Transparent single-line TextField used as the search input. */
@Composable
private fun SearchInputField(
    textFieldState: TextFieldState,
    onFocusChanged: (Boolean) -> Unit,
    onSubmit: () -> Unit
) {
    TextField(
        value = textFieldState.text.toString(),
        onValueChange = { textFieldState.edit { replace(0, length, it) } },
        placeholder = { Text("Search location..", color = Color.Gray) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.primary,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChanged(it.isFocused) },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() })
    )
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