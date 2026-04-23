package com.example.cinet.core.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

object PermissionManager {
    val APP_PERMISSIONS = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Tiramisu is the dessert code for android 13
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }.toTypedArray()

    private const val PERMISSION_REQUEST_CODE = 100

    fun hasAllPermissions(context: Context): Boolean {
        return APP_PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestAllPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            APP_PERMISSIONS,
            PERMISSION_REQUEST_CODE
        )
    }
}