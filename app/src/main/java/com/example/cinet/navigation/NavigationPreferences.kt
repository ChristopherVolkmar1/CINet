package com.example.cinet.navigation

import android.content.SharedPreferences

// Loads a saved list of title/detail pairs from SharedPreferences.
internal fun SharedPreferences.loadPairItems(key: String): List<Pair<String, String>> {
    val saved = getString(key, null) ?: return emptyList()

    return saved
        .split("||")
        .filter { it.contains("|") }
        .map {
            val parts = it.split("|")
            parts[0] to parts[1]
        }
}

// Saves a list of title/detail pairs into SharedPreferences.
internal fun SharedPreferences.savePairItems(key: String, items: List<Pair<String, String>>) {
    val stringified = items.joinToString("||") { "${it.first}|${it.second}" }
    edit().putString(key, stringified).apply()
}
