package com.bundl.app.screens.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.bundl.app.R
import com.bundl.app.presentation.theme.Blue
import com.bundl.app.utils.PermissionHandler

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun NotificationPermissionScreen(
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberMultiplePermissionsState(
        permissions = PermissionHandler.notificationPermissions.toList()
    )
    
    var showRationale by remember { mutableStateOf(false) }
    
    LaunchedEffect(permissionState.allPermissionsGranted) {
        if (permissionState.allPermissionsGranted) {
            onPermissionGranted()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Notification icon
        Image(
            painter = painterResource(id = R.drawable.ic_notification),
            contentDescription = "Notification Permission",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Notification Access Required",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Bundl needs to send you notifications to keep you updated about your orders. You'll receive updates when someone joins your order, when the order is confirmed, and when your food is on the way!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (permissionState.shouldShowRationale) {
                    showRationale = true
                } else {
                    permissionState.launchMultiplePermissionRequest()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Allow Notifications")
        }
        
        if (showRationale) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Notification access is required for secure authentication. Please grant the permission to continue.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    openAppSettings(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Settings")
            }
        }
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
} 