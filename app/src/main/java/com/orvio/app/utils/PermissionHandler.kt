package com.orvio.app.utils

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
    // Keep the array for future extensibility
    private val requiredPermissions = mutableStateListOf(
        Manifest.permission.SEND_SMS
    )

    fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getMissingPermissions(context: Context): List<String> {
        return requiredPermissions.filter { !hasPermission(context, it) }
    }

    @Composable
    fun RequestPermissions(
        onAllPermissionsGranted: () -> Unit = {},
        onPermissionDenied: (String) -> Unit = {}
    ) {
        val context = LocalContext.current
        var missingPermissions by remember { mutableStateOf(getMissingPermissions(context)) }

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                missingPermissions = getMissingPermissions(context)
                if (missingPermissions.isEmpty()) {
                    onAllPermissionsGranted()
                }
            } else {
                onPermissionDenied(missingPermissions.first())
            }
        }

        LaunchedEffect(missingPermissions) {
            if (missingPermissions.isNotEmpty()) {
                permissionLauncher.launch(missingPermissions.first())
            }
        }
    }
} 