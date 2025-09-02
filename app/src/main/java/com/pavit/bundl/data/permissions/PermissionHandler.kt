package com.pavit.bundl.data.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object PermissionHandler {
    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    
    // Background location permission (for "Allow all the time")
    val backgroundLocationPermissions = arrayOf(
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    val notificationPermissions = arrayOf(
        Manifest.permission.POST_NOTIFICATIONS
    )

    @Composable
    fun RequestPermissions(
        permissions: Array<String>,
        onAllPermissionsGranted: () -> Unit,
        onPermissionDenied: (String) -> Unit
    ) {
        val context = LocalContext.current
        var requestPermissions by remember { mutableStateOf(false) }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            val deniedPermissions = results.filter { !it.value }.keys
            if (deniedPermissions.isEmpty()) {
                onAllPermissionsGranted()
            } else {
                deniedPermissions.firstOrNull()?.let { onPermissionDenied(it) }
            }
        }

        LaunchedEffect(requestPermissions) {
            if (requestPermissions) {
                val missingPermissions = permissions.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missingPermissions.isNotEmpty()) {
                    launcher.launch(missingPermissions.toTypedArray())
                } else {
                    onAllPermissionsGranted()
                }
                requestPermissions = false
            }
        }
    }

    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
} 