package com.example.cinet.feature.profile

import android.content.Context
import com.example.cinet.R
import com.example.cinet.data.model.Program
import kotlinx.serialization.json.Json
import java.io.IOException

fun loadProgramsFromRaw(context: Context): List<Program> {
    val jsonString: String
    try {
        jsonString = context.resources.openRawResource(R.raw.programs)
            .bufferedReader()
            .use { it.readText() }
    } catch (ioException: IOException) {
        ioException.printStackTrace()
        return emptyList()
    }
    return Json.decodeFromString<List<Program>>(jsonString)
}