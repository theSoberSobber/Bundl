package com.orvio.app.screens.onboarding

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
import com.orvio.app.R
import com.orvio.app.presentation.theme.Blue
import com.orvio.app.utils.PermissionHandler

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhonePermissionScreen(
    navController: NavController,
    onPermissionGranted: () -> Unit
) {
    val context = LocalContext.current
    val permissionState = rememberMultiplePermissionsState(
        permissions = PermissionHandler.phonePermissions.toList()
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
        // Phone icon
        Image(
            painter = painterResource(id = R.drawable.ic_phone_white),
            contentDescription = "Phone Permission",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Phone Number Access",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We need access to your phone number to make login easier and prevent abuse. Your number helps us verify you're a real person.",
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
            Text("Allow Phone Access")
        }
        
        if (showRationale) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Phone access is required for secure authentication. Please grant the permission to continue.",
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